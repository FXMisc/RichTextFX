package org.fxmisc.easybind.select;

import javafx.beans.value.ObservableValue;

import org.fxmisc.easybind.UnbindableBinding;

public interface SelectBuilder<T> {
    <U> SelectBuilder<U> select(Selector<T, U> selector);
    <U> UnbindableBinding<U> selectObject(Selector<T, U> selector);

    static <T> SelectBuilder<T> startAt(ObservableValue<T> selectionRoot) {
        return new RootSelectedBuilder<T>(selectionRoot);
    }
}