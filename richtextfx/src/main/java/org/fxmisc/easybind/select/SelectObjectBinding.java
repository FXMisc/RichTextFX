package org.fxmisc.easybind.select;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;

import org.fxmisc.easybind.UnbindableBinding;

class SelectObjectBinding<T, U> extends ObjectBinding<U> implements UnbindableBinding<U> {
    private final ObservableValue<T> root;
    private final NestedSelectionElement<T, U> nestedSelection;
    private final InvalidationListener rootInvalidationListener;

    public SelectObjectBinding(
            ObservableValue<T> root,
            NestedSelectionElementFactory<T, U> nestedSelectionFactory) {
        this.root = root;
        nestedSelection = nestedSelectionFactory.create(this::invalidate);
        rootInvalidationListener = obs -> {
            nestedSelection.disconnect();
            invalidate();
        };
        root.addListener(rootInvalidationListener);
    }

    @Override
    protected U computeValue() {
        if(!nestedSelection.isConnected()) {
            T rootVal = root.getValue();
            if(rootVal == null) {
                return null;
            }
            nestedSelection.connect(rootVal);
        }
        return nestedSelection.getValue();
    }

    @Override
    public void unbind() {
        root.removeListener(rootInvalidationListener);
        nestedSelection.disconnect();
    }
}