package undo;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import reactfx.Source;
import undo.impl.UndoManagerImpl;

public interface UndoManagerFactory<C> {

    UndoManager create(
            Consumer<C> apply,
            Consumer<C> undo,
            BiFunction<C, C, Optional<C>> merge,
            Source<C> changeSource);

    public static <C> UndoManagerFactory<C> defaultFactory() {
        return (apply, undo, merge, changeSource) -> new UndoManagerImpl<>(apply, undo, merge, changeSource);
    }
}
