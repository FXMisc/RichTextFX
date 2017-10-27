package org.fxmisc.richtext.api;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.event.MouseOverTextEvent;
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
    public void null_delay_never_fires() {
        setDelay(null);

        moveTo(firstLineOfArea()).sleep(300);
        assertFalse(beginFired.get());
        assertFalse(endFired.get());
    }

    @Ignore("END events are fired multiple times when BEGIN event hasn't yet fired")
    @Test
    public void events_fire_after_delay_and_post_move() {
        setDelay(100);

        moveTo(firstLineOfArea()).sleep(300);
        assertTrue(beginFired.get());
        assertFalse(endFired.get());    // fails here

        resetBegin();

        moveBy(20, 0);
        assertFalse(beginFired.get());
        assertTrue(endFired.get());
    }

    @Ignore("setting delay while mouse is over text fires END event when BEGIN event hasn't yet fired")
    @Test
    public void setting_delay_while_mouse_is_over_text_does_not_fire_event() {
        setDelay(null);

        moveTo(firstLineOfArea()).sleep(300);
        assertFalse(beginFired.get());
        assertFalse(endFired.get());

        setDelay(100);
        assertFalse(beginFired.get());
        assertFalse(endFired.get());

        moveBy(20, 0);
        assertTrue(beginFired.get());   // fails here
        assertFalse(endFired.get());
    }

    @Ignore("this test is only important when above two tests get fixed")
    @Test
    public void setting_delay_before_end_fires_prevents_end_from_firing() {
        setDelay(100);

        moveTo(firstLineOfArea()).sleep(200);
        assertTrue(beginFired.get());
        assertFalse(endFired.get());

        resetBegin();
        setDelay(null);

        moveMouseOutsideOfArea();
        assertFalse(beginFired.get());
        assertFalse(endFired.get());

        setDelay(100);
        assertFalse(beginFired.get());
        assertFalse(endFired.get());

        moveTo(firstLineOfArea()).sleep(300);
        assertTrue(beginFired.get());
        assertFalse(endFired.get());

        resetBegin();

        moveBy(20, 0);
        assertFalse(beginFired.get());
        assertTrue(endFired.get());
    }

}