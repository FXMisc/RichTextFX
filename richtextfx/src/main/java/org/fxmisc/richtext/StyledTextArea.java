package org.fxmisc.richtext;

import java.util.function.BiConsumer;

import javafx.beans.NamedArg;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.text.TextFlow;

import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.SimpleEditableStyledDocument;
import org.fxmisc.richtext.model.StyledSegment;
import org.fxmisc.richtext.model.TextOps;

/**
 * A {@link GenericStyledArea} whose segment generic has been specified to be a {@link String}. How the text
 * will be styled is not yet specified in this class, but use {@link StyleClassedTextArea} for a style class
 * approach to styling the text and {@link InlineCssTextArea} for an inline css approach to styling the text.
 *
 * @param <PS> type of paragraph style
 * @param <S> type of style that can be applied to text.
 */
public class StyledTextArea<PS, S> extends GenericStyledArea<PS, String, S> {

    public StyledTextArea(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
                          @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
                          @NamedArg("initialTextStyle")      S initialTextStyle,
                          @NamedArg("applyStyle")            BiConsumer<? super TextExt, S> applyStyle,
                          @NamedArg("document")              EditableStyledDocument<PS, String, S> document,
                          @NamedArg("segmentOps")            TextOps<String, S> segmentOps,
                          @NamedArg("preserveStyle")         boolean preserveStyle) {
        super(initialParagraphStyle, applyParagraphStyle,
                initialTextStyle, document, segmentOps, preserveStyle,
                seg -> createStyledTextNode(seg, applyStyle)
        );
    }

    public StyledTextArea(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
                          @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
                          @NamedArg("initialTextStyle")      S initialTextStyle,
                          @NamedArg("applyStyle")            BiConsumer<? super TextExt, S> applyStyle,
                          @NamedArg("document")              EditableStyledDocument<PS, String, S> document,
                          @NamedArg("preserveStyle")         boolean preserveStyle) {
        this(initialParagraphStyle, applyParagraphStyle,
              initialTextStyle, applyStyle,
              document, SegmentOps.styledTextOps(), preserveStyle);
    }

    public StyledTextArea(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
                          @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
                          @NamedArg("initialTextStyle")      S initialTextStyle,
                          @NamedArg("applyStyle")            BiConsumer<? super TextExt, S> applyStyle,
                          @NamedArg("document")              EditableStyledDocument<PS, String, S> document) {
        this(initialParagraphStyle, applyParagraphStyle,
             initialTextStyle, applyStyle, document, true);
    }

    public StyledTextArea(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
                          @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
                          @NamedArg("initialTextStyle")      S initialTextStyle,
                          @NamedArg("applyStyle")            BiConsumer<? super TextExt, S> applyStyle,
                          @NamedArg("preserveStyle")         boolean preserveStyle) {
        this(
                initialParagraphStyle,
                applyParagraphStyle,
                initialTextStyle,
                applyStyle,
                new SimpleEditableStyledDocument<>(initialParagraphStyle, initialTextStyle),
                preserveStyle);
    }

    public StyledTextArea(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
                          @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
                          @NamedArg("initialTextStyle")      S initialTextStyle,
                          @NamedArg("applyStyle")            BiConsumer<? super TextExt, S> applyStyle) {
        this(initialParagraphStyle, applyParagraphStyle,
             initialTextStyle, applyStyle, true);
    }

    /**
     * Creates a {@link TextExt} node using the given styled text.
     */
    public static <S> Node createStyledTextNode(StyledSegment<String, S> seg,
                                                BiConsumer<? super TextExt, S> applyStyle) {
        return createStyledTextNode(seg.getSegment(), seg.getStyle(), applyStyle);
    }

    /**
     * Creates a {@link TextExt} node using the given styled text.
     */
    public static <S> Node createStyledTextNode(String text, S style,
                                                BiConsumer<? super TextExt, S> applyStyle) {

        TextExt t = new TextExt(text);
        t.setTextOrigin(VPos.TOP);
        t.getStyleClass().add("text");
        applyStyle.accept(t, style);
        return t;
    }
}
