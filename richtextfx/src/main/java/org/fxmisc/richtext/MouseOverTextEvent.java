package org.fxmisc.richtext;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;

public abstract class MouseOverTextEvent extends Event {

    private static final long serialVersionUID = 1L;

    private static final End DEFAULT_END = new End(null, null);

    public static final EventType<MouseOverTextEvent> ANY =
            new EventType<>(Event.ANY, "MOUSE_OVER_TEXT_ANY");
    public static final EventType<MouseOverTextEvent> MOUSE_OVER_TEXT_BEGIN =
            new EventType<>(ANY, "MOUSE_OVER_TEXT_BEGIN");
    public static final EventType<MouseOverTextEvent> MOUSE_OVER_TEXT_END =
            new EventType<>(ANY, "MOUSE_OVER_TEXT_END");

    public static final MouseOverTextEvent beginAt(Point2D screenPos, int charIdx) {
        return new Begin(null, null, screenPos, charIdx);
    }

    public static final MouseOverTextEvent end() {
        return DEFAULT_END;
    }

    private static class Begin extends MouseOverTextEvent {

        private static final long serialVersionUID = 1L;

        private final Point2D screenPos;
        private final int charIdx;

        public Begin(Object source, EventTarget target, Point2D screenPos, int charIdx) {
            super(source, target, MOUSE_OVER_TEXT_BEGIN);
            this.screenPos = screenPos;
            this.charIdx = charIdx;
        }

        @Override
        public int getCharacterIndex() {
            return charIdx;
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
            return new Begin(newSource, newTarget, screenPos, charIdx);
        }

    }

    private static class End extends MouseOverTextEvent {

        private static final long serialVersionUID = 1L;

        public End(Object source, EventTarget target) {
            super(source, target, MOUSE_OVER_TEXT_END);
        }

        @Override
        public int getCharacterIndex() {
            return -1;
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


    private MouseOverTextEvent(
            Object source,
            EventTarget target,
            EventType<? extends MouseOverTextEvent> type) {
        super(source, target, type);
    }

    /**
     * For {@link #MOUSE_OVER_TEXT_BEGIN} events returns the position at which
     * the mouse is standing still, relative to the node on which the event
     * handler was registered.
     *
     * <p>For {@link #MOUSE_OVER_TEXT_END} events returns {@code null}
     */
    public abstract Point2D getPosition();

    public abstract Point2D getScenePosition();

    public abstract Point2D getScreenPosition();

    /**
     * Returns index of the character that the mouse stopped over if this event
     * is a {@link #MOUSE_OVER_TEXT_BEGIN} event and -1 if this event is a
     * {@link #MOUSE_OVER_TEXT_END}.
     *
     */
    public abstract int getCharacterIndex();
}
