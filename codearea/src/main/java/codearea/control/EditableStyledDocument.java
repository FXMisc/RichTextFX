package codearea.control;

import static codearea.control.TwoDimensional.Bias.*;
import inhibeans.property.ReadOnlyIntegerWrapper;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import reactfx.PushSource;
import reactfx.Source;

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
    private final StringBinding text = BindingFactories.createStringBinding(() -> getText(0, getLength()));
    @Override
    public String getText() { return text.get(); }
    public ObservableStringValue textProperty() { return text; }

    /**
     * Length of this {@code StyledDocument}.
     */
    private final ReadOnlyIntegerWrapper length = new ReadOnlyIntegerWrapper();
    @Override
    public int getLength() { return length.get(); }
    public ObservableIntegerValue lengthProperty() { return length.getReadOnlyProperty(); }

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

    private final PushSource<TextChange> textChanges = new PushSource<>();
    public Source<TextChange> textChanges() { return textChanges; }


    /**************************************************************************
     *                                                                        *
     * Constructors                                                           *
     *                                                                        *
     **************************************************************************/

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
        if (replacement == null)
            throw new NullPointerException("replacement text is null");

        Position start2D = navigator.offsetToPosition(start, Forward);
        Position end2D = start2D.offsetBy(end - start, Forward);
        int firstParIdx = start2D.getMajor();
        int firstParFrom = start2D.getMinor();
        int lastParIdx = end2D.getMajor();
        int lastParTo = end2D.getMinor();

        replacement = filterInput(replacement);
        String replacedText = getText(start, end);

        // Get the leftovers after cutting out the deletion
        Paragraph<S> firstPar = paragraphs.get(firstParIdx);
        Paragraph<S> lastPar = paragraphs.get(lastParIdx);
        Paragraph<S> left = firstPar.trim(firstParFrom);
        Paragraph<S> right = lastPar.subParagraph(lastParTo);

        String[] replacementLines = replacement.split("\n", -1);
        int n = replacementLines.length;

        S replacementStyle = firstPar.getStyleAt(firstParFrom-1);

        if(n == 1) {
            // replacement is just a single line,
            // use it to join the two leftover lines
            left = left.append(replacementLines[0]).append(right);

            // replace the affected liens with the merger of leftovers and the replacement line
            // TODO: use setAll(from, to, col) when implemented (see https://javafx-jira.kenai.com/browse/RT-32655)
            paragraphs.set(firstParIdx, left); // use set() instead of remove and add to make sure the number of lines is never 0
            paragraphs.remove(firstParIdx+1, lastParIdx+1);
        } else {
            // append the first replacement line to the left leftover
            // and prepend the last replacement line to the right leftover
            left = left.append(replacementLines[0]);
            right = right.insert(0, replacementLines[n-1]);

            // create list of new lines to replace the affected lines
            List<Paragraph<S>> newLines = new ArrayList<>(n-1);
            for(int i = 1; i < n - 1; ++i)
                newLines.add(new Paragraph<S>(replacementLines[i], replacementStyle));
            newLines.add(right);

            // replace the affected lines with the new lines
            // TODO: use setAll(from, to, col) when implemented (see https://javafx-jira.kenai.com/browse/RT-32655)
            paragraphs.set(firstParIdx, left); // use set() instead of remove and add to make sure the number of lines is never 0
            paragraphs.remove(firstParIdx+1, lastParIdx+1);
            paragraphs.addAll(firstParIdx+1, newLines);
        }

        // update length, invalidate text
        int newLength = length.get() - (end - start) + replacement.length();
        length.blockWhile(() -> { // don't publish length change until text is invalidated
            length.set(newLength);
            text.invalidate();
        });

        // emit change event
        fireTextChange(start, replacedText, replacement);
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

    private void fireTextChange(int pos, String removedText, String addedText) {
        textChanges.push(new TextChange(pos, removedText, addedText));
    }

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
}