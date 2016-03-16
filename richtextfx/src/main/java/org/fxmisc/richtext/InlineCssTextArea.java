package org.fxmisc.richtext;


import javafx.scene.text.TextFlow;

/**
 * Text area that uses inline css to define style of text segments and paragraph segments.
 */
public class InlineCssTextArea<Model extends InlineCssTextAreaModel> extends StyledTextArea<String, String, Model> {

    public InlineCssTextArea(Model model) {
        super(TextFlow::setStyle, TextExt::setStyle, model);

        setStyleCodecs(Codec.STRING_CODEC, Codec.STRING_CODEC);
    }

}
