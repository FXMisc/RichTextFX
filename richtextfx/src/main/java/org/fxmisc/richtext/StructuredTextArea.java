package org.fxmisc.richtext;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import javafx.beans.DefaultProperty;
import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Geoff on 3/29/2016.
 */
@DefaultProperty("highlights")
public class StructuredTextArea extends CodeArea {

    private final Class<? extends Parser>     parserClass;
    private final Class<? extends Lexer>      lexerClass;
    private final Function<Parser, ParseTree> rootProduction;

    private boolean wasHighlightingBrackets = false;

    // TODO if i make this type generic on the parser I get a little extra type safety on root production
    // similarly a kotlin data class would do it.

    private final ObservableList<ContextualHighlight> highlights = FXCollections.observableArrayList();

    public StructuredTextArea(@NamedArg("parserClass") String parserClass,
                              @NamedArg("lexerClass") String lexerClass,
                              @NamedArg("rootProduction") String rootProduction) {
        super();

        this.parserClass = loadClass("parserClass", parserClass, Parser.class);
        this.lexerClass = loadClass("lexerClass", lexerClass, Lexer.class);

        try {
            Method rootProductionMethod = this.parserClass.getMethod(rootProduction);
            this.rootProduction = parser -> {
                try {
                    return ParseTree.class.cast(rootProductionMethod.invoke(parser));
                }
                catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            };
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        richChanges().subscribe(change -> reApplyStyles());
        caretPositionProperty().addListener((source, oldIdx, newIdx) -> {
            //TODO filter this to a toggle...
            //or something more immutable? If i can embed the cursor position in their document,
            // then I dont need to keep a stateful toggle.
            reApplyStyles();
        });

        highlights.addListener((ListChangeListener<ContextualHighlight>) c -> {
            while(c.next()){
                c.getAddedSubList().forEach(highlight -> highlight.setAndValidateParentContext(this.parserClass));
            }
        });
    }

    private void reApplyStyles() {

        ANTLRInputStream antlrStringStream = new ANTLRInputStream(getText());

        Lexer lexer = null;
        try {
            lexer = lexerClass.getConstructor(CharStream.class).newInstance(antlrStringStream);
        }
        catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        lexer.removeErrorListeners();
        TokenStream tokens = new CommonTokenStream(lexer);

        Parser parser = null;
        try {
            parser = parserClass.getConstructor(TokenStream.class).newInstance(tokens);
        }
        catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        parser.getErrorListeners().removeIf(ConsoleErrorListener.class::isInstance);

        ParseTree expr = rootProduction.apply(parser);

        List<HighlightedTextInteveral> foundHighlights = new ArrayList<>();

        ParseTreeWalker walker = new ParseTreeWalker();
        ParseTreeListener walkListener = new ParseTreeListener() {
            @Override public void visitTerminal(TerminalNode terminalNode) {}
            @Override public void visitErrorNode(ErrorNode errorNode) {}
            @Override public void enterEveryRule(ParserRuleContext parserRuleContext) {}
            @Override public void exitEveryRule(ParserRuleContext ctx) {

                Optional<HighlightedTextInteveral> targetHighlight = getHighlights().stream()
                        .map(highlight -> highlight.getMatchingText(ctx))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst();

                targetHighlight.ifPresent(foundHighlights::add);
            }
        };
        walker.walk(walkListener, expr);

        foundHighlights.addAll(highlightBrackets(tokens));
        foundHighlights.sort((l, r) -> l.getLowerBound() - r.getLowerBound());

        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int nextHighlightStart = 0;
        //TODO overlapping highlights?

        for(HighlightedTextInteveral interval : foundHighlights){
            spansBuilder.add(Collections.emptyList(), interval.getLowerBound() - nextHighlightStart);
            spansBuilder.add(Collections.singleton(interval.getStyleClass()), (interval.getUpperBound() + 1) - interval.getLowerBound());
            nextHighlightStart = interval.getUpperBound() + 1;
        }

        assert false : "math isn't quite right here, but then my styles aren't being applied anyways...";
        //also, make the HighlightedTextInterval toString nicely.
        if(nextHighlightStart < getLength()) {
            spansBuilder.add(Collections.emptyList(), getLength() - nextHighlightStart);
        }

        setStyleSpans(0, spansBuilder.create());
    }

    private Collection<HighlightedTextInteveral> highlightBrackets(TokenStream tokens) {

        int currentIndex = getCaretPosition();

        //TODO surely theres a better way to do this.
        Token currentToken = null;
        int tokenIdx = 0;

        for(int characterIndex = 0; characterIndex < currentIndex && tokens.get(tokenIdx).getType() != Token.EOF;){
            currentToken = tokens.get(tokenIdx);
            characterIndex += (currentToken.getStopIndex() - currentToken.getStartIndex() + 1);
            tokenIdx += 1;
        }

        if(currentToken == null || currentToken.getType() == Token.EOF){
            return Collections.emptyList();
        }

        String currentText = currentToken.getText();
        if(currentText == null || (! currentText.equals("(") && ! currentText.equals(")"))){
            return Collections.emptyList();
        }

        Token openingBracketToken = currentToken;

        int openCount = 1;
        do{
            currentToken = tokens.get(tokenIdx);
            if(currentToken.getText().equals(")")) { openCount += 1; }
            if(currentToken.getText().equals(")")) { openCount -= 1; }
            tokenIdx += 1;
        }
        while(openCount != 0 && currentToken.getType() != Token.EOF);

        if(currentToken.getType() == Token.EOF){
            return Collections.singletonList(
                    new HighlightedTextInteveral(openingBracketToken.getStartIndex(), Math.max(openingBracketToken.getStopIndex(), getLength() - 1), "bracket")
            );
        }

        Token closingBracketToken = currentToken;

        return Arrays.asList(openingBracketToken, closingBracketToken).stream()
                .map(token -> new HighlightedTextInteveral(token.getStartIndex(), Math.max(token.getStopIndex(), getLength() - 1), "bracket"))
                .collect(Collectors.toList());
    }

    public final ObservableList<ContextualHighlight> getHighlights(){
        return highlights;
    }

    public static <TClass> Class<? extends TClass> loadClass(String varName, String className, Class<TClass> neededSuperClass){
        try{
            Class candidate = Class.forName(className);
            if ( ! neededSuperClass.isAssignableFrom(candidate)){
                throw new IllegalArgumentException(varName + "; " + className + " must be subclass of " + neededSuperClass.getCanonicalName());
            }
            return (Class) candidate;
        }
        catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(className + " not found for " + varName, e);
        }
    }
}
