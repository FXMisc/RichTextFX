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

    ImmutableRangeMap<Integer, String> NO_NEW_HIGHLIGHTS = ImmutableRangeMap.of(); //empty

    interface TokenHighlighter extends StructuredHighlighters {

        //TODO docs
        //EOF is at range [-1, -1]
        RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, RangeMap<Integer, Token> newTokenStream);

        //TODO currently the bracket implementation of this abuses the caretPosition listener
        // to force a restyle every time the caret position changes.
        // No recompile, but that still isn't necessarily cheap.
        // If i made this interface stateful, I could optimize that, but state is icky.
        // could do a `boolean needsRestyle(STA parent, int oldPosition, intNewPosition)`,
        // with a default implementation to return false, but, ehhh,
    }

    interface ParseRuleHighlighter extends StructuredHighlighters {

        default RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, ParseTree newParseTree){
            return NO_NEW_HIGHLIGHTS;
        }

        default RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, ParserRuleContext productionOnNewTree){
            return generateNewStyles(parent, (ParseTree) productionOnNewTree);
        }
        default RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, TerminalNode terminalOnNewTree){
            return generateNewStyles(parent, (ParseTree) terminalOnNewTree);
        }
    }

    interface ErrorHighlighter extends StructuredHighlighters {

        RangeMap<Integer, String> generateNewStylesForLexerError(StructuredTextArea parent,
                                                                 Token problemToken,
                                                                 String antlrGeneratedMessage,
                                                                 RecognitionException exception);

        RangeMap<Integer, String> generateNewStylesForParserError(StructuredTextArea parent, ErrorNode error);

        //TODO docs
        // this is the case where the document is length zero, or the token is an EOF problem.
        default RangeMap<Integer, String> generateNewStylesForTokenFailure(StructuredTextArea parent,
                                                                           Token problemToken,
                                                                           String antlrGeneratedMessage,
                                                                           RecognitionException exception){
            return NO_NEW_HIGHLIGHTS;
        }
    }
}
