package codearea.control;

import undo.UndoManagerFactory;

/**
 * A convenience subclass of {@link StyleClassedTextArea}
 * with fixed-width font and an undo manager that observes
 * only plain text changes (not styled changes).
 */
public class CodeArea extends StyleClassedTextArea {

    {
        getStyleClass().add("code-area");

        // load the default style that defines a fixed-width font
        getStylesheets().add(CodeArea.class.getResource("code-area.css").toExternalForm());
    }

    public CodeArea() {
        super(UndoType.plain(), UndoManagerFactory.defaultFactory());
    }

    /**
     * Creates a text area with initial text content.
     * Initial caret position is set at the beginning of text content.
     *
     * @param text Initial text content.
     */
    public CodeArea(String text) {
        this();

        appendText(text);

        // position the caret at the beginning
        selectRange(0, 0);
    }
}
