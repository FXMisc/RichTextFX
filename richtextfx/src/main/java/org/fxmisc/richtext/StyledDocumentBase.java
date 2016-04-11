package org.fxmisc.richtext;

import static org.fxmisc.richtext.ReadOnlyStyledDocument.ParagraphsPolicy.*;
import static org.fxmisc.richtext.TwoDimensional.Bias.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javafx.scene.control.IndexRange;

abstract class StyledDocumentBase<PS, S, L extends List<Paragraph<PS, S>>>
implements StyledDocument<PS, S> {

    protected final L paragraphs;
    protected final TwoLevelNavigator navigator;

    protected StyledDocumentBase(L paragraphs) {
        this.paragraphs = paragraphs;
        navigator = new TwoLevelNavigator(
                paragraphs::size,
                i -> paragraphs.get(i).length() + (i == paragraphs.size() - 1 ? 0 : 1));
    }


    /***************************************************************************
     *                                                                         *
     * Queries                                                                 *
     *                                                                         *
     ***************************************************************************/

    @Override
    public Position offsetToPosition(int offset, Bias bias) {
        return navigator.offsetToPosition(offset, bias);
    }

    @Override
    public Position position(int row, int col) {
        return navigator.position(row, col);
    }

    @Override
    public String getText(IndexRange range) {
        return getText(range.getStart(), range.getEnd());
    }

    @Override
    public String getText(int start, int end) {
        StyledDocument<PS, S> sub = subSequence(start, end);
        String[] strings = sub.getParagraphs().stream()
                .map(Paragraph::getText)
                .toArray(n -> new String[n]);
        return String.join("\n", strings);
    }

    @Override
    public String toString() {
        return paragraphs
                .stream()
                .map(Paragraph::toString)
                .reduce((p1, p2) -> p1 + "\n" + p2)
                .orElse("");
    }

    public char charAt(int index) {
        Position pos = offsetToPosition(index, Forward);
        return paragraphs.get(pos.getMajor()).charAt(pos.getMinor());
    }

    @Override
    public StyledDocument<PS, S> subSequence(IndexRange range) {
        return subSequence(range.getStart(), range.getEnd());
    }

    @Override
    public StyledDocument<PS, S> subSequence(int start, int end) {
        Position start2D = navigator.offsetToPosition(start, Forward);
        Position end2D = end == start
                ? start2D
                : start2D.offsetBy(end - start, Forward);
        int p1 = start2D.getMajor();
        int col1 = start2D.getMinor();
        int p2 = end2D.getMajor();
        int col2 = end2D.getMinor();

        List<Paragraph<PS, S>> pars = new ArrayList<>(p2 - p1 + 1);

        if(p1 == p2) {
            pars.add(paragraphs.get(p1).subSequence(col1, col2));
        } else {
            Paragraph<PS, S> par1 = paragraphs.get(p1);
            pars.add(par1.subSequence(col1, par1.length()));

            for(int i = p1 + 1; i < p2; ++i) {
                pars.add(paragraphs.get(i));
            }

            pars.add(paragraphs.get(p2).subSequence(0, col2));
        }

        return new ReadOnlyStyledDocument<>(pars, ADOPT);
    }

    @Override
    public StyledDocument<PS, S> subDocument(int paragraphIndex) {
        return new ReadOnlyStyledDocument<>(Arrays.asList(paragraphs.get(paragraphIndex)), ADOPT);
    }

    @Override
    public final StyledDocument<PS, S> concat(StyledDocument<PS, S> that) {
        List<Paragraph<PS, S>> pars1 = this.getParagraphs();
        List<Paragraph<PS, S>> pars2 = that.getParagraphs();
        int n1 = pars1.size();
        int n2 = pars2.size();
        List<Paragraph<PS, S>> pars = new ArrayList<>(n1 + n2 - 1);
        pars.addAll(pars1.subList(0, n1 - 1));
        pars.add(pars1.get(n1 - 1).concat(pars2.get(0)));
        pars.addAll(pars2.subList(1, n2));
        return new ReadOnlyStyledDocument<>(pars, ADOPT);
    }

    @Override
    public S getStyleOfChar(int index) {
        Position pos2D = navigator.offsetToPosition(index, Forward);
        int paragraph = pos2D.getMajor();
        int col = pos2D.getMinor();
        return paragraphs.get(paragraph).getStyleOfChar(col);
    }

    @Override
    public S getStyleOfChar(int paragraph, int column) {
        return paragraphs.get(paragraph).getStyleOfChar(column);
    }

    @Override
    public S getStyleAtPosition(int position) {
        Position pos2D = navigator.offsetToPosition(position, Forward);
        return getStyleAtPosition(pos2D.getMajor(), pos2D.getMinor());
    }

    @Override
    public S getStyleAtPosition(int paragraph, int position) {
        return paragraphs.get(paragraph).getStyleAtPosition(position);
    }


    @Override
    public PS getParagraphStyle(int paragraph) {
        return paragraphs.get(paragraph).getParagraphStyle();
    }

    @Override
    public PS getParagraphStyleAtPosition(int position) {
        Position pos = offsetToPosition(position, Forward);
        return getParagraphStyle(pos.getMajor());
    }

    @Override
    public IndexRange getStyleRangeAtPosition(int position) {
        Position pos2D = navigator.offsetToPosition(position, Forward);
        int paragraph = pos2D.getMajor();
        int col = pos2D.getMinor();
        return paragraphs.get(paragraph).getStyleRangeAtPosition(col);
    }

    @Override
    public IndexRange getStyleRangeAtPosition(int paragraph, int position) {
        return paragraphs.get(paragraph).getStyleRangeAtPosition(position);
    }

    @Override
    public StyleSpans<S> getStyleSpans(int from, int to) {
        Position start = offsetToPosition(from, Forward);
        Position end = to == from
                ? start
                : start.offsetBy(to - from, Backward);
        int startParIdx = start.getMajor();
        int endParIdx = end.getMajor();

        int affectedPars = endParIdx - startParIdx + 1;
        List<StyleSpans<S>> subSpans = new ArrayList<>(affectedPars);

        if(startParIdx == endParIdx) {
            Paragraph<PS, S> par = paragraphs.get(startParIdx);
            subSpans.add(par.getStyleSpans(start.getMinor(), end.getMinor()));
        } else {
            Paragraph<PS, S> startPar = paragraphs.get(startParIdx);
            subSpans.add(startPar.getStyleSpans(start.getMinor(), startPar.length() + 1));

            for(int i = startParIdx + 1; i < endParIdx; ++i) {
                Paragraph<PS, S> par = paragraphs.get(i);
                subSpans.add(par.getStyleSpans(0, par.length() + 1));
            }

            Paragraph<PS, S> endPar = paragraphs.get(endParIdx);
            subSpans.add(endPar.getStyleSpans(0, end.getMinor()));
        }

        int n = subSpans.stream().mapToInt(StyleSpans::getSpanCount).sum();
        StyleSpansBuilder<S> builder = new StyleSpansBuilder<>(n);
        for(StyleSpans<S> spans: subSpans) {
            for(StyleSpan<S> span: spans) {
                builder.add(span);
            }
        }

        return builder.create();
    }

    @Override
    public StyleSpans<S> getStyleSpans(int paragraph) {
        return paragraphs.get(paragraph).getStyleSpans();
    }

    @Override
    public StyleSpans<S> getStyleSpans(int paragraph, int from, int to) {
        return paragraphs.get(paragraph).getStyleSpans(from, to);
    }

    @Override
    public final boolean equals(Object other) {
        if(other instanceof StyledDocument) {
            StyledDocument<?, ?> that = (StyledDocument<?, ?>) other;
            return Objects.equals(this.paragraphs, that.getParagraphs());
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return paragraphs.hashCode();
    }
}
