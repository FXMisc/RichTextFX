package org.fxmisc.richtext;

import javafx.scene.text.TextFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Created by jordan on 11/20/15.
 */
public class AreaFactory {
    // StyledTextArea factory methods
    public static <S, PS> StyledTextArea<S, PS> styledTextArea(
        S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
        PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle,
        boolean preserveStyle) {
        return new StyledTextArea<S, PS>(initialStyle, applyStyle, initialParagraphStyle, applyParagraphStyle, preserveStyle);
    }

    public static <S, PS> StyledTextArea<S, PS> styledTextArea(
            S initialStyle, BiConsumer<? super TextExt, S> applyStyle,
            PS initialParagraphStyle, BiConsumer<TextFlow, PS> applyParagraphStyle) {
        return new StyledTextArea<S, PS>(initialStyle, applyStyle, initialParagraphStyle, applyParagraphStyle, true);
    }

    /**
     * Text area that uses inline css derived from the style info to define
     * style of text segments.
     *
     * @param <S> type of style information for Text within a Paragraph.
     * @param <PS> type of style information for Paragraphs
     */
    public static <S, PS> StyledTextArea<S, PS> inlineStyleTextArea(
            S initialStyle, Function<S, String> styleToCss,
            PS initialParagraphStyle, Function<PS, String> paragraphStyleToCss) {
        return styledTextArea(
                initialStyle,
                (text, style) -> text.setStyle(styleToCss.apply(style)),
                initialParagraphStyle,
                (paragraph, style) -> paragraph.setStyle(paragraphStyleToCss.apply(style))
        );
    }

    /**
     * Creates a text area that uses inline css to define style of text segments and paragraph segments.
     */
    public static StyledTextArea<String, String> inlineCssTextArea() {
        return inlineStyleTextArea("", css -> css, "", css -> css);
    }

    /**
     * {@link #inlineCssTextArea()} but with text content already initialized and
     * caret position set to the begginning of the text content.
     *
     * @param text Initial text content.
     */
    public static StyledTextArea<String, String> inlineCssTextArea(String text) {
        StyledTextArea<String, String> area = inlineCssTextArea();

        area.replaceText(0, 0, text);
        area.getUndoManager().forgetHistory();
        area.getUndoManager().mark();

        area.setStyleCodecs(Codec.STRING_CODEC, Codec.STRING_CODEC);

        // position the caret at the beginning
        area.selectRange(0, 0);
        return area;
    }

    /**
     * Text area that uses style classes to define style of text segments and paragraph segments.
     */
    public static StyledTextArea<Collection<String>, Collection<String>> styleClassedTextArea(boolean preserveStyle) {
        StyledTextArea<Collection<String>, Collection<String>> area = styledTextArea(
                Collections.<String>emptyList(),
                (text, styleClasses) -> text.getStyleClass().addAll(styleClasses),
                Collections.<String>emptyList(),
                (paragraph, styleClasses) -> paragraph.getStyleClass().addAll(styleClasses),
                preserveStyle
        );

        area.setStyleCodecs(
                SuperCodec.upCast(SuperCodec.collectionListCodec(Codec.STRING_CODEC)),
                SuperCodec.upCast(SuperCodec.collectionListCodec(Codec.STRING_CODEC))
        );

        /*
            Note, this convenience method would be lost in this translation:
            /
            * Convenient method to assign a single style class.
            /
                public void setStyleClass(int from, int to, String styleClass) {
                    List<String> styleClasses = new ArrayList<>(1);
                    styleClasses.add(styleClass);
                    setStyle(from, to, styleClasses);
                }
         */

        return area;
    }

    /**
     * Creates a {@link #styleClassedTextArea(boolean preserveStyle)} with empty text content.
     */
    public static StyledTextArea<Collection<String>, Collection<String>> styleClassedTextArea() {
        return styleClassedTextArea(true);
    }

    /**
     * A convenience {@link #styleClassedTextArea(boolean)}
     * with fixed-width font and an undo manager that observes
     * only plain text changes (not styled changes).
     */
    public static StyledTextArea<Collection<String>, Collection<String>> codeArea() {
        StyledTextArea<Collection<String>, Collection<String>> area = styleClassedTextArea(false);

        // The below part of the translation seems awkward

        area.getStyleClass().add("code-area");

        // load the default style that defines a fixed-width font
        area.getStylesheets().add(StyledTextArea.class.getResource("code-area.css").toExternalForm());

        // don't apply preceding style to typed text
        area.setUseInitialStyleForInsertion(true);

        return area;
    }

    /**
     * Creates a {@link #codeArea()} with initial text content.
     * Initial caret position is set at the beginning of text content.
     *
     * @param text Initial text content.
     */
    public static StyledTextArea<Collection<String>, Collection<String>> codeArea(String text) {
        StyledTextArea<Collection<String>, Collection<String>> area = codeArea();

        area.appendText(text);
        area.getUndoManager().forgetHistory();
        area.getUndoManager().mark();

        // position the caret at the beginning
        area.selectRange(0, 0);

        return area;
    }

    // Embedded StyledTextArea factory methods
    public static <S, PS> VirtualizedScrollPane<StyledTextArea<S, PS>> embeddedStyledTextArea(
            S initialStyle, BiConsumer<? super TextExt, S> applyStyle, PS initialParagraphStyle,
            BiConsumer<TextFlow, PS> applyParagraphStyle) {
        return embeddedStyledTextArea(initialStyle, applyStyle, initialParagraphStyle, applyParagraphStyle, true);
    }

    public static <S, PS> VirtualizedScrollPane<StyledTextArea<S, PS>> embeddedStyledTextArea(
            S initialStyle, BiConsumer<? super TextExt, S> applyStyle, PS initialParagraphStyle,
            BiConsumer<TextFlow, PS> applyParagraphStyle, boolean preserveStyle) {
        return new VirtualizedScrollPane<>(new StyledTextArea<S, PS>(initialStyle, applyStyle, initialParagraphStyle, applyParagraphStyle, preserveStyle));
    }

    public static <S, PS> VirtualizedScrollPane<StyledTextArea<S, PS>> embeddedInlineStyleTextArea(
            S initialStyle, Function<S, String> styleToCss,
            PS initialParagraphStyle, Function<PS, String> paragraphStyleToCss) {
        return new VirtualizedScrollPane<>(inlineStyleTextArea(initialStyle, styleToCss, initialParagraphStyle, paragraphStyleToCss));
    }

    public static VirtualizedScrollPane<StyledTextArea<String, String>> embeddedInlineCssTextArea() {
        return new VirtualizedScrollPane<>(inlineCssTextArea());
    }

    public static VirtualizedScrollPane<StyledTextArea<String, String>> embeddedInlineCssTextArea(String text) {
        return new VirtualizedScrollPane<>(inlineCssTextArea(text));
    }

    public static VirtualizedScrollPane<StyledTextArea<Collection<String>, Collection<String>>> embeddedStyleClassedTextArea(boolean preserveStyle) {
        return new VirtualizedScrollPane<>(styleClassedTextArea(preserveStyle));
    }

    public static VirtualizedScrollPane<StyledTextArea<Collection<String>, Collection<String>>> embeddedStyleClassedTextArea() {
        return new VirtualizedScrollPane<>(styleClassedTextArea());
    }

    public static VirtualizedScrollPane<StyledTextArea<Collection<String>, Collection<String>>> embeddedCodeArea() {
        return new VirtualizedScrollPane<>(codeArea());
    }

    public static VirtualizedScrollPane<StyledTextArea<Collection<String>, Collection<String>>> embeddedCodeArea(String text) {
        return new VirtualizedScrollPane<>(codeArea(text));
    }
}
