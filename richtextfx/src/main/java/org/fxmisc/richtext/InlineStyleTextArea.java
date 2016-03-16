package org.fxmisc.richtext;

import java.util.function.Function;

/**
 * Text area that uses inline css derived from the style info to define
 * style of text segments.
 *
 * @param <S> type of style information.
 * @deprecated
 */
@Deprecated
public class InlineStyleTextArea<PS, S, Model extends StyledTextAreaModel<PS, S>> extends StyledTextArea<PS, S, Model> {

    public InlineStyleTextArea(Function<PS, String> paragraphStyleToCss, Function<S, String> styleToCss, Model model) {
        super((paragraph, style) -> paragraph.setStyle(paragraphStyleToCss.apply(style)),
                (text, style) -> text.setStyle(styleToCss.apply(style)), model
        );
    }

}
