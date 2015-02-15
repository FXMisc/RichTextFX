package org.fxmisc.richtext;

import static org.fxmisc.richtext.ReadOnlyStyledDocument.ParagraphsPolicy.*;
import static org.fxmisc.richtext.TwoDimensional.Bias.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javafx.scene.control.IndexRange;

abstract class StyledDocumentBase<S, L extends List<Paragraph<S>>>
implements StyledDocument<S> {

    protected final L paragraphs;
    protected final TwoLevelNavigator navigator;

    protected StyledDocumentBase(L paragraphs) {
        this.paragraphs = paragraphs;
        navigator = new TwoLevelNavigator(
                () -> paragraphs.size(),
                i -> paragraphs.get(i).fullLength());
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
        return sub(
                start, end,
                Paragraph::fullText,
                Paragraph::substring,
                StyledDocumentBase::concat);
    }

    @Override
    public String toString() {
        return getText();
    }

    @Override
    public char charAt(int index) {
        Position pos = offsetToPosition(index, Forward);
        return paragraphs.get(pos.getMajor()).charAt(pos.getMinor());
    }

    @Override
    public StyledDocument<S> subSequence(IndexRange range) {
        return subSequence(range.getStart(), range.getEnd());
    }

    @Override
    public StyledDocument<S> subSequence(int start, int end) {
        return sub(
                start, end,
                p -> p,
                (p, a, b) -> p.subSequence(a, b),
                (List<Paragraph<S>> pars) -> new ReadOnlyStyledDocument<S>(pars, ADOPT));
    }

    @Override
    public StyledDocument<S> subDocument(int paragraphIndex) {
        return new ReadOnlyStyledDocument<>(Arrays.asList(paragraphs.get(paragraphIndex)), ADOPT);
    }

    @Override
    public final StyledDocument<S> concat(StyledDocument<S> that) {
        List<Paragraph<S>> pars1 = this.getParagraphs();
        List<Paragraph<S>> pars2 = that.getParagraphs();
        int n1 = pars1.size();
        int n2 = pars2.size();
        Paragraph<S> pars1last = pars1.get(n1 - 1);
        List<Paragraph<S>> pars;
        if(pars1last.isTerminated()) {
            pars = new ArrayList<>(n1 + n2);
            pars.addAll(pars1);
            pars.addAll(pars2);
        } else {
            pars = new ArrayList<>(n1 + n2 - 1);
            pars.addAll(pars1.subList(0, n1 - 1));
            pars.add(pars1last.concat(pars2.get(0)));
            pars.addAll(pars2.subList(1, n2));
        }
        return new ReadOnlyStyledDocument<S>(pars, ADOPT);
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
        int paragraph = pos2D.getMajor();
        int col = pos2D.getMinor();
        return paragraphs.get(paragraph).getStyleAtPosition(col);
    }

    @Override
    public S getStyleAtPosition(int paragraph, int position) {
        return paragraphs.get(paragraph).getStyleAtPosition(position);
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
            Paragraph<S> par = paragraphs.get(startParIdx);
            subSpans.add(par.getStyleSpans(start.getMinor(), end.getMinor()));
        } else {
            Paragraph<S> startPar = paragraphs.get(startParIdx);
            subSpans.add(startPar.getStyleSpans(start.getMinor(), startPar.length() + 1)); // +1 for the newline

            for(int i = startParIdx + 1; i < endParIdx; ++i) {
                Paragraph<S> par = paragraphs.get(i);
                subSpans.add(par.getStyleSpans(0, par.length() + 1)); // +1 for the newline
            }

            Paragraph<S> endPar = paragraphs.get(endParIdx);
            subSpans.add(endPar.getStyleSpans(0, end.getMinor()));
        }

        int n = subSpans.stream().mapToInt(sr -> sr.getSpanCount()).sum();
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


    /**************************************************************************
     *                                                                        *
     * Private methods                                                        *
     *                                                                        *
     **************************************************************************/

    private interface SubMap<A, B> {
        B subrange(A par, int start, int end);
    }

    /**
     * Returns a subrange of this document.
     * @param start
     * @param end
     * @param map maps a paragraph to an object of type {@code P}.
     * @param subMap maps a subrange of paragraph to an object of type {@code P}.
     * @param combine combines mapped paragraphs to form the result.
     * It is safe for the client code to take ownership of the list instance
     * passed to combine.
     * @param <P> type to which paragraphs are mapped.
     * @param <R> type of the resulting sub-document.
     */
    private <P, R> R sub(
            int start, int end,
            Function<Paragraph<S>, P> map,
            SubMap<Paragraph<S>, P> subMap,
            Function<List<P>, R> combine) {

        Position start2D = navigator.offsetToPosition(start, Forward);
        Position end2D = end == start
                ? start2D
                : start2D.offsetBy(end - start, Backward);
        int p1 = start2D.getMajor();
        int col1 = start2D.getMinor();
        int p2 = end2D.getMajor();
        int col2 = end2D.getMinor();

        List<P> pars = new ArrayList<>(p2 - p1 + 1);

        if(p1 == p2) {
            pars.add(subMap.subrange(paragraphs.get(p1), col1, col2));
        } else {
            Paragraph<S> par1 = paragraphs.get(p1);
            pars.add(subMap.subrange(par1, col1, par1.fullLength()));

            for(int i = p1 + 1; i < p2; ++i) {
                pars.add(map.apply(paragraphs.get(i)));
            }

            pars.add(subMap.subrange(paragraphs.get(p2), 0, col2));
        }

        return combine.apply(pars);
    }

    /**
     * Joins a list of strings, using the given separator string.
     */
    private static String concat(List<String> list) {
        int len = list.stream().mapToInt(String::length).sum();
        StringBuilder sb = new StringBuilder(len);
        for(String s: list){
            sb.append(s);
        }
        return sb.toString();
    }
}
