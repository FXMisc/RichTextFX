package org.fxmisc.richtext.api;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.stage.Stage;
import org.fxmisc.richtext.CaretNode;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.Selection;
import org.fxmisc.richtext.SelectionImpl;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(NestedRunner.class)
public class MultipleCaretSelectionTests extends InlineCssTextAreaAppTest {

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);

        area.replaceText("first line\nsecond line\nthird line");
    }

    @Test
    public void adding_caret_works() {
        CaretNode caret = new CaretNode("test caret", area, 0);

        interact(() -> assertTrue(area.addCaret(caret)));
        assertTrue(caret.getCaretBounds().isPresent());
        assertEquals(0, caret.getPosition());
    }

    @Test
    public void removing_caret_works() {
        CaretNode caret = new CaretNode("test caret", area, 0);
        interact(() -> {
            assertTrue(area.addCaret(caret));

            assertTrue(area.removeCaret(caret));
        });
    }

    @Test
    public void removing_caret_in_folded_paragraph_works() {
        interact(() -> {
            // after folding the first two paragraphs, we should still be able to add/remove carets in them
            // note: the first line is still visible as the 'preview' of the folded paragraph
            area.foldParagraphs(0, 1);

            CaretNode caret = new CaretNode("folded caret", area, area.getAbsolutePosition(1, 0));
            assertTrue(area.addCaret(caret));
            assertTrue(area.removeCaret(caret));

            // for the visible paragraphs it should still work as well
            caret = new CaretNode("folded caret", area, area.getAbsolutePosition(0, 0));
            assertTrue(area.addCaret(caret));
            assertTrue(area.removeCaret(caret));
            caret = new CaretNode("folded caret", area, area.getAbsolutePosition(2, 0));
            assertTrue(area.addCaret(caret));
            assertTrue(area.removeCaret(caret));
        });
    }

    @Test
    public void adding_selection_works() {
        Selection<String, String, String> selection = new SelectionImpl<>("test selection", area);
        interact(() -> assertTrue(area.addSelection(selection)));
        // no selection made yet
        assertFalse(selection.getSelectionBounds().isPresent());

        // now bounds should be present
        interact(selection::selectAll);
        assertTrue(selection.getSelectionBounds().isPresent());
    }

    @Test
    public void removing_selection_works() {
        Selection<String, String, String> selection = new SelectionImpl<>("test selection", area);
        interact(() -> {
            assertTrue(area.addSelection(selection));
            assertTrue(area.removeSelection(selection));
        });

    }

    @Test
    public void removing_selection_spanning_folded_paragraph_works() {
        Selection<String, String, String> selection = new SelectionImpl<>("folded selection", area);
        interact(() -> {
            // folding the first two paragraphs, we should still be able to:
            //  - add a selection that spans the folded paragraphs
            //  - select the entire area
            //  - remove that selection
            area.foldParagraphs(0, 1);
            assertTrue(area.addSelection(selection));

            selection.selectRange(0, area.getLength());
            assertTrue(selection.getSelectionBounds().isPresent());

            assertTrue(area.removeSelection(selection));
        });
    }

    @Test
    public void attempting_to_remove_original_caret_fails() {
        interact(() ->
            assertFalse(area.removeCaret(area.getCaretSelectionBind().getUnderlyingCaret()))
        );
    }

    @Test
    public void attempting_to_remove_original_selection_fails() {
        interact(() ->
                assertFalse(area.removeSelection(area.getCaretSelectionBind().getUnderlyingSelection()))
        );
    }

    @Test
    public void attempting_to_add_caret_associated_with_different_area_fails() {
        InlineCssTextArea area2 = new InlineCssTextArea();
        CaretNode caret = new CaretNode("test caret", area2);
        interact(() -> {
            try {
                area.addCaret(caret);
                fail();
            } catch (IllegalArgumentException e) {
                // cannot add a caret associated with a different area
            }
        });
    }

    @Test
    public void attempting_to_add_selection_associated_with_different_area_fails() {
        InlineCssTextArea area2 = new InlineCssTextArea();
        Selection<String, String, String> selection = new SelectionImpl<>("test selection", area2);
        interact(() -> {
            try {
                area.addSelection(selection);
                fail();
            } catch (IllegalArgumentException e) {
                // cannot add a selection associated with a different area
            }
        });
    }

    @Test
    public void modifying_caret_before_adding_to_area_does_not_throw_exception() {
        CaretNode caret = new CaretNode("test caret", area);
        interact(() -> {
            caret.moveToAreaEnd();
            area.addCaret(caret);

            caret.moveToParEnd();
            area.removeCaret(caret);

            caret.moveToParStart();
            area.addCaret(caret);
            area.removeCaret(caret);
        });
    }

    @Test
    public void modifying_selection_before_adding_to_area_does_not_throw_exception() {
        Selection<String, String, String> selection = new SelectionImpl<>("test selection", area);
        interact(() -> {
            selection.selectAll();
            area.addSelection(selection);

            selection.selectRange(0, 4);
            area.removeSelection(selection);

            selection.deselect();
            area.addSelection(selection);
            area.removeSelection(selection);
        });
    }

}
