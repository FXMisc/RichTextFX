package org.fxmisc.richtext;

import java.util.Collection;
import java.util.Collections;

import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.SimpleEditableStyledDocument;
import org.fxmisc.undo.UndoManager;

/**
 * Text area that uses style classes to define style of text segments and paragraph segments.
 */
public class StyleClassedTextArea extends StyledTextArea<Collection<String>, Collection<String>> {

    public StyleClassedTextArea(EditableStyledDocument<Collection<String>, Collection<String>> document, boolean preserveStyle) {
        this(document, preserveStyle, null);
    }

    public StyleClassedTextArea(EditableStyledDocument<Collection<String>, Collection<String>> document, boolean preserveStyle,
                                UndoManager undoManager
    ) {
        super(Collections.<String>emptyList(),
                (paragraph, styleClasses) -> paragraph.getStyleClass().addAll(styleClasses),
                Collections.<String>emptyList(),
                (text, styleClasses) -> text.getStyleClass().addAll(styleClasses),
                document, preserveStyle, undoManager
        );

        setStyleCodecs(
                Codec.collectionCodec(Codec.STRING_CODEC),
                Codec.collectionCodec(Codec.STRING_CODEC)
        );
    }
    public StyleClassedTextArea(boolean preserveStyle) {
        this(
                new SimpleEditableStyledDocument<>(
                    Collections.<String>emptyList(), Collections.<String>emptyList()
                ), preserveStyle);
    }

    /**
     * Creates a text area with empty text content.
     */
    public StyleClassedTextArea() {
        this(true);
    }

    /**
     * Convenient method to assign a single style class.
     */
    public void setStyleClass(int from, int to, String styleClass) {
        setStyle(from, to, Collections.singletonList(styleClass));
    }
}
