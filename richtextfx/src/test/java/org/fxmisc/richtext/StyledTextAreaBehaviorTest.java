package org.fxmisc.richtext;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static javafx.scene.input.KeyEvent.KEY_PRESSED;

@RunWith(Parameterized.class)
public class StyledTextAreaBehaviorTest {

    private static StyledTextArea area;
    private final KeyEvent event;
    private final boolean shouldBeConsumed;

    public StyledTextAreaBehaviorTest(KeyEvent event, boolean shouldBeConsumed) {
        this.event = event;
        this.shouldBeConsumed = shouldBeConsumed;
    }

    @Parameterized.Parameters
    public static Object[][] data() {
        area = new CodeArea();
        return new Object[][]{
            //with Unmodified; Level 1
            {new KeyEvent(area, area, KEY_PRESSED, "f", "f", KeyCode.F, false, false, false, false), true},
            //with Shift; Level 2
            {new KeyEvent(area, area, KEY_PRESSED, "f", "F", KeyCode.F, true, false, false, false), true},
            //with AltGr; Level 3
            {new KeyEvent(area, area, KEY_PRESSED, "f", "ð", KeyCode.F, false, false, false, false), true},
            //with Shift + AltGr; Level 4
            {new KeyEvent(area, area, KEY_PRESSED, "f", "ª", KeyCode.F, false, false, false, false), true},
            //with Ctrl
            {new KeyEvent(area, area, KEY_PRESSED, "f", "", KeyCode.F, false, true, false, false), false},
            //with Alt
            {new KeyEvent(area, area, KEY_PRESSED, "f", "", KeyCode.F, false, false, true, false), false},
            //with Meta
            {new KeyEvent(area, area, KEY_PRESSED, "f", "", KeyCode.F, false, false, false, true), false},
            };
    }

    @Test
    public void testKeyboardLevels() {
        area.fireEvent(event);

        Assert.assertEquals(shouldBeConsumed, event.isConsumed());
    }
}

