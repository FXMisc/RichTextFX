package codearea.control;

import inhibeans.property.ReadOnlyIntegerWrapper;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.IndexRange;
import reactfx.PushSource;
import reactfx.Source;

/**
 * Content model for {@link StyledTextArea}. Implements edit operations
 * on styled text, but not worrying about additional aspects such as
 * caret or selection.
 */
final class StyledDocument<S> implements TwoDimensional {

    /***************************************************************************
     *                                                                         *
     * Observables                                                             *
     *                                                                         *
     * Observables are "dynamic" (i.e. changing) characteristics of an object. *
     * They are not directly settable by the client code, but change in        *
     * response to user input and/or API actions.                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Content of this {@code StyledDocument}.
     */
    private final StringBinding text = BindingFactories.createStringBinding(() -> getContent());
    public String getText() { return text.get(); }
    public ObservableStringValue textProperty() { return text; }

    /**
     * Length of this {@code StyledDocument}.
     */
    private final ReadOnlyIntegerWrapper length = new ReadOnlyIntegerWrapper();
    public int getLength() { return length.get(); }
    public ObservableIntegerValue lengthProperty() { return length.getReadOnlyProperty(); }

    /**
     * Unmodifiable observable list of styled paragraphs of this document.
     */
    public ObservableList<Paragraph<S>> getParagraphs() {
        return FXCollections.unmodifiableObservableList(paragraphs);
    }


    /***************************************************************************
     *                                                                         *
     * Event streams                                                           *
     *                                                                         *
     **************************************************************************/

    private final PushSource<TextChange> textChanges = new PushSource<>();
    public Source<TextChange> textChanges() { return textChanges; }


    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     ***************************************************************************/

    private final ObservableList<Paragraph<S>> paragraphs =
            FXCollections.observableArrayList();

    private final TwoLevelNavigator navigator = new TwoLevelNavigator(
            () -> paragraphs.size(),
            i -> {
                int len = paragraphs.get(i).length();
                // add 1 for newline to every paragraph except last
                return i == paragraphs.size()-1 ? len : len + 1;
            });


    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     ***************************************************************************/

    StyledDocument(S initialStyle) {
        paragraphs.add(new Paragraph<S>("", initialStyle));
        length.set(0);
    }


    /***************************************************************************
     *                                                                         *
     * Queries                                                                 *
     *                                                                         *
     * Queries are parameterized observables.                                  *
     *                                                                         *
     ***************************************************************************/

    public String getText(IndexRange range) {
        return getText(range.getStart(), range.getEnd());
    }

    public String getText(int start, int end) {
        int length = end - start;
        StringBuilder sb = new StringBuilder(length);

        Position start2D = navigator.offsetToPosition(start);
        Position end2D = start2D.offsetBy(length);
        int p1 = start2D.getMajor();
        int col1 = start2D.getMinor();
        int p2 = end2D.getMajor();
        int col2 = end2D.getMinor();

        if(p1 == p2) {
            sb.append(paragraphs.get(p1).substring(col1, col2));
        } else {
            sb.append(paragraphs.get(p1).substring(col1));
            sb.append('\n');

            for(int i = p1 + 1; i < p2; ++i) {
                sb.append(paragraphs.get(i).toString());
                sb.append('\n');
            }

            sb.append(paragraphs.get(p2).substring(0, col2));
        }

        // If we were instructed to go beyond the end in a non-last paragraph,
        // we omitted a newline. Add it back.
        if(col2 > paragraphs.get(p2).length() && p2 < paragraphs.size() - 1) {
            sb.append('\n');
        }

        return sb.toString();
    }

    public S getStyleAt(int pos) {
        Position pos2D = navigator.offsetToPosition(pos);
        int line = pos2D.getMajor();
        int col = pos2D.getMinor();
        return paragraphs.get(line).getStyleAt(col);
    }

    public S getStyleAt(int paragraph, int column) {
        return paragraphs.get(paragraph).getStyleAt(column);
    }

    @Override
    public Position offsetToPosition(int offset) {
        return navigator.offsetToPosition(offset);
    }

    @Override
    public Position position(int row, int col) {
        return navigator.position(row, col);
    }


    /***************************************************************************
     *                                                                         *
     * Actions                                                                 *
     *                                                                         *
     * Actions change the state of the object. They typically cause a change   *
     * of one or more observables and/or produce an event.                     *
     *                                                                         *
     **************************************************************************/

    public void replaceText(int start, int end, String replacement) {
        if (replacement == null)
            throw new NullPointerException("replacement text is null");

        Position start2D = navigator.offsetToPosition(start);
        Position end2D = start2D.offsetBy(end - start);
        int leadingLineIndex = start2D.getMajor();
        int leadingLineFrom = start2D.getMinor();
        int trailingLineIndex = end2D.getMajor();
        int trailingLineTo = end2D.getMinor();

        replacement = filterInput(replacement);
        String replacedText = getText(start, end);

        // Get the leftovers after cutting out the deletion
        Paragraph<S> leadingLine = paragraphs.get(leadingLineIndex);
        Paragraph<S> trailingLine = paragraphs.get(trailingLineIndex);
        Paragraph<S> left = leadingLine.split(leadingLineFrom)[0];
        Paragraph<S> right = trailingLine.split(trailingLineTo)[1];

        String[] replacementLines = replacement.split("\n", -1);
        int n = replacementLines.length;

        S replacementStyle = leadingLine.getStyleAt(leadingLineFrom-1);

        if(n == 1) {
            // replacement is just a single line,
            // use it to join the two leftover lines
            left.append(replacementLines[0]);
            left.appendFrom(right);

            // replace the affected liens with the merger of leftovers and the replacement line
            // TODO: use setAll(from, to, col) when implemented (see https://javafx-jira.kenai.com/browse/RT-32655)
            paragraphs.set(leadingLineIndex, left); // use set() instead of remove and add to make sure the number of lines is never 0
            paragraphs.remove(leadingLineIndex+1, trailingLineIndex+1);
        }
        else {
            // append the first replacement line to the left leftover
            // and prepend the last replacement line to the right leftover
            left.append(replacementLines[0]);
            right.insert(0, replacementLines[n-1]);

            // create list of new lines to replace the affected lines
            List<Paragraph<S>> newLines = new ArrayList<>(n-1);
            for(int i = 1; i < n - 1; ++i)
                newLines.add(new Paragraph<S>(replacementLines[i], replacementStyle));
            newLines.add(right);

            // replace the affected lines with the new lines
            // TODO: use setAll(from, to, col) when implemented (see https://javafx-jira.kenai.com/browse/RT-32655)
            paragraphs.set(leadingLineIndex, left); // use set() instead of remove and add to make sure the number of lines is never 0
            paragraphs.remove(leadingLineIndex+1, trailingLineIndex+1);
            paragraphs.addAll(leadingLineIndex+1, newLines);
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
        Position start = navigator.offsetToPosition(from);
        Position end = start.offsetBy(to - from);
        int firstLineIndex = start.getMajor();
        int firstLineFrom = start.getMinor();
        int lastLineIndex = end.getMajor();
        int lastLineTo = end.getMinor();

        if(from == to)
            return;

        if(firstLineIndex == lastLineIndex) {
            setStyle(firstLineIndex, firstLineFrom, lastLineTo, style);
        }
        else {
            int firstLineLen = paragraphs.get(firstLineIndex).length();
            setStyle(firstLineIndex, firstLineFrom, firstLineLen, style);
            for(int i=firstLineIndex+1; i<lastLineIndex; ++i) {
                setStyle(i, style);
            }
            setStyle(lastLineIndex, 0, lastLineTo, style);
        }
    }

    public void setStyle(int paragraph, S style) {
        Paragraph<S> p = paragraphs.get(paragraph);
        p.setStyle(style);
        paragraphs.set(paragraph, p); // to generate change event
    }

    public void setStyle(int paragraph, int fromCol, int toCol, S style) {
        Paragraph<S> p = paragraphs.get(paragraph);
        p.setStyle(fromCol, toCol, style);
        paragraphs.set(paragraph, p); // to generate change event
    }


    /***************************************************************************
     *                                                                         *
     * Private methods                                                         *
     *                                                                         *
     **************************************************************************/

    private String getContent() {
        return getText(0, getLength());
    }

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