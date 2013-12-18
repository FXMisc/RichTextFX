package codearea.rx;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PushSource<T> implements Source<T>, Sink<T> {

    private final List<Consumer<T>> valueSubscribers = new ArrayList<>();
    private final List<Runnable> onCompletedSubscribers = new ArrayList<>();

    @Override
    public void push(T value) {
        for(Consumer<T> subscriber: valueSubscribers) {
            subscriber.accept(value);
        }
    }

    @Override
    public void close() {
        for(Runnable onCompleted: onCompletedSubscribers) {
            onCompleted.run();
        }
    }

    @Override
    public Subscription subscribe(Consumer<T> consumer) {
        valueSubscribers.add(consumer);
        return () -> valueSubscribers.remove(consumer);
    }

    @Override
    public Subscription subscribe(Consumer<T> consumer, Runnable onCompleted) {
        valueSubscribers.add(consumer);
        onCompletedSubscribers.add(onCompleted);
        return () -> {
            valueSubscribers.remove(consumer);
            onCompletedSubscribers.remove(onCompleted);
        };
    }
}
