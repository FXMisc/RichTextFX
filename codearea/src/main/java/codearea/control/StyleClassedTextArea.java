package codearea.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import undo.UndoManagerFactory;

/**
 * Text area that uses style classes to define style of text segments.
 */
public class StyleClassedTextArea extends StyledTextArea<Collection<String>> {

    public <C> StyleClassedTextArea(
            UndoType<Collection<String>, C> undoType,
            UndoManagerFactory<C> undoManagerFactory) {
        super(Collections.<String>emptyList(),
                (text, styleClasses) -> text.getStyleClass().addAll(styleClasses),
                undoType, undoManagerFactory);
    }

    /**
     * Creates a text area with empty text content.
     */
    public StyleClassedTextArea() {
        this(UndoType.rich(), UndoManagerFactory.defaultFactory());
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
