package org.fxmisc.richtext.model;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.Suspendable;
import org.reactfx.SuspendableEventStream;
import org.reactfx.SuspendableNo;
import org.reactfx.collection.LiveList;
import org.reactfx.collection.LiveListBase;
import org.reactfx.collection.MaterializedListModification;
import org.reactfx.collection.QuasiListModification;
import org.reactfx.collection.SuspendableList;
import org.reactfx.collection.UnmodifiableByDefaultLiveList;
import org.reactfx.util.BiIndex;
import org.reactfx.util.Lists;
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
            return parChanges.subscribe(mod -> {
                mod = mod.trim();
                QuasiListModification<Paragraph<PS, SEG, S>> qmod =
                        QuasiListModification.create(mod.getFrom(), mod.getRemoved(), mod.getAddedSize());
                notifyObservers(qmod.asListChange());
            });
        }
    }

    private ReadOnlyStyledDocument<PS, SEG, S> doc;

    private final EventSource<RichTextChange<PS, SEG, S>> internalRichChanges = new EventSource<>();
    private final SuspendableEventStream<RichTextChange<PS, SEG, S>> richChanges = internalRichChanges.pausable();
    @Override public EventStream<RichTextChange<PS, SEG, S>> richChanges() { return richChanges; }

    private final Val<String> internalText = Val.create(() -> doc.getText(), internalRichChanges);
    private final SuspendableVal<String> text = internalText.suspendable();
    @Override public String getText() { return text.getValue(); }
    @Override public Val<String> textProperty() { return text; }


    private final Val<Integer> internalLength = Val.create(() -> doc.length(), internalRichChanges);
    private final SuspendableVal<Integer> length = internalLength.suspendable();
    @Override public int getLength() { return length.getValue(); }
    @Override public Val<Integer> lengthProperty() { return length; }
    @Override public int length() { return length.getValue(); }

    private final EventSource<MaterializedListModification<Paragraph<PS, SEG, S>>> parChanges =
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

    GenericEditableStyledDocumentBase(Paragraph<PS, SEG, S> initialParagraph/*, SegmentOps<SEG, S> segmentOps*/) {
        this.doc = new ReadOnlyStyledDocument<>(Collections.singletonList(initialParagraph));

        final Suspendable omniSuspendable = Suspendable.combine(
                text,
                length,

                // add streams after properties, to be released before them
                richChanges,

                // paragraphs to be released first
                paragraphs);
        omniSuspendable.suspendWhen(beingUpdated);
    }

    /**
     * Creates an empty {@link EditableStyledDocument}
     */
    public GenericEditableStyledDocumentBase(PS initialParagraphStyle, S initialStyle, TextOps<SEG, S> segmentOps) {
        this(new Paragraph<>(initialParagraphStyle, segmentOps, segmentOps.create("", initialStyle)));
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
    public void replace(int start, int end, StyledDocument<PS, SEG, S> replacement) {
        ensureValidRange(start, end);
        doc.replace(start, end, ReadOnlyStyledDocument.from(replacement)).exec(this::update);
    }

    @Override
    public void setStyle(int from, int to, S style) {
        ensureValidRange(from, to);
        doc.replace(from, to, removed -> removed.mapParagraphs(par -> par.restyle(style))).exec(this::update);
    }

    @Override
    public void setStyle(int paragraph, S style) {
        ensureValidParagraphIndex(paragraph);
        doc.replaceParagraph(paragraph, p -> p.restyle(style)).exec(this::update);
    }

    @Override
    public void setStyle(int paragraph, int fromCol, int toCol, S style) {
        ensureValidParagraphRange(paragraph, fromCol, toCol);
        doc.replace(
            new BiIndex(paragraph, fromCol),
            new BiIndex(paragraph, toCol),
            d -> d.mapParagraphs(p -> p.restyle(style))
        ).exec(this::update);
    }

    @Override
    public void setStyleSpans(int from, StyleSpans<? extends S> styleSpans) {
        int len = styleSpans.length();
        ensureValidRange(from, from + len);
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
        }).exec(this::update);
    }

    @Override
    public void setStyleSpans(int paragraph, int from, StyleSpans<? extends S> styleSpans) {
        setStyleSpans(doc.position(paragraph, from).toOffset(), styleSpans);
    }

    @Override
    public void setParagraphStyle(int parIdx, PS style) {
        ensureValidParagraphIndex(parIdx);
        doc.replaceParagraph(parIdx, p -> p.setParagraphStyle(style)).exec(this::update);
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

    private void ensureValidParagraphIndex(int parIdx) {
        Lists.checkIndex(parIdx, doc.getParagraphCount());
    }

    private void ensureValidRange(int start, int end) {
        Lists.checkRange(start, end, length());
    }

    private void ensureValidParagraphRange(int par, int start, int end) {
        ensureValidParagraphIndex(par);
        Lists.checkRange(start, end, fullLength(par));
    }

    private int fullLength(int par) {
        int n = doc.getParagraphCount();
        return doc.getParagraph(par).length() + (par == n-1 ? 0 : 1);
    }

    private void update(
            ReadOnlyStyledDocument<PS, SEG, S> newValue,
            RichTextChange<PS, SEG, S> change,
            MaterializedListModification<Paragraph<PS, SEG, S>> parChange) {
        this.doc = newValue;
        beingUpdated.suspendWhile(() -> {
            internalRichChanges.push(change);
            parChanges.push(parChange);
        });
    }
}
