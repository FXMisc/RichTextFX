package org.fxmisc.richtext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReadOnlyStyledDocument<S> extends StyledDocumentBase<S, List<Paragraph<S>>> {

    static enum ParagraphsPolicy {
        ADOPT,
        COPY,
    }

    private int length = -1;

    private String text = null;

    ReadOnlyStyledDocument(List<Paragraph<S>> paragraphs, ParagraphsPolicy policy) {
        super(policy == ParagraphsPolicy.ADOPT ? paragraphs : new ArrayList<Paragraph<S>>(paragraphs));
    }

    @Override
    public int length() {
        if(length == -1) {
            length = computeLength();
        }
        return length;
    }

    @Override
    public String getText() {
        if(text == null) {
            text = getText(0, length());
        }
        return text;
    }

    @Override
    public List<Paragraph<S>> getParagraphs() {
        return Collections.unmodifiableList(paragraphs);
    }

    private int computeLength() {
        return paragraphs.stream().mapToInt(p -> p.fullLength()).sum();
    }
}
