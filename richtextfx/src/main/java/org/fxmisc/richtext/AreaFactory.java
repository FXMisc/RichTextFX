package org.fxmisc.richtext;

import javafx.scene.text.TextFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * AreaFactory is a convenience class used to create StyledTextArea
 * and any of its subclasses. and optionally embed them
 * into a {@link VirtualizedScrollPane}.
 *
 */
public class AreaFactory {
    public static <S, PS> StyledTextArea<S, PS> styledTextArea(
            S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle, boolean preserveStyle
    ) {
        return new StyledTextArea<S, PS>(initialStyle, applyStyle, initialParagraphStyle, applyParagraphStyle, preserveStyle);
    }

    public static <S, PS> StyledTextArea<S, PS> styledTextArea(
            S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle
    ) {
        return new StyledTextArea<S, PS>(initialStyle, applyStyle, initialParagraphStyle, applyParagraphStyle, true);
    }

    public static <S, PS>VirtualizedScrollPane<StyledTextArea<S, PS>> embeddedStyledTextArea(
            S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle, boolean preserveStyle
    ) {
        return new VirtualizedScrollPane<>(styledTextArea(initialStyle, applyStyle, initialParagraphStyle, applyParagraphStyle, preserveStyle));
    }

    public static <S, PS>VirtualizedScrollPane<StyledTextArea<S, PS>> embeddedStyledTextArea(
            S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle
    ) {
        return new VirtualizedScrollPane<>(styledTextArea(initialStyle, applyStyle, initialParagraphStyle, applyParagraphStyle));
    }

    public static StyleClassedTextArea styleClassedTextArea(boolean preserveStyle) {
        return new StyleClassedTextArea(preserveStyle);
    }

    public static StyleClassedTextArea styleClassedTextArea() {
        return styleClassedTextArea(true);
    }

    public static VirtualizedScrollPane<StyleClassedTextArea> embeddedStyleClassedTextArea(boolean preserveStyle) {
        return new VirtualizedScrollPane<>(styleClassedTextArea(preserveStyle));
    }

    public static VirtualizedScrollPane<StyleClassedTextArea> embeddedStyleClassedTextArea() {
        return new VirtualizedScrollPane<>(styleClassedTextArea());
    }

    public static CodeArea codeArea() {
        return new CodeArea();
    }

    public static CodeArea codeArea(String text) {
        return new CodeArea(text);
    }

    public static VirtualizedScrollPane<CodeArea> embeddedCodeArea() {
        return new VirtualizedScrollPane<>(codeArea());
    }

    public static VirtualizedScrollPane<CodeArea> embeddedCodeArea(String text) {
        return new VirtualizedScrollPane<>(codeArea(text));
    }

    public static <S, PS> InlineStyleTextArea<S, PS> inlineStyleTextArea(
            S initialStyle, Function<S, String> styleToCss, PS initialParagraphStyle, Function<PS, String> paragraphStyleToCss
    ) {
        return new InlineStyleTextArea<>(initialStyle, styleToCss, initialParagraphStyle, paragraphStyleToCss);
    }

    public static <S, PS> VirtualizedScrollPane<InlineStyleTextArea<S, PS>> embeddedInlineStyleTextArea(
            S initialStyle, Function<S, String> styleToCss, PS initialParagraphStyle, Function<PS, String> paragraphStyleToCss
    ) {
        return new VirtualizedScrollPane<>(inlineStyleTextArea(initialStyle, styleToCss, initialParagraphStyle, paragraphStyleToCss));
    }

    public static InlineCssTextArea inlineCssTextArea() {
        return new InlineCssTextArea();
    }

    public static InlineCssTextArea inlineCssTextArea(String text) {
        return new InlineCssTextArea(text);
    }

    public static VirtualizedScrollPane<InlineCssTextArea> embeddedInlineCssTextArea() {
        return new VirtualizedScrollPane<>(inlineCssTextArea());
    }

    public static VirtualizedScrollPane<InlineCssTextArea> embeddedInlineCssTextArea(String text) {
        return new VirtualizedScrollPane<>(inlineCssTextArea(text));
    }
}
