package org.fxmisc.richtext.antlr;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Created by Geoff on 4/6/2016.
 */
public interface StructuredHighlighters {

    interface LexicalAnalysisHighlighter {

        RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, RangeMap<Integer, Token> newTokenStream);
    }

    interface SemanticAnalysisHighlighter {

        default RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, ParseTree newParseTree){
            return ImmutableRangeMap.of(); //empty
        }

        default RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, ErrorNode errorNode){
            return ImmutableRangeMap.of(); //by default ignore error nodes.

            // this method is ~an artifact of ANTLRs own error handling system,
            // but its included here for completeness.
            // its easier to work with the the other error handling strategy
            // because error nodes often have no tokens or EOF/invalid tokens.
        }
        default RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, ParserRuleContext productionOnNewTree){
            return generateNewStyles(parent, (ParseTree) productionOnNewTree);
        }
        default RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, TerminalNode terminalOnNewTree){
            return generateNewStyles(parent, (ParseTree) terminalOnNewTree);
        }
    }

    interface ErrorAnalysisHighlighter {

        RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent,
                                                    Token problemToken,
                                                    String antlrGeneratedMessage,
                                                    RecognitionException exception);
    }
}
