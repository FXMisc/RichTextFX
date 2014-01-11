package reactfx;

public interface Sink<T> {
    void push(T value);
    void close();
}
