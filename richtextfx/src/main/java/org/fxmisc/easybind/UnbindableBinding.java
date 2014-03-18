package org.fxmisc.easybind;

import javafx.beans.binding.Binding;

/**
 * Binding that can be unbound from its inputs.
 * When this binding's lifetime is shorter than the lifetime of its inputs,
 * {@link #unbind()} should be called to prevent leaks.
 * @param <T>
 */
public interface UnbindableBinding<T> extends Binding<T> {
    /**
     * Stops observing the inputs for invalidations. When this binding is
     * no longer being used and its inputs are still in use, this method
     * should be called to allow this binding to be garbage collected.
     */
    void unbind();
}