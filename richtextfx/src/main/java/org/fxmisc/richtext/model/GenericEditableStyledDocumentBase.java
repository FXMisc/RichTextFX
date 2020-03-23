package org.fxmisc.richtext.model;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.Suspendable;
import org.reactfx.SuspendableEventStream;
import org.reactfx.SuspendableNo;
import org.reactfx.collection.ListChangeAccumulator;
import org.reactfx.collection.LiveList;
import org.reactfx.collection.LiveListBase;
import org.reactfx.collection.MaterializedListModification;
import org.reactfx.collection.QuasiListModification;
import org.reactfx.collection.SuspendableList;
import org.reactfx.collection.UnmodifiableByDefaultLiveList;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuples;
import org.reactfx.value.SuspendableVal;
import org.reactfx.value.Val;

class GenericEditableStyledDocumentBase<PS, SEG, S> implements EditableStyledDocument<PS, SEG, S> {

    private class ParagraphList
    extends LiveListBase<Paragraph<PS, SEG, S>>
    implements UnmodifiableByDefaultLiveList<Paragraph<PS, SEG, S>> {

        @Override
        public Paragraph<PS, SEG, S> get(int index) {
            return doc.getParagraph(index);
        }

        @Override
        public int size() {
            return doc.getParagraphCount();
        }

        @Override
        protected Subscription observeInputs() {
            return parChangesList.subscribe(list -> {
                ListChangeAccumulator<Paragraph<PS, SEG, S>> accumulator = new ListChangeAccumulator<>();
                for (MaterializedListModification<Paragraph<PS, SEG, S>> mod : list) {
                	try { mod = mod.trim(); } catch ( IndexOutOfBoundsException EX ) {}

                    // add the quasiListModification itself, not as a quasiListChange, in case some overlap
                    accumulator.add(QuasiListModification.create(mod.getFrom(), mod.getRemoved(), mod.getAddedSize()));
                }
                notifyObservers(accumulator.asListChange());
            });
        }
    }

    private ReadOnlyStyledDocument<PS, SEG, S> doc;

    private final EventSource<List<RichTextChange<PS, SEG, S>>> internalRichChangeList = new EventSource<>();
    private final SuspendableEventStream<List<RichTextChange<PS, SEG, S>>> richChangeList = internalRichChangeList.pausable();
    @Override public EventStream<List<RichTextChange<PS, SEG, S>>> multiRichChanges() { return richChangeList; }

    private final Val<String> internalText = Val.create(() -> doc.getText(), internalRichChangeList);
    private final SuspendableVal<String> text = internalText.suspendable();
    @Override public String getText() { return text.getValue(); }
    @Override public Val<String> textProperty() { return text; }


    private final Val<Integer> internalLength = Val.create(() -> doc.length(), internalRichChangeList);
    private final SuspendableVal<Integer> length = internalLength.suspendable();
    @Override public int getLength() { return length.getValue(); }
    @Override public Val<Integer> lengthProperty() { return length; }
    @Override public int length() { return length.getValue(); }

    private final EventSource<List<MaterializedListModification<Paragraph<PS, SEG, S>>>> parChangesList =
            new EventSource<>();

    private final SuspendableList<Paragraph<PS, SEG, S>> paragraphs = new ParagraphList().suspendable();
    @Override
    public LiveList<Paragraph<PS, SEG, S>> getParagraphs() {
        return paragraphs;
    }

    @Override
    public ReadOnlyStyledDocument<PS, SEG, S> snapshot() {
        return doc;
    }

    private final SuspendableNo beingUpdated = new SuspendableNo();
    @Override public final SuspendableNo beingUpdatedProperty() { return beingUpdated; }
    @Override public final boolean isBeingUpdated() { return beingUpdated.get(); }

    /**
     * Creates an {@link EditableStyledDocument} with the given document as its initial content
     */
    GenericEditableStyledDocumentBase(ReadOnlyStyledDocument<PS, SEG, S> initialContent) {
        this.doc = initialContent;

        final Suspendable omniSuspendable = Suspendable.combine(
                text,
                length,

                // add streams after properties, to be released before them
                richChangeList,

                // paragraphs to be released first
                paragraphs);
        omniSuspendable.suspendWhen(beingUpdated);
    }

    /**
     * Creates an {@link EditableStyledDocument} with the given paragraph as its initial content
     */
    GenericEditableStyledDocumentBase(Paragraph<PS, SEG, S> initialParagraph) {
        this(new ReadOnlyStyledDocument<>(Collections.singletonList(initialParagraph)));
    }

    /**
     * Creates an empty {@link EditableStyledDocument}
     */
    GenericEditableStyledDocumentBase(PS initialParagraphStyle, S initialStyle, SegmentOps<SEG, S> segmentOps) {
        this(new Paragraph<>(initialParagraphStyle, segmentOps, segmentOps.createEmptySeg(), initialStyle));
    }


    @Override
    public Position position(int major, int minor) {
        return doc.position(major, minor);
    }

    @Override
    public Position offsetToPosition(int offset, Bias bias) {
        return doc.offsetToPosition(offset, bias);
    }

    @Override
    public void replaceMulti(List<Replacement<PS, SEG, S>> replacements) {
        doc.replaceMulti(replacements).exec(this::updateMulti);
    }

    @Override
    public void replace(int start, int end, StyledDocument<PS, SEG, S> replacement) {
        doc.replace(start, end, ReadOnlyStyledDocument.from(replacement)).exec(this::updateSingle);
    }

    @Override
    public void setStyle(int from, int to, S style) {
        doc.replace(from, to, removed -> removed.mapParagraphs(par -> par.restyle(style))).exec(this::updateSingle);
    }

    @Override
    public void setStyle(int paragraphIndex, S style) {
        doc.replaceParagraph(paragraphIndex, p -> p.restyle(style)).exec(this::updateSingle);
    }

    @Override
    public void setStyle(int paragraphIndex, int fromCol, int toCol, S style) {
        doc.replace(paragraphIndex, fromCol, toCol, d -> d.mapParagraphs(p -> p.restyle(style))).exec(this::updateSingle);
    }

    @Override
    public void setStyleSpans(int from, StyleSpans<? extends S> styleSpans) {
        int len = styleSpans.length();
        doc.replace(from, from + len, d -> {
            Position i = styleSpans.position(0, 0);
            List<Paragraph<PS, SEG, S>> pars = new ArrayList<>(d.getParagraphs().size());
            for(Paragraph<PS, SEG, S> p: d.getParagraphs()) {
                Position j = i.offsetBy(p.length(), Backward);
                StyleSpans<? extends S> spans = styleSpans.subView(i, j);
                pars.add(p.restyle(0, spans));
                i = j.offsetBy(1, Forward); // skip the newline
            }
            return new ReadOnlyStyledDocument<>(pars);
        }).exec(this::updateSingle);
    }

    @Override
    public void setStyleSpans(int paragraphIndex, int from, StyleSpans<? extends S> styleSpans) {
        setStyleSpans(doc.position(paragraphIndex, from).toOffset(), styleSpans);
    }

    @Override
    public void setParagraphStyle(int paragraphIndex, PS style) {
        doc.replaceParagraph(paragraphIndex, p -> p.setParagraphStyle(style)).exec(this::updateSingle);
    }

    @Override
    public StyledDocument<PS, SEG, S> concat(StyledDocument<PS, SEG, S> that) {
        return doc.concat(that);
    }

    @Override
    public StyledDocument<PS, SEG, S> subSequence(int start, int end) {
        return doc.subSequence(start, end);
    }


    /* ********************************************************************** *
     *                                                                        *
     * Private and package private methods                                    *
     *                                                                        *
     * ********************************************************************** */

    private void updateSingle(
            ReadOnlyStyledDocument<PS, SEG, S> newValue,
            RichTextChange<PS, SEG, S> change,
            MaterializedListModification<Paragraph<PS, SEG, S>> parChange) {
        updateMulti(newValue, Collections.singletonList(change), Collections.singletonList(parChange));
    }

    private void updateMulti(
            ReadOnlyStyledDocument<PS, SEG, S> newValue,
            List<RichTextChange<PS, SEG, S>> richChanges,
            List<MaterializedListModification<Paragraph<PS, SEG, S>>> parChanges) {
        this.doc = newValue;
        beingUpdated.suspendWhile(() -> {
            internalRichChangeList.push(richChanges);
            parChangesList.push(parChanges);
        });
    }

}
