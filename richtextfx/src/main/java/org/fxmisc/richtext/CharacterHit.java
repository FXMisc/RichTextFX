package org.fxmisc.richtext;

import java.util.OptionalInt;

/**
 * <p>Object that stores information relating to the position in an area's content that corresponds to a given position
 * in some visible entity (e.g. the area, a paragraph in the area, a line on a paragraph).</p>
 * <p>A character hit can either be before a given character (leading) or after a given character (trailing).</p>
 * <p>A special case is planned when the character hit happens outside the boundaries of some visible entity. In this case
 * there is an insertion index but there isn't any actual character position in the content.</p>
 */
public class CharacterHit {

    /**
     * @return a {@link CharacterHit} for cases where the insertion occurs outside the bounds of some visible entity
     * (e.g. the area, the paragraph in an area, the line in a paragraph)
     */
    public static CharacterHit insertionAt(int insertionIndex) {
        return new CharacterHit(OptionalInt.empty(), insertionIndex);
    }

    /**
     * <p>Represents a hit happening before a character at a given position (meaning the character is located forward).</p>
     * <p>Example: If you have a hit on {@code ALPHA} at position 1, it means before the character {@code L}. The character
     * index would be 1 and the insertion would be at 1.</p>
     * @return a {@link CharacterHit} for cases where the hit occurs inside the bounds of some visible entity
     * (e.g. the area, the paragraph in an area, the line in a paragraph) and the character is leading.
     */
    public static CharacterHit leadingHalfOf(int charIdx) {
        return new CharacterHit(OptionalInt.of(charIdx), charIdx);
    }

    /**
     * <p>A hit that happens after the character at the given position (meaning the character is before).</p>
     * <p>Example: If you have a hit on {@code ALPHA} at position 1, it means after the character {@code L}. The character
     * index would be 1 but the insertion would be at 2.</p>
     * @return a {@link CharacterHit} for cases where the hit occurs inside the bounds of some visible entity
     * (e.g. the area, the paragraph in an area, the line in a paragraph) and the character is trailing.
     */
    public static CharacterHit trailingHalfOf(int charIdx) {
        return new CharacterHit(OptionalInt.of(charIdx), charIdx + 1);
    }

    private final OptionalInt charIdx;
    private final int insertionIndex;

    private CharacterHit(OptionalInt charIdx, int insertionIndex) {
        this.charIdx = charIdx;
        this.insertionIndex = insertionIndex;
    }

    /**
     * Returns an {@link OptionalInt} whose value is present when the hit occurs within the visible
     * entity and is the index of the closest character to the give position on the screen.
     */
    public OptionalInt getCharacterIndex() {
        return charIdx;
    }

    /**
     * When {@link #getCharacterIndex()} is present, returns either the same value as {@link #getCharacterIndex()} when
     * the character index is leading and {@code getCharacterIndex() + 1} when the index is trailing.
     * When {@link #getCharacterIndex()} is absent, returns the bounds of that visible entity (either {@code 0} or
     * the length of the area, the length of a paragraph, or the character count of a line).
     */
    public int getInsertionIndex() {
        return insertionIndex;
    }

    /**
     * Offsets the values stored in this {@link CharacterHit} by the given amount
     */
    public CharacterHit offset(int offsetAmount) {
        return new CharacterHit(
                charIdx.isPresent()
                        ? OptionalInt.of(charIdx.getAsInt() + offsetAmount)
                        : charIdx,
                insertionIndex + offsetAmount);
    }
}
