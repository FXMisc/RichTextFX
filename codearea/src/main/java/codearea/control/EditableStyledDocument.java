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
import reactfx.PushSource;
import reactfx.Source;
import reactfx.Sources;

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

    private final PushSource<Integer> textChangePosition = new PushSource<>();
    private final PushSource<Integer> styleChangePosition = new PushSource<>();

    private final PushSource<Integer> textRemovalEnd = new PushSource<>();
    private final PushSource<Integer> styleChangeEnd = new PushSource<>();

    private final PushSource<String> insertedText = new PushSource<>();

    private final PushSource<StyledDocument<S>> insertedDocument = new PushSource<>();
    private final PushSource<Integer> insertionLength = new PushSource<>();
    private final PushSource<Void> styleChangeDone = new PushSource<>();

    private final Source<TextChange> textChanges;
    public Source<TextChange> textChanges() { return textChanges; }

    private final Source<SequenceChange<StyledDocument<S>>> richChanges;
    public Source<SequenceChange<StyledDocument<S>>> richChanges() { return richChanges; }

    {
        Source<String> removedText = Sources.zip(textChangePosition, textRemovalEnd, (a, b) -> getText(a, b));
        Source<Integer> changePosition = Sources.merge(textChangePosition, styleChangePosition);
        Source<Integer> removalEnd = Sources.merge(textRemovalEnd, styleChangeEnd);
        Source<StyledDocument<S>> removedDocument = Sources.zip(changePosition, removalEnd, (a, b) -> subSequence(a, b));
        Source<Integer> insertionEnd = Sources.merge(
                Sources.combine(changePosition).on(insertionLength).by((start, len) -> start + len),
                Sources.release(styleChangeEnd).on(styleChangeDone));
        Source<StyledDocument<S>> insertedDocument = Sources.merge(
                this.insertedDocument,
                Sources.combine(changePosition).on(insertionEnd).by((a, b) -> subSequence(a, b)));

        textChanges = Sources.zip(textChangePosition, removedText, insertedText,
                (pos, removed, inserted) -> new TextChange(pos, removed, inserted));

        richChanges = Sources.zip(changePosition, removedDocument, insertedDocument,
                (pos, removed, inserted) -> new SequenceChange<StyledDocument<S>>(pos, removed, inserted));
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
                    insertedDocument.push(
                            repl instanceof ReadOnlyStyledDocument
                            ? repl
                            : new ReadOnlyStyledDocument<>(repl.getParagraphs(), COPY));
                });
    }

    public void setStyle(int from, int to, S style) {
        if(from == to)
            return;

        Position start = navigator.offsetToPosition(from, Forward);
        Position end = start.offsetBy(to - from, Forward);
        int firstParIdx = start.getMajor();
        int firstParFrom = start.getMinor();
        int lastParIdx = end.getMajor();
        int lastParTo = end.getMinor();

        if(firstParIdx == lastParIdx) {
            setStyle(firstParIdx, firstParFrom, lastParTo, style);
        } else {
            int firstParLen = paragraphs.get(firstParIdx).length();
            setStyle(firstParIdx, firstParFrom, firstParLen, style);
            for(int i=firstParIdx+1; i<lastParIdx; ++i) {
                setStyle(i, style);
            }
            setStyle(lastParIdx, 0, lastParTo, style);
        }
    }

    public void setStyle(int paragraph, S style) {
        Paragraph<S> p = paragraphs.get(paragraph);
        p = p.restyle(style);
        paragraphs.set(paragraph, p);
    }

    public void setStyle(int paragraph, int fromCol, int toCol, S style) {
        Paragraph<S> p = paragraphs.get(paragraph);
        p = p.restyle(fromCol, toCol, style);
        paragraphs.set(paragraph, p);
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
        Paragraph<S> firstPar = paragraphs.get(firstParIdx);
        Paragraph<S> lastPar = paragraphs.get(lastParIdx);
        Paragraph<S> left = firstPar.trim(firstParFrom);
        Paragraph<S> right = lastPar.subSequence(lastParTo);

        List<Paragraph<S>> replacementPars = replacementToParagraphs.apply(replacement, start2D);
        int n = replacementPars.size();

        if(n == 1) {
            // replacement is just a single line,
            // use it to join the two leftover lines
            left = left.append(replacementPars.get(0)).append(right);

            // replace the affected liens with the merger of leftovers and the replacement line
            // TODO: use setAll(from, to, col) when implemented (see https://javafx-jira.kenai.com/browse/RT-32655)
            paragraphs.set(firstParIdx, left); // use set() instead of remove and add to make sure the number of lines is never 0
            paragraphs.remove(firstParIdx+1, lastParIdx+1);
        } else {
            // append the first replacement line to the left leftover
            // and prepend the last replacement line to the right leftover
            left = left.append(replacementPars.get(0));
            right = replacementPars.get(n-1).append(right);

            // replace the affected lines with the new lines
            // TODO: use setAll(from, to, col) when implemented (see https://javafx-jira.kenai.com/browse/RT-32655)
            paragraphs.set(lastParIdx, right); // use set() instead of remove and add to make sure the number of lines is never 0
            paragraphs.remove(firstParIdx, lastParIdx);
            paragraphs.add(firstParIdx, left);
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

    private List<Paragraph<S>> stringToParagraphs(String str, S style) {
        String[] strings = str.split("\n", -1);
        List<Paragraph<S>> res = new ArrayList<>(strings.length);
        for(String s: strings) {
            res.add(new Paragraph<S>(s, style));
        }
        return res;
    }
}