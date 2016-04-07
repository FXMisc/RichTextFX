package org.fxmisc.richtext;

import com.google.common.collect.RangeMap;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ErrorNode;

/**
 * Created by Geoff on 4/6/2016.
 */
public interface StructuredTextAreaListener{

    interface LexicalAnalysisListener {

        RangeMap<Integer, String> generateNewStyles(TokenStream newTokenStream);
    }

    interface SemanticAnalysisListener {

        RangeMap<Integer, String> generateNewStyles(ParserRuleContext newParseTree);
    }

    interface ErrorAnalysisListener {

        RangeMap<Integer, String> generateNewStyles(ErrorNode event);
    }
}
