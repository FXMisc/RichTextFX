package org.fxmisc.richtext.antlr;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import javafx.beans.NamedArg;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;

public class ErrorUnderlineHighlighter implements StructuredHighlighter.ErrorHighlighter {

    private final String cssStyleClass;

    public ErrorUnderlineHighlighter(@NamedArg(value="styleClass", defaultValue="error") String styleClass) {
        this.cssStyleClass = styleClass;
    }

    @Override
    public RangeMap<Integer, String> generateNewStylesForLexerError(StructuredTextArea parent,
                                                                    Token problemToken,
                                                                    String antlrGeneratedMessage,
                                                                    RecognitionException exception) {

        Range<Integer> errorRange = Range.closed(problemToken.getStartIndex(), problemToken.getStopIndex());

//        TODO: boolean flag, should underline things with the cursor beside them?
        return ImmutableRangeMap.of(errorRange, cssStyleClass);
    }

    @Override
    public RangeMap<Integer, String> generateNewStylesForParserError(StructuredTextArea parent, ErrorNode error) {
        return NO_NEW_HIGHLIGHTS;
    }
}
