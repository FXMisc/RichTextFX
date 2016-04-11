package org.fxmisc.richtext;

import static org.reactfx.util.Either.*;
import static org.reactfx.util.Tuples.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.reactfx.util.Either;
import org.reactfx.util.FingerTree;
import org.reactfx.util.FingerTree.NonEmptyFingerTree;
import org.reactfx.util.ToSemigroup;
import org.reactfx.util.Tuple2;

public final class ReadOnlyStyledDocument<PS, S> implements StyledDocument<PS, S> {

    private static class Summary {
        private final int paragraphCount;
        private final int charCount;

        public Summary(int paragraphCount, int charCount) {
            assert paragraphCount > 0;
            assert charCount >= 0;

            this.paragraphCount = paragraphCount;
            this.charCount = charCount;
        }

        public int length() {
            return charCount + paragraphCount - 1;
        }
    }

    private static <PS, S> ToSemigroup<Paragraph<PS, S>, Summary> summaryProvider() {
        return new ToSemigroup<Paragraph<PS, S>, Summary>() {

            @Override
            public Summary apply(Paragraph<PS, S> p) {
                return new Summary(1, p.length());
            }

            @Override
            public Summary reduce(Summary left, Summary right) {
                return new Summary(
                        left.paragraphCount + right.paragraphCount,
                        left.charCount + right.charCount);
            }
        };

    }

    private static final Pattern LINE_TERMINATOR = Pattern.compile("\r\n|\r|\n");

    private static final BiFunction<Summary, Integer, Either<Integer, Integer>> NAVIGATE =
            (s, i) -> i <= s.length() ? left(i) : right(i - (s.length() + 1));

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

        return new ReadOnlyStyledDocument<>(res);
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
                return new ReadOnlyStyledDocument<>(codec.decode(is));
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


    private final NonEmptyFingerTree<Paragraph<PS, S>, Summary> tree;

    private String text = null;
    private List<Paragraph<PS, S>> paragraphs = null;

    private ReadOnlyStyledDocument(NonEmptyFingerTree<Paragraph<PS, S>, Summary> tree) {
        this.tree = tree;
    }

    ReadOnlyStyledDocument(List<Paragraph<PS, S>> paragraphs) {
        this.tree =
                FingerTree.mkTree(paragraphs, summaryProvider()).caseEmpty().unify(
                        emptyTree -> { throw new AssertionError("Unreachable code"); },
                        neTree -> neTree);
    }

    @Override
    public int length() {
        return tree.getSummary().length();
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
        if(paragraphs == null) {
            paragraphs = new AbstractList<Paragraph<PS, S>>() {

                @Override
                public Paragraph<PS, S> get(int index) {
                    return tree.getLeaf(index);
                }

                @Override
                public int size() {
                    return tree.getLeafCount();
                }
            };
        }
        return paragraphs;
    }

    @Override
    public Position position(int major, int minor) {
        return new Pos(major, minor);
    }

    @Override
    public Position offsetToPosition(int offset, Bias bias) {
        return position(0, 0).offsetBy(offset, bias);
    }

    public Tuple2<ReadOnlyStyledDocument<PS, S>, ReadOnlyStyledDocument<PS, S>> split(int position) {
        return tree.locate(NAVIGATE, position).map(this::split);
    }

    public Tuple2<ReadOnlyStyledDocument<PS, S>, ReadOnlyStyledDocument<PS, S>> split(
            int row, int col) {
        return tree.splitAt(row).map((l, p, r) -> {
            Paragraph<PS, S> p1 = p.trim(col);
            Paragraph<PS, S> p2 = p.subSequence(col);
            ReadOnlyStyledDocument<PS, S> doc1 = new ReadOnlyStyledDocument<>(l.append(p1));
            ReadOnlyStyledDocument<PS, S> doc2 = new ReadOnlyStyledDocument<>(r.prepend(p2));
            return t(doc1, doc2);
        });
    }

    @Override
    public ReadOnlyStyledDocument<PS, S> concat(StyledDocument<PS, S> other) {
        int n = tree.getLeafCount() - 1;
        Paragraph<PS, S> p0 = tree.getLeaf(n);
        Paragraph<PS, S> p1 = other.getParagraphs().get(0);
        Paragraph<PS, S> p = p0.concat(p1);
        NonEmptyFingerTree<Paragraph<PS, S>, Summary> tree1 = tree.updateLeaf(n, p);
        FingerTree<Paragraph<PS, S>, Summary> tree2 = (other instanceof ReadOnlyStyledDocument)
                ? ((ReadOnlyStyledDocument<PS, S>) other).tree.split(1)._2
                : FingerTree.mkTree(other.getParagraphs().subList(1, other.getParagraphs().size()), summaryProvider());
        return new ReadOnlyStyledDocument<>(tree1.join(tree2));
    }

    @Override
    public StyledDocument<PS, S> subSequence(int start, int end) {
        return split(end)._1.split(start)._2;
    }

    @Override
    public String toString() {
        return getParagraphs()
                .stream()
                .map(Paragraph::toString)
                .reduce((p1, p2) -> p1 + "\n" + p2)
                .orElse("");
    }

    @Override
    public final boolean equals(Object other) {
        if(other instanceof StyledDocument) {
            StyledDocument<?, ?> that = (StyledDocument<?, ?>) other;
            return Objects.equals(this.getParagraphs(), that.getParagraphs());
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return getParagraphs().hashCode();
    }


    private class Pos implements Position {

        private final int major;
        private final int minor;

        private Pos(int major, int minor) {
            this.major = major;
            this.minor = minor;
        }

        @Override
        public String toString() {
            return "(" + major + ", " + minor + ")";
        }

        @Override
        public boolean sameAs(Position other) {
            return getTargetObject() == other.getTargetObject()
                    && major == other.getMajor()
                    && minor == other.getMinor();
        }

        @Override
        public TwoDimensional getTargetObject() {
            return ReadOnlyStyledDocument.this;
        }

        @Override
        public int getMajor() {
            return major;
        }

        @Override
        public int getMinor() {
            return minor;
        }

        @Override
        public Position clamp() {
            if(major == tree.getLeafCount() - 1) {
                int elemLen = tree.getLeaf(major).length();
                if(minor < elemLen) {
                    return this;
                } else {
                    return new Pos(major, elemLen-1);
                }
            } else {
                return this;
            }
        }

        @Override
        public Position offsetBy(int offset, Bias bias) {
            return tree.locateProgressively(s -> s.charCount + s.paragraphCount, toOffset() + offset)
                    .map(Pos::new);
        }

        @Override
        public int toOffset() {
            if(major == 0) {
                return minor;
            } else {
                return tree.getSummaryBetween(0, major).get().length() + 1 + minor;
            }
        }
    }
}
