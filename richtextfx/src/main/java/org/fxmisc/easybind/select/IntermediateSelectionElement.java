package org.fxmisc.easybind.select;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ObservableValue;

class IntermediateSelectionElement<T, U, V> implements NestedSelectionElement<T, V> {
    private final InvalidationListener observableInvalidationListener = obs -> observableInvalidated();
    private final Selector<T, U> selector;
    private final NestedSelectionElement<U, V> nested;
    private final Runnable onInvalidation;

    private ObservableValue<U> observable = null;

    public IntermediateSelectionElement(
            Runnable onInvalidation,
            Selector<T, U> selector,
            NestedSelectionElementFactory<U, V> nestedSelectionFactory) {
        this.onInvalidation = onInvalidation;
        this.selector = selector;
        this.nested = nestedSelectionFactory.create(this::nestedInvalidated);
    }

    @Override
    public void connect(T baseVal) {
        if(isConnected()) {
            throw new IllegalStateException("Already connected");
        }

        observable = selector.select(baseVal);
        observable.addListener(observableInvalidationListener);
    }

    @Override
    public void disconnect() {
        if(isConnected()) {
            nested.disconnect();
            observable.removeListener(observableInvalidationListener);
            observable = null;
        }
    }

    @Override
    public final boolean isConnected() {
        return observable != null;
    }

    @Override
    public V getValue() {
        if(!isConnected()) {
            throw new IllegalStateException("Not connected");
        }

        if(!nested.isConnected()) {
            U observableVal = observable.getValue();
            if(observableVal == null) {
                return null;
            }
            nested.connect(observableVal);
        }

        return nested.getValue();
    }

    private void nestedInvalidated() {
        onInvalidation.run();
    }

    private void observableInvalidated() {
        nested.disconnect();
        onInvalidation.run();
    }
}