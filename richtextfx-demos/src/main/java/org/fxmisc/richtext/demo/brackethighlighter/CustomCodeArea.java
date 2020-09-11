package org.fxmisc.richtext.demo.brackethighlighter;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.StyledDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomCodeArea extends CodeArea {

    private List<TextInsertionListener> insertionListeners;

    public CustomCodeArea() {
        this.insertionListeners = new ArrayList<>();
    }

    public CustomCodeArea(String text) {
        super(text);
        this.insertionListeners = new ArrayList<>();
    }

    public CustomCodeArea(EditableStyledDocument<Collection<String>, String, Collection<String>> document) {
        super(document);
        this.insertionListeners = new ArrayList<>();
    }

    public void addTextInsertionListener(TextInsertionListener listener) {
        insertionListeners.add(listener);
    }

    public void removeTextInsertionListener(TextInsertionListener listener) {
        insertionListeners.remove(listener);
    }

    @Override
    public void replace(int start, int end, StyledDocument<Collection<String>, String, Collection<String>> replacement) {
        // notify all listeners
        for (TextInsertionListener listener : insertionListeners) {
            listener.codeInserted(start, end, replacement.getText());
        }

        super.replace(start, end, replacement);
    }

}
