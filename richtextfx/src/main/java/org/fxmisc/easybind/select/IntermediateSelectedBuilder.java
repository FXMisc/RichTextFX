package org.fxmisc.easybind.select;

import org.fxmisc.easybind.UnbindableBinding;

class IntermediateSelectedBuilder<T, U> implements ParentSelectedBuilder<U> {
    private final ParentSelectedBuilder<T> parent;
    private final Selector<T, U> selector;

    public IntermediateSelectedBuilder(ParentSelectedBuilder<T> parent, Selector<T, U> selector) {
        this.parent = parent;
        this.selector = selector;
    }

    @Override
    public <V> UnbindableBinding<V> create(
            NestedSelectionElementFactory<U, V> nestedSelectionFactory) {
        NestedSelectionElementFactory<T, V> intermediateSelectionFactory = onInvalidation -> {
            return new IntermediateSelectionElement<T, U, V>(onInvalidation, selector, nestedSelectionFactory);
        };
        return parent.create(intermediateSelectionFactory);
    }

}