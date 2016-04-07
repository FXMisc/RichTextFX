package org.fxmisc.richtext.antlr;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.sun.javafx.scene.DirtyBits;
import javafx.beans.NamedArg;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.StyleSpansBuilder;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

/**
 * Created by Geoff on 3/29/2016.
 */
public class StructuredTextArea extends CodeArea {

    private final Class<? extends Parser>     parserClass;
    private final Class<? extends Lexer>      lexerClass;
    private final Function<Parser, ParseTree> rootProduction;


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

        lexerListeners.add(new LexicalBracketHighlight());
        errorListeners.add(new ErrorUnderlineHighlight());

        richChanges().subscribe(change -> reApplyStyles());
        caretPositionProperty().addListener((source, oldIdx, newIdx) -> {
            //TODO filter this to a toggle...
            // or something more immutable? If i can embed the cursor position in their document,
            // then I dont need to keep a stateful toggle.
            reApplyStyles();
        });
    }

    public Class<? extends Parser> getParserClass(){
        return parserClass;
    }


    //region (FXML accessible) properties

    // note prescedence,
    // lexical is first to run,
    // then semantics,
    // then errors
    // and styles are LIFO (most recently added wins)


    private final ObservableList<StructuredTextAreaListener.LexicalAnalysisListener> lexerListeners = FXCollections.observableArrayList();
    private final ObservableList<StructuredTextAreaListener.ErrorAnalysisListener> errorListeners = FXCollections.observableArrayList();
    private final ObservableList<StructuredTextAreaListener.SemanticAnalysisListener> semanticListeners = FXCollections.observableArrayList();


    /**
     * describes whether or not
     */
    private final BooleanProperty implicitTerminalStyle = new SimpleBooleanProperty(this, "implicitTerminalStyle", false);
    {
        ImplicitTerminalStyleListener listener = new ImplicitTerminalStyleListener();

        implicitTerminalStyle.addListener((source, was, isNowImplicitlyHighlightingTerminalNodes) -> {
            if(isNowImplicitlyHighlightingTerminalNodes == was){ return; }

            //wonder if scala or kotlin has something for this
            if(isNowImplicitlyHighlightingTerminalNodes){
                getSemanticListeners().add(listener);
            }
            else{
                getSemanticListeners().remove(listener);
            }
        });
    }
    public final BooleanProperty implicitTerminalStyleProperty(){ return implicitTerminalStyle; }
    public final boolean getImplicitTerminalStyle(){ return implicitTerminalStyleProperty().get(); }
    public final void setImplicitTerminalStyle(boolean implicitlyStyleTerminalNodes){
        implicitTerminalStyleProperty().set(implicitlyStyleTerminalNodes);
    }


    public final ObservableList<StructuredTextAreaListener.SemanticAnalysisListener> getSemanticListeners(){
        return semanticListeners;
    }

    public final ObservableList<StructuredTextAreaListener.ErrorAnalysisListener> getErrorListeners(){
        return errorListeners;
    }

    public final ObservableList<StructuredTextAreaListener.LexicalAnalysisListener> getLexerListeners(){
        return lexerListeners;
    }

    //endregion

    //region implementation of apply Styles, maps, invocation of listeners

    //TODO make this a task? some kind of queue to ensure we dont flood?
    private void reApplyStyles() {

        ANTLRInputStream antlrStringStream = new ANTLRInputStream(getText());
        Lexer lexer = runOrThrowUnchecked(() -> lexerClass.getConstructor(CharStream.class).newInstance(antlrStringStream));
        lexer.removeErrorListeners();
        TokenStream tokens = new CommonTokenStream(lexer);
        Parser parser = runOrThrowUnchecked(() -> parserClass.getConstructor(TokenStream.class).newInstance(tokens));
        parser.getErrorListeners().removeIf(ConsoleErrorListener.class::isInstance);

        ParseTree expr = rootProduction.apply(parser);

        RangeMap<Integer, Token> tokensByCharIndex = ANTLRTokenStreamExtensions.indexByCharacterRange(tokens);

        RangeMap<Integer, String> styleByIndex = TreeRangeMap.create();

        lexerListeners.stream()
                .map(l -> l.generateNewStyles(this, tokensByCharIndex))
                .forEach(styleByIndex::putAll);

        ParseTreeWalker walker = new ParseTreeWalker();
        ParseTreeListener walkListener = new ParseTreeListener() {
            @Override public void visitTerminal(TerminalNode terminalNode) {
                if(terminalNode.getSymbol().getType() == Token.EOF){ return; }

                semanticListeners.stream()
                        .map(l -> l.generateNewStyles(StructuredTextArea.this, terminalNode))
                        .forEach(styleByIndex::putAll);
            }
            @Override public void visitErrorNode(ErrorNode errorNode) {
                errorListeners.stream()
                        .map(l -> l.generateNewStyles(StructuredTextArea.this, errorNode))
                        .forEach(styleByIndex::putAll);
            }
            @Override public void enterEveryRule(ParserRuleContext ctx) {

                semanticListeners.stream()
                        .map(l -> l.generateNewStyles(StructuredTextArea.this, ctx))
                        .forEach(styleByIndex::putAll);

            }
            @Override public void exitEveryRule(ParserRuleContext ctx) {}
        };
        walker.walk(walkListener, expr);

        //TODO overlapping highlights?

        snapRanges(styleByIndex);

        StyleSpansBuilder<Collection<String>> spansBuilder = convertToSpanList(styleByIndex);

        setStyleSpans(0, spansBuilder.create());
    }

    private StyleSpansBuilder<Collection<String>> convertToSpanList(RangeMap<Integer, String> styleByIndex) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int nextHighlightStart = 0;

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
        return spansBuilder;
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
    //endregion

    //region static helpers for loading and exception handling

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

    //endregion
}