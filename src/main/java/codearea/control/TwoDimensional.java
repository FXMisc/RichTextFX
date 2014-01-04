package codearea.control;



public interface TwoDimensional {

    interface Position {

        TwoDimensional getTargetObject();

        int getMajor();

        int getMinor();

        /**
         * Returns {@code true} if the given position is equal to this
         * position, that is they both point to the same place in the
         * same two-dimensional object. Otherwise returns {@code false}.
         */
        boolean sameAs(Position other);

        public Position clamp();

        public Position offsetBy(int offset);

        int toOffset();

    }

    Position position(int major, int minor);

    Position offsetToPosition(int offset);
}
