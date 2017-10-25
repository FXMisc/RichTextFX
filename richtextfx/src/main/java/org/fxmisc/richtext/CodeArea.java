package org.fxmisc.richtext;


import java.util.Collection;

import javafx.beans.NamedArg;
import org.fxmisc.richtext.model.EditableStyledDocument;

/**
 * A convenience subclass of {@link StyleClassedTextArea} with fixed-width font and an undo manager that observes
 * only plain text changes (not styled changes). It's style class is {@code code-area}.
 */
public class CodeArea extends StyleClassedTextArea {

    {
        getStyleClass().add("code-area");

        // load the default style that defines a fixed-width font
        getStylesheets().add(CodeArea.class.getResource("code-area.css").toExternalForm());

        // don't apply preceding style to typed text
        setUseInitialStyleForInsertion(true);
    }

    /**
     * Creates an area that can render and edit the same {@link EditableStyledDocument} as another {@link CodeArea}.
     */
    public CodeArea(@NamedArg("document") EditableStyledDocument<Collection<String>, String, Collection<String>> document) {
        super(document, false);
    }

    /**
     * Creates an area with no text.
     */
    public CodeArea() {
        super(false);
    }

    /**
     * Creates a text area with initial text content.
     * Initial caret position is set at the beginning of text content.
     *
     * @param text Initial text content.
     */
    public CodeArea(@NamedArg("text") String text) {
        this();

        appendText(text);
        getUndoManager().forgetHistory();
        getUndoManager().mark();

        // position the caret at the beginning
        selectRange(0, 0);
    }
}
