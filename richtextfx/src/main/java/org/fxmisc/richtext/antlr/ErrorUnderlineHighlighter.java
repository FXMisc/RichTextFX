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
    private final boolean includeTextUnderCaret;

    //TODO docs
    // include cursor = should we apply the error style if the cursor is on that element?
    public ErrorUnderlineHighlighter(@NamedArg(value="styleClass", defaultValue="error") String styleClass,
                                     @NamedArg(value="includeCaret", defaultValue="false") boolean includeTextUnderCaret) {
        this.cssStyleClass = styleClass;
        this.includeTextUnderCaret = includeTextUnderCaret;
    }

    @Override
    public RangeMap<Integer, String> generateNewStylesForLexerError(StructuredTextArea parent,
                                                                    Token problemToken,
                                                                    String antlrGeneratedMessage,
                                                                    RecognitionException exception) {

        Range<Integer> errorRange = Range.closed(problemToken.getStartIndex(), problemToken.getStopIndex());

        return errorRange.contains(parent.getCaretPosition() - 1)
                ? NO_NEW_HIGHLIGHTS
                : ImmutableRangeMap.of(errorRange, cssStyleClass);
    }

    @Override
    public RangeMap<Integer, String> generateNewStylesForParserError(StructuredTextArea parent, ErrorNode error) {
        return NO_NEW_HIGHLIGHTS;
    }
}
