package org.fxmisc.richtext.antlr;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * first attempt at getting error highlights, its actually better than the current one at highlighting missing things.
 *
 * Created by Geoff on 4/7/2016.
 */
public class ErrorUnderlineHighlighter_old implements StructuredTextAreaHighlighter.SemanticAnalysisListener {

    @Override
    public RangeMap<Integer, String> generateNewStyles(StructuredTextArea parseTreeListener, ErrorNode errorNode) {

        Token symbol = errorNode.getSymbol();

        ImmutableRangeMap.Builder<Integer, String> styleByIndex = ImmutableRangeMap.builder();

        int startIndex = -1, endIndex = -1;

        //TODO this doesnt highlight anything for the expression '(10 + )' (a postfix unary '+'?)
        if (symbol.getStartIndex() != -1) {
            startIndex = symbol.getStartIndex();
            endIndex = symbol.getStopIndex();
        }
        else {
            ParserRuleContext parent = (ParserRuleContext) errorNode.getParent();
            int thisIndex = parent.children.indexOf(errorNode);
            ParseTree olderSib = parent.getChild(thisIndex - 1);
            ParseTree youngerSib = parent.getChild(thisIndex + 1);

            Token preceedingToken =
                    olderSib instanceof ParserRuleContext ? ((ParserRuleContext) olderSib).getStop() :
                    olderSib instanceof TerminalNode ? ((TerminalNode) olderSib).getSymbol() :
                            null;
            Token succeedingToken =
                    youngerSib instanceof ParserRuleContext ? ((ParserRuleContext) youngerSib).getStart() :
                    youngerSib instanceof TerminalNode ? ((TerminalNode) youngerSib).getSymbol() :
                            null;

            if (preceedingToken != null) {
                startIndex = preceedingToken.getStopIndex();
            }
            if (succeedingToken != null) {
                endIndex = succeedingToken.getStartIndex();
            }

            startIndex = startIndex == -1 ? endIndex : startIndex;
            endIndex = endIndex == -1 ? startIndex : endIndex;
        }

        Range<Integer> range = Range.closed(startIndex, endIndex);
        styleByIndex.put(range, "error");

        return styleByIndex.build();
    }
}

