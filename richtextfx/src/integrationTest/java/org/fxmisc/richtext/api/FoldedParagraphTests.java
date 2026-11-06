package org.fxmisc.richtext.api;

import javafx.event.Event;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.fxmisc.richtext.CaretNode;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Test;

import java.util.Optional;
import java.util.stream.IntStream;

import static javafx.scene.input.MouseButton.PRIMARY;
import static org.testfx.util.WaitForAsyncUtils.asyncFx;
import static org.junit.jupiter.api.Assertions.*;

public class FoldedParagraphTests extends InlineCssTextAreaAppTest {

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        stage.setHeight(100);
    }

    @Test
    public void line_index_works_for_visible_and_folded_paragraphs() {
        interact(() -> {
            // fold 'second' paragraph, so that 'first' and 'third' are visible
            area.replaceText("first\nsecond\nthird");
            area.foldParagraphs(0, 1);

            // verify lineIndex[par, 0] is zero for all paragraphs
            // - visible content is short enough to fit on one line
            // - folded content is not realized and thus there is no line wrapping to be done
            assertEquals(0, area.lineIndex(0, 0));
            assertEquals(0, area.lineIndex(1, 0));
            assertEquals(0, area.lineIndex(2, 0));

            // verify current line start/end range is the length of 'first' (5 chars)
            area.moveTo(0, 2);
            assertEquals(0, area.getCurrentParagraph());
            assertEquals(0, area.getCurrentLineStartInParargraph());
            assertEquals(5, area.getCurrentLineEndInParargraph());

            // moving to the folded paragraph will skip forward to the next visible paragraph, which is 'third' (5 chars)
            area.moveTo(1, 2);
            assertEquals(2, area.getCurrentParagraph());
            assertEquals(0, area.getCurrentLineStartInParargraph());
            assertEquals(5, area.getCurrentLineEndInParargraph());
        });
    }

    @Test
    public void paragraph_bounds_are_empty_for_folded_paragraph_and_present_for_visible_paragraph() {
        // fold 'two' paragraph, so that 'zero', 'one', and 'three' are visible
        interact(() -> {
            area.replaceText("zero\none\ntwo\nthree");
            area.foldParagraphs(1, 2);
            area.showParagraphAtTop(3);
        });

        // the folded paragraph should not have bounds on screen, but the visible paragraphs should.
        assertTrue(area.getParagraphBoundsOnScreen(0).isPresent());
        assertTrue(area.getParagraphBoundsOnScreen(1).isPresent());
        assertFalse(area.getParagraphBoundsOnScreen(2).isPresent());
        assertTrue(area.getParagraphBoundsOnScreen(3).isPresent());

        // visible paragraph filtering will only go up to three paragraphs since one of them is folded.
        assertNotNull(area.getVisibleParagraphBoundsOnScreen(0));
        assertNotNull(area.getVisibleParagraphBoundsOnScreen(1));
        assertNotNull(area.getVisibleParagraphBoundsOnScreen(2));
        assertThrows(IndexOutOfBoundsException.class, () -> area.getVisibleParagraphBoundsOnScreen(3));

        // since 'two' is folded then 'three' is the third visible paragraph (but fourth in the unfiltered list).
        assertEquals(area.getVisibleParagraphBoundsOnScreen(2), area.getParagraphBoundsOnScreen(3).get());
    }

    @Test
    public void character_bounds_are_empty_for_folded_paragraph() {
        // fold 'bbb' paragraph, so that 'aaa' and 'ccc' are visible
        interact(() -> {
            area.replaceText("aaa\nbbb\nccc");
            area.foldParagraphs(0, 1);

            // the folded paragraph (lineIndex 1) should not have character bounds on screen, but the visible paragraphs should.
            int foldedLine = 1;
            for (int lineIndex = 0; lineIndex <= 2; lineIndex++) {
                for (int charIndex = 0; charIndex <= 2; charIndex++) {
                    int pos = area.getAbsolutePosition(lineIndex, charIndex);
                    Optional<Bounds> charBounds = area.getCharacterBoundsOnScreen(pos, pos + 1);
                    if (lineIndex == foldedLine) {
                        assertFalse(charBounds.isPresent());
                    } else {
                        assertTrue(charBounds.isPresent());
                    }
                }
            }
        });
    }

    @Test
    public void caret_bounds_are_empty_for_caret_in_folded_paragraph() {
        // fold 'one' paragraph, so that 'zero' and 'two' are visible
        interact(() -> {
            area.replaceText("zero\none\ntwo");
            area.foldParagraphs(0, 1);
        });

        // create a caret in the folded paragraph, which should not have bounds on screen.
        CaretNode caret = new CaretNode("folded caret", area, area.getAbsolutePosition(1, 0));
        interact(() -> {
            assertFalse(area.getCaretBoundsOnScreen(caret).isPresent());
            caret.dispose();
        });
    }

    @Test
    public void paragraph_graphic_access_rejects_folded_paragraph() {
        // fold 'one' paragraph, so that 'zero' and 'two' are visible
        interact(() -> {
            area.setParagraphGraphicFactory(i -> new Label("Line: " + i));
            area.replaceText("zero\none\ntwo");
            area.foldParagraphs(0, 1);
        });

        // getting/creating graphics for the folded paragraph is no-op (returns null and does not throw an exception).
        assertNull(area.getParagraphGraphic(1));
        assertDoesNotThrow(() -> area.recreateParagraphGraphic(1));

        // getting/creating graphics for the visible paragraphs works as expected.
        interact(() -> {
            assertNotNull(area.getParagraphGraphic(0));
            assertDoesNotThrow(() -> area.recreateParagraphGraphic(0));
            assertNotNull(area.getParagraphGraphic(2));
            assertDoesNotThrow(() -> area.recreateParagraphGraphic(2));
        });
    }

    @Test
    public void hit_on_visible_paragraph_after_folded_paragraph_uses_source_index() throws Exception {
        // fold 'two' paragraph, make 'two' the top visible paragraph.
        interact(() -> {
            area.replaceText("zero\none\ntwo\nthree");
            area.foldParagraphs(1, 2);
        });

        // click on the first character of the 'three' paragraph,
        int start = area.getAbsolutePosition(3, 0);
        Bounds bounds = asyncFx(() -> area.getCharacterBoundsOnScreen(start, start + 1).get()).get();
        moveTo(bounds).clickOn(PRIMARY);

        // verify that the caret is now in the 'three' paragraph, which is the fourth paragraph in the source text.
        assertEquals(3, area.getCurrentParagraph());
        assertEquals(0, area.getCaretColumn());
    }

    @Test
    public void show_paragraph_in_viewport_resolves_a_folded_target_to_a_visible_paragraph() {
        interact(() -> {
            // create a large number of paragraphs, fold the first 50 paragraphs
            area.replaceText(IntStream.range(0, 100).mapToObj(i -> "paragraph " + i).reduce((a, b) -> a + "\n" + b).get());
            area.foldParagraphs(1, 50);

            // scroll down
            area.showParagraphAtTop(99);

            // scroll back up to a folded paragraph, which should resolve to paragraph index 1 since that is
            // the first visible paragraph in the folded range.
            area.showParagraphInViewport(2);
        });

        // after scrolling to the folded paragraph index 2 the first visible paragraph index 1 should be at the top of the viewport.
        assertVisibleParagraph(1);
    }

    @Test
    public void show_paragraph_region_resolves_a_folded_target_to_a_visible_paragraph() {
        interact(() -> {
            // same idea as the test before, but with showParagraphRegion
            area.replaceText(IntStream.range(0, 100).mapToObj(i -> "paragraph " + i).reduce((a, b) -> a + "\n" + b).get());
            area.foldParagraphs(1, 50);
            area.showParagraphAtTop(99);
            area.showParagraphRegion(2, new BoundingBox(0, 0, 1, 1));
        });

        // after scrolling to the folded paragraph index 2 the first visible paragraph index 1 should be at the top of the viewport.
        assertVisibleParagraph(1);
    }

    @Test
    public void show_paragraph_at_center_resolves_a_folded_target_to_a_visible_paragraph() {
        interact(() -> {
            // same idea as the test before, but with showParagraphAtCenter
            area.replaceText(IntStream.range(0, 100).mapToObj(i -> "paragraph " + i).reduce((a, b) -> a + "\n" + b).get());
            area.foldParagraphs(1, 50);
            area.showParagraphAtTop(99);
            area.showParagraphAtCenter(2);
        });

        // after scrolling to the folded paragraph index 2 the first visible paragraph index 1 should be at the top of the viewport.
        // this is mainly because you can't really center an index that close to the start of the document,
        // so of course the first visible paragraph is at the top of the viewport.
        assertVisibleParagraph(1);
    }

    @Test
    public void navigation_skips_folded_paragraphs_in_both_directions() {
        interact(() -> {
            // fold 'two' and 'three' paragraphs, so that 'zero', 'one', and 'four' are visible
            area.replaceText("zero\none\ntwo\nthree\nfour");
            area.foldParagraphs(1, 3);

            // verify that moving forward to the folded paragraph skips to the next visible paragraph
            area.moveTo(2, 0);
            assertEquals(4, area.getCurrentParagraph());

            // verify that moving backward to the folded paragraph skips to the previous visible paragraph
            area.moveTo(3, 0);
            assertEquals(1, area.getCurrentParagraph());
        });
    }

    @Test
    public void navigation_skips_folded_paragraphs_in_both_directions_key_events() {
        // variant of the prior test, but with key events instead of moveTo
        interact(() -> {
            // fold 'two' and 'three' paragraphs, so that 'zero', 'one', and 'four' are visible
            area.replaceText("zero\none\ntwo\nthree\nfour");
            area.foldParagraphs(1, 3);

            // verify that moving forward to the folded paragraph skips to the next visible paragraph
            assertEquals(1, area.getCurrentParagraph());
            assertEquals(5, area.getCaretPosition());
            area.fireEvent(createKeyEvent(KeyCode.RIGHT));
            assertEquals(6, area.getCaretPosition());
            area.fireEvent(createKeyEvent(KeyCode.RIGHT));
            assertEquals(7, area.getCaretPosition());
            area.fireEvent(createKeyEvent(KeyCode.RIGHT));
            assertEquals(8, area.getCaretPosition());
            assertEquals(1, area.getCurrentParagraph());
            area.fireEvent(createKeyEvent(KeyCode.RIGHT));
            assertEquals(19, area.getCaretPosition());
            assertEquals(4, area.getCurrentParagraph());

            // verify that moving backward to the folded paragraph skips to the previous visible paragraph
            area.fireEvent(createKeyEvent(KeyCode.LEFT));
            assertEquals(8, area.getCaretPosition());
            assertEquals(1, area.getCurrentParagraph());
        });
    }

    @Test
    public void auto_height_excludes_folded_paragraphs() {
        // if we set auto-height we effectively verify GenericStyledArea#computePrefHeight counts only visible paragraphs.
        interact(() -> {
            area.setAutoHeight(true);
            area.replaceText("zero\none\ntwo\nthree");
        });

        double unfoldedHeight = area.prefHeight(-1);
        interact(() -> area.foldParagraphs(0, 2));
        double foldedHeight = area.prefHeight(-1);

        // the folded height should be less than the unfolded height since we folded some paragraphs.
        assertTrue(foldedHeight < unfoldedHeight);
    }

    private void assertVisibleParagraph(int allParagraphIndex) {
        assertTrue(area.allParToVisibleParIndex(allParagraphIndex).isPresent(),
                "Paragraph " + allParagraphIndex + " should be visible");
    }

    private static Event createKeyEvent(KeyCode keyCode) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, keyCode.getChar(), "unset", keyCode, false, false, false ,false);
    }
}
