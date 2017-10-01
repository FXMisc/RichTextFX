package org.fxmisc.richtext.api;

import javafx.geometry.Bounds;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CharacterBoundsTest extends InlineCssTextAreaAppTest {

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        area.replaceText("a line of sample text");
    }

    @Test
    public void selection_bounds_are_unchanged_when_call_getCharacterBounds() {
        area.selectAll();
        Bounds bounds = area.getSelectionBounds().get();

        // getCharacterBoundsOnScreen() uses the selection shape to calculate the bounds
        // so insure it doesn't affect the selection shape if something is selected
        // before it gets called
        area.getCharacterBoundsOnScreen(0, area.getLength() - 1);

        assertEquals(bounds, area.getSelectionBounds().get());
    }

}
