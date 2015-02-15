package org.fxmisc.richtext;

import static org.fxmisc.richtext.ReadOnlyStyledDocument.ParagraphsPolicy.*;
import static org.fxmisc.richtext.TwoDimensional.Bias.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.fxmisc.richtext.ReadOnlyStyledDocument.ParagraphsPolicy;
import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.Guard;
import org.reactfx.value.SuspendableVar;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

/**
 * Content model for {@link StyledTextArea}. Implements edit operations
 * on styled text, but not worrying about additional aspects such as
 * caret or selection.
 */
final class EditableStyledDocument<S>
extends StyledDocumentBase<S, ObservableList<Paragraph<S>>> {

    /* ********************************************************************** *
     *                                                                        *
     * Observables                                                            *
     *                                                                        *
     * Observables are "dynamic" (i.e. changing) characteristics of an object.*
     * They are not directly settable by the client code, but change in       *
     * response to user input and/or API actions.                             *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Content of this {@code StyledDocument}.
     */
    private final StringBinding text = Bindings.createStringBinding(() -> getText(0, length()));
    @Override
    public String getText() { return text.getValue(); }
    public ObservableValue<String> textProperty() { return text; }

    /**
     * Length of this {@code StyledDocument}.
     */
    private final SuspendableVar<Integer> length = Var.newSimpleVar(0).suspendable();
    public int getLength() { return length.getValue(); }
    public Val<Integer> lengthProperty() { return length; }
    @Override
    public int length() { return length.getValue(); }

    /**
     * Unmodifiable observable list of styled paragraphs of this document.
     */
    @Override
    public ObservableList<Paragraph<S>> getParagraphs() {
        return FXCollections.unmodifiableObservableList(paragraphs);
    }

    /**
     * Read-only snapshot of the current state of this document.
     */
    public ReadOnlyStyledDocument<S> snapshot() {
        return new ReadOnlyStyledDocument<>(paragraphs, ParagraphsPolicy.COPY);
    }


    /* ********************************************************************** *
     *                                                                        *
     * Event streams                                                          *
     *                                                                        *
     * ********************************************************************** */

    // To publish a text change:
    //   1. push to textChangePosition,
    //   2. push to textRemovalEnd,
    //   3. push to insertedText.
    //
    // To publish a rich change:
    //   a)
    //     1. push to textChangePosition,
    //     2. push to textRemovalEnd,
    //     3. push to insertedDocument;
    //   b)
    //     1. push to textChangePosition,
    //     2. push to textRemovalEnd,
    //     3. push to insertionLength;
    //   c)
    //     1. push to styleChangePosition
    //     2. push to styleChangeEnd
    //     3. push to styleChangeDone.

    private final EventSource<Integer> textChangePosition = new EventSource<>();
    private final EventSource<Integer> styleChangePosition = new EventSource<>();

    private final EventSource<Integer> textRemovalEnd = new EventSource<>();
    private final EventSource<Integer> styleChangeEnd = new EventSource<>();

    private final EventSource<String> insertedText = new EventSource<>();

    private final EventSource<StyledDocument<S>> insertedDocument = new EventSource<>();
    private final EventSource<Integer> insertionLength = new EventSource<>();
    private final EventSource<Void> styleChangeDone = new EventSource<>();

    private final EventStream<PlainTextChange> plainTextChanges;
    public EventStream<PlainTextChange> plainTextChanges() { return plainTextChanges; }

    private final EventStream<RichTextChange<S>> richChanges;
    public EventStream<RichTextChange<S>> richChanges() { return richChanges; }

    {
        EventStream<String> removedText = EventStreams.zip(textChangePosition, textRemovalEnd).map(t2 -> t2.map((a, b) -> getText(a, b)));
        EventStream<Integer> changePosition = EventStreams.merge(textChangePosition, styleChangePosition);
        EventStream<Integer> removalEnd = EventStreams.merge(textRemovalEnd, styleChangeEnd);
        EventStream<StyledDocument<S>> removedDocument = EventStreams.zip(changePosition, removalEnd).map(t2 -> t2.map((a, b) -> subSequence(a, b)));
        EventStream<Integer> insertionEnd = EventStreams.merge(
                changePosition.emitBothOnEach(insertionLength).map(t2 -> t2.map((start, len) -> start + len)),
                styleChangeEnd.emitOn(styleChangeDone));
        EventStream<StyledDocument<S>> insertedDocument = EventStreams.merge(
                this.insertedDocument,
                changePosition.emitBothOnEach(insertionEnd).map(t2 -> t2.map((a, b) -> subSequence(a, b))));

        plainTextChanges = EventStreams.zip(textChangePosition, removedText, insertedText)
                .map(t3 -> t3.map((pos, removed, inserted) -> new PlainTextChange(pos, removed, inserted)));

        richChanges = EventStreams.zip(changePosition, removedDocument, insertedDocument)
                .map(t3 -> t3.map((pos, removed, inserted) -> new RichTextChange<S>(pos, removed, inserted)));
    }


    /* ********************************************************************** *
     *                                                                        *
     * Properties                                                             *
     *                                                                        *
     * ********************************************************************** */

    final BooleanProperty useInitialStyleForInsertion = new SimpleBooleanProperty();


    /* ********************************************************************** *
     *                                                                        *
     * Fields                                                                 *
     *                                                                        *
     * ********************************************************************** */

    private final S initialStyle;


    /* ********************************************************************** *
     *                                                                        *
     * Constructors                                                           *
     *                                                                        *
     * ********************************************************************** */

    @SuppressWarnings("unchecked")
    EditableStyledDocument(S initialStyle) {
        super(FXCollections.observableArrayList(new Paragraph<S>("", initialStyle)));
        this.initialStyle = initialStyle;
    }


    /* ********************************************************************** *
     *                                                                        *
     * Actions                                                                *
     *                                                                        *
     * Actions change the state of the object. They typically cause a change  *
     * of one or more observables and/or produce an event.                    *
     *                                                                        *
     * ********************************************************************** */

    public void replaceText(int start, int end, String replacement) {
        ensureValidRange(start, end);
        replace(start, end, replacement,
                (repl, pos) -> stringToParagraphs(repl, getStyleForInsertionAt(pos)),
                repl -> {
                    insertedText.push(repl);
                    insertionLength.push(repl.length());
                });
    }

    public void replace(int start, int end, StyledDocument<S> replacement) {
        ensureValidRange(start, end);
        replace(start, end, replacement,
                (repl, pos) -> repl.getParagraphs(),
                repl -> {
                    insertedText.push(repl.toString());
                    StyledDocument<S> doc =
                            repl instanceof ReadOnlyStyledDocument
                            ? repl
                            : new ReadOnlyStyledDocument<>(repl.getParagraphs(), COPY);
                    insertedDocument.push(doc);
                });
    }

    public void setStyle(int from, int to, S style) {
        ensureValidRange(from, to);

        try(Guard commitOnClose = beginStyleChange(from, to)) {
            Position start = navigator.offsetToPosition(from, Forward);
            Position end = to == from
                    ? start
                    : start.offsetBy(to - from, Backward);
            int firstParIdx = start.getMajor();
            int firstParFrom = start.getMinor();
            int lastParIdx = end.getMajor();
            int lastParTo = end.getMinor();

            if(firstParIdx == lastParIdx) {
                Paragraph<S> p = paragraphs.get(firstParIdx);
                p = p.restyle(firstParFrom, lastParTo, style);
                paragraphs.set(firstParIdx, p);
            } else {
                int affectedPars = lastParIdx - firstParIdx + 1;
                List<Paragraph<S>> restyledPars = new ArrayList<>(affectedPars);

                Paragraph<S> firstPar = paragraphs.get(firstParIdx);
                restyledPars.add(firstPar.restyle(firstParFrom, firstPar.length(), style));

                for(int i = firstParIdx + 1; i < lastParIdx; ++i) {
                    Paragraph<S> p = paragraphs.get(i);
                    restyledPars.add(p.restyle(style));
                }

                Paragraph<S> lastPar = paragraphs.get(lastParIdx);
                restyledPars.add(lastPar.restyle(0, lastParTo, style));

                setAll(firstParIdx, lastParIdx + 1, restyledPars);
            }
        }
    }

    public void setStyle(int paragraph, S style) {
        Paragraph<S> p = paragraphs.get(paragraph);
        int start = position(paragraph, 0).toOffset();
        int end = start + p.length();

        try(Guard commitOnClose = beginStyleChange(start, end)) {
            p = p.restyle(style);
            paragraphs.set(paragraph, p);
        }
    }

    public void setStyle(int paragraph, int fromCol, int toCol, S style) {
        ensureValidParagraphRange(paragraph, fromCol, toCol);
        int parOffset = position(paragraph, 0).toOffset();
        int start = parOffset + fromCol;
        int end = parOffset + toCol;

        try(Guard commitOnClose = beginStyleChange(start, end)) {
            Paragraph<S> p = paragraphs.get(paragraph);
            p = p.restyle(fromCol, toCol, style);
            paragraphs.set(paragraph, p);
        }
    }

    public void setStyleSpans(int from, StyleSpans<? extends S> styleSpans) {
        int len = styleSpans.length();
        ensureValidRange(from, from + len);

        Position start = offsetToPosition(from, Forward);
        Position end = start.offsetBy(len, Backward);
        int skip = terminatorLengthToSkip(start);
        int trim = terminatorLengthToTrim(end);
        if(skip + trim >= len) {
            return;
        } else if(skip + trim > 0) {
            styleSpans = styleSpans.subView(skip, len - trim);
            len -= skip + trim;
            from += skip;
            start = start.offsetBy(skip, Forward);
            end = end.offsetBy(-trim, Backward);
        }

        try(Guard commitOnClose = beginStyleChange(from, from + len)) {
            int firstParIdx = start.getMajor();
            int firstParFrom = start.getMinor();
            int lastParIdx = end.getMajor();
            int lastParTo = end.getMinor();

            if(firstParIdx == lastParIdx) {
                Paragraph<S> p = paragraphs.get(firstParIdx);
                Paragraph<S> q = p.restyle(firstParFrom, styleSpans);
                if(q != p) {
                    paragraphs.set(firstParIdx, q);
                }
            } else {
                Paragraph<S> firstPar = paragraphs.get(firstParIdx);
                Position spansFrom = styleSpans.position(0, 0);
                Position spansTo = spansFrom.offsetBy(firstPar.length() - firstParFrom, Backward);
                Paragraph<S> q = firstPar.restyle(firstParFrom, styleSpans.subView(spansFrom, spansTo));
                if(q != firstPar) {
                    paragraphs.set(firstParIdx, q);
                }
                spansFrom = spansTo.offsetBy(firstPar.getLineTerminator().map(LineTerminator::length).orElse(0), Forward); // skip the newline

                for(int i = firstParIdx + 1; i < lastParIdx; ++i) {
                    Paragraph<S> par = paragraphs.get(i);
                    spansTo = spansFrom.offsetBy(par.length(), Backward);
                    q = par.restyle(0, styleSpans.subView(spansFrom, spansTo));
                    if(q != par) {
                        paragraphs.set(i, q);
                    }
                    spansFrom = spansTo.offsetBy(par.getLineTerminator().map(LineTerminator::length).orElse(0), Forward); // skip the newline
                }

                Paragraph<S> lastPar = paragraphs.get(lastParIdx);
                spansTo = spansFrom.offsetBy(lastParTo, Backward);
                q = lastPar.restyle(0, styleSpans.subView(spansFrom, spansTo));
                if(q != lastPar) {
                    paragraphs.set(lastParIdx, q);
                }
            }
        }
    }

    public void setStyleSpans(int paragraph, int from, StyleSpans<? extends S> styleSpans) {
        int len = styleSpans.length();
        ensureValidParagraphRange(paragraph, from, len);
        int parOffset = position(paragraph, 0).toOffset();
        int start = parOffset + from;
        int end = start + len;

        try(Guard commitOnClose = beginStyleChange(start, end)) {
            Paragraph<S> p = paragraphs.get(paragraph);
            Paragraph<S> q = p.restyle(from, styleSpans);
            if(q != p) {
                paragraphs.set(paragraph, q);
            }
        }
    }


    /* ********************************************************************** *
     *                                                                        *
     * Private methods                                                        *
     *                                                                        *
     * ********************************************************************** */

    private static <S> List<Paragraph<S>> stringToParagraphs(String str, S style) {
        Matcher m = LineTerminator.regex().matcher(str);

        int n = 1;
        while(m.find()) ++n;
        List<Paragraph<S>> res = new ArrayList<>(n);

        int start = 0;
        m.reset();
        while(m.find()) {
            String s = str.substring(start, m.start());
            LineTerminator t = LineTerminator.from(m.group());
            res.add(new Paragraph<S>(s, style).terminate(t));
            start = m.end();
        }
        String last = str.substring(start);
        res.add(new Paragraph<>(last, style));

        return res;
    }

    private void ensureValidRange(int start, int end) {
        ensureValidRange(start, end, length());
    }

    private void ensureValidParagraphRange(int par, int start, int end) {
        if(par < 0 || par >= paragraphs.size()) {
            throw new IllegalArgumentException(par + " is not a valid paragraph index. Must be from [0, " + paragraphs.size() + ")");
        }
        ensureValidRange(start, end, paragraphs.get(par).fullLength());
    }

    private void ensureValidRange(int start, int end, int len) {
        if(start < 0) {
            throw new IllegalArgumentException("start cannot be negative: " + start);
        }
        if(end > len) {
            throw new IllegalArgumentException("end is greater than length: " + end + " > " + len);
        }
        if(start > end) {
            throw new IllegalArgumentException("start is greater than end: " + start + " > " + end);
        }
    }

    private int terminatorLengthToSkip(Position pos) {
        Paragraph<S> par = paragraphs.get(pos.getMajor());
        int skipSum = 0;
        while(pos.getMinor() >= par.length() && pos.getMinor() < par.fullLength()) {
            int skipLen = par.fullLength() - pos.getMinor();
            skipSum += skipLen;
            pos = pos.offsetBy(skipLen, Forward); // will jump to the next paragraph, if not at the end
            par = paragraphs.get(pos.getMajor());
        }
        return skipSum;
    }

    private int terminatorLengthToTrim(Position pos) {
        int parLen = paragraphs.get(pos.getMajor()).length();
        int trimSum = 0;
        while(pos.getMinor() > parLen) {
            int trimLen = pos.getMinor() - parLen;
            trimSum += trimLen;
            pos = pos.offsetBy(-trimLen, Backward); // may jump to the end of previous paragraph, if parLen was 0
            parLen = paragraphs.get(pos.getMajor()).length();
        }
        return trimSum;
    }

    private Guard beginStyleChange(int start, int end) {
        styleChangePosition.push(start);
        styleChangeEnd.push(end);
        return () -> styleChangeDone.push(null);
    }

    /**
     * Generic implementation for text replacement.
     * @param start
     * @param end
     * @param replacement
     * @param replacementToParagraphs function to convert the replacement
     * to paragraphs. In addition to replacement itself, it is also given
     * the position at which the replacement is going to be inserted. This
     * position can be used to determine the style of the resulting paragraphs.
     * @param publishReplacement completes the change events. It has to push
     * exactly one value {@link #insertedText} and exactly one value to exactly
     * one of {@link #insertedDocument}, {@link #insertionLength}.
     */
    private <D extends CharSequence> void replace(
            int start, int end, D replacement,
            BiFunction<D, Position, List<Paragraph<S>>> replacementToParagraphs,
            Consumer<D> publishReplacement) {

        textChangePosition.push(start);
        textRemovalEnd.push(end);

        Position start2D = navigator.offsetToPosition(start, Forward);
        Position end2D = start2D.offsetBy(end - start, Forward);
        int firstParIdx = start2D.getMajor();
        int firstParFrom = start2D.getMinor();
        int lastParIdx = end2D.getMajor();
        int lastParTo = end2D.getMinor();

        // Get the leftovers after cutting out the deletion
        Paragraph<S> firstPar = paragraphs.get(firstParIdx).trim(firstParFrom);
        Paragraph<S> lastPar = paragraphs.get(lastParIdx).subSequence(lastParTo);

        List<Paragraph<S>> replacementPars = replacementToParagraphs.apply(replacement, start2D);

        List<Paragraph<S>> newPars = join(firstPar, replacementPars, lastPar);
        setAll(firstParIdx, lastParIdx + 1, newPars);

        // update length, invalidate text
        int newLength = length.getValue() - (end - start) + replacement.length();
        length.suspendWhile(() -> { // don't publish length change until text is invalidated
            length.setValue(newLength);
            text.invalidate();
        });

        // complete the change events
        publishReplacement.accept(replacement);
    }

    private List<Paragraph<S>> join(Paragraph<S> first, List<Paragraph<S>> middle, Paragraph<S> last) {
        int m = middle.size();
        if(m == 0) {
            return join(first, last);
        } else if(!first.isTerminated()) {
            first = first.concat(middle.get(0));
            middle = middle.subList(1, m);
            return join(first, middle, last);
        } else {
            Paragraph<S> lastMiddle = middle.get(m - 1);
            if(lastMiddle.isTerminated()) {
                int n = 1 + m + 1;
                List<Paragraph<S>> res = new ArrayList<>(n);
                res.add(first);
                res.addAll(middle);
                res.add(last);
                return res;
            } else {
                int n = 1 + m;
                List<Paragraph<S>> res = new ArrayList<>(n);
                res.add(first);
                res.addAll(middle.subList(0, m - 1));
                res.add(lastMiddle.concat(last));
                return res;
            }
        }
    }

    private List<Paragraph<S>> join(Paragraph<S> first, Paragraph<S> last) {
        return first.isTerminated()
                ? Arrays.asList(first, last)
                : Arrays.asList(first.concat(last));
    }

    // TODO: Replace with ObservableList.setAll(from, to, col) when implemented.
    // See https://javafx-jira.kenai.com/browse/RT-32655.
    private void setAll(int startIdx, int endIdx, Collection<Paragraph<S>> pars) {
        if(startIdx > 0 || endIdx < paragraphs.size()) {
            paragraphs.subList(startIdx, endIdx).clear(); // note that paragraphs remains non-empty at all times
            paragraphs.addAll(startIdx, pars);
        } else {
            paragraphs.setAll(pars);
        }
    }

    private S getStyleForInsertionAt(Position insertionPos) {
        if(useInitialStyleForInsertion.get()) {
            return initialStyle;
        } else {
            Paragraph<S> par = paragraphs.get(insertionPos.getMajor());
            return par.getStyleAtPosition(insertionPos.getMinor());
        }
    }
}
