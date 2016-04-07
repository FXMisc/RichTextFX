package org.fxmisc.richtext.antlr;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.RangeMap;
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Created by Geoff on 4/6/2016.
 */
public interface StructuredTextAreaHighlighter {

    interface LexicalAnalysisListener {

        RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, RangeMap<Integer, Token> newTokenStream);
    }

    interface SemanticAnalysisListener {

        default RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, ParseTree newParseTree){
            return ImmutableRangeMap.of(); //empty
        }

        default RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, ParserRuleContext productionOnNewTree){
            return generateNewStyles(parent, (ParseTree) productionOnNewTree);
        }
        default RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, TerminalNode terminalOnNewTree){
            return generateNewStyles(parent, (ParseTree) terminalOnNewTree);
        }
    }

    interface ErrorAnalysisListener {

        RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, ErrorNode errorNode);
    }
}
