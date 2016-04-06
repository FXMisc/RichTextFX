package org.fxmisc.richtext;

import com.google.common.collect.BoundType;
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
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Created by Geoff on 3/29/2016.
 */
@DefaultProperty("highlights")
public class StructuredTextArea extends CodeArea {

    private final Class<? extends Parser>     parserClass;
    private final Class<? extends Lexer>      lexerClass;
    private final Function<Parser, ParseTree> rootProduction;

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
            this.rootProduction = parser -> runOrThrowUnchecked(() -> ParseTree.class.cast(rootProductionMethod.invoke(parser)));
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        richChanges().subscribe(change -> reApplyStyles());
        caretPositionProperty().addListener((source, oldIdx, newIdx) -> {
            //TODO filter this to a toggle...
            // or something more immutable? If i can embed the cursor position in their document,
            // then I dont need to keep a stateful toggle.
            reApplyStyles();
        });

        highlights.addListener((ListChangeListener<ContextualHighlight>) c -> {
            while(c.next()){
                c.getAddedSubList().forEach(highlight -> highlight.setAndValidateParentContext(this.parserClass));
            }
        });
    }

    //TODO make this a task? some kind of queue to ensure we dont flood?
    private void reApplyStyles() {

        ANTLRInputStream antlrStringStream = new ANTLRInputStream(getText());
        Lexer lexer = runOrThrowUnchecked(() -> lexerClass.getConstructor(CharStream.class).newInstance(antlrStringStream));
        lexer.removeErrorListeners();
        TokenStream tokens = new CommonTokenStream(lexer);
        Parser parser = runOrThrowUnchecked(() -> parserClass.getConstructor(TokenStream.class).newInstance(tokens));
        parser.getErrorListeners().removeIf(ConsoleErrorListener.class::isInstance);

        ParseTree expr = rootProduction.apply(parser);

        RangeMap<Integer, String> styleByIndex = TreeRangeMap.create();

        //listeners.ofType(LexerListener.class).forEac(l -> l.handleTokens(tokenMap));
        styleByIndex.putAll(highlightBrackets(tokens));

        ParseTreeWalker walker = new ParseTreeWalker();
        ParseTreeListener walkListener = new ParseTreeListener() {
            @Override public void visitTerminal(TerminalNode terminalNode) {}
            @Override public void visitErrorNode(ErrorNode errorNode) {
                //note order is important here
                Token symbol = errorNode.getSymbol();

                //here, something like
                // listeners.ofType(ErrorListener.class).forEach(l -> l.handleError(errorNode));

                int startIndex = -1, endIndex = -1;

                if(symbol.getStartIndex() != -1){
                    startIndex = symbol.getStartIndex();
                    endIndex = symbol.getStopIndex();
                }
                else{
                    ParserRuleContext parent = (ParserRuleContext) errorNode.getParent();
                    int thisIndex = parent.children.indexOf(errorNode);
                    ParseTree olderSib = parent.getChild(thisIndex - 1);
                    ParseTree youngerSib = parent.getChild(thisIndex + 1);

                    Token preceedingToken = olderSib instanceof ParserRuleContext ? ((ParserRuleContext) olderSib).getStop() :
                                            olderSib instanceof TerminalNode ? ((TerminalNode) olderSib).getSymbol() :
                                            null;
                    Token succeedingToken = youngerSib instanceof ParserRuleContext ? ((ParserRuleContext) youngerSib).getStart() :
                                            youngerSib instanceof TerminalNode ? ((TerminalNode) youngerSib).getSymbol() :
                                            null;

                    if(preceedingToken != null) { startIndex = preceedingToken.getStopIndex(); }
                    if(succeedingToken != null) { endIndex = succeedingToken.getStartIndex(); }

                    startIndex = startIndex == -1 ? endIndex : startIndex;
                    endIndex = endIndex == -1 ? startIndex : endIndex;
                }

                Range<Integer> range = Range.closed(startIndex, endIndex);
                styleByIndex.put(range, "error");
            }
            @Override public void enterEveryRule(ParserRuleContext ctx) {
                Optional<HighlightedTextInteveral> targetHighlight = getHighlights().stream()
                        .map(highlight -> highlight.getMatchingText(ctx))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst();

                targetHighlight.ifPresent(highlight -> {
                    Range<Integer> range = Range.closed(highlight.getLowerBound(), highlight.getUpperBound());
                    styleByIndex.put(range, highlight.getStyleClass());
                });
            }
            @Override public void exitEveryRule(ParserRuleContext ctx) {}
        };
        walker.walk(walkListener, expr);

        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int nextHighlightStart = 0;
        //TODO overlapping highlights?

        snapRanges(styleByIndex);

        for(Map.Entry<Range<Integer>, String> styledInterval : styleByIndex.asMapOfRanges().entrySet()){
            int lowerBound = styledInterval.getKey().lowerEndpoint();
            int upperBound = styledInterval.getKey().upperEndpoint();
            String style = styledInterval.getValue();

            spansBuilder.add(Collections.emptyList(), lowerBound - nextHighlightStart);
            spansBuilder.add(Collections.singleton(style), upperBound + 1 - lowerBound);
            nextHighlightStart = upperBound + 1;
        }

        //also, make the HighlightedTextInterval toString nicely.
        if(nextHighlightStart < getLength()) {
            spansBuilder.add(Collections.emptyList(), getLength() - nextHighlightStart);
        }

        setStyleSpans(0, spansBuilder.create());
    }

    private void snapRanges(RangeMap<Integer, String> styleByIndex) {

        Map<Range<Integer>, String> rangeMap = styleByIndex.asMapOfRanges();

        for(Range<Integer> range : new HashSet<>(rangeMap.keySet())){
            if(range.lowerBoundType() == BoundType.OPEN){
                String value = rangeMap.remove(range);
                range = Range.closed(range.lowerEndpoint() + 1, range.upperEndpoint());
                if(range.isEmpty()){ continue; }
                styleByIndex.put(range, value);
            }
            if(range.upperBoundType() == BoundType.OPEN){
                //TODO copy-pasta much?
                String value = rangeMap.remove(range);
                range = Range.closed(range.lowerEndpoint(), range.upperEndpoint() - 1);
                if(range.isEmpty()){ continue; }
                styleByIndex.put(range, value);
            }
        }
    }

    private RangeMap<Integer, String> highlightBrackets(TokenStream tokens) {

        int currentCharIndex = getCaretPosition();

        Optional<Integer> tokenIndexCandidate = getTokenIndexFor(tokens, currentCharIndex);

        TreeRangeMap<Integer, String> bracketsSoFar = TreeRangeMap.create();
        if ( ! tokenIndexCandidate.isPresent()){ return bracketsSoFar; }
        int tokenIndex = tokenIndexCandidate.get();

        if( ! isBracket(tokens, tokenIndex) && tokens.get(tokenIndex).getStartIndex() == currentCharIndex){
            tokenIndex -= 1;
        }

        if ( ! isBracket(tokens, tokenIndex)){ return bracketsSoFar; }

        Token openingBracketToken = tokens.get(tokenIndex);

        Range<Integer> openingRange = Range.closed(
                openingBracketToken.getStartIndex(),
                openingBracketToken.getStopIndex()
        );
        bracketsSoFar.put(openingRange, "bracket");

        Optional<Integer> counterpartsCandidateIndex = findIndexOfCounterpart(tokens, tokenIndex);
        if ( ! counterpartsCandidateIndex.isPresent()) { return bracketsSoFar; }
        int counterpartsIndex = counterpartsCandidateIndex.get();

        Token closingBracketToken = tokens.get(counterpartsIndex);

        Range<Integer> closingRange = Range.closed(
                closingBracketToken.getStartIndex(),
                closingBracketToken.getStopIndex()
        );
        bracketsSoFar.put(closingRange, "bracket");

        return bracketsSoFar;
    }

    private Optional<Integer> findIndexOfCounterpart(TokenStream tokens, int tokenIndex) {

        Token currentToken = tokens.get(tokenIndex);
        String openingBracketText = currentToken.getText();

        UnaryOperator<Integer> moveNext = current -> openingBracketText.equals("(") ? current + 1 : current - 1;

        int openCount = 0;
        do{
            currentToken = tokens.get(tokenIndex);
            if(currentToken.getText().equals("(")) { openCount += 1; }
            if(currentToken.getText().equals(")")) { openCount -= 1; }

            if(openCount == 0) { break; }

            tokenIndex = moveNext.apply(tokenIndex);
        }
        while(currentToken.getType() != Token.EOF);

        if(currentToken.getType() == Token.EOF){ return Optional.empty(); }

        return Optional.of(tokenIndex);
    }

    private boolean isBracket(TokenStream tokens, int tokenIndex) {
        String text = tokens.get(tokenIndex).getText();
        return text.equals("(") || text.equals(")");
    }

    private Optional<Integer> getTokenIndexFor(TokenStream tokens, int targetCharIndex) {

        //TODO surely theres a better way to do this.
        //build a map of indexes -> tokens and ask that?
        //cache invalidation logic isnt very functional :\
        //well i suppose it is if i use an immutable map.

        int tokenIdx = 0;

        for (int characterIndex = 0; characterIndex < targetCharIndex && tokens.get(tokenIdx).getType() != Token.EOF;) {
            Token currentToken = tokens.get(tokenIdx);
            assert currentToken.getStopIndex() - currentToken.getStartIndex() >= 0 : "zero-width token: " + currentToken;
            characterIndex = currentToken.getStartIndex();
            tokenIdx += 1;
        }

        tokenIdx -= 1;

        if( ! isOnToken(tokens, tokenIdx, targetCharIndex)){
            return Optional.empty();
        }

        return Optional.of(tokenIdx);
    }

    private boolean isOnToken(TokenStream tokens, int tokenIdx, int characterIndex) {
        if(tokenIdx < 0 || tokenIdx >= tokens.size()){ return false; }
        Token token = tokens.get(tokenIdx);
        return token.getStartIndex() <= characterIndex
                //remember, stopIndex is inclusive.
                && token.getStopIndex() >= characterIndex;
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

    @FunctionalInterface interface FailingSupplier<T> { T get() throws Exception; }
    private static <TResult> TResult runOrThrowUnchecked(FailingSupplier<TResult> supplier){
        try{ return supplier.get(); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
