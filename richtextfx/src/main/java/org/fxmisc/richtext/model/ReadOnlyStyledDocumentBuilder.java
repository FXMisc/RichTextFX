package org.fxmisc.richtext.model;

import org.reactfx.util.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Helper class via {@link #constructDocument(SegmentOps, Object, Consumer)} or one of its constructors and
 * {@link #build()} for easily creating a {@link ReadOnlyStyledDocument} from a list
 * of {@link Paragraph}s that is constructed via "addParagraph(s)" methods.
 *
 * @param <PS> the paragraph style type
 * @param <SEG> the segment type
 * @param <S> the segment style type
 */
public final class ReadOnlyStyledDocumentBuilder<PS, SEG, S> {

    /**
     * Constructs a list of paragraphs
     *
     * @param segmentOps the {@link SegmentOps} object to use for one of the {@link Paragraph}'s constructors
     * @param defaultParagraphStyle the paragraph style object to use when it is not specified in the
     *                              "addParagraph" methods
     * @param configuration call the builder's {@link #addParagraph(Object, Object)} methods here
     */
    public static <PS, SEG, S> ReadOnlyStyledDocument<PS, SEG, S> constructDocument(
            SegmentOps<SEG, S> segmentOps, PS defaultParagraphStyle,
            Consumer<ReadOnlyStyledDocumentBuilder<PS, SEG, S>> configuration) {
        ReadOnlyStyledDocumentBuilder<PS, SEG, S> builder = new ReadOnlyStyledDocumentBuilder<>(segmentOps, defaultParagraphStyle);
        configuration.accept(builder);
        return builder.build();
    }

    /**
     * Constructs a list of paragraphs
     *
     * @param segmentOps the {@link SegmentOps} object to use for one of the {@link Paragraph}'s constructors
     * @param defaultParagraphStyle the paragraph style object to use when it is not specified in the
     *                              "addParagraph" methods
     * @param initialCapacity the initial capaicty for the underlying {@link ArrayList}
     * @param configuration call the builder's {@link #addParagraph(Object, Object)} methods here
     */
    public static <PS, SEG, S> ReadOnlyStyledDocument<PS, SEG, S> constructDocument(
            SegmentOps<SEG, S> segmentOps, PS defaultParagraphStyle,
            int initialCapacity, Consumer<ReadOnlyStyledDocumentBuilder<PS, SEG, S>> configuration) {
        ReadOnlyStyledDocumentBuilder<PS, SEG, S> builder = new ReadOnlyStyledDocumentBuilder<>(segmentOps, defaultParagraphStyle, initialCapacity);
        configuration.accept(builder);
        return builder.build();
    }

    private final SegmentOps<SEG, S> segmentOps;
    private final PS defaultParagraphStyle;
    private final List<Paragraph<PS, SEG, S>> paragraphList;

    private boolean alreadyCreated = false;

    /**
     * Creates a builder
     *
     * @param segmentOps the {@link SegmentOps} to use for each call to one of the {@link Paragraph}'s constructors.
     * @param defaultParagraphStyle the default paragraph style to use when one is not specified in the
     *                              "addParagraph"-prefixed methods
     */
    public ReadOnlyStyledDocumentBuilder(SegmentOps<SEG, S> segmentOps, PS defaultParagraphStyle) {
        this(segmentOps, defaultParagraphStyle, new ArrayList<>());
    }

    /**
     * Creates a builder
     *
     * @param segmentOps the {@link SegmentOps} to use for each call to one of the {@link Paragraph}'s constructors.
     * @param defaultParagraphStyle the default paragraph style to use when one is not specified in the
     *                              "addParagraph"-prefixed methods
     * @param initialCapacity the initial capacity of the underlying {@link ArrayList}.
     */
    public ReadOnlyStyledDocumentBuilder(SegmentOps<SEG, S> segmentOps, PS defaultParagraphStyle, int initialCapacity) {
        this(segmentOps, defaultParagraphStyle, new ArrayList<>(initialCapacity));
    }

    private ReadOnlyStyledDocumentBuilder(SegmentOps<SEG, S> segmentOps, PS defaultParagraphStyle, List<Paragraph<PS, SEG, S>> list) {
        this.segmentOps = segmentOps;
        this.defaultParagraphStyle = defaultParagraphStyle;
        this.paragraphList = list;
    }

    /**
     * Adds to the list a paragraph that is constructed using the list of {@link StyledSegment}s.
     */
    public ReadOnlyStyledDocumentBuilder<PS, SEG, S> addParagraph(List<StyledSegment<SEG, S>> styledSegments) {
        return addParagraph(styledSegments, null);
    }

    /**
     * Adds to the list a paragraph that is constructed using the list of {@link StyledSegment}s.
     */
    public ReadOnlyStyledDocumentBuilder<PS, SEG, S> addParagraph(List<StyledSegment<SEG, S>> styledSegments, PS paragraphStyle) {
        return addPar(new Paragraph<>(argumentOrDefault(paragraphStyle), segmentOps, styledSegments));
    }

    /**
     * Adds to the list  a paragraph that has only one segment that has the same given style throughout.
     */
    public ReadOnlyStyledDocumentBuilder<PS, SEG, S> addParagraph(SEG segment, S style) {
        return addParagraph(segment, style, null);
    }

    /**
     * Adds to the list  a paragraph that has only one segment that has the same given style throughout.
     */
    public ReadOnlyStyledDocumentBuilder<PS, SEG, S> addParagraph(SEG segment, S style, PS paragraphStyle) {
        return addPar(new Paragraph<>(argumentOrDefault(paragraphStyle), segmentOps, segment, style));
    }

    /**
     * Adds to the list  a paragraph that has only one segment but a number of different styles throughout that segment
     */
    public ReadOnlyStyledDocumentBuilder<PS, SEG, S> addParagraph(SEG segment, StyleSpans<S> styles) {
        return addParagraph(segment, styles, null);
    }

    /**
     * Adds to the list  a paragraph that has only one segment but a number of different styles throughout that segment
     */
    public ReadOnlyStyledDocumentBuilder<PS, SEG, S> addParagraph(SEG segment, StyleSpans<S> styles, PS paragraphStyle) {
        return addPar(new Paragraph<>(argumentOrDefault(paragraphStyle), segmentOps, segment, styles));
    }

    /**
     * Adds to the list a paragraph that has multiple segments with multiple styles throughout those segments
     */
    public ReadOnlyStyledDocumentBuilder<PS, SEG, S> addParagraph(List<SEG> segments, StyleSpans<S> styles) {
        return addParagraph(segments, styles, null);
    }

    /**
     * Adds to the list a paragraph that has multiple segments with multiple styles throughout those segments
     */
    public ReadOnlyStyledDocumentBuilder<PS, SEG, S> addParagraph(List<SEG> segments, StyleSpans<S> styles, PS paragraphStyle) {
        return addPar(new Paragraph<>(argumentOrDefault(paragraphStyle), segmentOps, segments, styles));
    }

    /**
     * Adds multiple paragraphs to the list, using the {@link #defaultParagraphStyle} for each paragraph. For
     * more configuration on each paragraph's paragraph style, use {@link #addParagraphs0(List, StyleSpans)}
     *
     * @param listOfSegLists each item is the list of segments for a single paragraph
     * @param entireDocumentStyleSpans style spans for the entire document. It's length should be equal to the length
     *                                 of all the segments' length combined
     */
    public ReadOnlyStyledDocumentBuilder<PS, SEG, S> addParagraphs(List<List<SEG>> listOfSegLists,
                                 StyleSpans<S> entireDocumentStyleSpans) {
        return addParagraphList(listOfSegLists, entireDocumentStyleSpans, ignore -> null, Function.identity());
    }

    /**
     * Adds multiple paragraphs to the list, allowing one to specify each paragraph's paragraph style.
     *
     * @param paragraphArgList each item is a Tuple2 that represents the paragraph style and segment list
     *                         for a single paragraph. If the paragraph style is {@code null},
     *                         the {@link #defaultParagraphStyle} will be used instead.
     * @param entireDocumentStyleSpans style spans for the entire document. It's length should be equal to the length
     *                                 of all the segments' length combined
     */
    public ReadOnlyStyledDocumentBuilder<PS, SEG, S> addParagraphs0(List<Tuple2<PS, List<SEG>>> paragraphArgList,
                                 StyleSpans<S> entireDocumentStyleSpans) {
        return addParagraphList(paragraphArgList, entireDocumentStyleSpans, Tuple2::get1, Tuple2::get2);
    }

    /**
     * Returns an unmodifiable list of the constructed {@link Paragraph}s and ensures this builder cannot be used again.
     */
    public ReadOnlyStyledDocument<PS, SEG, S> build() {
        ensureNotYetCreated();
        if (paragraphList.isEmpty()) {
            throw new IllegalStateException("Cannot build a ReadOnlyStyledDocument with an empty list of paragraphs!");
        }
        alreadyCreated = true;
        return new ReadOnlyStyledDocument<>(paragraphList);
    }

    private <T> ReadOnlyStyledDocumentBuilder<PS, SEG, S> addParagraphList(List<T> paragraphContentList, StyleSpans<S> spansThroughoutDocument,
                                       Function<T, PS> getStyle, Function<T, List<SEG>> getSegList) {
        int docLength = paragraphContentList.stream()
                .map(getSegList)
                .flatMap(l -> l.stream().map(segmentOps::length))
                .reduce(0, (a, b) -> a + b);

        if (docLength != spansThroughoutDocument.length()) {
            throw new IllegalArgumentException(String.format(
                    "Document length does not equal style spans length! docLength=%s styleSpans' length=%s",
                    docLength, spansThroughoutDocument.length()
            ));
        }

        int styleOffset = 0;
        for (T paragraphContent : paragraphContentList) {
            PS paragraphStyle = argumentOrDefault(getStyle.apply(paragraphContent));
            List<SEG> segList = getSegList.apply(paragraphContent);

            int paragraphLength = segList.stream().mapToInt(segmentOps::length).sum();
            int styleEnd = styleOffset + paragraphLength;
            StyleSpans<S> spans = spansThroughoutDocument.subView(styleOffset, styleEnd);
            addPar(new Paragraph<>(paragraphStyle, segmentOps, segList, spans));

            styleOffset = styleEnd;
        }
        return this;
    }

    /**
     * Returns the argument if it is not null; otherwise, returns {@link #defaultParagraphStyle}
     */
    private PS argumentOrDefault(PS paragraphStyle) {
        return paragraphStyle != null ? paragraphStyle : defaultParagraphStyle;
    }

    private void ensureNotYetCreated() {
        if (alreadyCreated) {
            throw new IllegalStateException("This builder has already been used to create a list of Paragraphs. " +
                    "One builder can only be used to build a single list. To create a new one, create a new builder");
        }
    }

    private ReadOnlyStyledDocumentBuilder<PS, SEG, S> addPar(Paragraph<PS, SEG, S> paragraph) {
        ensureNotYetCreated();
        paragraphList.add(paragraph);
        return this;
    }
}
