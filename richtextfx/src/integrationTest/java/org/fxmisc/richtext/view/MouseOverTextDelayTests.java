package org.fxmisc.richtext.view;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.MouseOverTextEvent;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MouseOverTextDelayTests extends InlineCssTextAreaAppTest {

    private SimpleBooleanProperty beginFired = new SimpleBooleanProperty();
    private SimpleBooleanProperty endFired = new SimpleBooleanProperty();

    private void resetBegin() { beginFired.set(false); }
    private void resetEnd() { endFired.set(false); }

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);

        area.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN, e -> beginFired.set(true));
        area.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_END,   e -> endFired.set(true));

        area.replaceText("a long line of some example text");

        resetBegin();
        resetEnd();

        // insure mouse is off of area
        moveMouseOutsideOfArea();
    }

    private void moveMouseOutsideOfArea() {
        moveTo(point(scene).atPosition(Pos.TOP_LEFT).atOffset(-20, -20));
    }

    private void setDelay(Duration delay) {
        interact(() -> area.setMouseOverTextDelay(delay));
    }

    private void setDelay(long milliseconds) {
        setDelay(Duration.ofMillis(milliseconds));
    }

    @Test
    public void nullDelayNeverFires() {
        setDelay(null);

        moveTo(firstLineOfArea()).sleep(300);
        assertFalse(beginFired.get());
        assertFalse(endFired.get());
    }

    @Test
    public void eventsFireAfterDelayAndPostMove() {
        setDelay(100);

        moveTo(firstLineOfArea()).sleep(300);
        assertTrue(beginFired.get());
        assertFalse(endFired.get());

        resetBegin();

        moveBy(20, 0);
        assertFalse(beginFired.get());
        assertTrue(endFired.get());
    }

    @Test
    public void settingDelayWhileMouseAlreadyOverTextDoesNotFireEvent() {
        setDelay(null);

        moveTo(firstLineOfArea());
        assertFalse(beginFired.get());
        assertFalse(endFired.get());

        setDelay(100);
        assertFalse(beginFired.get());
        assertFalse(endFired.get());
    }

    @Test
    public void settingDelayToNullValueBeforeEndFiresPreventsEndFromFiring() {
        setDelay(100);

        moveTo(firstLineOfArea()).sleep(200);
        assertTrue(beginFired.get());
        assertFalse(endFired.get());

        resetBegin();
        setDelay(null);

        moveBy(20, 0);
        assertFalse(beginFired.get());
        assertFalse(endFired.get());
    }

    @Test
    public void settingDelayToNonNullValueBeforeEndFiresStillFiresEndEvent() {
        setDelay(100);

        moveTo(firstLineOfArea()).sleep(200);
        assertTrue(beginFired.get());
        assertFalse(endFired.get());

        resetBegin();
        setDelay(200);

        moveBy(20, 0);
        assertFalse(beginFired.get());
        assertTrue(endFired.get());
    }

}