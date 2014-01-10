package codearea.rx;

public interface Sink<T> {
    void push(T value);
    void close();
}
