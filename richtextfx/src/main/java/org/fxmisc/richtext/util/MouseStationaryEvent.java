package org.fxmisc.richtext.util;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.InputEvent;

public abstract class MouseStationaryEvent extends InputEvent {

    private static final long serialVersionUID = 1L;

    private static final End DEFAULT_END = new End(null, null);

    public static final EventType<MouseStationaryEvent> ANY =
            new EventType<>(InputEvent.ANY, "MOUSE_STATIONARY_ANY");
    public static final EventType<MouseStationaryEvent> MOUSE_STATIONARY_BEGIN =
            new EventType<>(ANY, "MOUSE_STATIONARY_BEGIN");
    public static final EventType<MouseStationaryEvent> MOUSE_STATIONARY_END =
            new EventType<>(ANY, "MOUSE_STATIONARY_END");

    static final MouseStationaryEvent beginAt(Point2D screenPos) {
        return new Begin(null, null, screenPos);
    }

    static final MouseStationaryEvent end() {
        return DEFAULT_END;
    }

    private static class Begin extends MouseStationaryEvent {

        private static final long serialVersionUID = 1L;

        private final Point2D screenPos;

        public Begin(Object source, EventTarget target, Point2D screenPos) {
            super(source, target, MOUSE_STATIONARY_BEGIN);
            this.screenPos = screenPos;
        }

        @Override
        public Point2D getPosition() {
            if(source instanceof Node) {
                return ((Node) source).screenToLocal(screenPos);
            } else if(source instanceof Scene) {
                return getScenePosition();
            } else {
                return null;
            }
        }

        @Override
        public Point2D getScenePosition() {
            Scene scene;

            if(source instanceof Node) {
                scene = ((Node) source).getScene();
            } else if(source instanceof Scene) {
                scene = (Scene) source;
            } else {
                return null;
            }

            return screenPos.subtract(
                    scene.getX() + scene.getWindow().getX(),
                    scene.getY() + scene.getWindow().getY());
        }

        @Override
        public Point2D getScreenPosition() {
            return screenPos;
        }

        @Override
        public Begin copyFor(Object newSource, EventTarget newTarget) {
            return new Begin(newSource, newTarget, screenPos);
        }

    }

    private static class End extends MouseStationaryEvent {

        private static final long serialVersionUID = 1L;

        public End(Object source, EventTarget target) {
            super(source, target, MOUSE_STATIONARY_END);
        }

        @Override
        public Point2D getPosition() { return null; }

        @Override
        public Point2D getScenePosition() { return null; }

        @Override
        public Point2D getScreenPosition() { return null; }

        @Override
        public End copyFor(Object newSource, EventTarget newTarget) {
            return new End(newSource, newTarget);
        }

    }


    private MouseStationaryEvent(
            Object source,
            EventTarget target,
            EventType<? extends MouseStationaryEvent> type) {
        super(source, target, type);
    }

    public abstract Point2D getPosition();
    public abstract Point2D getScenePosition();
    public abstract Point2D getScreenPosition();
}
