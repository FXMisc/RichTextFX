package codearea.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Text area that uses style classes to define style of text segments.
 */
public class StyleClassedTextArea extends StyledTextArea2<Collection<String>> {

    /**
     * Creates a text area with empty text content.
     */
    public StyleClassedTextArea() {
        super(Collections.<String>emptyList(),
                (text, styleClasses) -> text.getStyleClass().addAll(styleClasses));
    }

    /**
     * Creates a text area with initial text content.
     * Initial caret position is set at the beginning of text content.
     *
     * @param text Initial text content.
     */
    public StyleClassedTextArea(String text) {
        this();

        replaceText(0, 0, text);

        // position the caret at the beginning
        selectRange(0, 0);
    }

    /**
     * Convenient method to assign a single style class.
     */
    public void setStyleClass(int from, int to, String styleClass) {
        List<String> styleClasses = new ArrayList<>(1);
        styleClasses.add(styleClass);
        setStyle(from, to, styleClasses);
    }

    /**
     * @deprecated on 2013-12-11. Use {@link #setStyle(int, int, Collection)} instead.
     */
    @Deprecated
    public void setStyleClasses(int from, int to, Set<String> styleClasses) {
        setStyle(from, to, styleClasses);
    }

    /**
     * @deprecated on 2013-12-11. Use {@link #clearStyle(int, int)} instead.
     */
    @Deprecated
    public void clearStyleClasses(int from, int to) {
        clearStyle(from, to);
    }
}
