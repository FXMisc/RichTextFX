package org.fxmisc.richtext;

import java.util.function.Function;
import java.util.function.Supplier;

import javafx.beans.binding.Binding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;

/**
 * Methods for easy creation of bindings.
 */
class EasyBind {

    /**
     * Returns a binding that recomputes its value using the given function.
     * The returned binding is invalidated only by invoking its
     * {@link ObjectBinding#invalidate()} method.
     * @param computeValue
     * @return
     */
    public static <T> Binding<T> supply(Supplier<T> computeValue) {
        return new ObjectBinding<T>() {
            @Override
            protected T computeValue() {
                return computeValue.get();
            }
        };
    }

    public static <T, U> Binding<U> map(ObservableValue<T> dep, Function<T, U> f) {
        return new ObjectBinding<U>() {
            { bind(dep); }

            @Override
            protected U computeValue() {
                return f.apply(dep.getValue());
            }
        };
    }

}
