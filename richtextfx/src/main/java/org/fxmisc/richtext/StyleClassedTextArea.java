package org.fxmisc.richtext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Text area that uses style classes to define style of text segments and paragraph segments.
 */
public class StyleClassedTextArea<Model extends StyleClassedTextAreaModel> extends StyledTextArea<Collection<String>, Collection<String>, Model> {

    public StyleClassedTextArea(Model model) {
        super((paragraph, styleClasses) -> paragraph.getStyleClass().addAll(styleClasses),
              (text, styleClasses) -> text.getStyleClass().addAll(styleClasses), model
        );

        setStyleCodecs(
                SuperCodec.upCast(SuperCodec.collectionListCodec(Codec.STRING_CODEC)),
                SuperCodec.upCast(SuperCodec.collectionListCodec(Codec.STRING_CODEC))
        );
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
