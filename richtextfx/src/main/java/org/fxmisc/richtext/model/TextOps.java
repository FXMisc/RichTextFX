package org.fxmisc.richtext.model;

import org.reactfx.util.Either;

import java.util.Optional;
import java.util.function.BiFunction;

public interface TextOps<SEG, S> extends SegmentOps<SEG, S> {
    public SEG create(String text);

    public default <R> TextOps<Either<SEG, R>, S> _or(SegmentOps<R, S> rOps, BiFunction<S, S, Optional<S>> mergeStyle) {
        return eitherL(this, rOps, mergeStyle);
    }

    public static <L, R, S> TextOps<Either<L, R>, S> eitherL(TextOps<L, S> lOps, SegmentOps<R, S> rOps,
                                                             BiFunction<S, S, Optional<S>> mergeStyle) {
        return new LeftTextOps<>(lOps, rOps, mergeStyle);
    }

    public static <L, R, S> TextOps<Either<L, R>, S> eitherR(SegmentOps<L, S> lOps, TextOps<R, S> rOps,
                                                             BiFunction<S, S, Optional<S>> mergeStyle) {
        return new RightTextOps<>(lOps, rOps, mergeStyle);
    }

}

class LeftTextOps<L, R, S> extends EitherSegmentOps<L, R, S> implements TextOps<Either<L, R>, S> {

    private final TextOps<L, S> lOps;

    LeftTextOps(TextOps<L, S> lOps, SegmentOps<R, S> rOps, BiFunction<S, S, Optional<S>> mergeStyle) {
        super(lOps, rOps, mergeStyle);
        this.lOps = lOps;
    }

    @Override
    public Either<L, R> create(String text) {
        return Either.left(lOps.create(text));
    }

}

class RightTextOps<L, R, S> extends EitherSegmentOps<L, R, S> implements TextOps<Either<L, R>, S> {

    private final TextOps<R, S> rOps;

    RightTextOps(SegmentOps<L, S> lOps, TextOps<R, S> rOps, BiFunction<S, S, Optional<S>> mergeStyle) {
        super(lOps, rOps, mergeStyle);
        this.rOps = rOps;
    }

    @Override
    public Either<L, R> create(String text) {
        return Either.right(rOps.create(text));
    }

}