package codearea.control;

import java.util.Collections;
import java.util.List;

public class ReadOnlyStyledDocument<S> extends StyledDocumentBase<S, List<Paragraph<S>>> {

    private int length = -1;

    private String text = null;

    ReadOnlyStyledDocument(List<Paragraph<S>> paragraphs) {
        super(paragraphs);
    }

    @Override
    public int getLength() {
        if(length == -1) {
            length = computeLength();
        }
        return length;
    }

    @Override
    public String getText() {
        if(text == null) {
            text = getText(0, getLength());
        }
        return text;
    }

    @Override
    public List<Paragraph<S>> getParagraphs() {
        return Collections.unmodifiableList(paragraphs);
    }

    private int computeLength() {
        return paragraphs.stream().mapToInt(p -> p.length()).sum() + (paragraphs.size()-1);
    }
}
