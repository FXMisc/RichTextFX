package org.fxmisc.richtext;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.control.IndexRange;

import java.util.Optional;

public interface Selection {

    IndexRange getSelection();

    ObservableValue<IndexRange> selectionProperty();

    String getSelectedText();

    ObservableValue<String> selectedTextProperty();

    Optional<Bounds> getSelectionBounds();

    ObservableValue<Optional<Bounds>> selectionBoundsProperty();

}
