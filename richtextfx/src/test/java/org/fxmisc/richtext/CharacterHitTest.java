package org.fxmisc.richtext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CharacterHitTest {
    private void checkHitAndNoCharIndex(CharacterHit hit, int insertion) {
        assertEquals(insertion, hit.getInsertionIndex());
        assertTrue(hit.getCharacterIndex().isEmpty());
    }

    private void checkHit(CharacterHit hit, int insertion, int charIdx) {
        assertEquals(insertion, hit.getInsertionIndex());
        assertEquals(charIdx, hit.getCharacterIndex().orElseThrow());
    }

    @Test
    @DisplayName("Character hit insertion at a given position")
    void insertionAt() {
        checkHitAndNoCharIndex(CharacterHit.insertionAt(2), 2);
        checkHitAndNoCharIndex(CharacterHit.insertionAt(2).offset(12), 14);
    }

    @Test
    @DisplayName("Character hit leading half at a given position")
    void leadingHalfOf() {
        checkHit(CharacterHit.leadingHalfOf(2), 2, 2);
        checkHit(CharacterHit.leadingHalfOf(2).offset(12), 14, 14);
    }

    @Test
    @DisplayName("Character hit trailing half at a given position")
    void trailingHalfOf() {
        checkHit(CharacterHit.trailingHalfOf(2), 3, 2);
        checkHit(CharacterHit.trailingHalfOf(2).offset(12), 15, 14);
    }
}
