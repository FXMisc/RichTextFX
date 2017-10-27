package org.fxmisc.richtext.event;

import static javafx.scene.input.MouseEvent.*;
import static org.reactfx.EventStreams.*;

import java.time.Duration;

import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.util.Either;

/**
 * Helper class for setting up the code that will fire both kinds of {@link MouseStationaryEvent} when
 * these events occur.
 */
public class MouseStationaryHelper {
    private final Node node;

    private Subscription installed = null;

    /**
     * Creates a helper class that can install/uninstall the code needed to fire events when the mouse becomes
     * stationary over the given node.
     */
    public MouseStationaryHelper(Node node) {
        this.node = node;
    }

    /**
     * Returns an {@link EventStream} that emits a {@link Point2D} whenever the mouse becomes stationary
     * over the helper's node and emits a {@code null} value whenever the mouse moves after being stationary.
     */
    public EventStream<Either<Point2D, Void>> events(Duration delay) {
        EventStream<MouseEvent> mouseEvents = eventsOf(node, MouseEvent.ANY);
        EventStream<Point2D> stationaryPositions = mouseEvents
                .successionEnds(delay)
                .filter(e -> e.getEventType() == MOUSE_MOVED)
                .map(e -> new Point2D(e.getX(), e.getY()));
        EventStream<Void> stoppers = mouseEvents.supply((Void) null);
        return stationaryPositions.or(stoppers).distinct();
    }

    /**
     * Sets up the code to fire a {@code BEGIN} event when the mouse becomes stationary over the node and has not
     * moved for the given amount of time ({@code delay}), and to fire a {@code END} event when the stationary
     * mouse moves again. Note: any previously installed delays will be removed without creating memory leaks.
     */
    public void install(Duration delay) {
        if(installed != null) {
            installed.unsubscribe();
        }
        installed = events(delay).<Event>map(either -> either.unify(
                pos -> MouseStationaryEvent.beginAt(node.localToScreen(pos)),
                stop -> MouseStationaryEvent.end()))
            .subscribe(evt -> Event.fireEvent(node, evt));
    }

    /**
     * Removes uninstalls the code that would fire {@code BEGIN} and {@code END} events when the mouse became
     * stationary over this helper's node.
     */
    public void uninstall() {
        if(installed != null) {
            installed.unsubscribe();
            installed = null;
        }
    }
}
