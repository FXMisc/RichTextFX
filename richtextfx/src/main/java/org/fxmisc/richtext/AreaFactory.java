package org.fxmisc.richtext;

import javafx.scene.text.TextFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.util.function.BiConsumer;

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

    // StyledTextArea 1 (Base)
    public static <PS, S> StyledTextArea<PS, S, StyledTextAreaModel<PS, S>> styledTextArea(
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
            S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle,
            boolean preserveStyle
    ) {
        return new StyledTextArea<>(applyParagraphStyle, applyStyle,
                new StyledTextAreaModel<>(initialParagraphStyle, initialTextStyle, preserveStyle));
    }

    // Embeds StyledTextArea 1
    public static <PS, S>VirtualizedScrollPane<StyledTextArea<PS, S, StyledTextAreaModel<PS, S>>> embeddedStyledTextArea(
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
            S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle, boolean preserveStyle
    ) {
        return new VirtualizedScrollPane<>(styledTextArea(initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle, preserveStyle));
    }

    // StyledTextArea 2
    public static <PS, S> StyledTextArea<PS, S, StyledTextAreaModel<PS, S>> styledTextArea(
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
            S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle
    ) {
        return styledTextArea(initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle, true);
    }

    // Embeds StyledTextArea 2
    public static <PS, S>VirtualizedScrollPane<StyledTextArea<PS, S, StyledTextAreaModel<PS, S>>> embeddedStyledTextArea(
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
            S initialTextStyle, BiConsumer<? super TextExt, S> applyStyle
    ) {
        return new VirtualizedScrollPane<>(styledTextArea(initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle));
    }

    // Clones StyledTextArea
    public static <PS, S> StyledTextArea<PS, S, StyledTextAreaModel<PS, S>> cloneStyleTextArea(
            StyledTextArea<PS, S, StyledTextAreaModel<PS, S>> area
    ) {
        return new StyledTextArea<>(area.getApplyParagraphStyle(),area.getApplyStyle(),
                new StyledTextAreaModel<>(area.getModel())
        );
    }

    // Embeds cloned StyledTextArea
    public static <PS, S> VirtualizedScrollPane<StyledTextArea<PS, S, StyledTextAreaModel<PS, S>>> embeddedClonedStyledTextArea(
            StyledTextArea<PS, S, StyledTextAreaModel<PS, S>> area) {
        return new VirtualizedScrollPane<>(cloneStyleTextArea(area));
    }

    // Embeds a cloned StyledTextArea from an embedded StyledTextArea
    public static <PS, S>VirtualizedScrollPane<StyledTextArea<PS, S, StyledTextAreaModel<PS, S>>> embeddedClonedStyledTextArea(
            VirtualizedScrollPane<StyledTextArea<PS, S, StyledTextAreaModel<PS, S>>> virtualizedScrollPaneWithArea
    ) {
        return embeddedClonedStyledTextArea(virtualizedScrollPaneWithArea.getContent());
    }

    /* ********************************************************************** *
     *                                                                        *
     * StyleClassedTextArea                                                   *
     *                                                                        *
     * ********************************************************************** */

    // StyleClassedTextArea 1 (Base)
    public static StyleClassedTextArea<StyleClassedTextAreaModel> styleClassedTextArea(boolean preserveStyle) {
        return new StyleClassedTextArea<>(new StyleClassedTextAreaModel(preserveStyle));
    }

    // Embeds StyleClassedTextArea  1
    public static VirtualizedScrollPane<StyleClassedTextArea> embeddedStyleClassedTextArea(boolean preserveStyle) {
        return new VirtualizedScrollPane<>(styleClassedTextArea(preserveStyle));
    }

    // StyleClassedTextArea  2
    public static StyleClassedTextArea<StyleClassedTextAreaModel> styleClassedTextArea() {
        return styleClassedTextArea(true);
    }

    // Embeds StyleClassedTextArea  2
    public static VirtualizedScrollPane<StyleClassedTextArea<StyleClassedTextAreaModel>> embeddedStyleClassedTextArea() {
        return new VirtualizedScrollPane<>(styleClassedTextArea());
    }

    // Clones StyleClassedTextArea
    public static StyleClassedTextArea<StyleClassedTextAreaModel> cloneStyleClassedTextArea(
            StyleClassedTextArea<StyleClassedTextAreaModel> area
    ) {
        return new StyleClassedTextArea<>(new StyleClassedTextAreaModel(area.getModel()));
    }

    // Embeds cloned StyleClassedTextArea
    public static VirtualizedScrollPane<StyleClassedTextArea<StyleClassedTextAreaModel>> embeddedClonedStyleClassedTextArea(
            StyleClassedTextArea<StyleClassedTextAreaModel> area
    ) {
        return new VirtualizedScrollPane<>(cloneStyleClassedTextArea(area));
    }

    // Embeds a cloned StyleClassedTextArea from an embedded StyleClassedTextArea
    public static VirtualizedScrollPane<StyleClassedTextArea<StyleClassedTextAreaModel>> embeddedClonedStyleClassedTextArea(
            VirtualizedScrollPane<StyleClassedTextArea<StyleClassedTextAreaModel>> virtualizedScrollPaneWithArea
    ) {
        return embeddedClonedStyleClassedTextArea(virtualizedScrollPaneWithArea.getContent());
    }

    /* ********************************************************************** *
     *                                                                        *
     * CodeArea                                                               *
     *                                                                        *
     * ********************************************************************** */

    // CodeArea 1
    public static CodeArea<CodeAreaModel> codeArea() {
        return new CodeArea<>(new CodeAreaModel());
    }

    // Embeds CodeArea 1
    public static VirtualizedScrollPane<CodeArea<CodeAreaModel>> embeddedCodeArea() {
        return new VirtualizedScrollPane<>(codeArea());
    }

    // CodeArea 2
    public static CodeArea<CodeAreaModel> codeArea(String text) {
        return new CodeArea<>(new CodeAreaModel(text));
    }

    // Embeds CodeArea 2
    public static VirtualizedScrollPane<CodeArea> embeddedCodeArea(String text) {
        return new VirtualizedScrollPane<>(codeArea(text));
    }

    // Clones CodeArea
    public static CodeArea<CodeAreaModel> cloneCodeArea(CodeArea<CodeAreaModel> area) {
        return new CodeArea<>(new CodeAreaModel(area.getModel()));
    }

    // Embeds a cloned CodeArea
    public static VirtualizedScrollPane<CodeArea<CodeAreaModel>> embeddedClonedCodeArea(
            CodeArea<CodeAreaModel> area
    ) {
        return new VirtualizedScrollPane<>(cloneCodeArea(area));
    }

    // Embeds a cloned CodeArea from an embedded CodeArea
    public static VirtualizedScrollPane<CodeArea<CodeAreaModel>> embeddedClonedCodeArea(
            VirtualizedScrollPane<CodeArea<CodeAreaModel>> virtualizedScrollPaneWithArea
    ) {
        return new VirtualizedScrollPane<>(cloneCodeArea(virtualizedScrollPaneWithArea.getContent()));
    }

    /* ********************************************************************** *
     *                                                                        *
     * InlineCssTextArea                                                      *
     *                                                                        *
     * ********************************************************************** */

    // InlineCssTextArea 1
    public static InlineCssTextArea<InlineCssTextAreaModel> inlineCssTextArea() {
        return new InlineCssTextArea<>(new InlineCssTextAreaModel());
    }

    // Embeds InlineCssTextArea 1
    public static VirtualizedScrollPane<InlineCssTextArea<InlineCssTextAreaModel>> embeddedInlineCssTextArea() {
        return new VirtualizedScrollPane<>(inlineCssTextArea());
    }

    // InlineCssTextArea 2
    public static InlineCssTextArea<InlineCssTextAreaModel> inlineCssTextArea(String text) {
        return new InlineCssTextArea<>(new InlineCssTextAreaModel(text));
    }

    // Embeds InlineCssTextArea 2
    public static VirtualizedScrollPane<InlineCssTextArea<InlineCssTextAreaModel>> embeddedInlineCssTextArea(String text) {
        return new VirtualizedScrollPane<>(inlineCssTextArea(text));
    }

    // Clones InlineCssTextArea
    public static InlineCssTextArea<InlineCssTextAreaModel> cloneInlineCssTextArea(InlineCssTextArea<InlineCssTextAreaModel> area) {
        return new InlineCssTextArea<>(new InlineCssTextAreaModel(area.getModel()));
    }

    // Embeds a cloned InlineCssTextArea
    public static VirtualizedScrollPane<InlineCssTextArea<InlineCssTextAreaModel>> embeddedClonedInlineCssTextArea(InlineCssTextArea<InlineCssTextAreaModel> area) {
        return new VirtualizedScrollPane<>(cloneInlineCssTextArea(area));
    }

    // Embeds a cloned InlineCssTextArea from an embedded InlineCssTextArea
    public static VirtualizedScrollPane<InlineCssTextArea<InlineCssTextAreaModel>> embeddedClonedInlineCssTextArea(
            VirtualizedScrollPane<InlineCssTextArea<InlineCssTextAreaModel>> virtualizedScrollPaneWithArea
    ) {
        return new VirtualizedScrollPane<>(cloneInlineCssTextArea(virtualizedScrollPaneWithArea.getContent()));
    }
}
