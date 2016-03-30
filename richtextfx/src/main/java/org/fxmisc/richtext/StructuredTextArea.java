package org.fxmisc.richtext;

import javafx.beans.DefaultProperty;
import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

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

        richChanges().subscribe(this::reApplyStyles);

        highlights.addListener((ListChangeListener<ContextualHighlight>) c -> {
            while(c.next()){
                c.getAddedSubList().forEach(highlight -> highlight.setAndValidateParentContext(this.parserClass));
            }
        });
    }

    private void reApplyStyles(RichTextChange<Collection<String>, Collection<String>> collectionRichTextChange) {

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

        //act
        ParseTreeWalker walker = new ParseTreeWalker();
        ParseTreeListener walkListener = new ParseTreeListener() {
            @Override public void visitTerminal(@NotNull TerminalNode terminalNode) {}
            @Override public void visitErrorNode(@NotNull ErrorNode errorNode) {}
            @Override public void enterEveryRule(@NotNull ParserRuleContext parserRuleContext) {}
            @Override public void exitEveryRule(@NotNull ParserRuleContext ctx) {

                Optional<HighlightedTextInteveral> targetHighlight = getHighlights().stream()
                        .map(highlight -> highlight.getMatchingText(ctx))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst();

                targetHighlight.ifPresent(foundHighlights::add);
            }
        };
        walker.walk(walkListener, expr);

        foundHighlights.sort((l, r) -> l.lowerBound - r.lowerBound);

        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastHighlightEnd = 0;
        //TODO overlapping highlights?

        for(HighlightedTextInteveral interval : foundHighlights){
            spansBuilder.add(Collections.emptyList(), interval.lowerBound - lastHighlightEnd);
            spansBuilder.add(Collections.singleton(interval.styleClass), (interval.upperBound + 1) - interval.lowerBound);
            lastHighlightEnd = interval.upperBound + 1;
        }

        spansBuilder.add(Collections.emptyList(), getLength() - lastHighlightEnd);

        setStyleSpans(0, spansBuilder.create());
    }

    public ObservableList<ContextualHighlight> getHighlights(){
        return highlights;
    }

    public static class HighlightedTextInteveral{

        private final int lowerBound;
        private final int upperBound;
        private final String styleClass;

        public HighlightedTextInteveral(int lowerBound, int upperBound, String styleClass) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.styleClass = styleClass;
        }
    }

    public static class ContextualHighlight{

        private final Optional<String> antlrContextPrefix;
        private final Optional<String> antlrParentContextPrefix;
        private final Optional<String> targetText;
        private final String styleClass;

        //TODO I dont like these long running constructor things
        // any chance I can get the FXML loader to tell me what my parent element is?
        // might be getting too dynamic. Whats an elegant static solution? fx:reference?
        private boolean wasFullyInitialized = false;

        private Optional<Class> antlrContextClass = Optional.empty();
        private Optional<Class> antlrParentContextClass = Optional.empty();

        public ContextualHighlight(
                @NamedArg(value = "node", defaultValue = "") String antlrLocalContextClassnamePrefix,
                @NamedArg(value = "parent", defaultValue = "") String antlrParentContextClassnamePrefix,
                @NamedArg(value = "targetText", defaultValue = "") String targetText,
                @NamedArg("styleClass") String styleClass
        ){
            if(styleClass == null) { throw new IllegalArgumentException("styleClass"); }
            // TODO additional validation? whats the minimum required value set for a possible match?

            this.styleClass = styleClass;
            this.antlrContextPrefix = Optional.of(antlrLocalContextClassnamePrefix).filter(str -> ! str.isEmpty());
            this.antlrParentContextPrefix = Optional.of(antlrParentContextClassnamePrefix).filter(str -> ! str.isEmpty());
            this.targetText = Optional.of(targetText).filter(str -> ! str.isEmpty());
        }

        public void setAndValidateParentContext(Class<? extends Parser> parentParser){

            if(wasFullyInitialized){ throw new UnsupportedOperationException("cannot re-use highlights"); }

            antlrContextClass = loadParserNesterClass(parentParser, antlrContextPrefix);
            antlrParentContextClass = loadParserNesterClass(parentParser, antlrParentContextPrefix);

            wasFullyInitialized = true;
        }

        private Optional<Class> loadParserNesterClass(Class<? extends Parser> parentParser, Optional<String> contextPrefix) {
            return contextPrefix
                    .map(prefix -> parentParser.getName() + "$" + prefix + "Context")
                    .map(className -> loadClass("node aka antlrLocalContextPrefix", className, ParserRuleContext.class));
        }

        public Optional<HighlightedTextInteveral> getMatchingText(ParserRuleContext context){

            if ( ! wasFullyInitialized){
                throw new IllegalStateException("cannot determine matching text since no parent parser has yet been given!");
            }

            if( ! antlrContextClass.map(t -> t.isInstance(context)).orElse(true)){
                return Optional.empty();
            }

            if ( ! antlrParentContextClass.map(neededParent -> neededParent.isInstance(context.getParent())).orElse(true)){
                return Optional.empty();
            }

            Optional<Token> matchingToken;
            Stream<Token> childTokens = (context.children == null ? new ArrayList<>() : context.children).stream()
                    .filter(TerminalNode.class::isInstance)
                    .map(TerminalNode.class::cast)
                    .map(TerminalNode::getSymbol);

            if( targetText.isPresent()) {
                matchingToken = childTokens
                        .filter(token -> targetText.map(token.getText()::equals).orElse(false))
                        .findFirst();
            }
            //todo god damn terminal operations,
            // what is the problem the streams API is solving with this nonsense!!
//            else if (childTokens.count() == 1){
            else if (context.getChildCount() == 1){
                matchingToken = childTokens.findFirst();
            }
            else{
                matchingToken = Optional.empty();
            }

            return matchingToken.map(token -> new HighlightedTextInteveral(token.getStartIndex(), token.getStopIndex(), styleClass));
        }
    }

    private static <TClass> Class<? extends TClass> loadClass(String varName, String className, Class<TClass> neededSuperClass){
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
