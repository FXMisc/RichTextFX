package org.fxmisc.richtext;

import static org.fxmisc.richtext.TwoDimensional.Bias.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.scene.control.IndexRange;

public interface StyledDocument<PS, S> extends TwoDimensional {

    int length();

    String getText();

    List<Paragraph<PS, S>> getParagraphs();


    default String getText(IndexRange range) {
        return getText(range.getStart(), range.getEnd());
    }

    default String getText(int start, int end) {
        StyledDocument<PS, S> sub = subSequence(start, end);
        String[] strings = sub.getParagraphs().stream()
                .map(Paragraph::getText)
                .toArray(n -> new String[n]);
        return String.join("\n", strings);
    }

    default StyledDocument<PS, S> subSequence(IndexRange range) {
        return subSequence(range.getStart(), range.getEnd());
    }

    default StyledDocument<PS, S> subSequence(int start, int end) {
        Position start2D = offsetToPosition(start, Forward);
        Position end2D = end == start
                ? start2D
                : start2D.offsetBy(end - start, Forward);
        int p1 = start2D.getMajor();
        int col1 = start2D.getMinor();
        int p2 = end2D.getMajor();
        int col2 = end2D.getMinor();

        List<Paragraph<PS, S>> pars = new ArrayList<>(p2 - p1 + 1);

        if(p1 == p2) {
            pars.add(getParagraphs().get(p1).subSequence(col1, col2));
        } else {
            Paragraph<PS, S> par1 = getParagraphs().get(p1);
            pars.add(par1.subSequence(col1, par1.length()));

            for(int i = p1 + 1; i < p2; ++i) {
                pars.add(getParagraphs().get(i));
            }

            pars.add(getParagraphs().get(p2).subSequence(0, col2));
        }

        return new ReadOnlyStyledDocument<>(pars);
    }

    default StyledDocument<PS, S> subDocument(int paragraphIndex) {
        return new ReadOnlyStyledDocument<>(Collections.singletonList(getParagraphs().get(paragraphIndex)));
    }

    default char charAt(int index) {
        Position pos = offsetToPosition(index, Forward);
        return getParagraphs().get(pos.getMajor()).charAt(pos.getMinor());
    }

    default StyledDocument<PS, S> concat(StyledDocument<PS, S> that) {
        List<Paragraph<PS, S>> pars1 = this.getParagraphs();
        List<Paragraph<PS, S>> pars2 = that.getParagraphs();
        int n1 = pars1.size();
        int n2 = pars2.size();
        List<Paragraph<PS, S>> pars = new ArrayList<>(n1 + n2 - 1);
        pars.addAll(pars1.subList(0, n1 - 1));
        pars.add(pars1.get(n1 - 1).concat(pars2.get(0)));
        pars.addAll(pars2.subList(1, n2));
        return new ReadOnlyStyledDocument<>(pars);
    }

    default S getStyleOfChar(int index) {
        Position pos2D = offsetToPosition(index, Forward);
        int paragraph = pos2D.getMajor();
        int col = pos2D.getMinor();
        return getParagraphs().get(paragraph).getStyleOfChar(col);
    }

    default S getStyleOfChar(int paragraph, int column) {
        return getParagraphs().get(paragraph).getStyleOfChar(column);
    }

    default S getStyleAtPosition(int position) {
        Position pos2D = offsetToPosition(position, Forward);
        return getStyleAtPosition(pos2D.getMajor(), pos2D.getMinor());
    }

    default S getStyleAtPosition(int paragraph, int position) {
        return getParagraphs().get(paragraph).getStyleAtPosition(position);
    }


    default PS getParagraphStyle(int paragraph) {
        return getParagraphs().get(paragraph).getParagraphStyle();
    }

    default PS getParagraphStyleAtPosition(int position) {
        Position pos = offsetToPosition(position, Forward);
        return getParagraphStyle(pos.getMajor());
    }

    default IndexRange getStyleRangeAtPosition(int position) {
        Position pos2D = offsetToPosition(position, Forward);
        int paragraph = pos2D.getMajor();
        int col = pos2D.getMinor();
        return getParagraphs().get(paragraph).getStyleRangeAtPosition(col);
    }

    default IndexRange getStyleRangeAtPosition(int paragraph, int position) {
        return getParagraphs().get(paragraph).getStyleRangeAtPosition(position);
    }

    default StyleSpans<S> getStyleSpans(int from, int to) {
        Position start = offsetToPosition(from, Forward);
        Position end = to == from
                ? start
                : start.offsetBy(to - from, Backward);
        int startParIdx = start.getMajor();
        int endParIdx = end.getMajor();

        int affectedPars = endParIdx - startParIdx + 1;
        List<StyleSpans<S>> subSpans = new ArrayList<>(affectedPars);

        if(startParIdx == endParIdx) {
            Paragraph<PS, S> par = getParagraphs().get(startParIdx);
            subSpans.add(par.getStyleSpans(start.getMinor(), end.getMinor()));
        } else {
            Paragraph<PS, S> startPar = getParagraphs().get(startParIdx);
            subSpans.add(startPar.getStyleSpans(start.getMinor(), startPar.length() + 1));

            for(int i = startParIdx + 1; i < endParIdx; ++i) {
                Paragraph<PS, S> par = getParagraphs().get(i);
                subSpans.add(par.getStyleSpans(0, par.length() + 1));
            }

            Paragraph<PS, S> endPar = getParagraphs().get(endParIdx);
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

    default StyleSpans<S> getStyleSpans(int paragraph) {
        return getParagraphs().get(paragraph).getStyleSpans();
    }

    default StyleSpans<S> getStyleSpans(int paragraph, int from, int to) {
        return getParagraphs().get(paragraph).getStyleSpans(from, to);
    }
}
