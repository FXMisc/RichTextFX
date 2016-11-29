package org.fxmisc.richtext.model;

import static org.reactfx.util.Either.*;
import static org.reactfx.util.Tuples.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.reactfx.collection.MaterializedListModification;
import org.reactfx.util.BiIndex;
import org.reactfx.util.Either;
import org.reactfx.util.FingerTree;
import org.reactfx.util.FingerTree.NonEmptyFingerTree;
import org.reactfx.util.ToSemigroup;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuple3;

public final class ReadOnlyStyledDocument<PS, SEG, S> implements StyledDocument<PS, SEG, S> {

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

    private static <PS, SEG, S> ToSemigroup<Paragraph<PS, SEG, S>, Summary> summaryProvider() {
        return new ToSemigroup<Paragraph<PS, SEG, S>, Summary>() {

            @Override
            public Summary apply(Paragraph<PS, SEG, S> p) {
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

    public static <PS, SEG, S> ReadOnlyStyledDocument<PS, SEG, S> fromString(String str, PS paragraphStyle, S style, TextOps<SEG, S> segmentOps) {
        Matcher m = LINE_TERMINATOR.matcher(str);

        int n = 1;
        while(m.find()) ++n;
        List<Paragraph<PS, SEG, S>> res = new ArrayList<>(n);

        int start = 0;
        m.reset();
        while(m.find()) {
            String s = str.substring(start, m.start());
            res.add(new Paragraph<>(paragraphStyle, segmentOps, segmentOps.create(s, style)));
            start = m.end();
        }
        String last = str.substring(start);
        res.add(new Paragraph<>(paragraphStyle, segmentOps, segmentOps.create(last, style)));

        return new ReadOnlyStyledDocument<>(res);
    }

    public static <PS, SEG, S> ReadOnlyStyledDocument<PS, SEG, S> fromSegment(SEG segment,  PS paragraphStyle, S style, SegmentOps<SEG, S> segmentOps) {
        Paragraph<PS, SEG, S> content = new Paragraph<PS, SEG, S>(paragraphStyle, segmentOps, Arrays.asList(segment));
        List<Paragraph<PS, SEG, S>> res = Arrays.asList(content);
        return new ReadOnlyStyledDocument<>(res);
    }

    public static <PS, SEG, S> ReadOnlyStyledDocument<PS, SEG, S> from(StyledDocument<PS, SEG, S> doc) {
        if(doc instanceof ReadOnlyStyledDocument) {
            return (ReadOnlyStyledDocument<PS, SEG, S>) doc;
        } else {
            return new ReadOnlyStyledDocument<>(doc.getParagraphs());
        }
    }


    public static <PS, SEG, S> Codec<StyledDocument<PS, SEG, S>> codec(Codec<PS> pCodec, Codec<SEG> segCodec, SegmentOps<SEG, S> segmentOps) {
        return new Codec<StyledDocument<PS, SEG, S>>() {
            private final Codec<List<Paragraph<PS, SEG, S>>> codec = Codec.listCodec(paragraphCodec(pCodec, segCodec, segmentOps));

            @Override
            public String getName() {
                return "application/richtextfx-styled-document<" + pCodec.getName() + ";" + segCodec.getName() + ">";
            }

            @Override
            public void encode(DataOutputStream os, StyledDocument<PS, SEG, S> doc) throws IOException {
                codec.encode(os, doc.getParagraphs());
            }

            @Override
            public StyledDocument<PS, SEG, S> decode(DataInputStream is) throws IOException {
                return new ReadOnlyStyledDocument<>(codec.decode(is));
            }

        };
    }

    private static <PS, SEG, S> Codec<Paragraph<PS, SEG, S>> paragraphCodec(Codec<PS> pCodec, Codec<SEG> segCodec, SegmentOps<SEG, S> segmentOps) {
        return new Codec<Paragraph<PS, SEG, S>>() {
            private final Codec<List<SEG>> segmentsCodec = Codec.listCodec(segCodec);

            @Override
            public String getName() {
                return "paragraph<" + pCodec.getName() + ";" + segCodec.getName() + ">";
            }

            @Override
            public void encode(DataOutputStream os, Paragraph<PS, SEG, S> p) throws IOException {
                pCodec.encode(os, p.getParagraphStyle());
                segmentsCodec.encode(os, p.getSegments());
            }

            @Override
            public Paragraph<PS, SEG, S> decode(DataInputStream is) throws IOException {
                PS paragraphStyle = pCodec.decode(is);
                List<SEG> segments = segmentsCodec.decode(is);
                return new Paragraph<>(paragraphStyle, segmentOps, segments);
            }
        };
    }


    private final NonEmptyFingerTree<Paragraph<PS, SEG, S>, Summary> tree;

    private String text = null;
    private List<Paragraph<PS, SEG, S>> paragraphs = null;

    private ReadOnlyStyledDocument(NonEmptyFingerTree<Paragraph<PS, SEG, S>, Summary> tree) {
        this.tree = tree;
    }

    ReadOnlyStyledDocument(List<Paragraph<PS, SEG, S>> paragraphs) {
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
            String[] strings = getParagraphs().stream()
                    .map(Paragraph::getText)
                    .toArray(n -> new String[n]);
            text = String.join("\n", strings);
        }
        return text;
    }

    public int getParagraphCount() {
        return tree.getLeafCount();
    }

    public Paragraph<PS, SEG, S> getParagraph(int index) {
        return tree.getLeaf(index);
    }

    @Override
    public List<Paragraph<PS, SEG, S>> getParagraphs() {
        if(paragraphs == null) {
            paragraphs = tree.asList();
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

    public Tuple2<ReadOnlyStyledDocument<PS, SEG, S>, ReadOnlyStyledDocument<PS, SEG, S>> split(int position) {
        return tree.locate(NAVIGATE, position).map(this::split);
    }

    public Tuple2<ReadOnlyStyledDocument<PS, SEG, S>, ReadOnlyStyledDocument<PS, SEG, S>> split(
            int row, int col) {
        return tree.splitAt(row).map((l, p, r) -> {
            Paragraph<PS, SEG, S> p1 = p.trim(col);
            Paragraph<PS, SEG, S> p2 = p.subSequence(col);
            ReadOnlyStyledDocument<PS, SEG, S> doc1 = new ReadOnlyStyledDocument<>(l.append(p1));
            ReadOnlyStyledDocument<PS, SEG, S> doc2 = new ReadOnlyStyledDocument<>(r.prepend(p2));
            return t(doc1, doc2);
        });
    }

    @Override
    public ReadOnlyStyledDocument<PS, SEG, S> concat(StyledDocument<PS, SEG, S> other) {
        return concat0(other, Paragraph::concat);
    }

    private ReadOnlyStyledDocument<PS, SEG, S> concatR(StyledDocument<PS, SEG, S> other) {
        return concat0(other, Paragraph::concatR);
    }

    private ReadOnlyStyledDocument<PS, SEG, S> concat0(StyledDocument<PS, SEG, S> other, BinaryOperator<Paragraph<PS, SEG, S>> parConcat) {
        int n = tree.getLeafCount() - 1;
        Paragraph<PS, SEG, S> p0 = tree.getLeaf(n);
        Paragraph<PS, SEG, S> p1 = other.getParagraphs().get(0);
        Paragraph<PS, SEG, S> p = parConcat.apply(p0, p1);
        NonEmptyFingerTree<Paragraph<PS, SEG, S>, Summary> tree1 = tree.updateLeaf(n, p);
        FingerTree<Paragraph<PS, SEG, S>, Summary> tree2 = (other instanceof ReadOnlyStyledDocument)
                ? ((ReadOnlyStyledDocument<PS, SEG, S>) other).tree.split(1)._2
                : FingerTree.mkTree(other.getParagraphs().subList(1, other.getParagraphs().size()), summaryProvider());
        return new ReadOnlyStyledDocument<>(tree1.join(tree2));
    }

    @Override
    public StyledDocument<PS, SEG, S> subSequence(int start, int end) {
        return split(end)._1.split(start)._2;
    }

    public Tuple3<ReadOnlyStyledDocument<PS, SEG, S>, RichTextChange<PS, SEG, S>, MaterializedListModification<Paragraph<PS, SEG, S>>> replace(
            int from, int to, ReadOnlyStyledDocument<PS, SEG, S> replacement) {
        return replace(from, to, x -> replacement);
    }

    Tuple3<ReadOnlyStyledDocument<PS, SEG, S>, RichTextChange<PS, SEG, S>, MaterializedListModification<Paragraph<PS, SEG, S>>> replace(
            int from, int to, UnaryOperator<ReadOnlyStyledDocument<PS, SEG, S>> f) {
        BiIndex start = tree.locate(NAVIGATE, from);
        BiIndex end = tree.locate(NAVIGATE, to);
        return replace(start, end, f);
    }

    Tuple3<ReadOnlyStyledDocument<PS, SEG, S>, RichTextChange<PS, SEG, S>, MaterializedListModification<Paragraph<PS, SEG, S>>> replace(
            BiIndex start, BiIndex end, UnaryOperator<ReadOnlyStyledDocument<PS, SEG, S>> f) {
        int pos = tree.getSummaryBetween(0, start.major).map(s -> s.length() + 1).orElse(0) + start.minor;

        List<Paragraph<PS, SEG, S>> removedPars =
                getParagraphs().subList(start.major, end.major + 1);

        return end.map(this::split).map((l0, r) -> {
            return start.map(l0::split).map((l, removed) -> {
                ReadOnlyStyledDocument<PS, SEG, S> replacement = f.apply(removed);
                ReadOnlyStyledDocument<PS, SEG, S> doc = l.concatR(replacement).concat(r);
                RichTextChange<PS, SEG, S> change = new RichTextChange<>(pos, removed, replacement);
                List<Paragraph<PS, SEG, S>> addedPars = doc.getParagraphs().subList(start.major, start.major + replacement.getParagraphCount());
                MaterializedListModification<Paragraph<PS, SEG, S>> parChange =
                        MaterializedListModification.create(start.major, removedPars, addedPars);
                return t(doc, change, parChange);
            });
        });
    }

    Tuple3<ReadOnlyStyledDocument<PS, SEG, S>, RichTextChange<PS, SEG, S>, MaterializedListModification<Paragraph<PS, SEG, S>>> replaceParagraph(
            int parIdx, UnaryOperator<Paragraph<PS, SEG, S>> f) {
        return replace(
                new BiIndex(parIdx, 0),
                new BiIndex(parIdx, tree.getLeaf(parIdx).length()),
                doc -> doc.mapParagraphs(f));
    }

    ReadOnlyStyledDocument<PS, SEG, S> mapParagraphs(UnaryOperator<Paragraph<PS, SEG, S>> f) {
        int n = tree.getLeafCount();
        List<Paragraph<PS, SEG, S>> pars = new ArrayList<>(n);
        for(int i = 0; i < n; ++i) {
            pars.add(f.apply(tree.getLeaf(i)));
        }
        return new ReadOnlyStyledDocument<>(pars);
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
            StyledDocument<?, ?, ?> that = (StyledDocument<?, ?, ?>) other;
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
