package org.fxmisc.richtext.model;

/**
 * @param <S> the data type
 * @param <Self> the type of the subclass
 */
public interface TextChangeData<S, Self extends TextChangeData<S, Self>> {
    int length();
    Self concat(Self b);
    Self sub(int from, int to);
    default Self sub(int to) { return sub(0, to); }
    // Optimally, this should not exist, it breaks encapsulation. But during cleanup that seemed
    // the only way to avoid rewriting more code
    S data();
}
