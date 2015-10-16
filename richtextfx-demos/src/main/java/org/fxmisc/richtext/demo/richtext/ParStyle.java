package org.fxmisc.richtext.demo.richtext;

import java.util.Objects;

/**
 * Holds information about the style of a paragraph.
 */
class ParStyle {

    public static final ParStyle EMPTY = new ParStyle();

    @Override
    public int hashCode() {
        return Objects.hash();
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof ParStyle) {
            return true;
        } else {
            return false;
        }
    }

    public String toCss() {
        return "";
    }

}
