package org.fxmisc.richtext;

import javafx.beans.binding.Binding;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.GenericStyledArea.CaretVisibility;
import org.fxmisc.richtext.model.CaretSelectionModel;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.StateMachine;
import org.reactfx.Subscription;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import java.time.Duration;
import java.util.Optional;

import static javafx.util.Duration.ZERO;

public class CaretSelectionView implements Caret {

    private final CaretSelectionModel model;

    public final int getCaretPosition() { return model.getCaretPosition(); }
    public final ObservableValue<Integer> caretPositionProperty() { return model.caretPositionProperty(); }

    public final int getAnchor() { return model.getAnchor(); }
    public final ObservableValue<Integer> anchorProperty() { return model.anchorProperty(); }

    public final IndexRange getSelection() { return model.getSelection(); }
    public final ObservableValue<IndexRange> selectionProperty() { return model.selectionProperty(); }

    public final String getSelectedText() { return model.getSelectedText(); }
    public final ObservableValue<String> selectedTextProperty() { return model.selectedTextProperty(); }

    public final int getCaretParagraph() { return model.getCaretParagraph(); }
    public final ObservableValue<Integer> caretParagraphProperty() { return model.caretParagraphProperty(); }

    public final int getCaretColumn() { return model.getCaretColumn(); }
    public final ObservableValue<Integer> caretColumnProperty() { return model.caretColumnProperty(); }

//    private final Val<Integer> caretLine;
//    public final int getCaretLine() { return caretLine.getValue(); }
//    public final ObservableValue<Integer> caretLineProperty() { return caretLine; }

    private final Var<CaretVisibility> showCaret = Var.newSimpleVar(CaretVisibility.AUTO);
    public final CaretVisibility getShowCaret() { return showCaret.getValue(); }
    public final void setShowCaret(CaretVisibility value) { showCaret.setValue(value); }
    public final Var<CaretVisibility> showCaretProperty() { return showCaret; }

    private final Binding<Boolean> caretVisible;
    final boolean isCaretVisible() { return caretVisible.getValue(); }
    final ObservableValue<Boolean> caretVisibleProperty() { return caretVisible; }

    /**
     * The bounds of the caret in the Screen's coordinate system or {@link Optional#empty()} if caret is not visible
     * in the viewport.
     */
    private final Val<Optional<Bounds>> caretBounds;
    public final Optional<Bounds> getCaretBounds() { return caretBounds.getValue(); }
    public final ObservableValue<Optional<Bounds>> caretBoundsProperty() { return caretBounds; }

    private Subscription subscriptions;

    /**
     * The bounds of the selection in the Screen's coordinate system if something is selected and visible in the
     * viewport, {@link #caretBounds} if nothing is selected and caret is visible in the viewport, or
     * {@link Optional#empty()} if selection is not visible in the viewport.
     */
    private final Val<Optional<Bounds>> selectionBounds;
    public final Optional<Bounds> getSelectionBounds() { return selectionBounds.getValue(); }
    public final ObservableValue<Optional<Bounds>> selectionBoundsProperty() { return selectionBounds; }

    CaretSelectionView(CaretSelectionModel model, GenericStyledArea<?, ?, ?> area) {
        this.model = model;

        // whether or not to display the caret
        EventStream<Boolean> blinkCaret = showCaret.values()
                .flatMap(mode -> {
                    switch (mode) {
                        case ON:
                            return Val.constant(true).values();
                        case OFF:
                            return Val.constant(false).values();
                        default:
                        case AUTO:
                            return EventStreams.valuesOf(area.focusedProperty()
                                    .and(area.editableProperty())
                                    .and(area.disabledProperty().not()));
                    }
                });

        // The caret is visible in periodic intervals,
        // but only when blinkCaret is true.
        caretVisible = EventStreams.combine(blinkCaret, area.caretBlinkRateEvents())
                .flatMap(tuple -> {
                    Boolean blink = tuple.get1();
                    javafx.util.Duration rate = tuple.get2();
                    if(blink) {
                        return rate.lessThanOrEqualTo(ZERO)
                                ? Val.constant(true).values()
                                : booleanPulse(rate, model.caretDirtyEvents());
                    } else {
                        return Val.constant(false).values();
                    }
                })
                .toBinding(false);
        manageBinding(caretVisible);

        caretBounds = Val.create(
                () -> area.getCaretBoundsOnScreen(getCaretParagraph()),
                area.boundsDirtyFor(model.caretDirtyEvents())
        );
        selectionBounds = Val.create(
                area::impl_bounds_getSelectionBoundsOnScreen,
                area.boundsDirtyFor(model.selectionDirtyEvents())
        );
    }

    public void dispose() {
        subscriptions.unsubscribe();
    }

    private void manageBinding(Binding<?> binding) {
        subscriptions = subscriptions.and(binding::dispose);
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
