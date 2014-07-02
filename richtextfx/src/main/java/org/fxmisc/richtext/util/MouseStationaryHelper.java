package org.fxmisc.richtext.util;

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

public class MouseStationaryHelper {
    private final Node node;

    private Subscription installed = null;

    public MouseStationaryHelper(Node node) {
        this.node = node;
    }

    public EventStream<Either<Point2D, Void>> events(Duration delay) {
        EventStream<MouseEvent> mouseEvents = eventsOf(node, MouseEvent.ANY);
        EventStream<Point2D> stationaryPositions = mouseEvents
                .successionEnds(delay)
                .filter(e -> e.getEventType() == MOUSE_MOVED)
                .map(e -> new Point2D(e.getX(), e.getY()));
        EventStream<Void> stoppers = mouseEvents.supply((Void) null);
        return stationaryPositions.or(stoppers).distinct();
    }

    public void install(Duration delay) {
        if(installed != null) {
            installed.unsubscribe();
        }
        installed = events(delay).<Event>map(either -> either.unify(
                pos -> MouseStationaryEvent.beginAt(node.localToScreen(pos)),
                stop -> MouseStationaryEvent.end()))
            .subscribe(evt -> Event.fireEvent(node, evt));
    }

    public void uninstall() {
        if(installed != null) {
            installed.unsubscribe();
            installed = null;
        }
    }
}
