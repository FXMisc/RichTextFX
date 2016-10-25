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

    public Optional<SEG> subSequence(SEG seg, int start, int end);

    public Optional<SEG> subSequence(SEG seg, int start);

    public S defaultStyle();

    public S getStyle(SEG seg);

    public SEG setStyle(SEG seg, S style);

    public Optional<SEG> join(SEG currentSeg, SEG nextSeg);

    public default <R> SegmentOps<Either<SEG, R>, S> or(SegmentOps<R, S> rOps) {
        return either(this, rOps, EitherSegmentOps.StyleChoice.LEFT);
    }

    public default <R> TextOps<Either<SEG, R>, S> or_(TextOps<R, S> rOps, EitherSegmentOps.StyleChoice choice) {
        return TextOps.eitherR(this, rOps, choice);
    }

    public static <L, R, S> SegmentOps<Either<L, R>, S> either(SegmentOps<L, S> lOps, SegmentOps<R, S> rOps, EitherSegmentOps.StyleChoice choice) {
        return new EitherSegmentOps<>(lOps, rOps, choice);
    }
}

class EitherSegmentOps<L, R, S> implements SegmentOps<Either<L, R>, S> {

    public static enum StyleChoice {
        LEFT,
        RIGHT
    }

    private final SegmentOps<L, S> lOps;
    private final SegmentOps<R, S> rOps;
    private final StyleChoice choice;

    EitherSegmentOps(SegmentOps<L, S> lOps, SegmentOps<R, S> rOps, StyleChoice choice) {
        this.lOps = lOps;
        this.rOps = rOps;
        this.choice = choice;
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
    public Optional<Either<L, R>> subSequence(Either<L, R> seg, int start, int end) {
        return seg.unify(ll -> seg.unify(l -> lOps.subSequence(l, start, end).map(Either::left),
                                         e -> Optional.empty()),
                         rr -> seg.unify(e -> Optional.empty(),
                                         r -> rOps.subSequence(r, start, end).map(Either::right)));
    }

    @Override
    public Optional<Either<L, R>> subSequence(Either<L, R> seg, int start) {
        return seg.unify(ll -> seg.unify(l -> lOps.subSequence(l, start).map(Either::left),
                                         e -> Optional.empty()),
                         rr -> seg.unify(e -> Optional.empty(),
                                         r -> rOps.subSequence(r, start).map(Either::right)));
    }

    @Override
    public S defaultStyle() {
        return choice == StyleChoice.LEFT
                ? lOps.defaultStyle()
                : rOps.defaultStyle();
    }

    @Override
    public S getStyle(Either<L, R> seg) {
        return seg.unify(l -> lOps.getStyle(l),
                         r -> rOps.getStyle(r));
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
}