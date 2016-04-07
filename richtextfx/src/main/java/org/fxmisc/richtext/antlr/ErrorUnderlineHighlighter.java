package org.fxmisc.richtext.antlr;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;

public class ErrorUnderlineHighlighter implements StructuredTextAreaHighlighter.ErrorAnalysisListener{

    @Override
    public RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, 
                                                       Token problemToken, 
                                                       String antlrGeneratedMessage, 
                                                       RecognitionException exception) {

        Range<Integer> errorRange = Range.closed(problemToken.getStartIndex(), problemToken.getStopIndex());
        return ImmutableRangeMap.of(errorRange, "error");
    }
}
