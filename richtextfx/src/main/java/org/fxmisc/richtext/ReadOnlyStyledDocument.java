package org.fxmisc.richtext;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReadOnlyStyledDocument<S> extends StyledDocumentBase<S, List<Paragraph<S>>> {

    static enum ParagraphsPolicy {
        ADOPT,
        COPY,
    }

    static <S> Codec<StyledDocument<S>> codec(Codec<S> styleCodec) {
        return new Codec<StyledDocument<S>>() {
            private final Codec<List<Paragraph<S>>> codec = Codec.listCodec(paragraphCodec(styleCodec));

            @Override
            public String getName() {
                return "application/richtextfx-styled-document<" + styleCodec.getName() + ">";
            }

            @Override
            public void encode(DataOutputStream os, StyledDocument<S> doc) throws IOException {
                codec.encode(os, doc.getParagraphs());
            }

            @Override
            public StyledDocument<S> decode(DataInputStream is) throws IOException {
                return new ReadOnlyStyledDocument<>(
                        codec.decode(is),
                        ParagraphsPolicy.ADOPT);
            }

        };
    }

    private static <S> Codec<Paragraph<S>> paragraphCodec(Codec<S> styleCodec) {
        return new Codec<Paragraph<S>>() {
            private final Codec<List<StyledText<S>>> segmentsCodec = Codec.listCodec(styledTextCodec(styleCodec));

            @Override
            public String getName() {
                return "paragraph<" + styleCodec.getName() + ">";
            }

            @Override
            public void encode(DataOutputStream os, Paragraph<S> p) throws IOException {
                segmentsCodec.encode(os, p.getSegments());
            }

            @Override
            public Paragraph<S> decode(DataInputStream is) throws IOException {
                List<StyledText<S>> segments = segmentsCodec.decode(is);
                return new Paragraph<>(segments);
            }
        };
    }

    private static <S> Codec<StyledText<S>> styledTextCodec(Codec<S> styleCodec) {
        return new Codec<StyledText<S>>() {

            @Override
            public String getName() {
                return "styledtext<" + styleCodec.getName() + ">";
            }

            @Override
            public void encode(DataOutputStream os, StyledText<S> t) throws IOException {
                STRING_CODEC.encode(os, t.toString());
                styleCodec.encode(os, t.getStyle());
            }

            @Override
            public StyledText<S> decode(DataInputStream is) throws IOException {
                String text = STRING_CODEC.decode(is);
                S style = styleCodec.decode(is);
                return new StyledText<>(text, style);
            }

        };
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
        return paragraphs.stream().mapToInt(Paragraph::length).sum() + paragraphs.size() - 1;
    }
}
