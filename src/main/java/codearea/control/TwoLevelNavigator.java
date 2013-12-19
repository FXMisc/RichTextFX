package codearea.control;

import java.util.List;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public class TwoLevelNavigator<T> {

    public class Position {
        private final int outer;
        private final int inner;

        private Position(int outer, int inner) {
            this.outer = outer;
            this.inner = inner;
        }

        public boolean sameAs(TwoLevelNavigator<T>.Position other) {
            // XXX we should also check that they come from the same navigator
            return outer == other.outer && inner == other.inner;
        }

        public int getOuter() {
            return outer;
        }

        public int getInner() {
            return inner;
        }

        public Position clamp() {
            if(outer == elemCount-1) {
                int elemLen = elemLength.applyAsInt(elems.apply(outer));
                if(inner < elemLen) {
                    return this;
                } else {
                    return new Position(outer, elemLen-1);
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
            offset += inner;
            int outer = this.outer;
            int curElemLength = elemLength.applyAsInt(elems.apply(outer));

            while(outer < elemCount-1) {
                if(offset < curElemLength + spacing) {
                    return new Position(outer, offset);
                } else {
                    offset -= curElemLength + spacing;
                    outer += 1;
                    curElemLength = elemLength.applyAsInt(elems.apply(outer));
                }
            }

            // now the position is either in the last high-level element or beyond
            return new Position(elemCount-1, offset);
        }

        private Position backward(int offset) {
            int inner = this.inner;
            int outer = this.outer;
            while(offset > inner && outer > 0) {
                offset -= inner;
                outer -= 1; // move to the previous element
                // set inner position to the end of the previous element
                inner = elemLength.applyAsInt(elems.apply(outer)) + spacing;
            }

            if(offset <= inner) {
                return new Position(outer, inner - offset);
            } else {
                // we went beyond the start
                return new Position(0, 0);
            }
        }
    }

    private final IntFunction<T> elems;
    private final int elemCount;
    private final ToIntFunction<T> elemLength;
    private int spacing;

    public TwoLevelNavigator(List<T> highLevelElems, ToIntFunction<T> elemLength) {
        this(highLevelElems, elemLength, 0);
    }

    public TwoLevelNavigator(List<T> highLevelElems, ToIntFunction<T> elemLength, int spacing) {
        this(i -> highLevelElems.get(i), highLevelElems.size(), elemLength, 0);
    }

    public TwoLevelNavigator(IntFunction<T> highLevelElems, int elemCount, ToIntFunction<T> elemLength) {
        this(highLevelElems, elemCount, elemLength, 0);
    }

    public TwoLevelNavigator(IntFunction<T> highLevelElems, int elemCount, ToIntFunction<T> elemLength, int spacing) {
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
