package org.fxmisc.richtext.antlr;

import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import javafx.beans.NamedArg;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.fxmisc.richtext.MouseOverTextEvent;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.StyleSpansBuilder;
import org.fxmisc.richtext.antlr.StructuredHighlighter.ErrorHighlighter;
import org.fxmisc.richtext.antlr.StructuredHighlighter.ParseRuleHighlighter;
import org.fxmisc.richtext.antlr.StructuredHighlighter.TokenHighlighter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 * Created by Geoff on 3/29/2016.
 */
public class StructuredTextArea extends StyleClassedTextArea {

    //TODO SLF4J? what is the best behaviour here?
    static final Logger log = Logger.getLogger(StructuredTextArea.class.getCanonicalName());

    private final Class<? extends Lexer>              lexerClass;
    private final Class<? extends Parser>             parserClass;

    private final Function<Parser, ParserRuleContext>     rootProduction;
    private final Function<CharStream, ? extends Lexer>   lexerCtor;
    private final Function<TokenStream, ? extends Parser> parserCtor;

    //TODO private String currentSelectorName; // add methods to override getTypeSelector()-> return javishmath-lexer, or javishmath-parser, etc?

    private ImmutableRangeMap<Integer, Token> mostRecentTokens;
    private ImmutableRangeMap<Integer, ParseTree> mostRecentParseTree;
    private ImmutableRangeMap<Integer, ParseError> mostRecentErrors;
    private ParserRuleContext mostRecentRoot;

    @SuppressWarnings("unchecked") //casting Func<TPar, ?Ctx> to Func<Parser, Ctx>,
    //contravariance on `Function`'s first type-param and covariance on its second definitely has something to do with it.
    public <TParser extends Parser> StructuredTextArea(Class<TParser> parserClass,
                                                       Class<? extends Lexer> lexerClass,
                                                       Function<? super TParser, ? extends ParserRuleContext> rootProduction){
        super();

        this.parserClass = parserClass;
        this.lexerClass = lexerClass;
        this.rootProduction = (Function<Parser, ParserRuleContext>) rootProduction;

        try {
            this.lexerCtor = buildConstructorClosure(this.lexerClass, CharStream.class);
            this.parserCtor = buildConstructorClosure(this.parserClass, TokenStream.class);

            init();
        }
        catch(Throwable exception){
            logConstructionFailure(parserClass.toString(), lexerClass.toString(), rootProduction.toString(), exception);
            throw exception;
        }
    }

    public StructuredTextArea(@NamedArg("parserClass") String parserClass,
                              @NamedArg("lexerClass") String lexerClass,
                              @NamedArg("rootProduction") String rootProduction) {
        super();

        try {
            this.parserClass = loadClass("parserClass", parserClass, Parser.class);
            this.lexerClass = loadClass("lexerClass", lexerClass, Lexer.class);

            Method rootProductionMethod = runOrThrowUnchecked(() -> this.parserClass.getMethod(rootProduction));
            this.rootProduction = parser -> runOrThrowUnchecked(() -> ParserRuleContext.class.cast(rootProductionMethod.invoke(parser)));

            this.lexerCtor = buildConstructorClosure(this.lexerClass, CharStream.class);
            this.parserCtor = buildConstructorClosure(this.parserClass, TokenStream.class);

            init();
        }
        catch(Throwable exception){
            logConstructionFailure(parserClass, lexerClass, rootProduction, exception);
            throw exception;
        }
    }

    private void logConstructionFailure(String parserClass, String lexerClass, String rootProduction, Throwable exception) {

        // so, I don't like this, but I don't like the FXML loader more, and this is designed to be used from FXML.
        // see line 461 in ProxyBuilder.java, which will silently suppress these errors.

        log.warning(format(
                "attempted to construct a %1s with " +
                "parserClass=%2s, lexerClass=%3s, rootProduction=%4s, " +
                "but that threw an execption: %5s",
                getClass(), parserClass, lexerClass, rootProduction, exception
        ));
    }

    //too much reflection going on in the constructor body.
    private void init() {
        // as per @{link org.fxmisc.richtext.CodeArea}, don't apply preceding style to typed text
        setUseInitialStyleForInsertion(true);

        //not using lambdas to make it a little easier on the debugger.
        RecompileAndRestyleListener recompileAndRestyleOnAnyChange = new RecompileAndRestyleListener();

        getHighlighters().addListener(recompileAndRestyleOnAnyChange);
        richChanges().subscribe(recompileAndRestyleOnAnyChange);
        caretPositionProperty().addListener(recompileAndRestyleOnAnyChange);

        recompileAndRestyleOnAnyChange.recompileAndReapply();
    }

    //region POJO accessors

    public Class<? extends Parser> getParserClass(){
        return parserClass;
    }

    public ImmutableRangeMap<Integer, Token> getTokensByCharIndex(){
        return mostRecentTokens;
    }

    public ImmutableRangeMap<Integer, ParseTree> getNodeByCharIndex(){
        return mostRecentParseTree;
    }

    public ImmutableRangeMap<Integer, ParseError> getErrorsByCharIndex(){
        return mostRecentErrors;
    }

    public ParserRuleContext getParseTreeRoot(){
        return mostRecentRoot;
    }

    //endregion

    //region (FXML accessible) properties

    // note prescedence,
    // lexical is first to run,
    // then semantics,
    // then errors
    // and styles are LIFO (most recently added wins)

    private ObservableList<StructuredHighlighter> highlighters = FXCollections.observableArrayList();

    //TODO fancy reactfx event pipes?
    //how do i put a "hey checkout my new token stream" event into one of these fancy stream-pipe-things?

    public final ObservableList<StructuredHighlighter> getHighlighters(){
        return highlighters;
    }


    //TODO: observable list of tokens and obsrvable tree (?) of nodes?

    //TODO docs
    //name is css-convention-ified version of terminal name from Parser.VOCABULARY
    // eg VARIABLE -> variable
    // eg SOME_INTS -> some-ints
    // see tests for other behaviours?

    private final BooleanProperty implicitTerminalStyle = new SimpleBooleanProperty(this, "implicitTerminalStyle", true);
    {
        //I'm not really sure what the best practices/idioms are here,
        // but I really don't like anonymous classes, especially under the debugger
        ImplicitTokenHighlighter listener = new ImplicitTokenHighlighter();
        toggleListenerMembership(listener, getImplicitTerminalStyle(), implicitTerminalStyle);
    }
    public final BooleanProperty implicitTerminalStyleProperty(){ return implicitTerminalStyle; }
    public final boolean getImplicitTerminalStyle(){ return implicitTerminalStyleProperty().get(); }
    public final void setImplicitTerminalStyle(boolean implicitlyStyleTerminalNodes){
        implicitTerminalStyleProperty().set(implicitlyStyleTerminalNodes);
    }

    //TODO docs
    //css style is "error"

    private final BooleanProperty implicitErrorStyle = new SimpleBooleanProperty(this, "implicitErrorStyle", true);
    {
        ErrorUnderlineHighlighter listener = new ErrorUnderlineHighlighter("error", /*includeTextUnderCaret*/false);
        toggleListenerMembership(listener, getImplicitErrorStyle(), implicitErrorStyle);
    }
    public final BooleanProperty implicitErrorStyleProperty(){ return implicitErrorStyle; }
    public final boolean getImplicitErrorStyle(){ return implicitErrorStyleProperty().get(); }
    public final void setImplicitErrorStyle(boolean implicitlyStyleErrorRanges){
        implicitErrorStyleProperty().set(implicitlyStyleErrorRanges);
    }


    // as has probably been made clear, I'm a fan of FXML,
    // so it annoyed me that I could only really do this from java
    // so I'm adding it as an FXML-referencable handler here
    // TODO split this into entered and exited?

    private final ObjectProperty<EventHandler<? super MouseOverTextEvent>> onMouseOverTextProperty
            = new SimpleObjectProperty<>(this, "onMouseOverText");
    {
        onMouseOverTextProperty.addListener((observable, oldHandler, newHandler) -> {
            setEventHandler(MouseOverTextEvent.ANY, newHandler);
        });
    }
    public final ObjectProperty<EventHandler<? super MouseOverTextEvent>> onMouseOverTextProperty() {
        return onMouseOverTextProperty;
    }
    public final EventHandler<? super MouseOverTextEvent> getOnMouseOverText() { return onMouseOverTextProperty().get(); }
    public final void setOnMouseOverText(EventHandler<? super MouseOverTextEvent> value) { onMouseOverTextProperty().set(value); }


    //endregion

    //region implementation of apply Styles, maps, invocation of listeners

    //TODO make this a task? some kind of queue to ensure we dont flood?
    private void reApplyStyles() {

        RangeMap<Integer, String> styleByIndex = TreeRangeMap.create();

        getHighlighters().stream()
                .filter(TokenHighlighter.class::isInstance).map(TokenHighlighter.class::cast)
                .map(l -> l.generateNewStyles(this, mostRecentTokens))
                .forEach(styleByIndex::putAll);

        ParseTreeListener walkListener = new ParseTreeListener() {
            @Override public void visitTerminal(TerminalNode terminalNode) {
                if(terminalNode.getSymbol().getType() == Token.EOF){ return; }

                getHighlighters().stream()
                        .filter(ParseRuleHighlighter.class::isInstance).map(ParseRuleHighlighter.class::cast)
                        .map(l -> l.generateNewStyles(StructuredTextArea.this, terminalNode))
                        .forEach(styleByIndex::putAll);
            }
            @Override public void visitErrorNode(ErrorNode errorNode) {
                //TODO not sure how to set this.
            }
            @Override public void enterEveryRule(ParserRuleContext ctx) {

                getHighlighters().stream()
                        .filter(ParseRuleHighlighter.class::isInstance).map(ParseRuleHighlighter.class::cast)
                        .map(l -> l.generateNewStyles(StructuredTextArea.this, ctx))
                        .forEach(styleByIndex::putAll);

            }
            @Override public void exitEveryRule(ParserRuleContext ctx) {
                //exit call, already added in visitEnter();
            }
        };
        new ParseTreeWalker().walk(walkListener, mostRecentRoot);

        Stream<ErrorHighlighter> errorListeners = getHighlighters().stream()
                .filter(ErrorHighlighter.class::isInstance).map(ErrorHighlighter.class::cast);
        errorListeners.forEach(listener -> {
                mostRecentErrors.asMapOfRanges().values().stream()
                        .map(error -> error.isConcrete()
                                ? listener.generateNewStylesForLexerError(
                                        this,
                                        error.getProblemToken(),
                                        error.getMessage(),
                                        error.getException()
                                )
                                : listener.generateNewStylesForTokenFailure(
                                        this,
                                        error.getProblemToken(),
                                        error.getMessage(),
                                        error.getException()
                                )
                        )
                        .forEach(styleByIndex::putAll);
        });

        snapRanges(styleByIndex);

        StyleSpansBuilder<Collection<String>> spansBuilder = convertToSpanList(styleByIndex);

        if(getLength() > 0) { //uhh, no spansBuilder.size() or spansBuilder.isEmpty() method?
            setStyleSpans(0, spansBuilder.create());
        }
    }

    private void recompile() {
        try {
            ANTLRInputStream antlrStringStream = new ANTLRInputStream(getText());
            Lexer lexer = lexerCtor.apply(antlrStringStream);
            lexer.removeErrorListeners();

            TokenStream tokens = new CommonTokenStream(lexer);
            Parser parser = parserCtor.apply(tokens);
            parser.getErrorListeners().removeIf(ConsoleErrorListener.class::isInstance);

            TreeRangeMap<Integer, ParseError> errorsByIndex = TreeRangeMap.create();
            parser.addErrorListener(new ANTLRErrorListener() {

                @Override
                public void syntaxError(Recognizer<?, ?> recognizer,
                                        Object offendingSymbol,
                                        int line,
                                        int charPositionInLine,
                                        String antlrMessage,
                                        RecognitionException exception) {

                    if(offendingSymbol == null) { return; }
                    Token offendingToken = (Token) offendingSymbol;

                    Range<Integer> charRange = ParseError.isConcrete(offendingToken)
                            ? makeRange(offendingToken, offendingToken)
                            : Range.singleton(-1) ;

                    errorsByIndex.put(charRange, new ParseError(offendingToken, exception, antlrMessage));
                }

                @Override public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {}
                @Override public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {}
                @Override public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) { }
            });

            ParserRuleContext root = rootProduction.apply(parser);

            TreeRangeMap<Integer, ParseTree> treeNodeByCharIndex = TreeRangeMap.create();
            ParseTreeListener walkListener = new ParseTreeListener(){

                @Override
                public void visitTerminal(TerminalNode node) {
                    //do nothing, this map already exists in the lexer stuff.
                    //leave lexical stuff to the lexican indexer.
                }

                @Override
                public void visitErrorNode(ErrorNode node) {
                    Token symbol = node.getSymbol();

                    Range<Integer> errorNodeRange = makeRange(symbol, symbol);
                    treeNodeByCharIndex.put(errorNodeRange, node);
                }

                //user enter instead of exit -> widest ranges added first -> most narrow ranges override wider ones.

                @Override
                public void enterEveryRule(ParserRuleContext ctx) {

                    if(ctx.getStart().getType() == Token.EOF){ return; }

                    Range<Integer> nodeRange = makeRange(ctx.getStart(), ctx.getStop());
                    treeNodeByCharIndex.put(nodeRange, ctx);
                }

                @Override
                public void exitEveryRule(ParserRuleContext ctx) {}
            };

            new ParseTreeWalker().walk(walkListener, root);

            //is thread-confinement from javafx sufficient here or do i need some kind of lock?

            mostRecentRoot = root;
            mostRecentErrors = ImmutableRangeMap.copyOf(errorsByIndex);
            mostRecentParseTree = ImmutableRangeMap.copyOf(treeNodeByCharIndex);
            mostRecentTokens = ANTLRTokenStreamExtensions.indexByCharacterRange(tokens);
        }
        catch(Throwable exception){
            log.warning("failure in compilation: " + exception);
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), exception);
        }
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
                //only refactor would be adjustRange(rangeMap, range, lb -> lb + 1, ub -> ub);
                //or adjustRange(rangeMap, range, true); --if willing to tolerate a boolean literal.
                String value = rangeMap.remove(range);
                range = Range.closed(range.lowerEndpoint(), range.upperEndpoint() - 1);
                if(range.isEmpty()){ continue; }
                styleByIndex.put(range, value);
            }
        }
    }
    //endregion

    //region loading and exception handling and a couple other misc things

    private void toggleListenerMembership(StructuredHighlighter listener, boolean implicitErrorStyleAtCtor, BooleanProperty implicitErrorStyleObs) {
        implicitErrorStyleObs.addListener((source, wasImplicit, isNowImplicit) -> {
            if(isNowImplicit == wasImplicit){ return; }

            if(isNowImplicit){ getHighlighters().add(listener); }
            else{ getHighlighters().remove(listener); }
        });

        if(implicitErrorStyleAtCtor){
            getHighlighters().add(listener);
        }
    }

    private static Range<Integer> makeRange(Token startToken, Token stopToken) {

        // antlr claims that it will give us potentially inverted start and stop tokens
        // if the production consumes no input. For non-left factored grammars, this can be quite common.
        // so we're simply going to straighten them out here.

        int proposedStartIdx = startToken.getStartIndex();
        int proposedEndIndex = stopToken.getStopIndex();

        int startIndex = proposedStartIdx <= proposedEndIndex ? proposedStartIdx : proposedEndIndex;
        int endIndex = proposedEndIndex >= proposedStartIdx ? proposedEndIndex : proposedStartIdx;

        return Range.closed(startIndex, endIndex);
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

    private static <TInput, TResult> Function<TInput, TResult> buildConstructorClosure(Class<TResult> lexerClass, Class<TInput> inputType) {
        Constructor<TResult> lexerConstructor = runOrThrowUnchecked(() -> lexerClass.getConstructor(inputType));
        return (TInput charStream) -> runOrThrowUnchecked(() -> lexerConstructor.newInstance(charStream));
    }

    @FunctionalInterface interface FailingSupplier<T> { T get() throws Throwable; }

    private static <TResult> TResult runOrThrowUnchecked(FailingSupplier<TResult> supplier){
        //TODO use Clojure's sneakyThrow? Whats the advantage to the wrapper?
        // I'm excluding people from catching the exception since you cant declare a
        // try { A() } catch (SomeCheckedException e){}
        // if A doesnt throw SCE.
        try{ return supplier.get(); }
        catch (RuntimeException | Error e){ throw e; }
        catch (Throwable e) { throw new RuntimeException(e); }
    }

    private class RecompileAndRestyleListener implements ListChangeListener<Object>, Consumer<Object>, ChangeListener<Object> {

        @Override
        public void onChanged(Change c) {
            if(c.next()){
                recompileAndReapply();
            }
        }

        @Override
        public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
            recompileAndReapply();
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

    public static class ParseError{

        private final Token problemToken;
        private final RecognitionException ex;
        private final String message;

        public ParseError(Token problemToken, RecognitionException ex, String message) {
            this.problemToken = problemToken;
            this.ex = ex;
            this.message = message;
        }

        public @Nonnull  String getMessage() { return message; }
        public @Nullable RecognitionException getException() { return ex; }
        public @Nonnull  Token getProblemToken() { return problemToken; }

        public boolean isConcrete() {
            return isConcrete(getProblemToken());
        }

        public static boolean isConcrete(Token problemToken){
            return problemToken.getType() != Token.EOF;
        }
    }

    //endregion
}