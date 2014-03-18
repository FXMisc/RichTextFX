package org.fxmisc.easybind.select;

import javafx.beans.value.ObservableValue;

@FunctionalInterface
public interface Selector<T, U> {
    ObservableValue<U> select(T t);
}