package org.fxmisc.richtext;

import javafx.beans.NamedArg;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by Geoff on 4/4/2016.
 */
public class ContextualHighlight {

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
            @NamedArg("node") String antlrLocalContextClassnamePrefix,
            @NamedArg("styleClass") String styleClass) {
        this(antlrLocalContextClassnamePrefix, "", "", styleClass);
    }

    public ContextualHighlight(
            @NamedArg("node") String antlrLocalContextClassnamePrefix,
            @NamedArg("targetText") String targetText,
            @NamedArg("styleClass") String styleClass) {
        this(antlrLocalContextClassnamePrefix, "", targetText, styleClass);
    }

    public ContextualHighlight(
            @NamedArg("node") String antlrLocalContextClassnamePrefix,
            @NamedArg("parent") String antlrParentContextClassnamePrefix,
            @NamedArg("targetText") String targetText,
            @NamedArg("styleClass") String styleClass) {

        if (styleClass == null) {
            throw new IllegalArgumentException("styleClass");
        }
        // TODO additional validation? whats the minimum required value set for a possible match?

        this.styleClass = styleClass;
        this.antlrContextPrefix = Optional.of(antlrLocalContextClassnamePrefix).filter(str -> !str.isEmpty());
        this.antlrParentContextPrefix = Optional.of(antlrParentContextClassnamePrefix).filter(str -> !str.isEmpty());
        this.targetText = Optional.of(targetText).filter(str -> !str.isEmpty());
    }

    public void setAndValidateParentContext(Class<? extends Parser> parentParser) {

        if (wasFullyInitialized) {
            throw new UnsupportedOperationException("cannot re-use highlights");
        }

        antlrContextClass = loadParserNesterClass(parentParser, antlrContextPrefix);
        antlrParentContextClass = loadParserNesterClass(parentParser, antlrParentContextPrefix);

        wasFullyInitialized = true;
    }

    private Optional<Class> loadParserNesterClass(Class<? extends Parser> parentParser, Optional<String> contextPrefix) {
        return contextPrefix
                .map(prefix -> parentParser.getName() + "$" + prefix + "Context")
                .map(className -> StructuredTextArea.loadClass("node aka antlrLocalContextPrefix", className, ParserRuleContext.class));
    }

    public Optional<HighlightedTextInteveral> getMatchingText(ParserRuleContext context) {

        if (!wasFullyInitialized) {
            throw new IllegalStateException("cannot determine matching text since no parent parser has yet been given!");
        }

        if (!antlrContextClass.map(t -> t.isInstance(context)).orElse(true)) {
            return Optional.empty();
        }

        if (!antlrParentContextClass.map(neededParent -> neededParent.isInstance(context.getParent())).orElse(true)) {
            return Optional.empty();
        }

        Optional<Token> matchingToken;
        Stream<Token> childTokens = (context.children == null ? new ArrayList<>() : context.children).stream()
                .filter(TerminalNode.class::isInstance)
                .map(TerminalNode.class::cast)
                .map(TerminalNode::getSymbol);

        if (targetText.isPresent()) {
            matchingToken = childTokens
                    .filter(token -> targetText.map(token.getText()::equals).orElse(false))
                    .findFirst();
        }
        //todo god damn terminal operations,
        // what is the problem the streams API is solving with this nonsense!!
//            else if (childTokens.count() == 1){
        else if (context.getChildCount() == 1) {
            matchingToken = childTokens.findFirst();
        } else {
            matchingToken = Optional.empty();
        }

        return matchingToken.map(token -> new HighlightedTextInteveral(token.getStartIndex(), token.getStopIndex(), styleClass));
    }
}
