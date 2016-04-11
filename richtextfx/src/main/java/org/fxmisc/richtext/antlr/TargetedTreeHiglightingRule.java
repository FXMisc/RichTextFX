package org.fxmisc.richtext.antlr;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import javafx.beans.NamedArg;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by Geoff on 4/4/2016.
 */
public class TargetedTreeHiglightingRule implements StructuredHighlighter.ParseRuleHighlighter {

    private final Optional<String> antlrContextPrefix;
    private final Optional<String> antlrParentContextPrefix;
    private final Optional<String> targetText;
    private final String styleClass;

    private Optional<Class> antlrContextClass = Optional.empty();
    private Optional<Class> antlrParentContextClass = Optional.empty();

    public TargetedTreeHiglightingRule(
            @NamedArg("node") String antlrLocalContextClassnamePrefix,
            @NamedArg("styleClass") String styleClass) {
        this(antlrLocalContextClassnamePrefix, "", "", styleClass);
    }

    public TargetedTreeHiglightingRule(
            @NamedArg("node") String antlrLocalContextClassnamePrefix,
            @NamedArg("targetText") String targetText,
            @NamedArg("styleClass") String styleClass) {
        this(antlrLocalContextClassnamePrefix, "", targetText, styleClass);
    }

    public TargetedTreeHiglightingRule(
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

    private static Optional<Class> loadParserNesterClass(Class<? extends Parser> parentParser,
                                                         Optional<String> contextPrefix) {
        return contextPrefix
                .map(prefix -> parentParser.getName() + "$" + prefix + "Context")
                .map(className -> StructuredTextArea.loadClass("node aka antlrLocalContextPrefix", className, ParserRuleContext.class));
    }

    @Override
    public RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, ParseTree newParseTree) {
        return ImmutableRangeMap.of();
    }

    @Override
    public RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, ParserRuleContext context) {

        ImmutableRangeMap.Builder<Integer, String> result = ImmutableRangeMap.builder();

        antlrContextClass = loadParserNesterClass(parent.getParserClass(), antlrContextPrefix);
        antlrParentContextClass = loadParserNesterClass(parent.getParserClass(), antlrParentContextPrefix);

        if (!antlrContextClass.map(t -> t.isInstance(context)).orElse(true)) {
            return result.build();
        }

        if (!antlrParentContextClass.map(neededParent -> neededParent.isInstance(context.getParent())).orElse(true)) {
            return result.build();
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
        }
        else {
            matchingToken = Optional.empty();
        }

        matchingToken
                .map(token -> Range.closed(token.getStartIndex(), token.getStopIndex()))
                .ifPresent(range -> result.put(range, styleClass));

        return result.build();
    }
}
