package org.fxmisc.richtext;

import javafx.beans.binding.Binding;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import org.fxmisc.richtext.model.TwoDimensional;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.StateMachine;
import org.reactfx.Subscription;
import org.reactfx.Suspendable;
import org.reactfx.SuspendableNo;
import org.reactfx.value.SuspendableVal;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import static javafx.util.Duration.ZERO;
import static org.fxmisc.richtext.model.TwoDimensional.Bias.Forward;
import static org.reactfx.EventStreams.invalidationsOf;
import static org.reactfx.EventStreams.merge;

final class CaretImpl implements Caret {

    private final Var<Integer> internalTextPosition;

    private final SuspendableVal<Integer> position;
    @Override public final int getPosition() { return position.getValue(); }
    @Override public final ObservableValue<Integer> positionProperty() { return position; }

    private final SuspendableVal<Integer> paragraphIndex;
    @Override public final int getParagraphIndex() { return paragraphIndex.getValue(); }
    @Override public final ObservableValue<Integer> paragraphIndexProperty() { return paragraphIndex; }

    private final SuspendableVal<OptionalInt> lineIndex;
    @Override public final OptionalInt getLineIndex() { return lineIndex.getValue(); }
    @Override public final ObservableValue<OptionalInt> lineIndexProperty() { return lineIndex; }

    private final SuspendableVal<Integer> columnPosition;
    @Override public final int getColumnPosition() { return columnPosition.getValue(); }
    @Override public final ObservableValue<Integer> columnPositionProperty() { return columnPosition; }

    private final Var<CaretVisibility> showCaret = Var.newSimpleVar(CaretVisibility.AUTO);
    @Override public final CaretVisibility getShowCaret() { return showCaret.getValue(); }
    @Override public final void setShowCaret(CaretVisibility value) { showCaret.setValue(value); }
    @Override public final Var<CaretVisibility> showCaretProperty() { return showCaret; }

    private final Binding<Boolean> visible;
    @Override public final boolean isVisible() { return visible.getValue(); }
    @Override public final ObservableValue<Boolean> visibleProperty() { return visible; }

    private final Val<Optional<Bounds>> bounds;
    @Override public final Optional<Bounds> getBounds() { return bounds.getValue(); }
    @Override public final ObservableValue<Optional<Bounds>> boundsProperty() { return bounds; }

    private Optional<ParagraphBox.CaretOffsetX> targetOffset = Optional.empty();
    @Override public final void clearTargetOffset() { targetOffset = Optional.empty(); }
    @Override public final ParagraphBox.CaretOffsetX getTargetOffset() {
        if (!targetOffset.isPresent()) {
            targetOffset = Optional.of(area.getCaretOffsetX(getParagraphIndex()));
        }
        return targetOffset.get();
    }

    private final EventStream<?> dirty;
    @Override public final EventStream<?> dirtyEvents() { return dirty; }

    private final SuspendableNo beingUpdated = new SuspendableNo();
    @Override public final boolean isBeingUpdated() { return beingUpdated.get(); }
    @Override public final ObservableValue<Boolean> beingUpdatedProperty() { return beingUpdated; }

    private final GenericStyledArea<?, ?, ?> area;
    private final SuspendableNo dependentBeingUpdated;

    private Subscription subscriptions = () -> {};

    CaretImpl(GenericStyledArea<?, ?, ?> area) {
        this(area, 0);
    }

    CaretImpl(GenericStyledArea<?, ?, ?> area, int startingPosition) {
        this(area, area.beingUpdatedProperty(), 0);
    }

    CaretImpl(GenericStyledArea<?, ?, ?> area, SuspendableNo dependentBeingUpdated, int startingPosition) {
        this.area = area;
        this.dependentBeingUpdated = dependentBeingUpdated;
        internalTextPosition = Var.newSimpleVar(startingPosition);
        position = internalTextPosition.suspendable();

        Val<TwoDimensional.Position> caretPosition2D = Val.create(
                () -> area.offsetToPosition(internalTextPosition.getValue(), Forward),
                internalTextPosition, area.getParagraphs()
        );
        paragraphIndex = caretPosition2D.map(TwoDimensional.Position::getMajor).suspendable();
        columnPosition = caretPosition2D.map(TwoDimensional.Position::getMinor).suspendable();

        // when content is updated by an area, update the caret of all the other
        // clones that also display the same document
        manageSubscription(area.plainTextChanges(), (plainTextChange -> {
            int changeLength = plainTextChange.getInserted().length() - plainTextChange.getRemoved().length();
            if (changeLength != 0) {
                int indexOfChange = plainTextChange.getPosition();
                // in case of a replacement: "hello there" -> "hi."
                int endOfChange = indexOfChange + Math.abs(changeLength);

                int caretPosition = getPosition();
                if (indexOfChange < caretPosition) {
                    // if caret is within the changed content, move it to indexOfChange
                    // otherwise offset it by changeLength
                    moveTo(
                            caretPosition < endOfChange
                                    ? indexOfChange
                                    : caretPosition + changeLength
                    );
                }
            }
        }));

        // whether or not to display the caret
        EventStream<Boolean> blinkCaret = showCaret.values()
                .flatMap(mode -> {
                    switch (mode) {
                        case ON:   return Val.constant(true).values();
                        case OFF:  return Val.constant(false).values();
                        default:
                        case AUTO: return area.autoCaretBlink();
                    }
                });

        dirty = merge(
                invalidationsOf(positionProperty()),
                invalidationsOf(area.getParagraphs())
        );

        // The caret is visible in periodic intervals,
        // but only when blinkCaret is true.
        visible = EventStreams.combine(blinkCaret, area.caretBlinkRateEvents())
                .flatMap(tuple -> {
                    Boolean blink = tuple.get1();
                    javafx.util.Duration rate = tuple.get2();
                    if(blink) {
                        return rate.lessThanOrEqualTo(ZERO)
                                ? Val.constant(true).values()
                                : booleanPulse(rate, dirty);
                    } else {
                        return Val.constant(false).values();
                    }
                })
                .toBinding(false);
        manageBinding(visible);

        bounds = Val.create(
                () -> area.getCaretBoundsOnScreen(getParagraphIndex()),
                area.boundsDirtyFor(dirty)
        );

        lineIndex = Val.create(
                () -> OptionalInt.of(area.lineIndex(getParagraphIndex(), getColumnPosition())),
                dirty
        ).suspendable();

        Suspendable omniSuspendable = Suspendable.combine(
                beingUpdated,

                paragraphIndex,
                columnPosition,
                position
        );
        manageSubscription(omniSuspendable.suspendWhen(dependentBeingUpdated));
    }

    public void moveTo(int paragraphIndex, int columnPosition) {
        moveTo(textPosition(paragraphIndex, columnPosition));
    }

    public void moveTo(int position) {
        dependentBeingUpdated.suspendWhile(() -> internalTextPosition.setValue(position));
    }

    public void dispose() {
        subscriptions.unsubscribe();
    }

    private int textPosition(int row, int col) {
        return area.position(row, col).toOffset();
    }

    private <T> void manageSubscription(EventStream<T> stream, Consumer<T> subscriber) {
        manageSubscription(stream.subscribe(subscriber));
    }

    private void manageBinding(Binding<?> binding) {
        manageSubscription(binding::dispose);
    }

    private void manageSubscription(Subscription s) {
        subscriptions = subscriptions.and(s);
    }

    private static EventStream<Boolean> booleanPulse(javafx.util.Duration javafxDuration, EventStream<?> restartImpulse) {
        Duration duration = Duration.ofMillis(Math.round(javafxDuration.toMillis()));
        EventStream<?> ticks = EventStreams.restartableTicks(duration, restartImpulse);
        return StateMachine.init(false)
                .on(restartImpulse.withDefaultEvent(null)).transition((state, impulse) -> true)
                .on(ticks).transition((state, tick) -> !state)
                .toStateStream();
    }

}
