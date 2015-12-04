package org.fxmisc.richtext;

import java.util.OptionalInt;

public class CharacterHit {

    public static CharacterHit insertionAt(int insertionIndex) {
        return new CharacterHit(OptionalInt.empty(), insertionIndex);
    }

    public static CharacterHit leadingHalfOf(int charIdx) {
        return new CharacterHit(OptionalInt.of(charIdx), charIdx);
    }

    public static CharacterHit trailingHalfOf(int charIdx) {
        return new CharacterHit(OptionalInt.of(charIdx), charIdx + 1);
    }


    private final OptionalInt charIdx;
    private final int insertionIndex;

    private CharacterHit(OptionalInt charIdx, int insertionIndex) {
        this.charIdx = charIdx;
        this.insertionIndex = insertionIndex;
    }

    public OptionalInt getCharacterIndex() {
        return charIdx;
    }

    public int getInsertionIndex() {
        return insertionIndex;
    }

    public CharacterHit offset(int offset) {
        return new CharacterHit(
                charIdx.isPresent()
                        ? OptionalInt.of(charIdx.getAsInt() + offset)
                        : charIdx,
                insertionIndex + offset);
    }
}
