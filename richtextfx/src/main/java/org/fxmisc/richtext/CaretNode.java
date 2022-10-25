package org.fxmisc.richtext;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.geometry.Bounds;
import javafx.scene.shape.Path;
import org.fxmisc.richtext.model.PlainTextChange;
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

import java.text.BreakIterator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import static javafx.util.Duration.ZERO;
import static org.fxmisc.richtext.model.TwoDimensional.Bias.Forward;
import static org.reactfx.EventStreams.invalidationsOf;
import static org.reactfx.EventStreams.merge;

/**
 * Default implementation for a {@link Caret}. Since only one {@link Path} object is used per caret, the model
 * and view were combined into one item to grant easier access to and modification of CSS-related
 * properties. Caution must be exercised when depending on Path-related properties in any way (e.g.
 * {@link #boundsInLocalProperty()}, {@link #parentProperty()}, etc.). Also, {@link #caretBoundsProperty()}
 * is distinguishable from {@link #boundsInLocalProperty()}.
 *
 * <p>
 *     This class adds the css property "-rtfx-blink-rate" ({@link #blinkRateProperty()}}
 * </p>
 */
public class CaretNode extends Path implements Caret, Comparable<CaretNode> {

    private static final javafx.util.Duration HALF_A_SECOND = javafx.util.Duration.millis(500);

    private static final EventStream<Boolean> ALWAYS_FALSE = Val.constant(false).values();
    private static final EventStream<Boolean> ALWAYS_TRUE = Val.constant(true).values();

    /* ********************************************************************** *
     *                                                                        *
     * Observables                                                            *
     *                                                                        *
     * Observables are "dynamic" (i.e. changing) characteristics of this      *
     * control. They are not directly settable by the client code, but change *
     * in response to user input and/or API actions.                          *
     *                                                                        *
     * ********************************************************************** */

    /**
     * Controls the blink rate of the caret, when one is displayed. Setting
     * the duration to zero disables blinking.
     */
    private final StyleableObjectProperty<javafx.util.Duration> blinkRate
            = new CustomStyleableProperty<>(HALF_A_SECOND, "blinkRate", this, BLINK_RATE);

    /**
     * The blink rate of the caret.
     *
     * Can be styled from CSS using the "-rtfx-blink-rate" property.
     */
    @Override public ObjectProperty<javafx.util.Duration> blinkRateProperty() { return blinkRate; }
    @Override public javafx.util.Duration getBlinkRate() { return blinkRate.getValue(); }
    @Override public void setBlinkRate(javafx.util.Duration rate) { blinkRate.set(rate); }

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

    private final SuspendableVal<Optional<Bounds>> bounds;
    @Override public final Optional<Bounds> getCaretBounds() { return bounds.getValue(); }
    @Override public final ObservableValue<Optional<Bounds>> caretBoundsProperty() { return bounds; }

    private Optional<ParagraphBox.CaretOffsetX> targetOffset = Optional.empty();
    @Override public final void clearTargetOffset() { targetOffset = Optional.empty(); }
    @Override public final ParagraphBox.CaretOffsetX getTargetOffset() {
        if (!targetOffset.isPresent()) {
            targetOffset = Optional.of(area.getCaretOffsetX(this));
        }
        return targetOffset.get();
    }

    private final SuspendableNo beingUpdated = new SuspendableNo();
    @Override public final boolean isBeingUpdated() { return beingUpdated.get(); }
    @Override public final SuspendableNo beingUpdatedProperty() { return beingUpdated; }

    private final GenericStyledArea<?, ?, ?> area;
    @Override public GenericStyledArea<?, ?, ?> getArea() { return area; }

    private final String name;
    @Override public final String getCaretName() { return name; }

    private final SuspendableNo dependentBeingUpdated;
    private final EventStream<?> dirty;
    private final Var<Integer> internalTextPosition;

    private Subscription subscriptions = () -> {};

    public CaretNode(String name, GenericStyledArea<?, ?, ?> area) {
        this(name, area, 0);
    }

    public CaretNode(String name, GenericStyledArea<?, ?, ?> area, int startingPosition) {
        this(name, area, area.beingUpdatedProperty(), startingPosition);
    }

    public CaretNode(String name, GenericStyledArea<?, ?, ?> area, SuspendableNo dependentBeingUpdated, int startingPosition) {
        this.name = name;
        this.area = area;
        this.dependentBeingUpdated = dependentBeingUpdated;

        this.getStyleClass().add("caret");
        this.setManaged(false);

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
        manageSubscription(area.multiPlainChanges(), list -> {
            int finalPosition = getPosition();
            for (PlainTextChange plainTextChange : list) {
                int netLength = plainTextChange.getNetLength();
                if (netLength != 0) {
                    int indexOfChange = plainTextChange.getPosition();
                    // in case of a replacement: "hello there" -> "hi."
                    int endOfChange = indexOfChange + Math.abs(netLength);

                    /*
                        "->" means add (positive) netLength to position
                        "<-" means add (negative) netLength to position
                        "x" means don't update position

                        "+c" means caret was included in the deleted portion of content
                        "-c" means caret was not included in the deleted portion of content
                        Before/At/After means indexOfChange "<" / "==" / ">" position

                               |   Before +c   | Before -c | At | After
                        -------+---------------+-----------+----+------
                        Add    |      N/A      |    ->     | -> | x
                        Delete | indexOfChange |    <-     | x  | x
                     */
                    if (indexOfChange == finalPosition && netLength > 0) {
                        finalPosition = finalPosition + netLength;
                    } else if (indexOfChange < finalPosition) {
                        finalPosition = finalPosition < endOfChange
                                        ? indexOfChange
                                        : finalPosition + netLength;
                    }
                }
            }
            if (finalPosition != getPosition()) {
                moveTo(finalPosition);
            }
        });

        // whether or not to display the caret
        EventStream<Boolean> blinkCaret = showCaret.values()
                .flatMap(mode -> {
                    switch (mode) {
                        case ON:   return ALWAYS_TRUE;
                        case OFF:  return ALWAYS_FALSE;
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
        EventStream<javafx.util.Duration> nonNullBlinkRates = EventStreams.valuesOf(blinkRate).filter(i -> i != null);
        manageSubscription(
                EventStreams.combine(blinkCaret, nonNullBlinkRates)
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
                        .feedTo(visibleProperty())
        );

        bounds = Val.create(
                () -> area.getCaretBoundsOnScreen(this),
                EventStreams.merge(area.viewportDirtyEvents(), dirty)
        ).suspendable();

        lineIndex = Val.create(
                () -> OptionalInt.of(area.lineIndex(getParagraphIndex(), getColumnPosition())),
                dirty
        ).suspendable();

        Suspendable omniSuspendable = Suspendable.combine(
                beingUpdated,

                lineIndex,
                bounds,

                paragraphIndex,
                columnPosition,
                position
        );
        manageSubscription(omniSuspendable.suspendWhen(dependentBeingUpdated));
    }

    /* ********************************************************************** *
     *                                                                        *
     * Actions                                                                *
     *                                                                        *
     * Actions change the state of this control. They typically cause a       *
     * change of one or more observables and/or produce an event.             *
     *                                                                        *
     * ********************************************************************** */

    public void moveTo(int paragraphIndex, int columnPosition) {
        moveTo(textPosition(paragraphIndex, columnPosition));
    }

    public void moveTo(int position) {
        Runnable updatePos = () -> internalTextPosition.setValue(position);
        if (isBeingUpdated()) {
            updatePos.run();
        } else {
            dependentBeingUpdated.suspendWhile(updatePos);
        }
    }

    @Override
    public void moveToParStart() {
        moveTo(getPosition() - getColumnPosition());
    }

    @Override
    public void moveToParEnd() {
        moveTo(getPosition() - getColumnPosition() + area.getParagraphLength(getParagraphIndex()));
    }

    @Override
    public void moveToAreaEnd() {
        moveTo(area.getLength());
    }

    @Override
    public void moveToNextChar() {
        moveTo(getPosition() + 1);
    }

    @Override
    public void moveToPrevChar() {
        moveTo(getPosition() - 1);
    }

    @Override
    public void moveBreaksBackwards(int numOfBreaks, BreakIterator breakIterator) {
        moveContentBreaks(numOfBreaks, breakIterator, false);
    }

    @Override
    public void moveBreaksForwards(int numOfBreaks, BreakIterator breakIterator) {
        moveContentBreaks(numOfBreaks, breakIterator, true);
    }

    @Override
    public int compareTo(CaretNode o) {
        return Integer.compare(hashCode(), o.hashCode());
    }

    public void dispose() {
        subscriptions.unsubscribe();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return String.format(
                "CaretNode(name=%s position=%s paragraphIndex=%s columnPosition=%s %s)",
                getCaretName(), getPosition(), getParagraphIndex(), getColumnPosition(), super.toString()
        );
    }

    /* ********************************************************************** *
     *                                                                        *
     * Private methods                                                        *
     *                                                                        *
     * ********************************************************************** */

    private int textPosition(int row, int col) {
        return area.position(row, col).toOffset();
    }

    private <T> void manageSubscription(EventStream<T> stream, Consumer<T> subscriber) {
        manageSubscription(stream.subscribe(subscriber));
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

    /**
     * Helper method for reducing duplicate code
     * @param numOfBreaks the number of breaks
     * @param breakIterator the type of iterator to use
     * @param followingNotPreceding if true, use {@link BreakIterator#following(int)}.
     *                              Otherwise, use {@link BreakIterator#preceding(int)}.
     */
    private void moveContentBreaks(int numOfBreaks, BreakIterator breakIterator, boolean followingNotPreceding) {
        if (area.getLength() == 0) {
            return;
        }

        breakIterator.setText(area.getText());
        if (followingNotPreceding) {
            breakIterator.following(getPosition());
        } else {
            breakIterator.preceding(getPosition());
        }
        for (int i = 1; i < numOfBreaks; i++) {
            breakIterator.next();
        }
        moveTo(breakIterator.current());
    }

    /* ********************************************************************** *
     *                                                                        *
     * CSS                                                                    *
     *                                                                        *
     * ********************************************************************** */

    private static final CssMetaData<CaretNode, javafx.util.Duration> BLINK_RATE
            = new CustomCssMetaData<>("-rtfx-blink-rate", StyleConverter.getDurationConverter(),
            javafx.util.Duration.millis(500), s -> s.blinkRate
    );

    private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA_LIST;

    static {
        List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Path.getClassCssMetaData());

        styleables.add(BLINK_RATE);

        CSS_META_DATA_LIST = Collections.unmodifiableList(styleables);
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CSS_META_DATA_LIST;
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS_META_DATA_LIST;
    }

}
