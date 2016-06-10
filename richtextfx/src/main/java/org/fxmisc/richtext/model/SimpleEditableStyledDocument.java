package org.fxmisc.richtext.model;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.SuspendableNo;
import org.reactfx.collection.LiveList;
import org.reactfx.collection.LiveListBase;
import org.reactfx.collection.MaterializedListModification;
import org.reactfx.collection.QuasiListModification;
import org.reactfx.collection.UnmodifiableByDefaultLiveList;
import org.reactfx.util.BiIndex;
import org.reactfx.util.Lists;
import org.reactfx.value.Val;

/**
 * Provides an implementation of {@link EditableStyledDocument}
 */
public final class SimpleEditableStyledDocument<PS, S> implements EditableStyledDocument<PS, S> {

    private class ParagraphList
    extends LiveListBase<Paragraph<PS, S>>
    implements UnmodifiableByDefaultLiveList<Paragraph<PS, S>> {

        @Override
        public Paragraph<PS, S> get(int index) {
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
                QuasiListModification<Paragraph<PS, S>> qmod =
                        QuasiListModification.create(mod.getFrom(), mod.getRemoved(), mod.getAddedSize());
                notifyObservers(qmod.asListChange());
            });
        }
    }

    private ReadOnlyStyledDocument<PS, S> doc;

    private final EventSource<RichTextChange<PS, S>> richChanges = new EventSource<>();
    @Override public EventStream<RichTextChange<PS, S>> richChanges() { return richChanges; }

    private final Val<String> text = Val.create(() -> doc.getText(), richChanges);
    @Override public String getText() { return text.getValue(); }
    @Override public Val<String> textProperty() { return text; }


    private final Val<Integer> length = Val.create(() -> doc.length(), richChanges);
    @Override public int getLength() { return length.getValue(); }
    @Override public Val<Integer> lengthProperty() { return length; }
    @Override public int length() { return length.getValue(); }

    private final EventSource<MaterializedListModification<Paragraph<PS, S>>> parChanges =
            new EventSource<>();

    private final LiveList<Paragraph<PS, S>> paragraphs = new ParagraphList();

    @Override
    public LiveList<Paragraph<PS, S>> getParagraphs() {
        return paragraphs;
    }

    @Override
    public ReadOnlyStyledDocument<PS, S> snapshot() {
        return doc;
    }

    private final SuspendableNo beingUpdated = new SuspendableNo();
    @Override public final SuspendableNo beingUpdatedProperty() { return beingUpdated; }
    @Override public final boolean isBeingUpdated() { return beingUpdated.get(); }


    SimpleEditableStyledDocument(Paragraph<PS, S> initialParagraph) {
        this.doc = new ReadOnlyStyledDocument<>(Collections.singletonList(initialParagraph));
    }

    /**
     * Creates an empty {@link EditableStyledDocument}
     */
    public SimpleEditableStyledDocument(PS initialParagraphStyle, S initialStyle) {
        this(new Paragraph<>(initialParagraphStyle, "", initialStyle));
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
    public void replace(int start, int end, StyledDocument<PS, S> replacement) {
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
            List<Paragraph<PS, S>> pars = new ArrayList<>(d.getParagraphs().size());
            for(Paragraph<PS, S> p: d.getParagraphs()) {
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
    public StyledDocument<PS, S> concat(StyledDocument<PS, S> that) {
        return doc.concat(that);
    }

    @Override
    public StyledDocument<PS, S> subSequence(int start, int end) {
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
            ReadOnlyStyledDocument<PS, S> newValue,
            RichTextChange<PS, S> change,
            MaterializedListModification<Paragraph<PS, S>> parChange) {
        this.doc = newValue;
        beingUpdated.suspendWhile(() -> {
            richChanges.push(change);
            parChanges.push(parChange);
        });
    }
}
