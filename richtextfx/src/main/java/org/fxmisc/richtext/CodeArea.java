package org.fxmisc.richtext;


import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.Collection;

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

        // don't apply preceding style to typed text
        setUseInitialStyleForInsertion(true);
    }

    public CodeArea(EditableStyledDocument<Collection<String>, Collection<String>> document) {
        super(document, false);
    }

    public CodeArea() {
        super(false);
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
        getUndoManager().forgetHistory();
        getUndoManager().mark();

        // position the caret at the beginning
        selectRange(0, 0);
    }

    /**
     * Override the default copy action.
     * when copying without selecting a text, copy the whole line in the caret position.
     */
    @Override
    public void copy() {
        if (getSelectedText().length() == 0) {
            ClipboardContent content = new ClipboardContent();
            String selectingString = getText().substring(getStart(), getEnd());
            content.putString(selectingString);
            Clipboard.getSystemClipboard().setContent(content);
        } else {
            super.copy();
        }
    }

    private int getStart() {
        int caretPosition = getCaretPosition();
        for (int i = caretPosition; i > -1; i--) {
            if (isNL(getText().toCharArray(), i)) {
                return i;
            }
        }
        return 0;
    }

    private int getEnd() {
        int caretPosition = getCaretPosition();

        for (int i = caretPosition; i < getText().length(); i++) {
            if (isNL(getText().toCharArray(), i)) {
                return i;
            }
        }
        return 0;
    }

    private boolean isNL(char[] charArray, int index) {
        return !((charArray == null) || (charArray.length == 0) || (index < 0) || (index >= charArray.length)) && (((charArray[index] == '\n') || (charArray[index] == '\r')));
    }


}
