package org.fxmisc.wellbehaved.input;

import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

/**
 * The purpose of this class is to reuse the modifier-matching implementation
 * from KeyCombination.
 */
class KeyTypedCombination extends KeyCombination {
    private final String character;

    KeyTypedCombination(String character, Modifier... modifiers) {
        super(modifiers);
        this.character = character;
    }

    @Override
    public boolean match(KeyEvent event) {
        return super.match(event) // matches the modifiers
                && event.getEventType() == KeyEvent.KEY_TYPED
                && event.getCharacter().equals(character);
    }
}
