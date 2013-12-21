package codearea.control;

import java.util.List;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

public class TwoLevelNavigator<T> {

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

        public boolean sameAs(TwoLevelNavigator<T>.Position other) {
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
                int elemLen = elemLength.applyAsInt(elems.apply(major));
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
            int curElemLength = elemLength.applyAsInt(elems.apply(major));

            int elemCount = TwoLevelNavigator.this.elemCount.getAsInt();
            while(major < elemCount - 1) {
                if(offset < curElemLength + spacing) {
                    return new Position(major, offset);
                } else {
                    offset -= curElemLength + spacing;
                    major += 1;
                    curElemLength = elemLength.applyAsInt(elems.apply(major));
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
                minor = elemLength.applyAsInt(elems.apply(major)) + spacing;
            }

            if(offset <= minor) {
                return new Position(major, minor - offset);
            } else {
                // we went beyond the start
                return new Position(0, 0);
            }
        }
    }

    private final IntFunction<T> elems;
    private final IntSupplier elemCount;
    private final ToIntFunction<T> elemLength;
    private int spacing;

    public TwoLevelNavigator(List<T> highLevelElems, ToIntFunction<T> elemLength) {
        this(highLevelElems, elemLength, 0);
    }

    public TwoLevelNavigator(List<T> highLevelElems, ToIntFunction<T> elemLength, int spacing) {
        this(i -> highLevelElems.get(i), () -> highLevelElems.size(), elemLength, spacing);
    }

    public TwoLevelNavigator(IntFunction<T> highLevelElems, IntSupplier elemCount, ToIntFunction<T> elemLength) {
        this(highLevelElems, elemCount, elemLength, 0);
    }

    public TwoLevelNavigator(IntFunction<T> highLevelElems, IntSupplier elemCount, ToIntFunction<T> elemLength, int spacing) {
        this.elems = highLevelElems;
        this.elemCount = elemCount;
        this.elemLength = elemLength;
        this.spacing = spacing;
    }

    public Position position(int highLevel, int lowLevel) {
        return new Position(highLevel, lowLevel);
    }

    public Position offset(int lowLevelOffset) {
        return position(0, 0).offsetBy(lowLevelOffset);
    }
}
