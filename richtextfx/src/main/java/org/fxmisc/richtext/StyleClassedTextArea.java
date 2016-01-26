package org.fxmisc.richtext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Text area that uses style classes to define style of text segments and paragraph segments.
 */
public class StyleClassedTextArea extends StyledTextArea<Collection<String>, Collection<String>> {

    public StyleClassedTextArea(EditableStyledDocument<Collection<String>, Collection<String>> document, boolean preserveStyle) {
        super(Collections.<String>emptyList(),
                (paragraph, styleClasses) -> paragraph.getStyleClass().addAll(styleClasses),
                Collections.<String>emptyList(),
                (text, styleClasses) -> text.getStyleClass().addAll(styleClasses),
                document, preserveStyle
        );

        setStyleCodecs(
                SuperCodec.upCast(SuperCodec.collectionListCodec(Codec.STRING_CODEC)),
                SuperCodec.upCast(SuperCodec.collectionListCodec(Codec.STRING_CODEC))
        );
    }
    public StyleClassedTextArea(boolean preserveStyle) {
        this(
                new EditableStyledDocument<>(
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
        List<String> styleClasses = new ArrayList<>(1);
        styleClasses.add(styleClass);
        setStyle(from, to, styleClasses);
    }
}
