package org.fxmisc.richtext.model;

import org.reactfx.util.Either;

public interface TextOps<SEG, S> extends SegmentOps<SEG, S> {
    public SEG create(String text, S style);

    public default <R> TextOps<Either<SEG, R>, S> _or(SegmentOps<R, S> rOps) {
        return eitherL(this, rOps);
    }

    public static <L, R, S> TextOps<Either<L, R>, S> eitherL(TextOps<L, S> lOps, SegmentOps<R, S> rOps) {
        return new LeftTextOps<>(lOps, rOps);
    }

    public static <L, R, S> TextOps<Either<L, R>, S> eitherR(SegmentOps<L, S> lOps, TextOps<R, S> rOps) {
        return new RightTextOps<>(lOps, rOps);
    }

}

class LeftTextOps<L, R, S> extends EitherSegmentOps<L, R, S> implements TextOps<Either<L, R>, S> {

    private final TextOps<L, S> lOps;

    LeftTextOps(TextOps<L, S> lOps, SegmentOps<R, S> rOps) {
        super(lOps, rOps);
        this.lOps = lOps;
    }

    @Override
    public Either<L, R> create(String text, S style) {
        return Either.left(lOps.create(text, style));
    }

}

class RightTextOps<L, R, S> extends EitherSegmentOps<L, R, S> implements TextOps<Either<L, R>, S> {

    private final TextOps<R, S> rOps;

    RightTextOps(SegmentOps<L, S> lOps, TextOps<R, S> rOps) {
        super(lOps, rOps);
        this.rOps = rOps;
    }

    @Override
    public Either<L, R> create(String text, S style) {
        return Either.right(rOps.create(text, style));
    }

}