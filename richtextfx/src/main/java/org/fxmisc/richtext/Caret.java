package org.fxmisc.richtext;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.IndexRange;


public interface Caret {

    int getCaretPosition();

    ObservableValue<Integer> caretPositionProperty();

    int getAnchor();

    ObservableValue<Integer> anchorProperty();

    IndexRange getSelection();

    ObservableValue<IndexRange> selectionProperty();

    String getSelectedText();

    ObservableValue<String> selectedTextProperty();

    int getCaretParagraph();

    ObservableValue<Integer> caretParagraphProperty();

    int getCaretColumn();

    ObservableValue<Integer> caretColumnProperty();

}
