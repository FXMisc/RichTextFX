package org.fxmisc.richtext;

import static org.fxmisc.richtext.ReadOnlyStyledDocument.ParagraphsPolicy.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadOnlyStyledDocument<PS, S> extends StyledDocumentBase<PS, S, List<Paragraph<PS, S>>> {

    private static final Pattern LINE_TERMINATOR = Pattern.compile("\r\n|\r|\n");

    public static <PS, S> ReadOnlyStyledDocument<PS, S> fromString(String str, PS paragraphStyle, S style) {
        Matcher m = LINE_TERMINATOR.matcher(str);

        int n = 1;
        while(m.find()) ++n;
        List<Paragraph<PS, S>> res = new ArrayList<>(n);

        int start = 0;
        m.reset();
        while(m.find()) {
            String s = str.substring(start, m.start());
            res.add(new Paragraph<>(paragraphStyle, s, style));
            start = m.end();
        }
        String last = str.substring(start);
        res.add(new Paragraph<>(paragraphStyle, last, style));

        return new ReadOnlyStyledDocument<>(res, ADOPT);
    }

    enum ParagraphsPolicy {
        ADOPT,
        COPY,
    }

    static <PS, S> Codec<StyledDocument<PS, S>> codec(Codec<PS> pCodec, Codec<S> tCodec) {
        return new Codec<StyledDocument<PS, S>>() {
            private final Codec<List<Paragraph<PS, S>>> codec = Codec.listCodec(paragraphCodec(pCodec, tCodec));

            @Override
            public String getName() {
                return "application/richtextfx-styled-document<" + tCodec.getName() + ";" + pCodec.getName() + ">";
            }

            @Override
            public void encode(DataOutputStream os, StyledDocument<PS, S> doc) throws IOException {
                codec.encode(os, doc.getParagraphs());
            }

            @Override
            public StyledDocument<PS, S> decode(DataInputStream is) throws IOException {
                return new ReadOnlyStyledDocument<>(
                        codec.decode(is),
                        ParagraphsPolicy.ADOPT);
            }

        };
    }

    private static <PS, S> Codec<Paragraph<PS, S>> paragraphCodec(Codec<PS> pCodec, Codec<S> tCodec) {
        return new Codec<Paragraph<PS, S>>() {
            private final Codec<List<StyledText<S>>> segmentsCodec = Codec.listCodec(styledTextCodec(tCodec));

            @Override
            public String getName() {
                return "paragraph<" + tCodec.getName() + ";" + pCodec.getName() + ">";
            }

            @Override
            public void encode(DataOutputStream os, Paragraph<PS, S> p) throws IOException {
                pCodec.encode(os, p.getParagraphStyle());
                segmentsCodec.encode(os, p.getSegments());
            }

            @Override
            public Paragraph<PS, S> decode(DataInputStream is) throws IOException {
                PS paragraphStyle = pCodec.decode(is);
                List<StyledText<S>> segments = segmentsCodec.decode(is);
                return new Paragraph<>(paragraphStyle, segments);
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
                STRING_CODEC.encode(os, t.getText());
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

    ReadOnlyStyledDocument(List<Paragraph<PS, S>> paragraphs, ParagraphsPolicy policy) {
        super(policy == ParagraphsPolicy.ADOPT ? paragraphs : new ArrayList<>(paragraphs));
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
    public List<Paragraph<PS, S>> getParagraphs() {
        return Collections.unmodifiableList(paragraphs);
    }

    private int computeLength() {
        return paragraphs.stream().mapToInt(Paragraph::length).sum() + paragraphs.size() - 1;
    }
}
