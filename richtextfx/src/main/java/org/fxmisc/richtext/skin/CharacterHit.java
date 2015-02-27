package org.fxmisc.richtext.skin;

import static org.fxmisc.richtext.skin.CharacterHit.CharacterHitType.*;

class CharacterHit {
    public static enum CharacterHitType {
        LEADING_HALF,
        TRAILING_HALF,
        BEFORE,
        AFTER,
    }

    public static CharacterHit before(int charIdx) {
        return new CharacterHit(charIdx, BEFORE);
    }

    public static CharacterHit after(int charIdx) {
        return new CharacterHit(charIdx, AFTER);
    }

    public static CharacterHit leadingHalfOf(int charIdx) {
        return new CharacterHit(charIdx, LEADING_HALF);
    }

    public static CharacterHit trailingHalfOf(int charIdx) {
        return new CharacterHit(charIdx, TRAILING_HALF);
    }


    private final int charIdx;
    private final CharacterHitType hitType;

    CharacterHit(int charIdx, CharacterHitType hitType) {
        this.charIdx = charIdx;
        this.hitType = hitType;
    }

    public int getCharacterIndex() {
        return charIdx;
    }

    public int getInsertionIndex() {
        switch(hitType) {
        case LEADING_HALF: // fall through
        case BEFORE:
            return charIdx;
        case TRAILING_HALF: // fall through
        case AFTER:
            return charIdx + 1;
        }
        throw new AssertionError("Unreachable code");
    }

    public CharacterHitType getHitType() {
        return hitType;
    }

    public boolean isCharacterHit() {
        return hitType == LEADING_HALF || hitType == TRAILING_HALF;
    }
}
