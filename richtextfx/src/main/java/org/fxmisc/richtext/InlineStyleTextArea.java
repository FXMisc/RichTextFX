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
public class InlineStyleTextArea<S, PS> extends StyledTextArea<S, PS> {

    /**
     *
     * @param initialStyle style to use for text ranges where no other
     *     style is set via {@code setStyle(...)} methods.
     * @param styleToCss function that converts an instance of {@code S}
     *     to a CSS string.
     */
    public  InlineStyleTextArea(S initialStyle, Function<S, String> styleToCss, PS initialParagraphStyle, Function<PS, String> paragraphStyleToCss) {
        super(initialStyle,
                (text, style) -> text.setStyle(styleToCss.apply(style)),
                initialParagraphStyle,
                (paragraph, style) -> paragraph.setStyle(paragraphStyleToCss.apply(style)));
    }

}
