package codearea.control;

import static codearea.control.TwoDimensional.Bias.*;
import inhibeans.property.ReadOnlyIntegerWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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

    private StyledDocument(List<Paragraph<S>> pars) {
        paragraphs.addAll(pars);
        length.set(pars.stream().mapToInt(p -> p.length()).sum() + (pars.size()-1));
    };


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
        return sub(
                start, end,
                p -> p.toString(),
                (p, a, b) -> p.substring(a, b),
                (List<String> ss) -> join(ss, "\n"));
    }

    public StyledDocument<S> subDocument(IndexRange range) {
        return subDocument(range.getStart(), range.getEnd());
    }

    public StyledDocument<S> subDocument(int start, int end) {
        return sub(
                start, end,
                p -> p,
                (p, a, b) -> p.subParagraph(a, b),
                (List<Paragraph<S>> pars) -> new StyledDocument<S>(pars));
    }

    public S getStyleAt(int pos) {
        Position pos2D = navigator.offsetToPosition(pos, Forward);
        int line = pos2D.getMajor();
        int col = pos2D.getMinor();
        return paragraphs.get(line).getStyleAt(col);
    }

    public S getStyleAt(int paragraph, int column) {
        return paragraphs.get(paragraph).getStyleAt(column);
    }

    @Override
    public Position offsetToPosition(int offset, Bias bias) {
        return navigator.offsetToPosition(offset, bias);
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


    /***************************************************************************
     *                                                                         *
     * Private methods                                                         *
     *                                                                         *
     **************************************************************************/

    private String getContent() {
        return getText(0, getLength());
    }

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
     * @param <P> type to which paragraphs are mapped.
     * @param <R> type of the resulting sub-document.
     */
    private <P, R> R sub(
            int start, int end,
            Function<Paragraph<S>, P> map,
            SubMap<Paragraph<S>, P> subMap,
            Function<List<P>, R> combine) {

        int length = end - start;

        Position start2D = navigator.offsetToPosition(start, Forward);
        Position end2D = start2D.offsetBy(length, Forward); // Forward to make sure newline is not lost
        int p1 = start2D.getMajor();
        int col1 = start2D.getMinor();
        int p2 = end2D.getMajor();
        int col2 = end2D.getMinor();

        List<P> pars = new ArrayList<>(p2 - p1 + 1);

        if(p1 == p2) {
            pars.add(subMap.subrange(paragraphs.get(p1), col1, col2));
        } else {
            Paragraph<S> par1 = paragraphs.get(p1);
            pars.add(subMap.subrange(par1, col1, par1.length()));

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
    private static String join(List<String> list, String sep) {
        int len = list.stream().mapToInt(s -> s.length()).sum() + (list.size()-1) * sep.length();
        StringBuilder sb = new StringBuilder(len);
        sb.append(list.get(0));
        for(int i = 1; i < list.size(); ++i) {
            sb.append(sep).append(list.get(i));
        }
        return sb.toString();
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