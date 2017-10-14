package org.fxmisc.richtext.demo.locationtracking;

/**
 * A rudimentary visual indicator for a position within the text.
 */
public class Indicator extends javafx.scene.text.Text {
    private TextPosition pos = null;

    public Indicator(int offset) {
        this.setText("==>");
        this.pos = new TextPosition(offset);
    }

    public TextPosition getPosition() {
        return this.pos;
    }
}
