package org.fxmisc.richtext.model;

import java.util.Optional;
import java.util.function.BiFunction;

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

    public Optional<SEG> joinSeg(SEG currentSeg, SEG nextSeg);

    default Optional<S> joinStyle(S currentStyle, S nextStyle) {
        return Optional.empty();
    }

    public SEG createEmptySeg();

    public static <S> TextOps<String, S> styledTextOps() {
        return styledTextOps((s1, s2) -> Optional.empty());
    }

    public static <S> TextOps<String, S> styledTextOps(BiFunction<S, S, Optional<S>> mergeStyle) {
        return new TextOpsBase<String, S>("") {
            @Override
            public char realCharAt(String s, int index) {
                return s.charAt(index);
            }

            @Override
            public String realGetText(String s) {
                return s;
            }

            @Override
            public String realSubSequence(String s, int start, int end) {
                return s.substring(start, end);
            }

            @Override
            public String create(String text) {
                return text;
            }

            @Override
            public int length(String s) {
                return s.length();
            }

            @Override
            public Optional<String> joinSeg(String currentSeg, String nextSeg) {
                return Optional.of(currentSeg + nextSeg);
            }

            @Override
            public Optional<S> joinStyle(S currentStyle, S nextStyle) {
                return mergeStyle.apply(currentStyle, nextStyle);
            }
        };
    }

    public default <R> SegmentOps<Either<SEG, R>, S> or(SegmentOps<R, S> rOps) {
        return either(this, rOps);
    }

    public default <RSeg, RStyle> SegmentOps<Either<SEG, RSeg>, Either<S, RStyle>> orStyled(
            SegmentOps<RSeg, RStyle> rOps
    ) {
        return eitherStyles(this, rOps);
    }

    public static <LSeg, LStyle, RSeg, RStyle> SegmentOps<Either<LSeg, RSeg>, Either<LStyle, RStyle>> eitherStyles(
            SegmentOps<LSeg, LStyle> lOps,
            SegmentOps<RSeg, RStyle> rOps) {
        return new EitherStyledSegmentOps<>(lOps, rOps);
    }

    public static <L, R, S> SegmentOps<Either<L, R>, S> either(SegmentOps<L, S> lOps, SegmentOps<R, S> rOps) {
        return either(lOps, rOps, (leftStyle, rightStyle) -> Optional.empty());
    }

    public static <L, R, S> SegmentOps<Either<L, R>, S> either(SegmentOps<L, S> lOps, SegmentOps<R, S> rOps,
                                                               BiFunction<S, S, Optional<S>> mergeStyle) {
        return new EitherSegmentOps<>(lOps, rOps, mergeStyle);
    }
}

class EitherSegmentOps<L, R, S> implements SegmentOps<Either<L, R>, S> {

    private final SegmentOps<L, S> lOps;
    private final SegmentOps<R, S> rOps;
    private final BiFunction<S, S, Optional<S>> mergeStyle;

    EitherSegmentOps(SegmentOps<L, S> lOps, SegmentOps<R, S> rOps, BiFunction<S, S, Optional<S>> mergeStyle) {
        this.lOps = lOps;
        this.rOps = rOps;
        this.mergeStyle = mergeStyle;
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
    public Optional<Either<L, R>> joinSeg(Either<L, R> left, Either<L, R> right) {
        return left.unify(ll -> right.unify(rl -> lOps.joinSeg(ll, rl).map(Either::left), rr -> Optional.empty()),
                          lr -> right.unify(rl -> Optional.empty(), rr -> rOps.joinSeg(lr, rr).map(Either::right)));
    }

    @Override
    public Optional<S> joinStyle(S currentStyle, S nextStyle) {
        return mergeStyle.apply(currentStyle, nextStyle);
    }

    public Either<L, R> createEmptySeg() {
        return Either.left(lOps.createEmptySeg());
    }
}

class EitherStyledSegmentOps<LSeg, RSeg, LStyle, RStyle> implements SegmentOps<Either<LSeg, RSeg>, Either<LStyle, RStyle>> {

    private final SegmentOps<LSeg, LStyle> lOps;
    private final SegmentOps<RSeg, RStyle> rOps;

    EitherStyledSegmentOps(SegmentOps<LSeg, LStyle> lOps, SegmentOps<RSeg, RStyle> rOps) {
        this.lOps = lOps;
        this.rOps = rOps;
    }


    @Override
    public int length(Either<LSeg, RSeg> seg) {
        return seg.unify(
                lOps::length,
                rOps::length
        );
    }

    @Override
    public char charAt(Either<LSeg, RSeg> seg, int index) {
        return seg.unify(
                lSeg -> lOps.charAt(lSeg, index),
                rSeg -> rOps.charAt(rSeg, index)
        );
    }

    @Override
    public String getText(Either<LSeg, RSeg> seg) {
        return seg.unify(lOps::getText, rOps::getText);
    }

    @Override
    public Either<LSeg, RSeg> subSequence(Either<LSeg, RSeg> seg, int start, int end) {
        return seg.map(
                lSeg -> lOps.subSequence(lSeg, start, end),
                rSeg -> rOps.subSequence(rSeg, start, end)
        );
    }

    @Override
    public Either<LSeg, RSeg> subSequence(Either<LSeg, RSeg> seg, int start) {
        return seg.map(lSeg -> lOps.subSequence(lSeg, start),
                rSeg -> rOps.subSequence(rSeg, start));
    }

    @Override
    public Optional<Either<LSeg, RSeg>> joinSeg(Either<LSeg, RSeg> left, Either<LSeg, RSeg> right) {
        return left.unify(
                ll -> right.unify(
                        rl -> lOps.joinSeg(ll, rl).map(Either::left),
                        rr -> Optional.empty()),
                lr -> right.unify(
                        rl -> Optional.empty(),
                        rr -> rOps.joinSeg(lr, rr).map(Either::right))
        );
    }

    @Override
    public Optional<Either<LStyle, RStyle>> joinStyle(Either<LStyle, RStyle> left, Either<LStyle, RStyle> right) {
        return left.unify(
                ll -> right.unify(
                        rl -> lOps.joinStyle(ll, rl).map(Either::left),
                        rr -> Optional.empty()),
                lr -> right.unify(
                        rl -> Optional.empty(),
                        rr -> rOps.joinStyle(lr, rr).map(Either::right))
        );
    }

    public Either<LSeg, RSeg> createEmptySeg() {
        return Either.left(lOps.createEmptySeg());
    }
}