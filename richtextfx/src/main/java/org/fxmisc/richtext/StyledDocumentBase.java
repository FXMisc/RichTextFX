package org.fxmisc.richtext;

import java.util.List;
import java.util.Objects;

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
    public String toString() {
        return paragraphs
                .stream()
                .map(Paragraph::toString)
                .reduce((p1, p2) -> p1 + "\n" + p2)
                .orElse("");
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
