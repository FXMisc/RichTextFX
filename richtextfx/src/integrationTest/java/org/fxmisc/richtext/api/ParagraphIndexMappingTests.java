package org.fxmisc.richtext.api;

import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.TextBuildingUtils;
import org.fxmisc.richtext.model.Paragraph;
import org.junit.Test;
import org.reactfx.collection.LiveList;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ParagraphIndexMappingTests extends InlineCssTextAreaAppTest {

    private static final int TOTAL_NUMBER_OF_LINES = 80;
    private static final int LAST_PAR_INDEX = TOTAL_NUMBER_OF_LINES - 1;
    private static final String CONTENT = TextBuildingUtils.buildLines(TOTAL_NUMBER_OF_LINES);

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        area.replaceText(CONTENT);
    }

    @Test
    public void all_par_to_visible_par_index_is_correct() {
        interact(() -> area.showParagraphAtTop(0));
        assertEquals(Optional.of(0), area.allParToVisibleParIndex(0));

        interact(() -> area.showParagraphAtBottom(LAST_PAR_INDEX));
        assertEquals(Optional.of(area.getVisibleParagraphs().size() - 1), area.allParToVisibleParIndex(LAST_PAR_INDEX));
    }

    @Test
    public void all_par_to_visible_par_index_after_replace() {
        interact(() -> {
            area.clear();
            area.replaceText( "123\nabc" );
        });

        interact(() -> area.replaceText( "123\nxyz" ));

        interact(() -> {
            assertEquals(Optional.of(1), area.allParToVisibleParIndex(1));
            assertEquals(Optional.of(0), area.allParToVisibleParIndex(0));
        });
    }

    @Test
    public void visible_par_to_all_par_index_is_correct() {
        interact(() -> area.showParagraphAtTop(0));
        assertEquals(0, area.visibleParToAllParIndex(0));

        interact(() -> area.showParagraphAtBottom(LAST_PAR_INDEX));
        assertEquals(LAST_PAR_INDEX, area.visibleParToAllParIndex(area.getVisibleParagraphs().size() - 1));
    }

    @Test
    public void folding_keeps_all_paragraph_indices_and_removes_folded_paragraphs_from_view() {
        interact(() -> {
            area.foldParagraphs(1, LAST_PAR_INDEX - 1);
            area.showParagraphAtTop(LAST_PAR_INDEX);
        });

        // all paragraphs except the first and last are folded, so they should not be mapped to any visible index
        for (int i = 2; i < LAST_PAR_INDEX; i++) {
            assertTrue(area.isFolded(i));
            assertEquals(Optional.empty(), area.allParToVisibleParIndex(i));
        }

        // last paragraph is visible, so it should be mapped to the last visible index
        assertEquals(Optional.of(2), area.allParToVisibleParIndex(LAST_PAR_INDEX));
        assertEquals(LAST_PAR_INDEX, area.visibleParToAllParIndex(2));
    }

    @Test
    public void folding_contiguous_middle_paragraphs_preserves_source_indices() {
        // fold paragraphs 1-3, then show paragraph 4 at the top of the viewport
        interact(() -> {
            area.foldParagraphs(1, 3);
            area.showParagraphAtTop(4);
        });

        // sanity check expected folding state
        assertUnfoldedParagraphs(0, 1, 4);
        assertFoldedParagraphs(2, 3);

        // verify that the folded paragraphs are not mapped to any visible index
        assertEquals(Optional.empty(), area.allParToVisibleParIndex(2));
        assertEquals(Optional.empty(), area.allParToVisibleParIndex(3));

        // verify that the visible paragraph is mapped correctly
        assertVisibleMapping(4, 0);
    }

    @Test
    public void folding_paragraph_at_index_zero_keeps_following_visible_indices_in_document_space() {
        interact(() -> {
            area.setParagraphStyle(0, "visibility: collapse;");
            area.showParagraphAtTop(1);
        });

        assertFoldedParagraphs(0);
        assertVisibleMapping(1, 0);
    }

    @Test
    public void folding_disjoint_ranges_does_not_shift_unfolded_source_indices() {
        // fold paragraphs 1-2 and 4-5, then show paragraph 3 at the top of the viewport
        interact(() -> {
            area.foldParagraphs(1, 2);
            area.foldParagraphs(4, 5);
            area.showParagraphAtTop(3);
        });

        // sanity check expected folding state
        assertUnfoldedParagraphs(1,3,4);
        assertFoldedParagraphs(2,5);

        // verify that the folded paragraphs are not mapped to any visible index
        assertEquals(Optional.empty(), area.allParToVisibleParIndex(2));

        // verify the visible paragraph in between the folded regions is mapped correctly
        assertVisibleMapping(3, 0);

        // scroll down to show paragraph 6 at the top of the viewport
        interact(() -> area.showParagraphAtTop(6));

        // verify that the folded paragraphs are still not mapped to any visible index after scrolling
        assertEquals(Optional.empty(), area.allParToVisibleParIndex(5));
        assertEquals(Optional.of(0), area.allParToVisibleParIndex(6));
        assertEquals(6, area.visibleParToAllParIndex(0));
    }

    @Test
    public void showing_a_folded_paragraph_at_the_end_uses_the_last_visible_paragraph() {
        interact(() -> {
            area.setParagraphStyle(LAST_PAR_INDEX, "visibility: collapse;");
            area.showParagraphAtBottom(LAST_PAR_INDEX);
        });

        // verify that the last paragraph is folded and not mapped to any visible index
        assertTrue(area.isFolded(LAST_PAR_INDEX));
        assertEquals(Optional.empty(), area.allParToVisibleParIndex(LAST_PAR_INDEX));

        // verify that the last visible paragraph is mapped to the last visible index
        assertEquals(Optional.of(area.getVisibleParagraphs().size() - 1), area.allParToVisibleParIndex(LAST_PAR_INDEX - 1));
        assertEquals(LAST_PAR_INDEX - 1, area.visibleParToAllParIndex(area.getVisibleParagraphs().size() - 1));
    }

    @Test
    public void showing_paragraphs_is_a_noop_when_all_paragraphs_are_folded() {
        LiveList<Paragraph<String, String, String>> paragraphs = area.getParagraphs();
        interact(() -> {
            for (int i = 0; i < paragraphs.size(); i++) {
                area.setParagraphStyle(i, "visibility: collapse;");
            }

            // all paragraphs are folded, so showing any paragraph should be a no-op
            area.showParagraphInViewport(0);
            area.showParagraphAtTop(0);
            area.showParagraphAtBottom(0);
            area.showParagraphRegion(0, new javafx.geometry.BoundingBox(0, 0, 1, 1));
            area.showParagraphAtCenter(0);
        });

        // verify that all paragraphs are folded and not mapped to any visible index
        for (int i = 1; i < paragraphs.size(); i++) {
            final int j = i;
            assertTrue(area.allParToVisibleParIndex(i).isEmpty());
            assertThrows(IllegalArgumentException.class, () -> area.visibleParToAllParIndex(j));
        }
    }

    @Test
    public void invalid_paragraph_indices_are_rejected() {
        interact(() -> {
             assertEquals(Optional.empty(), area.allParToVisibleParIndex(-1));
             assertEquals(Optional.empty(), area.allParToVisibleParIndex(area.getParagraphs().size()));

            assertThrows(IllegalArgumentException.class, () -> area.visibleParToAllParIndex(-1));
            assertThrows(IllegalArgumentException.class, () -> area.visibleParToAllParIndex(Integer.MAX_VALUE));
        });
    }

    private void assertFoldedParagraphs(int... indices) {
        for (int index : indices) {
            assertFoldedParagraph(index);
        }
    }

    private void assertFoldedParagraph(int index) {
        assertTrue(area.isFolded(index),
                "Paragraph " + index + " should be folded");
        assertEquals(Optional.empty(), area.allParToVisibleParIndex(index),
                "Folded paragraph " + index + " should not be visible");
    }

    private void assertUnfoldedParagraphs(int... indices) {
        for (int index : indices) {
            assertUnfoldedParagraph(index);
        }
    }

    private void assertUnfoldedParagraph(int index) {
        assertFalse(area.isFolded(index),
                "Paragraph " + index + " should not be folded");
    }

    private void assertVisibleMapping(int allIndex, int expectedVisibleIndex) {
        assertEquals(Optional.of(expectedVisibleIndex), area.allParToVisibleParIndex(allIndex));
        assertEquals(allIndex, area.visibleParToAllParIndex(expectedVisibleIndex));
    }
}
