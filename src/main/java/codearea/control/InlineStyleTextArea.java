package codearea.control;

import java.util.function.Function;

/**
 * Text area that uses inline css derived from the style info to define
 * style of text segments.
 *
 * @param <S> type of style information.
 */
public class InlineStyleTextArea<S> extends StyledTextArea<S> {

    public  InlineStyleTextArea(S initialStyle, Function<S, String> styleToCss) {
        super(initialStyle, (text, style) -> text.setStyle(styleToCss.apply(style)));
    }

}
