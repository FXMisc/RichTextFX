package org.fxmisc.richtext.antlr;

import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import javafx.beans.NamedArg;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.RichTextChange;
import org.fxmisc.richtext.StyleSpansBuilder;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Geoff on 3/29/2016.
 */
public class StructuredTextArea extends CodeArea {

    private final Class<? extends Parser>     parserClass;
    private final Class<? extends Lexer>      lexerClass;
    private final Function<Parser, ParseTree> rootProduction;

    private ImmutableRangeMap<Integer, Token> mostRecentTokens;
    private ParseTree mostRecentParseTree;

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

        RecompileAndRestyleListener recompileAndRestyle = new RecompileAndRestyleListener();

        for(ObservableList<?> listenerList : new ObservableList[]{lexerListeners, errorListeners, semanticListeners}){
            listenerList.addListener(recompileAndRestyle);
        }

        richChanges().subscribe(recompileAndRestyle);
        caretPositionProperty().addListener((source, oldIdx, newIdx) -> {
            reApplyStyles();
        });

        recompile();
        reApplyStyles();
    }

    //region POJO accessors

    public Class<? extends Parser> getParserClass(){
        return parserClass;
    }

    public ImmutableRangeMap<Integer, Token> getTokensByCharIndex(){
        return mostRecentTokens;
    }

    public ParseTree getParseTree(){
        return mostRecentParseTree;
    }

    //endregion

    //region (FXML accessible) properties

    // note prescedence,
    // lexical is first to run,
    // then semantics,
    // then errors
    // and styles are LIFO (most recently added wins)

    private final ObservableList<StructuredTextAreaHighlighter.LexicalAnalysisListener> lexerListeners = FXCollections.observableArrayList();
    private final ObservableList<StructuredTextAreaHighlighter.ErrorAnalysisListener> errorListeners = FXCollections.observableArrayList();
    private final ObservableList<StructuredTextAreaHighlighter.SemanticAnalysisListener> semanticListeners = FXCollections.observableArrayList();

    public final ObservableList<StructuredTextAreaHighlighter.SemanticAnalysisListener> getSemanticListeners(){
        return semanticListeners;
    }

    public final ObservableList<StructuredTextAreaHighlighter.ErrorAnalysisListener> getErrorListeners(){
        return errorListeners;
    }

    public final ObservableList<StructuredTextAreaHighlighter.LexicalAnalysisListener> getLexerListeners(){
        return lexerListeners;
    }

    //TODO: observable list of tokens and obsrvable tree (?) of nodes?


    /**
     * describes whether or not
     */
    private final BooleanProperty implicitTerminalStyle = new SimpleBooleanProperty(this, "implicitTerminalStyle", true);
    {
        //I'm not really sure what the best practices/idioms are here,
        // but I really don't like anonymous classes, especially under the debugger
        StructuredTextAreaHighlighter.SemanticAnalysisListener listener = new ImplicitTerminalStyleHighlighter();

        implicitTerminalStyle.addListener((source, wasImplicit, isNowImplicit) -> {
            if(isNowImplicit == wasImplicit){ return; }

            if(isNowImplicit){
                getSemanticListeners().add(listener);
            }
            else{
                getSemanticListeners().remove(listener);
            }
        });

        if(getImplicitTerminalStyle()){
            getSemanticListeners().add(listener);
        }
    }
    public final BooleanProperty implicitTerminalStyleProperty(){ return implicitTerminalStyle; }
    public final boolean getImplicitTerminalStyle(){ return implicitTerminalStyleProperty().get(); }
    public final void setImplicitTerminalStyle(boolean implicitlyStyleTerminalNodes){
        implicitTerminalStyleProperty().set(implicitlyStyleTerminalNodes);
    }


    //endregion

    //region implementation of apply Styles, maps, invocation of listeners

    //TODO make this a task? some kind of queue to ensure we dont flood?
    private void reApplyStyles() {

        RangeMap<Integer, String> styleByIndex = TreeRangeMap.create();

        lexerListeners.stream()
                .map(l -> l.generateNewStyles(this, mostRecentTokens))
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
        walker.walk(walkListener, mostRecentParseTree);

        //TODO overlapping highlights?

        snapRanges(styleByIndex);

        StyleSpansBuilder<Collection<String>> spansBuilder = convertToSpanList(styleByIndex);

        if(getLength() > 0) { //uhh, no spansBuilder.size() or spansBuilder.isEmpty() method?
            setStyleSpans(0, spansBuilder.create());
        }
    }

    private void recompile() {
        ANTLRInputStream antlrStringStream = new ANTLRInputStream(getText());
        Lexer lexer = runOrThrowUnchecked(() -> lexerClass.getConstructor(CharStream.class).newInstance(antlrStringStream));
        lexer.removeErrorListeners();
        TokenStream tokens = new CommonTokenStream(lexer);
        Parser parser = runOrThrowUnchecked(() -> parserClass.getConstructor(TokenStream.class).newInstance(tokens));
        parser.getErrorListeners().removeIf(ConsoleErrorListener.class::isInstance);

        mostRecentParseTree = rootProduction.apply(parser);
        mostRecentTokens = ANTLRTokenStreamExtensions.indexByCharacterRange(tokens);
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

    private class RecompileAndRestyleListener implements ListChangeListener<Object>, Consumer<Object> {

        @Override
        public void onChanged(Change c) {
            if(c.next()){
                recompileAndReapply();
            }
        }

        @Override
        public void accept(Object o) {
            recompile();
        }

        private void recompileAndReapply() {
            recompile();
            reApplyStyles();
        }
    }

    //endregion
}