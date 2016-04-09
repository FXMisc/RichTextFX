package org.fxmisc.richtext.antlr;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import javafx.beans.NamedArg;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;

public class ErrorUnderlineHighlighter implements StructuredHighlighters.ErrorAnalysisHighlighter {

    private final String cssStyleClass;

    public ErrorUnderlineHighlighter(@NamedArg(value="styleClass", defaultValue="error") String styleClass) {
        this.cssStyleClass = styleClass;
    }

    @Override
    public RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, 
                                                       Token problemToken, 
                                                       String antlrGeneratedMessage, 
                                                       RecognitionException exception) {

        Range<Integer> errorRange = Range.closed(problemToken.getStartIndex(), problemToken.getStopIndex());
        return ImmutableRangeMap.of(errorRange, cssStyleClass);
    }
}
