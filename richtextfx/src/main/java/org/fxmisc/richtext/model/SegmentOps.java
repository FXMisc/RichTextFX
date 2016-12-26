package org.fxmisc.richtext.model;

import java.util.Optional;

import org.reactfx.util.Either;

/**
 * Defines the operations which are supported on a specific segment type.
 *
 * @param <SEG> The segment type
 * @param <S> The style type for the segment
 */
public interface SegmentOps<SEG, S> {
    public int length(SEG seg);

    public char charAt(SEG seg, int index);

    public String getText(SEG seg);

    public SEG subSequence(SEG seg, int start, int end);

    public SEG subSequence(SEG seg, int start);

    public S getStyle(SEG seg);

    public SEG setStyle(SEG seg, S style);

    public Optional<SEG> join(SEG currentSeg, SEG nextSeg);

    public SEG createEmpty();

    public default <R> SegmentOps<Either<SEG, R>, S> or(SegmentOps<R, S> rOps) {
        return either(this, rOps);
    }

    public default <R> TextOps<Either<SEG, R>, S> or_(TextOps<R, S> rOps) {
        return TextOps.eitherR(this, rOps);
    }

    public static <L, R, S> SegmentOps<Either<L, R>, S> either(SegmentOps<L, S> lOps, SegmentOps<R, S> rOps) {
        return new EitherSegmentOps<>(lOps, rOps);
    }
}

class EitherSegmentOps<L, R, S> implements SegmentOps<Either<L, R>, S> {

    private final SegmentOps<L, S> lOps;
    private final SegmentOps<R, S> rOps;

    EitherSegmentOps(SegmentOps<L, S> lOps, SegmentOps<R, S> rOps) {
        this.lOps = lOps;
        this.rOps = rOps;
    }


    @Override
    public int length(Either<L, R> seg) {
        return seg.unify(lOps::length, rOps::length);
    }

    @Override
    public char charAt(Either<L, R> seg, int index) {
        return seg.unify(l -> lOps.charAt(l, index),
                         r -> rOps.charAt(r, index));
    }

    @Override
    public String getText(Either<L, R> seg) {
        return seg.unify(lOps::getText, rOps::getText);
    }

    @Override
    public Either<L, R> subSequence(Either<L, R> seg, int start, int end) {
        return seg.map(l -> lOps.subSequence(l, start, end),
                       r -> rOps.subSequence(r, start, end));
    }

    @Override
    public Either<L, R> subSequence(Either<L, R> seg, int start) {
        return seg.map(l -> lOps.subSequence(l, start),
                       r -> rOps.subSequence(r, start));
    }

    @Override
    public S getStyle(Either<L, R> seg) {
        return seg.unify(lOps::getStyle,
                         rOps::getStyle);
    }

    @Override
    public Either<L, R> setStyle(Either<L, R> seg, S style) {
        return seg.map(l -> lOps.setStyle(l, style),
                       r -> rOps.setStyle(r, style));
    }

    @Override
    public Optional<Either<L, R>> join(Either<L, R> left, Either<L, R> right) {
        return left.unify(ll -> right.unify(rl -> lOps.join(ll, rl).map(Either::left), rr -> Optional.empty()),
                          lr -> right.unify(rl -> Optional.empty(), rr -> rOps.join(lr, rr).map(Either::right)));
    }

    public Either<L, R> createEmpty() {
        return Either.left(lOps.createEmpty());
    }
}