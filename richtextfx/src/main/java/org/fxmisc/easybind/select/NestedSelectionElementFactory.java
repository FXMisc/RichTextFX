package org.fxmisc.easybind.select;

@FunctionalInterface
interface NestedSelectionElementFactory<T, U> {
    NestedSelectionElement<T, U> create(Runnable invalidationCallback);
}
