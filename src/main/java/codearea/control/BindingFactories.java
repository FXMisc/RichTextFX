package codearea.control;

import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableValue;

/**
 * Helper methods for creating bindings.
 */
class BindingFactories {

    public static StringBinding createStringBinding(Supplier<String> computeValue) {
        return new StringBinding() {
            @Override
            protected String computeValue() {
                return computeValue.get();
            }

        };
    }

    public static <T> ObjectBinding<T> createBinding(ObservableIntegerValue dep, IntFunction<T> computeValue) {
        return new ObjectBinding<T>() {
            { bind(dep); }

            @Override
            protected T computeValue() {
                return computeValue.apply(dep.get());
            }
        };
    }

    public static <A> IntegerBinding createIntegerBinding(ObservableValue<A> dep, ToIntFunction<A> computeValue) {
        return new IntegerBinding() {
            { bind(dep); }

            @Override
            protected int computeValue() {
                return computeValue.applyAsInt(dep.getValue());
            }
        };
    }

}
