package org.fxmisc.richtext.model;

/**
 * TwoDimensional is an interface for any item which can be navigated
 * in two dimensions, such as a List of Lists. In short, it allows one to find a position within this object.
 * <p>
 *     There are two basic kinds of two dimensional objects.
 * </p>
*     <ol>
*         <li>
*             One has a type of {@code List<List<Object>>}. The {@code major} dimension's value indicates the index of
*             the "inner list" within the "outer list" while the {@code minor} dimension's value indicates the index
*             of the object within an "inner list."
*         </li>
*         <li>
*             One has a type of {@code List<ObjectWithLengthMethod} as in {@code List<String>}. The {@code major}
*             dimension's value indicates the index within the list while the {@code minor} dimension's value
*             indicates how far into that length-object a position is (e.g. how many characters into a {@link String}
*             is a position).
*         </li>
*     </ol>
 *
 * <p>
 *     Not all two dimensional objects are rectangular, so the valid values of the minor dimension depends on
 *     the major dimension.
 * </p>
 *
 */
public interface TwoDimensional {

    /**
     * Determines whether to add 1 when the end of an inner list is reached
     *
     * <p>For example, given the following two dimensional object (a list of lists of objects) where the inner lists
     * values show the absolute index of that specific item within the outer list...
     * <pre><code>
     * [                 // outer list
     *      [0, 1, 2, 3],   // inner list 1
     *      [4, 5, 6]       // inner list 2
     * ]                 // outer list
     * </code></pre>
     * ...navigating to {@code listOfListsObject.offsetTo(3, BACKWARD)} will return {@code 3} whereas
     * {@code listOfListsObject.offsetTo(3, FORWARD)} will return {@code 4}, the "next item" in the two
     * dimensional object.
     * The Bias does not apply if the index is any non-last-index in the inner list. Thus,
     * {@code listOfListsObject.offsetTo(1, BACKWARD)} and {@code listOfListsObject.offsetTo(1, FORWARD)}
     * will return the same value, {@code 1}, since the position is not at the end of the list.
     */
    enum Bias {
        /** When the returned value would be equal to the last index in an "inner list" or the length of some object
         * with length, returns the {@code value + 1}. See {@link Bias} for more clarification. */
        Forward,
        /** When the returned value would be equal to the last index in an "inner list" or the length of some object
         * with length, returns the value unmodified. See {@link Bias} for more clarification. */
        Backward,
    }

    /**
     * A two dimensional position, with a major offset (such as a paragraph index within a document)
     * and a minor dimension (such as a segment index or column position within a paragraph). Major and minor
     * positions begin at 0.
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

        /**
         * Returns a new position that offsets this position by the given amount
         */
        public Position offsetBy(int amount, Bias bias);

        /**
         * Converts this position to an overall offset within the original
         * TwoDimensional item (to which {@link #getTargetObject} refers).
         * For example, moving a caret to a relative position (paragraph 2, column 3)
         * might result in the offset value (absolute position) of 28.
         */
        int toOffset();

    }

    /**
     * Creates a two dimensional position in some entity (e.g. area, list of lists, list of some object with length)
     * where the {@code major} value is the index within the outer list) and the {@code minor}
     * value is either the index within the inner list or some amount of length in a list of objects that have length.
     */
    Position position(int major, int minor);

    /**
     * Creates a two dimensional position in some entity (e.g. area, list of lists, list of some object with length)
     * where the {@code offset} value is an absolute position in that entity.
     */
    Position offsetToPosition(int offset, Bias bias);
}
