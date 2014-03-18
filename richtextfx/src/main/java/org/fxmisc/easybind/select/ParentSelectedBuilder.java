package org.fxmisc.easybind.select;

import org.fxmisc.easybind.UnbindableBinding;

interface ParentSelectedBuilder<T> extends SelectBuilder<T> {
    @Override
    default <U> SelectBuilder<U> select(Selector<T, U> selector) {
        return new IntermediateSelectedBuilder<T, U>(this, selector);
    }

    @Override
    default <U> UnbindableBinding<U> selectObject(Selector<T, U> selector) {
        NestedSelectionElementFactory<T, U> leafSelectionFactory = onInvalidation -> {
            return new LeafSelectionElement<T, U>(onInvalidation, selector);
        };
        return create(leafSelectionFactory);
    }

    <U> UnbindableBinding<U> create(NestedSelectionElementFactory<T, U> nestedSelectionFactory);
}