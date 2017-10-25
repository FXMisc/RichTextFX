package org.fxmisc.richtext.model;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;

import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

/**
 * Default implementation of {@link TwoDimensional} that makes it trivial to calculate a position within a
 * two dimensional object.
 */
public class TwoLevelNavigator implements TwoDimensional {

    private class Pos implements Position {
        private final int major;
        private final int minor;

        private Pos(int major, int minor) {
            this.major = major;
            this.minor = minor;
        }

        @Override
        public String toString() {
            return "(" + major + ", " + minor + ")";
        }

        @Override
        public boolean sameAs(Position other) {
            return getTargetObject() == other.getTargetObject()
                    && major == other.getMajor()
                    && minor == other.getMinor();
        }

        @Override
        public TwoDimensional getTargetObject() {
            return TwoLevelNavigator.this;
        }

        @Override
        public int getMajor() {
            return major;
        }

        @Override
        public int getMinor() {
            return minor;
        }

        @Override
        public Position clamp() {
            if(major == elemCount.getAsInt() - 1) {
                int elemLen = elemLength.applyAsInt(major);
                if(minor < elemLen) {
                    return this;
                } else {
                    return new Pos(major, elemLen-1);
                }
            } else {
                return this;
            }
        }

        @Override
        public Position offsetBy(int amount, Bias bias) {
            if(amount > 0) {
                return forward(amount, bias);
            } else if(amount < 0) {
                return backward(-amount, bias);
            } else if(minor == 0 && major > 1 && bias == Backward) {
                return new Pos(major - 1, elemLength.applyAsInt(major - 1));
            } else if(minor == elemLength.applyAsInt(major) && major < elemCount.getAsInt() - 1 && bias == Forward){
                return new Pos(major + 1, 0);
            } else {
                return this;
            }
        }

        @Override
        public int toOffset() {
            int offset = 0;
            for(int i = 0; i < major; ++i) {
                offset += elemLength.applyAsInt(i);
            }
            return offset + minor;
        }

        private Position forward(int offset, Bias bias) {
            offset += minor;
            int major = this.major;
            int curElemLength = elemLength.applyAsInt(major);

            int elemCount = TwoLevelNavigator.this.elemCount.getAsInt();
            while(major < elemCount - 1) {
                if(offset < curElemLength || offset == curElemLength && bias == Backward) {
                    return new Pos(major, offset);
                } else {
                    offset -= curElemLength;
                    major += 1;
                    curElemLength = elemLength.applyAsInt(major);
                }
            }

            // now the position is either in the last high-level element or beyond
            return new Pos(elemCount - 1, offset);
        }

        private Position backward(int offset, Bias bias) {
            int minor = this.minor;
            int major = this.major;
            while(major > 0) {
                if(offset < minor || offset == minor && bias == Forward) {
                    return new Pos(major, minor - offset);
                } else {
                    offset -= minor;
                    major -= 1; // move to the previous element
                    // set inner position to the end of the previous element
                    minor = elemLength.applyAsInt(major);
                }
            }

            if(offset < minor) {
                return new Pos(0, minor - offset);
            } else {
                // we went beyond the start
                return new Pos(0, 0);
            }
        }
    }

    private final IntSupplier elemCount;
    private final IntUnaryOperator elemLength;

    /**
     * Creates a navigator that can be used to find a {@link TwoDimensional.Position} within a two dimensional object.
     *
     * @param elemCount a supplier that returns the number of "inner lists" within an "outer list" (see
     *                  {@link TwoDimensional} for clarification). For example,
     *                  {@link java.util.List#size() List::size}
     * @param elemLength a function that, given the index of an "inner list," returns either the size of that "inner
     *                   list" of the length of that inner object.
     */
    public TwoLevelNavigator(IntSupplier elemCount, IntUnaryOperator elemLength) {
        this.elemCount = elemCount;
        this.elemLength = elemLength;
    }

    @Override
    public Position position(int major, int minor) {
        return new Pos(major, minor);
    }

    @Override
    public Position offsetToPosition(int offset, Bias bias) {
        return position(0, 0).offsetBy(offset, bias);
    }
}
