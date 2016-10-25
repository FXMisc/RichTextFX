package org.fxmisc.richtext.model;

import javafx.scene.control.IndexRange;

import java.util.Collections;
import java.util.List;

public final class EmptyParagraph<PS, SEG, S> implements Paragraph<PS, SEG, S> {

    private static final IndexRange EMPTY = new IndexRange(0, 0);

    private final PS parStyle;
    private final S textStyle;

    public EmptyParagraph(PS parStyle, S textStyle) {
        this.parStyle = parStyle;
        this.textStyle = textStyle;
    }

    @Override
    public List<SEG> getSegments() {
        return Collections.emptyList();
    }

    @Override
    public PS getParagraphStyle() {
        return parStyle;
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public char charAt(int index) {
        return 0;
    }

    @Override
    public String substring(int from, int to) {
        return "";
    }

    @Override
    public String substring(int from) {
        return "";
    }

    @Override
    public Paragraph<PS, SEG, S> concat(Paragraph<PS, SEG, S> p) {
        return p;
    }

    @Override
    public Paragraph<PS, SEG, S> concatR(Paragraph<PS, SEG, S> that) {
        return that;
    }

    @Override
    public Paragraph<PS, SEG, S> subSequence(int start, int end) {
        return this;
    }

    @Override
    public Paragraph<PS, SEG, S> trim(int length) {
        return this;
    }

    @Override
    public Paragraph<PS, SEG, S> subSequence(int start) {
        return this;
    }

    @Override
    public Paragraph<PS, SEG, S> delete(int start, int end) {
        return this;
    }

    @Override
    public Paragraph<PS, SEG, S> restyle(S style) {
        return new EmptyParagraph<>(parStyle, style);
    }

    @Override
    public Paragraph<PS, SEG, S> restyle(int from, int to, S style) {
        return restyle(style);
    }

    @Override
    public Paragraph<PS, SEG, S> restyle(int from, StyleSpans<? extends S> styleSpans) {
        return this;
    }

    @Override
    public Paragraph<PS, SEG, S> setParagraphStyle(PS paragraphStyle) {
        return new EmptyParagraph<>(paragraphStyle, textStyle);
    }

    @Override
    public S getStyleOfChar(int charIdx) {
        return textStyle;
    }

    @Override
    public S getStyleAtPosition(int position) {
        return textStyle;
    }

    @Override
    public IndexRange getStyleRangeAtPosition(int position) {
        return EMPTY;
    }

    @Override
    public StyleSpans<S> getStyleSpans() {
        StyleSpansBuilder<S> b = new StyleSpansBuilder<>();
        b.add(textStyle, 0);
        return b.create();
    }

    @Override
    public StyleSpans<S> getStyleSpans(int from, int to) {
        return getStyleSpans();
    }

    @Override
    public String getText() {
        return "";
    }
}
