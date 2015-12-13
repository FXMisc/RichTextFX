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

    /* ********************************************************************** *
     *                                                                        *
     * StyledTextArea                                                         *
     *                                                                        *
     * ********************************************************************** */

    // StyledTextArea 1
    public static <S, PS> StyledTextArea<S, PS> styledTextArea(
            S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle
    ) {
        return new StyledTextArea<S, PS>(
                initialStyle, applyStyle,
                initialParagraphStyle, applyParagraphStyle,
                true);
    }

    // Embeds StyledTextArea 1
    public static <S, PS>VirtualizedScrollPane<StyledTextArea<S, PS>> embeddedStyledTextArea(
            S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle
    ) {
        return new VirtualizedScrollPane<>(styledTextArea(initialStyle, applyStyle, initialParagraphStyle, applyParagraphStyle));
    }

    // StyledTextArea 2
    public static <S, PS> StyledTextArea<S, PS> styledTextArea(
            S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
            boolean preserveStyle
    ) {
        return new StyledTextArea<S, PS>(
                initialStyle, applyStyle,
                initialParagraphStyle, applyParagraphStyle,
                preserveStyle);
    }

    // Embeds StyledTextArea 2
    public static <S, PS>VirtualizedScrollPane<StyledTextArea<S, PS>> embeddedStyledTextArea(
            S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle, boolean preserveStyle
    ) {
        return new VirtualizedScrollPane<>(styledTextArea(initialStyle, applyStyle, initialParagraphStyle, applyParagraphStyle, preserveStyle));
    }

    // StyledTextArea 3 (Clone of area)
    public static <S, PS> StyledTextArea<S, PS> cloneStyleTextArea(StyledTextArea<S, PS> area) {
        return new StyledTextArea<S, PS>(area.getInitialStyle(), area.getApplyStyle(),
                area.getInitialParagraphStyle(), area.getApplyParagraphStyle(),
                area.getCloneDocument(), area.isPreserveStyle());
    }

    // Embeds StyledTextArea 3
    public static <S, PS>VirtualizedScrollPane<StyledTextArea<S, PS>> embeddedClonedStyledTextArea(StyledTextArea<S, PS> area) {
        return new VirtualizedScrollPane<>(cloneStyleTextArea(area));
    }

    // Embeds StyledTextArea 3 using an area that is itself embedded in a VirtualizedScrollPane
    public static <S, PS>VirtualizedScrollPane<StyledTextArea<S, PS>> embeddedClonedStyledTextArea(
            VirtualizedScrollPane<StyledTextArea<S, PS>> virtualizedScrollPaneWithArea
    ) {
        return embeddedClonedStyledTextArea(virtualizedScrollPaneWithArea.getContent());
    }

    // refactor: Remove this as EditableStyledDocument is not public so nothing outside of RichTextFX can use it
    public static <S, PS> StyledTextArea<S, PS> styledTextArea(
            S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
            EditableStyledDocument<S, PS> document, boolean preserveStyle
    ) {
        return new StyledTextArea<S, PS>(
                initialStyle, applyStyle,
                initialParagraphStyle, applyParagraphStyle,
                document, preserveStyle
        );
    }

    /* ********************************************************************** *
     *                                                                        *
     * StyleClassedTextArea                                                   *
     *                                                                        *
     * ********************************************************************** */

    // StyleClassedTextArea 1
    public static StyleClassedTextArea styleClassedTextArea(boolean preserveStyle) {
        return new StyleClassedTextArea(preserveStyle);
    }

    // Embeds StyleClassedTextArea  1
    public static VirtualizedScrollPane<StyleClassedTextArea> embeddedStyleClassedTextArea(boolean preserveStyle) {
        return new VirtualizedScrollPane<>(styleClassedTextArea(preserveStyle));
    }

    // StyleClassedTextArea  2
    public static StyleClassedTextArea styleClassedTextArea() {
        return styleClassedTextArea(true);
    }

    // Embeds StyleClassedTextArea  2
    public static VirtualizedScrollPane<StyleClassedTextArea> embeddedStyleClassedTextArea() {
        return new VirtualizedScrollPane<>(styleClassedTextArea());
    }

    // Clones StyleClassedTextArea
    public static StyleClassedTextArea cloneStyleClassedTextArea(StyleClassedTextArea area) {
        return new StyleClassedTextArea(area.getCloneDocument(), area.isPreserveStyle());
    }

    // Embeds StyleClassedTextArea
    public static VirtualizedScrollPane<StyleClassedTextArea> embeddedClonedStyleClassedTextArea(StyleClassedTextArea area) {
        return new VirtualizedScrollPane<>(cloneStyleClassedTextArea(area));
    }

    // Embeds a cloned StyleClassedTextArea from an embedded StyleClassedTextArea
    public static VirtualizedScrollPane<StyleClassedTextArea> embeddedClonedStyleClassedTextArea(
            VirtualizedScrollPane<StyleClassedTextArea> virtualizedScrollPaneWithArea
    ) {
        return embeddedClonedStyleClassedTextArea(virtualizedScrollPaneWithArea.getContent());
    }

    /* ********************************************************************** *
     *                                                                        *
     * CodeArea                                                               *
     *                                                                        *
     * ********************************************************************** */

    // CodeArea 1
    public static CodeArea codeArea() {
        return new CodeArea();
    }

    // Embeds CodeArea 1
    public static VirtualizedScrollPane<CodeArea> embeddedCodeArea() {
        return new VirtualizedScrollPane<>(codeArea());
    }

    // CodeArea 2
    public static CodeArea codeArea(String text) {
        return new CodeArea(text);
    }

    // Embeds CodeArea 2
    public static VirtualizedScrollPane<CodeArea> embeddedCodeArea(String text) {
        return new VirtualizedScrollPane<>(codeArea(text));
    }

    // Clones CodeArea
    public static CodeArea cloneCodeArea(CodeArea area) {
        return new CodeArea(area.getCloneDocument());
    }

    // Embeds a cloned CodeArea
    public static VirtualizedScrollPane<CodeArea> embeddedClonedCodeArea(CodeArea area) {
        return new VirtualizedScrollPane<>(cloneCodeArea(area));
    }

    // Embeds a cloned CodeArea from an embedded CodeArea
    public static VirtualizedScrollPane<CodeArea> embeddedClonedCodeArea(VirtualizedScrollPane<CodeArea> virtualizedScrollPaneWithArea) {
        return new VirtualizedScrollPane<>(cloneCodeArea(virtualizedScrollPaneWithArea.getContent()));
    }



    // Refactor: Since InlineStyleTextArea was deprecated, remove this
    /**
     * Creates a text area that uses inline css derived from the style info to define
     * style of text segments.
     *
     * @param <S> type of text style information.
     * @param <PS> type of paragraph style information.
     * @param initialStyle style to use for text ranges where no other
     *     style is set via {@code setStyle(...)} methods.
     * @param styleToCss function that converts an instance of {@code S}
     *     to a CSS string.
     */
    public static <S, PS> StyledTextArea<S, PS> inlineStyleTextArea(
            S initialStyle, Function<S, String> styleToCss, PS initialParagraphStyle, Function<PS, String> paragraphStyleToCss
    ) {
        return styledTextArea(
                initialStyle,
                (text, style) -> text.setStyle(styleToCss.apply(style)),
                initialParagraphStyle,
                (paragraph, style) -> paragraph.setStyle(paragraphStyleToCss.apply(style)));
    }

    public static <S, PS> VirtualizedScrollPane<StyledTextArea<S, PS>> embeddedInlineStyleTextArea(
            S initialStyle, Function<S, String> styleToCss, PS initialParagraphStyle, Function<PS, String> paragraphStyleToCss
    ) {
        return new VirtualizedScrollPane<>(inlineStyleTextArea(initialStyle, styleToCss, initialParagraphStyle, paragraphStyleToCss));
    }

    /* ********************************************************************** *
     *                                                                        *
     * InlineCssTextArea                                                      *
     *                                                                        *
     * ********************************************************************** */

    // InlineCssTextArea 1
    public static InlineCssTextArea inlineCssTextArea() {
        return new InlineCssTextArea();
    }

    // Embeds InlineCssTextArea 1
    public static VirtualizedScrollPane<InlineCssTextArea> embeddedInlineCssTextArea() {
        return new VirtualizedScrollPane<>(inlineCssTextArea());
    }

    // InlineCssTextArea 2
    public static InlineCssTextArea inlineCssTextArea(String text) {
        return new InlineCssTextArea(text);
    }

    // Embeds InlineCssTextArea 2
    public static VirtualizedScrollPane<InlineCssTextArea> embeddedInlineCssTextArea(String text) {
        return new VirtualizedScrollPane<>(inlineCssTextArea(text));
    }

    public static InlineCssTextArea cloneInlineCssTextArea(InlineCssTextArea area) {
        return new InlineCssTextArea(area.getCloneDocument());
    }

    public static VirtualizedScrollPane<InlineCssTextArea> embeddedClonedInlineCssTextArea(InlineCssTextArea area) {
        return new VirtualizedScrollPane<>(cloneInlineCssTextArea(area));
    }

    public static VirtualizedScrollPane<InlineCssTextArea> embeddedClonedInlineCssTextArea(
            VirtualizedScrollPane<InlineCssTextArea> virtualizedScrollPaneWithArea
    ) {
        return new VirtualizedScrollPane<>(cloneInlineCssTextArea(virtualizedScrollPaneWithArea.getContent()));
    }
}
