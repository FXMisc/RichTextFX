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
    public void getCharacterBounds_works_even_when_a_selection_is_made() {
        area.selectAll();
        Bounds bounds = area.getSelectionBounds().get();

        interact(() -> area.getCharacterBoundsOnScreen(0, area.getLength() - 1));

        assertEquals(bounds, area.getSelectionBounds().get());
    }

}
