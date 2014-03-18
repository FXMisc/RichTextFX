package org.fxmisc.easybind;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.beans.binding.Binding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;

import org.fxmisc.easybind.select.SelectBuilder;

/**
 * Methods for easy creation of bindings.
 */
public class EasyBind {

    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    @FunctionalInterface
    public interface TetraFunction<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }

    @FunctionalInterface
    public interface PentaFunction<A, B, C, D, E, R> {
        R apply(A a, B b, C c, D d, E e);
    }

    @FunctionalInterface
    public interface HexaFunction<A, B, C, D, E, F, R> {
        R apply(A a, B b, C c, D d, E e, F f);
    }

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

    public static <T, U> Binding<U> map(
            ObservableValue<T> src,
            Function<T, U> f) {
        return new ObjectBinding<U>() {
            { bind(src); }

            @Override
            protected U computeValue() {
                return f.apply(src.getValue());
            }
        };
    }

    public static <A, B, R> Binding<R> combine(
            ObservableValue<A> src1,
            ObservableValue<B> src2,
            BiFunction<A, B, R> f) {
        return new ObjectBinding<R>() {
            { bind(src1, src2); }

            @Override
            protected R computeValue() {
                return f.apply(src1.getValue(), src2.getValue());
            }
        };
    }

    public static <A, B, C, R> Binding<R> combine(
            ObservableValue<A> src1,
            ObservableValue<B> src2,
            ObservableValue<C> src3,
            TriFunction<A, B, C, R> f) {
        return new ObjectBinding<R>() {
            { bind(src1, src2, src3); }

            @Override
            protected R computeValue() {
                return f.apply(
                        src1.getValue(), src2.getValue(), src3.getValue());
            }
        };
    }

    public static <A, B, C, D, R> Binding<R> combine(
            ObservableValue<A> src1,
            ObservableValue<B> src2,
            ObservableValue<C> src3,
            ObservableValue<D> src4,
            TetraFunction<A, B, C, D, R> f) {
        return new ObjectBinding<R>() {
            { bind(src1, src2, src3, src4); }

            @Override
            protected R computeValue() {
                return f.apply(
                        src1.getValue(), src2.getValue(),
                        src3.getValue(), src4.getValue());
            }
        };
    }

    public static <A, B, C, D, E, R> Binding<R> combine(
            ObservableValue<A> src1,
            ObservableValue<B> src2,
            ObservableValue<C> src3,
            ObservableValue<D> src4,
            ObservableValue<E> src5,
            PentaFunction<A, B, C, D, E, R> f) {
        return new ObjectBinding<R>() {
            { bind(src1, src2, src3, src4, src5); }

            @Override
            protected R computeValue() {
                return f.apply(
                        src1.getValue(), src2.getValue(), src3.getValue(),
                        src4.getValue(), src5.getValue());
            }
        };
    }

    public static <A, B, C, D, E, F, R> Binding<R> combine(
            ObservableValue<A> src1,
            ObservableValue<B> src2,
            ObservableValue<C> src3,
            ObservableValue<D> src4,
            ObservableValue<E> src5,
            ObservableValue<F> src6,
            HexaFunction<A, B, C, D, E, F, R> f) {
        return new ObjectBinding<R>() {
            { bind(src1, src2, src3, src4, src5, src6); }

            @Override
            protected R computeValue() {
                return f.apply(
                        src1.getValue(), src2.getValue(), src3.getValue(),
                        src4.getValue(), src5.getValue(), src6.getValue());
            }
        };
    }

    public static <T> SelectBuilder<T> select(ObservableValue<T> selectionRoot) {
        return SelectBuilder.startAt(selectionRoot);
    }
}
