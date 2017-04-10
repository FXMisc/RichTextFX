package org.fxmisc.richtext.model;

/**
 * TwoDimensional is an interface for any item which can be navigated
 * in two dimensions.  A two dimensional position has a major dimension
 * (e.g. paragraph number in a document) and a minor dimension (e.g.
 * segment index or column number within a paragraph).  Generally,
 * documents are not rectangular (paragraphs in a document are of differing lengths),
 * so the valid values of the minor dimension depends on the major dimension.
 */
public interface TwoDimensional {

    enum Bias {
        Forward,
        Backward,
    }

    /**
     * A two dimensional position, with a major offset (e.g. paragraph
     * number within a document) and a minor dimension (e.g. segment index or column
     * number within a paragraph).  Major and minor positions begin at 0.
     */
    interface Position {

        /**
         * The TwoDimensional object that this position refers to.
         */
        TwoDimensional getTargetObject();

        /**
         * The major dimension, e.g. paragraph number within a document
         */
        int getMajor();

        /**
         * The minor dimension, e.g. segment index or column offset within a paragraph.
         */
        int getMinor();

        /**
         * Returns {@code true} if the given position is equal to this
         * position, that is they both point to the same place in the
         * same two-dimensional object. Otherwise returns {@code false}.
         */
        boolean sameAs(Position other);

        /**
         * Returns a new position which clamps the minor position to be valid
         * given the major position.  (i.e. if the position is beyond the
         * end of a given paragraph, moves the position back to the end of the paragraph).
         */
        public Position clamp();

        public Position offsetBy(int offset, Bias bias);

        /**
         * Converts this position to an overall offset within the original
         * TwoDimensional item (which getTargetObject refers to)
         */
        int toOffset();

    }

    Position position(int major, int minor);

    Position offsetToPosition(int offset, Bias bias);
}
