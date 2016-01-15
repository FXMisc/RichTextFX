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

public class ReadOnlyStyledDocument<S, PS> extends StyledDocumentBase<S, PS, List<Paragraph<S, PS>>> {

    private static final Pattern LINE_TERMINATOR = Pattern.compile("\r\n|\r|\n");

    public static <S, PS> ReadOnlyStyledDocument<S, PS> fromString(String str, S style, PS paragraphStyle) {
        Matcher m = LINE_TERMINATOR.matcher(str);

        int n = 1;
        while(m.find()) ++n;
        List<Paragraph<S, PS>> res = new ArrayList<>(n);

        int start = 0;
        m.reset();
        while(m.find()) {
            String s = str.substring(start, m.start());
            res.add(s.isEmpty()
                ? new EmptyParagraph<>(paragraphStyle, style)
                : new NormalParagraph<>(paragraphStyle, s, style)
            );
            start = m.end();
        }
        String last = str.substring(start);
        res.add(last.isEmpty()
                ? new EmptyParagraph<>(paragraphStyle, style)
                : new NormalParagraph<>(paragraphStyle, last, style)
        );

        return new ReadOnlyStyledDocument<>(res, ADOPT);
    }

    enum ParagraphsPolicy {
        ADOPT,
        COPY,
    }

    static <S, PS> Codec<StyledDocument<S, PS>> codec(Codec<S> tCodec, Codec<PS> pCodec) {
        return new Codec<StyledDocument<S, PS>>() {
            private final Codec<List<Paragraph<S, PS>>> codec = Codec.listCodec(paragraphCodec(tCodec, pCodec));

            @Override
            public String getName() {
                return "application/richtextfx-styled-document<" + tCodec.getName() + ";" + pCodec.getName() + ">";
            }

            @Override
            public void encode(DataOutputStream os, StyledDocument<S, PS> doc) throws IOException {
                codec.encode(os, doc.getParagraphs());
            }

            @Override
            public StyledDocument<S, PS> decode(DataInputStream is) throws IOException {
                return new ReadOnlyStyledDocument<>(
                        codec.decode(is),
                        ParagraphsPolicy.ADOPT);
            }

        };
    }

    private static <S, PS> Codec<Paragraph<S, PS>> paragraphCodec(Codec<S> tCodec, Codec<PS> pCodec) {
        return new Codec<Paragraph<S, PS>>() {
            private final Codec<List<StyledText<S>>> segmentsCodec = Codec.listCodec(styledTextCodec(tCodec));

            @Override
            public String getName() {
                return "paragraph<" + tCodec.getName() + ";" + pCodec.getName() + ">";
            }

            @Override
            public void encode(DataOutputStream os, Paragraph<S, PS> p) throws IOException {
                pCodec.encode(os, p.getParagraphStyle());
                if (p instanceof EmptyParagraph) {
                    List<StyledText<S>> list = new ArrayList<>(1);
                    list.add(new StyledText<S>("", p.getStyleAtPosition(0)));
                    segmentsCodec.encode(os, list);
                } else {
                    segmentsCodec.encode(os, p.getSegments());
                }
            }

            @Override
            public Paragraph<S, PS> decode(DataInputStream is) throws IOException {
                PS paragraphStyle = pCodec.decode(is);
                List<StyledText<S>> segments = segmentsCodec.decode(is);
                return segments.get(0).length() == 0
                        ? new EmptyParagraph<>(paragraphStyle, segments.get(0).getStyle())
                        : new NormalParagraph<>(paragraphStyle, segments);
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

    ReadOnlyStyledDocument(List<Paragraph<S, PS>> paragraphs, ParagraphsPolicy policy) {
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
    public List<Paragraph<S, PS>> getParagraphs() {
        return Collections.unmodifiableList(paragraphs);
    }

    private int computeLength() {
        return paragraphs.stream().mapToInt(Paragraph::length).sum() + paragraphs.size() - 1;
    }
}
