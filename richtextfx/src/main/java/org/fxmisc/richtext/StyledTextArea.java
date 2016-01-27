package org.fxmisc.richtext;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.function.BiConsumer;

/**
 * Created by jordan on 1/26/16.
 */
public class StyledTextArea<PS, S> extends StyledTextAreaBase<PS, S, StyledTextAreaModel<PS, S>> {

    /**
     * Creates a text area with empty text content.
     *
     * @param initialTextStyle style to use in places where no other style is
     * specified (yet).
     * @param applyStyle function that, given a {@link Text} node and
     * a style, applies the style to the text node. This function is
     * used by the default skin to apply style to text nodes.
     * @param initialParagraphStyle style to use in places where no other style is
     * specified (yet).
     * @param applyParagraphStyle function that, given a {@link TextFlow} node and
     * a style, applies the style to the paragraph node. This function is
     * used by the default skin to apply style to paragraph nodes.
     */
    public StyledTextArea(PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
                              S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle
    ) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle, true);
    }

    public StyledTextArea(PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
                              S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle,
                              boolean preserveStyle
    ) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle,
                new EditableStyledDocument<>(initialParagraphStyle, initialTextStyle), preserveStyle);
    }

    /**
     * The same as {@link #StyledTextArea(Object, BiConsumer, Object, BiConsumer)} except that
     * this constructor can be used to create another {@code StyledTextArea} object that
     * shares the same {@link EditableStyledDocument}.
     */
    public StyledTextArea(PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
                              S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle,
                              EditableStyledDocument<PS, S> document
    ) {
        this(initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle, document, true);

    }

    public StyledTextArea(PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
                              S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle,
                              EditableStyledDocument<PS, S> document, boolean preserveStyle
    ) {
        super(applyParagraphStyle, applyStyle, new StyledTextAreaModel<>(initialParagraphStyle, initialTextStyle, document, preserveStyle));
    }
}
