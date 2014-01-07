package codearea.undo;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import codearea.rx.Source;

public interface UndoManagerProvider<C> {

    UndoManager get(
            Consumer<C> apply,
            Consumer<C> undo,
            BiFunction<C, C, Optional<C>> merge,
            Source<C> changeSource);
}
