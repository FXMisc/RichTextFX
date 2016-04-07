package org.fxmisc.richtext.antlr;

import com.google.common.collect.RangeMap;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;

/**
 * Created by Geoff on 4/6/2016.
 */
public interface StructuredTextAreaListener{

    interface LexicalAnalysisListener {

        RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, RangeMap<Integer, Token> newTokenStream);
    }

    interface SemanticAnalysisListener {

        RangeMap<Integer, String> generateNewStyles(StructuredTextArea parseTreeListener, ParserRuleContext newParseTree);
    }

    interface ErrorAnalysisListener {

        RangeMap<Integer, String> generateNewStyles(StructuredTextArea parseTreeListener, ErrorNode errorNode);
    }
}
