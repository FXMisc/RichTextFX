package org.fxmisc.richtext.util.skin;

import java.util.Collections;
import java.util.List;

import javafx.css.CssMetaData;
import javafx.css.Styleable;

public interface Visual {
    void dispose();

    default List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        // Return empty list by default.
        return Collections.emptyList();
    }
}
