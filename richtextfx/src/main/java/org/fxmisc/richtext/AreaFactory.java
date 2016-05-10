package org.fxmisc.richtext;

import javafx.scene.text.TextFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * AreaFactory is a convenience class used to create StyledTextArea
 * and any of its subclasses. and optionally embed them
 * into a {@link VirtualizedScrollPane}.
 */
public class AreaFactory {

    /*
        Area
            new instance
            new instance; embedded
            new instance with modifier; embedded
            (repeat for all non-clone constructors)

            clone
            embed a clone of area
            embed a clone of area with modifier
            embed a clone of area that is itself embedded
            embed a clone of area that is itself embedded with modifier
     */

    /* ********************************************************************** *
     *                                                                        *
     * StyledTextArea                                                         *
     *                                                                        *
     * ********************************************************************** */

    public static <PS, S> StyledTextArea<PS, S> styledTextArea(
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
            S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle) {
        return new StyledTextArea<>(initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle);
    }

    public static <PS, S> VirtualizedScrollPane<StyledTextArea<PS, S>> embeddedStyledTextArea(
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
            S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle
    ) {
        return new VirtualizedScrollPane<>(styledTextArea(initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle));
    }

    public static <PS, S> VirtualizedScrollPane<StyledTextArea<PS, S>> embeddedStyledTextArea(
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
            S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle,
            Consumer<StyledTextArea<PS, S>> modifier
    ) {
        StyledTextArea<PS, S> area = styledTextArea(initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle);
        modifier.accept(area);
        return new VirtualizedScrollPane<>(area);
    }

    public static <PS, S> StyledTextArea<PS, S> styledTextArea(
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
            S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle,
            boolean preserveStyle
    ) {
        return new StyledTextArea<>(initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle, preserveStyle);
    }

    public static <PS, S> VirtualizedScrollPane<StyledTextArea<PS, S>> embeddedStyledTextArea(
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
            S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle,
            boolean preserveStyle
    ) {
        return new VirtualizedScrollPane<>(styledTextArea(initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle, preserveStyle));
    }

    public static <PS, S> VirtualizedScrollPane<StyledTextArea<PS, S>> embeddedStyledTextArea(
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
            S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle,
            boolean preserveStyle, Consumer<StyledTextArea<PS, S>> modifier
    ) {
        StyledTextArea<PS, S> area = styledTextArea(initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle, preserveStyle);
        modifier.accept(area);
        return new VirtualizedScrollPane<>(area);
    }

    public static <PS, S> StyledTextArea<PS, S> cloneStyleTextArea(StyledTextArea<PS, S> area) {
        return new StyledTextArea<>(area.getInitialParagraphStyle(), area.getApplyParagraphStyle(),
                area.getInitialTextStyle(), area.getApplyStyle(),
                area.getContent(), area.isPreserveStyle());
    }

    public static <PS, S> VirtualizedScrollPane<StyledTextArea<PS, S>> embeddedClonedStyledTextArea(StyledTextArea<PS, S> area) {
        return new VirtualizedScrollPane<>(cloneStyleTextArea(area));
    }

    public static <PS, S> VirtualizedScrollPane<StyledTextArea<PS, S>> embeddedClonedStyledTextArea(StyledTextArea<PS, S> area,
                                                                                                    Consumer<StyledTextArea<PS, S>> modifier) {
        StyledTextArea<PS, S> clone = cloneStyleTextArea(area);
        modifier.accept(clone);
        return new VirtualizedScrollPane<>(clone);
    }

    public static <PS, S> VirtualizedScrollPane<StyledTextArea<PS, S>> embeddedClonedStyledTextArea(
            VirtualizedScrollPane<StyledTextArea<PS, S>> virtualizedScrollPaneWithArea
    ) {
        return embeddedClonedStyledTextArea(virtualizedScrollPaneWithArea.getContent());
    }

    public static <PS, S> VirtualizedScrollPane<StyledTextArea<PS, S>> embeddedClonedStyledTextArea(
            VirtualizedScrollPane<StyledTextArea<PS, S>> virtualizedScrollPaneWithArea,
            Consumer<StyledTextArea<PS, S>> modifier
    ) {
        return embeddedClonedStyledTextArea(virtualizedScrollPaneWithArea.getContent(), modifier);
    }

    /* ********************************************************************** *
     *                                                                        *
     * StyleClassedTextArea                                                   *
     *                                                                        *
     * ********************************************************************** */

    public static StyleClassedTextArea styleClassedTextArea(boolean preserveStyle) {
        return new StyleClassedTextArea(preserveStyle);
    }

    public static VirtualizedScrollPane<StyleClassedTextArea> embeddedStyleClassedTextArea(boolean preserveStyle) {
        return new VirtualizedScrollPane<>(styleClassedTextArea(preserveStyle));
    }

    public static VirtualizedScrollPane<StyleClassedTextArea> embeddedStyleClassedTextArea(boolean preserveStyle, Consumer<StyleClassedTextArea> modifier) {
        StyleClassedTextArea area = styleClassedTextArea(preserveStyle);
        modifier.accept(area);
        return new VirtualizedScrollPane<>(area);
    }

    public static StyleClassedTextArea styleClassedTextArea() {
        return new StyleClassedTextArea();
    }

    public static VirtualizedScrollPane<StyleClassedTextArea> embeddedStyleClassedTextArea() {
        return new VirtualizedScrollPane<>(styleClassedTextArea());
    }

    public static VirtualizedScrollPane<StyleClassedTextArea> embeddedStyleClassedTextArea(Consumer<StyleClassedTextArea> modifier) {
        StyleClassedTextArea area = styleClassedTextArea();
        modifier.accept(area);
        return new VirtualizedScrollPane<>(area);
    }

    public static StyleClassedTextArea cloneStyleClassedTextArea(StyleClassedTextArea area) {
        return new StyleClassedTextArea(area.getContent(), area.isPreserveStyle());
    }

    public static VirtualizedScrollPane<StyleClassedTextArea> embeddedClonedStyleClassedTextArea(StyleClassedTextArea area) {
        return new VirtualizedScrollPane<>(cloneStyleClassedTextArea(area));
    }

    public static VirtualizedScrollPane<StyleClassedTextArea> embeddedClonedStyleClassedTextArea(
            StyleClassedTextArea area, Consumer<StyleClassedTextArea> modifier) {
        StyleClassedTextArea clone = cloneStyleClassedTextArea(area);
        modifier.accept(clone);
        return new VirtualizedScrollPane<>(clone);
    }

    public static VirtualizedScrollPane<StyleClassedTextArea> embeddedClonedStyleClassedTextArea(
            VirtualizedScrollPane<StyleClassedTextArea> virtualizedScrollPaneWithArea
    ) {
        return embeddedClonedStyleClassedTextArea(virtualizedScrollPaneWithArea.getContent());
    }

    public static VirtualizedScrollPane<StyleClassedTextArea> embeddedClonedStyleClassedTextArea(
            VirtualizedScrollPane<StyleClassedTextArea> virtualizedScrollPaneWithArea,
            Consumer<StyleClassedTextArea> modifier
    ) {
        return embeddedClonedStyleClassedTextArea(virtualizedScrollPaneWithArea.getContent(), modifier);
    }

    /* ********************************************************************** *
     *                                                                        *
     * CodeArea                                                               *
     *                                                                        *
     * ********************************************************************** */

    public static CodeArea codeArea() {
        return new CodeArea();
    }

    public static VirtualizedScrollPane<CodeArea> embeddedCodeArea() {
        return new VirtualizedScrollPane<>(codeArea());
    }

    public static VirtualizedScrollPane<CodeArea> embeddedCodeArea(Consumer<CodeArea> modifier) {
        CodeArea area = codeArea();
        modifier.accept(area);
        return new VirtualizedScrollPane<>(area);
    }

    public static CodeArea codeArea(String text) {
        return new CodeArea(text);
    }

    public static VirtualizedScrollPane<CodeArea> embeddedCodeArea(String text) {
        return new VirtualizedScrollPane<>(codeArea(text));
    }

    public static VirtualizedScrollPane<CodeArea> embeddedCodeArea(String text, Consumer<CodeArea> modifier) {
        CodeArea area = codeArea(text);
        modifier.accept(area);
        return new VirtualizedScrollPane<>(area);
    }

    public static CodeArea cloneCodeArea(CodeArea area) {
        return new CodeArea(area.getContent());
    }

    public static VirtualizedScrollPane<CodeArea> embeddedClonedCodeArea(CodeArea area) {
        return new VirtualizedScrollPane<>(cloneCodeArea(area));
    }

    public static VirtualizedScrollPane<CodeArea> embeddedClonedCodeArea(CodeArea area, Consumer<CodeArea> modifier) {
        CodeArea clone = cloneCodeArea(area);
        modifier.accept(clone);
        return new VirtualizedScrollPane<>(clone);
    }

    public static VirtualizedScrollPane<CodeArea> embeddedClonedCodeArea(
            VirtualizedScrollPane<CodeArea> virtualizedScrollPaneWithArea) {
        return embeddedClonedCodeArea(virtualizedScrollPaneWithArea.getContent());
    }

    public static VirtualizedScrollPane<CodeArea> embeddedClonedCodeArea(
            VirtualizedScrollPane<CodeArea> virtualizedScrollPaneWithArea, Consumer<CodeArea> modifier) {
        return embeddedClonedCodeArea(virtualizedScrollPaneWithArea.getContent(), modifier);
    }

    /* ********************************************************************** *
     *                                                                        *
     * InlineCssTextArea                                                      *
     *                                                                        *
     * ********************************************************************** */

    public static InlineCssTextArea inlineCssTextArea() {
        return new InlineCssTextArea();
    }

    public static VirtualizedScrollPane<InlineCssTextArea> embeddedInlineCssTextArea() {
        return new VirtualizedScrollPane<>(inlineCssTextArea());
    }

    public static VirtualizedScrollPane<InlineCssTextArea> embeddedInlineCssTextArea(Consumer<InlineCssTextArea> modifier) {
        InlineCssTextArea area = inlineCssTextArea();
        modifier.accept(area);
        return new VirtualizedScrollPane<>(area);
    }

    public static InlineCssTextArea inlineCssTextArea(String text) {
        return new InlineCssTextArea(text);
    }

    public static VirtualizedScrollPane<InlineCssTextArea> embeddedInlineCssTextArea(String text) {
        return new VirtualizedScrollPane<>(inlineCssTextArea(text));
    }

    public static VirtualizedScrollPane<InlineCssTextArea> embeddedInlineCssTextArea(String text, Consumer<InlineCssTextArea> modifier) {
        InlineCssTextArea area = inlineCssTextArea(text);
        modifier.accept(area);
        return new VirtualizedScrollPane<>(area);
    }

    public static InlineCssTextArea cloneInlineCssTextArea(InlineCssTextArea area) {
        return new InlineCssTextArea(area.getContent());
    }

    public static VirtualizedScrollPane<InlineCssTextArea> embeddedClonedInlineCssTextArea(InlineCssTextArea area) {
        return new VirtualizedScrollPane<>(cloneInlineCssTextArea(area));
    }

    public static VirtualizedScrollPane<InlineCssTextArea> embeddedClonedInlineCssTextArea(InlineCssTextArea area, Consumer<InlineCssTextArea> modifier) {
        InlineCssTextArea clone = cloneInlineCssTextArea(area);
        modifier.accept(clone);
        return new VirtualizedScrollPane<>(cloneInlineCssTextArea(area));
    }

    public static VirtualizedScrollPane<InlineCssTextArea> embeddedClonedInlineCssTextArea(
            VirtualizedScrollPane<InlineCssTextArea> virtualizedScrollPaneWithArea
    ) {
        return new VirtualizedScrollPane<>(cloneInlineCssTextArea(virtualizedScrollPaneWithArea.getContent()));
    }

    public static VirtualizedScrollPane<InlineCssTextArea> embeddedClonedInlineCssTextArea(
            VirtualizedScrollPane<InlineCssTextArea> virtualizedScrollPaneWithArea,
            Consumer<InlineCssTextArea> modifier
    ) {
        return embeddedClonedInlineCssTextArea(virtualizedScrollPaneWithArea.getContent(), modifier);
    }
}
