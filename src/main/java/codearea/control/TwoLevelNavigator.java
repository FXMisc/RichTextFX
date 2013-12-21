package codearea.control;

import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

public class TwoLevelNavigator {

    public class Position {
        private final int major;
        private final int minor;

        private Position(int major, int minor) {
            this.major = major;
            this.minor = minor;
        }

        @Override
        public String toString() {
            return "(" + major + ", " + minor + ")";
        }

        public boolean sameAs(Position other) {
            // XXX we should also check that they come from the same navigator
            return major == other.major && minor == other.minor;
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        public Position clamp() {
            if(major == elemCount.getAsInt() - 1) {
                int elemLen = elemLength.applyAsInt(major);
                if(minor < elemLen) {
                    return this;
                } else {
                    return new Position(major, elemLen-1);
                }
            } else {
                return this;
            }
        }

        public Position offsetBy(int lowLevelOffset) {
            if(lowLevelOffset > 0) {
                return forward(lowLevelOffset);
            } else if(lowLevelOffset < 0) {
                return backward(-lowLevelOffset);
            } else {
                return this;
            }
        }

        private Position forward(int offset) {
            offset += minor;
            int major = this.major;
            int curElemLength = elemLength.applyAsInt(major);

            int elemCount = TwoLevelNavigator.this.elemCount.getAsInt();
            while(major < elemCount - 1) {
                if(offset < curElemLength) {
                    return new Position(major, offset);
                } else {
                    offset -= curElemLength;
                    major += 1;
                    curElemLength = elemLength.applyAsInt(major);
                }
            }

            // now the position is either in the last high-level element or beyond
            return new Position(elemCount - 1, offset);
        }

        private Position backward(int offset) {
            int minor = this.minor;
            int major = this.major;
            while(offset > minor && major > 0) {
                offset -= minor;
                major -= 1; // move to the previous element
                // set inner position to the end of the previous element
                minor = elemLength.applyAsInt(major);
            }

            if(offset <= minor) {
                return new Position(major, minor - offset);
            } else {
                // we went beyond the start
                return new Position(0, 0);
            }
        }
    }

    private final IntSupplier elemCount;
    private final IntUnaryOperator elemLength;

    public TwoLevelNavigator(IntSupplier elemCount, IntUnaryOperator elemLength) {
        this.elemCount = elemCount;
        this.elemLength = elemLength;
    }

    public Position position(int highLevel, int lowLevel) {
        return new Position(highLevel, lowLevel);
    }

    public Position offset(int lowLevelOffset) {
        return position(0, 0).offsetBy(lowLevelOffset);
    }
}
