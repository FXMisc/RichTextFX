package codearea.control;

import static codearea.control.ReadOnlyStyledDocument.ParagraphsPolicy.*;
import static codearea.control.TwoDimensional.Bias.*;
import inhibeans.property.ReadOnlyIntegerWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import reactfx.EventSource;
import reactfx.EventStream;
import reactfx.EventStreams;
import reactfx.Hold;

/**
 * Content model for {@link StyledTextArea}. Implements edit operations
 * on styled text, but not worrying about additional aspects such as
 * caret or selection.
 */
final class EditableStyledDocument<S>
extends StyledDocumentBase<S, ObservableList<Paragraph<S>>> {

    /**************************************************************************
     *                                                                        *
     * Observables                                                            *
     *                                                                        *
     * Observables are "dynamic" (i.e. changing) characteristics of an object.*
     * They are not directly settable by the client code, but change in       *
     * response to user input and/or API actions.                             *
     *                                                                        *
     **************************************************************************/

    /**
     * Content of this {@code StyledDocument}.
     */
    private final StringBinding text = BindingFactories.createStringBinding(() -> getText(0, length()));
    @Override
    public String getText() { return text.get(); }
    public ObservableStringValue textProperty() { return text; }

    /**
     * Length of this {@code StyledDocument}.
     */
    private final ReadOnlyIntegerWrapper length = new ReadOnlyIntegerWrapper();
    public int getLength() { return length.get(); }
    public ObservableIntegerValue lengthProperty() { return length.getReadOnlyProperty(); }
    @Override
    public int length() { return length.get(); }

    /**
     * Unmodifiable observable list of styled paragraphs of this document.
     */
    @Override
    public ObservableList<Paragraph<S>> getParagraphs() {
        return FXCollections.unmodifiableObservableList(paragraphs);
    }


    /**************************************************************************
     *                                                                        *
     * Event streams                                                          *
     *                                                                        *
     **************************************************************************/

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
        EventStream<String> removedText = EventStreams.zip(textChangePosition, textRemovalEnd).by((a, b) -> getText(a, b));
        EventStream<Integer> changePosition = EventStreams.merge(textChangePosition, styleChangePosition);
        EventStream<Integer> removalEnd = EventStreams.merge(textRemovalEnd, styleChangeEnd);
        EventStream<StyledDocument<S>> removedDocument = EventStreams.zip(changePosition, removalEnd).by((a, b) -> subSequence(a, b));
        EventStream<Integer> insertionEnd = EventStreams.merge(
                EventStreams.combine(changePosition).on(insertionLength).by((start, len) -> start + len),
                EventStreams.release(styleChangeEnd).on(styleChangeDone));
        EventStream<StyledDocument<S>> insertedDocument = EventStreams.merge(
                this.insertedDocument,
                EventStreams.combine(changePosition).on(insertionEnd).by((a, b) -> subSequence(a, b)));

        plainTextChanges = EventStreams.zip(textChangePosition, removedText, insertedText)
                .by((pos, removed, inserted) -> new PlainTextChange(pos, removed, inserted));

        richChanges = EventStreams.zip(changePosition, removedDocument, insertedDocument)
                .by((pos, removed, inserted) -> new RichTextChange<S>(pos, removed, inserted));
    }


    /**************************************************************************
     *                                                                        *
     * Constructors                                                           *
     *                                                                        *
     **************************************************************************/

    @SuppressWarnings("unchecked")
    EditableStyledDocument(S initialStyle) {
        super(FXCollections.observableArrayList(new Paragraph<S>("", initialStyle)));
        length.set(0);
    }


    /**************************************************************************
     *                                                                        *
     * Actions                                                                *
     *                                                                        *
     * Actions change the state of the object. They typically cause a change  *
     * of one or more observables and/or produce an event.                    *
     *                                                                        *
     **************************************************************************/

    public void replaceText(int start, int end, String replacement) {
        replace(start, end, filterInput(replacement),
                (repl, pos) -> stringToParagraphs(repl, getStyleForInsertionAt(pos)),
                 repl -> {
                     insertedText.push(repl);
                     insertionLength.push(repl.length());
                 });
    }

    public void replace(int start, int end, StyledDocument<S> replacement) {
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
        if(from == to)
            return;

        try(Hold commitOnClose = beginStyleChange(from, to)) {
            Position start = navigator.offsetToPosition(from, Forward);
            Position end = start.offsetBy(to - from, Forward); // Forward in order not to lose the newline if end points just beyond it
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

                if(affectedPars == paragraphs.size()) {
                    paragraphs.setAll(restyledPars);
                } else {
                    // TODO: use setAll(from, to, col) when implemented (see https://javafx-jira.kenai.com/browse/RT-32655)
                    // (and then we don't even need the above `if` branch)
                    paragraphs.remove(firstParIdx, lastParIdx + 1);
                    paragraphs.addAll(firstParIdx, restyledPars);
                }
            }
        }
    }

    public void setStyle(int paragraph, S style) {
        Paragraph<S> p = paragraphs.get(paragraph);
        int start = position(paragraph, 0).toOffset();
        int end = start + p.length();

        try(Hold commitOnClose = beginStyleChange(start, end)) {
            p = p.restyle(style);
            paragraphs.set(paragraph, p);
        }
    }

    public void setStyle(int paragraph, int fromCol, int toCol, S style) {
        int parOffset = position(paragraph, 0).toOffset();
        int start = parOffset + fromCol;
        int end = parOffset + toCol;

        try(Hold commitOnClose = beginStyleChange(start, end)) {
            Paragraph<S> p = paragraphs.get(paragraph);
            p = p.restyle(fromCol, toCol, style);
            paragraphs.set(paragraph, p);
        }
    }

    public void setStyleSpans(int from, StyleSpans<S> styleSpans) {
        int len = styleSpans.length();
        try(Hold commitOnClose = beginStyleChange(from, from + len)) {
            Position start = offsetToPosition(from, Forward);
            Position end = start.offsetBy(len, Forward); // Forward in order not to lose the newline if end points just beyond it
            int firstParIdx = start.getMajor();
            int firstParFrom = start.getMinor();
            int lastParIdx = end.getMajor();
            int lastParTo = end.getMinor();

            if(firstParIdx == lastParIdx) {
                Paragraph<S> p = paragraphs.get(firstParIdx);
                p = p.restyle(firstParFrom, styleSpans);
                paragraphs.set(firstParIdx, p);
            } else {
                int affectedPars = lastParIdx - firstParIdx + 1;
                List<Paragraph<S>> restyledPars = new ArrayList<>(affectedPars);

                Paragraph<S> firstPar = paragraphs.get(firstParIdx);
                Position spansFrom = styleSpans.position(0, 0);
                Position spansTo = spansFrom.offsetBy(firstPar.length() - firstParFrom, Backward);
                restyledPars.add(firstPar.restyle(firstParFrom, styleSpans.subView(spansFrom, spansTo)));

                for(int i = firstParIdx + 1; i < lastParIdx; ++i) {
                    Paragraph<S> par = paragraphs.get(i);
                    spansFrom = spansTo.offsetBy(1, Forward); // offset by 1 to skip the newline at the end of the previous paragraph
                    spansTo = spansFrom.offsetBy(par.length(), Backward);
                    restyledPars.add(par.restyle(0, styleSpans.subView(spansFrom, spansTo)));
                }

                Paragraph<S> lastPar = paragraphs.get(lastParIdx);
                spansFrom = spansTo.offsetBy(1, Forward); // offset by 1 to skip the newline at the end of the previous paragraph
                spansTo = spansFrom.offsetBy(lastParTo, Backward);
                restyledPars.add(lastPar.restyle(0, styleSpans.subView(spansFrom, spansTo)));

                if(affectedPars == paragraphs.size()) {
                    paragraphs.setAll(restyledPars);
                } else {
                    // TODO: use setAll(from, to, col) when implemented (see https://javafx-jira.kenai.com/browse/RT-32655)
                    // (and then we don't even need the above `if` branch)
                    paragraphs.remove(firstParIdx, lastParIdx + 1);
                    paragraphs.addAll(firstParIdx, restyledPars);
                }
            }
        }
    }

    public void setStyleSpans(int paragraph, int from, StyleSpans<S> styleSpans) {
        int parOffset = position(paragraph, 0).toOffset();
        int start = parOffset + from;
        int end = start + styleSpans.length();

        try(Hold commitOnClose = beginStyleChange(start, end)) {
            Paragraph<S> p = paragraphs.get(paragraph);
            p = p.restyle(from, styleSpans);
            paragraphs.set(paragraph, p);
        }
    }


    /**************************************************************************
     *                                                                        *
     * Private methods                                                        *
     *                                                                        *
     **************************************************************************/

    /**
     * Filters out illegal characters.
     */
    private static String filterInput(String txt) {
        if(txt.chars().allMatch(c -> isLegal((char) c))) {
            return txt;
        } else {
            StringBuilder sb = new StringBuilder(txt.length());
            txt.chars().filter(c -> isLegal((char) c)).forEach(c -> sb.append((char) c));
            return sb.toString();
        }
    }

    private static boolean isLegal(char c) {
        return !Character.isISOControl(c) || c == '\n' || c == '\t';
    }

    private static <S> List<Paragraph<S>> stringToParagraphs(String str, S style) {
        String[] strings = str.split("\n", -1);
        List<Paragraph<S>> res = new ArrayList<>(strings.length);
        for(String s: strings) {
            res.add(new Paragraph<S>(s, style));
        }
        return res;
    }

    private Hold beginStyleChange(int start, int end) {
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
        Position end2D = start2D.offsetBy(end - start, Forward); // Forward in order not to lose the newline if end points just beyond it
        int firstParIdx = start2D.getMajor();
        int firstParFrom = start2D.getMinor();
        int lastParIdx = end2D.getMajor();
        int lastParTo = end2D.getMinor();

        // Get the leftovers after cutting out the deletion
        Paragraph<S> firstPar = paragraphs.get(firstParIdx).trim(firstParFrom);
        Paragraph<S> lastPar = paragraphs.get(lastParIdx).subSequence(lastParTo);

        List<Paragraph<S>> replacementPars = replacementToParagraphs.apply(replacement, start2D);
        int n = replacementPars.size();

        if(n == 1) {
            // replacement is just a single line,
            // use it to join the two leftover lines
            Paragraph<S> par = firstPar.append(replacementPars.get(0)).append(lastPar);

            // replace the affected liens with the merger of leftovers and the replacement line
            // TODO: use setAll(from, to, col) when implemented (see https://javafx-jira.kenai.com/browse/RT-32655)
            paragraphs.set(firstParIdx, par); // use set() instead of remove and add to make sure the number of lines is never 0
            paragraphs.remove(firstParIdx+1, lastParIdx+1);
        } else {
            // append the first replacement line to the left leftover
            // and prepend the last replacement line to the right leftover
            firstPar = firstPar.append(replacementPars.get(0));
            lastPar = replacementPars.get(n-1).append(lastPar);

            // replace the affected lines with the new lines
            // TODO: use setAll(from, to, col) when implemented (see https://javafx-jira.kenai.com/browse/RT-32655)
            paragraphs.set(lastParIdx, lastPar); // use set() instead of remove and add to make sure the number of lines is never 0
            paragraphs.remove(firstParIdx, lastParIdx);
            paragraphs.add(firstParIdx, firstPar);
            paragraphs.addAll(firstParIdx+1, replacementPars.subList(1, n - 1));
        }

        // update length, invalidate text
        int newLength = length.get() - (end - start) + replacement.length();
        length.blockWhile(() -> { // don't publish length change until text is invalidated
            length.set(newLength);
            text.invalidate();
        });

        // complete the change events
        publishReplacement.accept(replacement);
    }

    private S getStyleForInsertionAt(Position insertionPos) {
        Paragraph<S> par = paragraphs.get(insertionPos.getMajor());
        int insertionCol = insertionPos.getMinor();
        int prevCharIdx = insertionCol - 1; // it is OK if prevCharIdx is -1
        return par.getStyleAt(prevCharIdx);
    }
}