package org.fxmisc.richtext.keyboard;

import javafx.geometry.Bounds;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.TextBuildingUtils;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.junit.Test;

import static javafx.scene.input.KeyCode.PAGE_DOWN;
import static javafx.scene.input.KeyCode.PAGE_UP;
import static javafx.scene.input.KeyCode.SHIFT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.testfx.util.WaitForAsyncUtils;

public class PageUpDownTests extends InlineCssTextAreaAppTest {

    private static final String EIGHT_LINES = TextBuildingUtils.buildLines(8);

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        clickOn( area );

        // Note that these test are Font size sensitive !!!  
        // allow 6 lines to be displayed
        stage.setHeight(96);
        area.replaceText(EIGHT_LINES);
    }

    @Test
    public void page_up_leaves_caret_at_BOTTOM_of_viewport_when_FIRST_line_NOT_visible() {
        interact(() -> {
            insert( 5, 1, " page_up_leaves_caret_at_BOTTOM_of_viewport" );
            insert( 7, 1, " page_up_leaves_caret_at_BOTTOM_of_viewport" );
            area.requestFollowCaret();
        });

        Bounds beforeBounds = area.getCaretBounds().get();

        //WaitForAsyncUtils.waitForFxEvents(250);
        type(PAGE_UP);
        //WaitForAsyncUtils.waitForFxEvents(250);

    	Bounds afterBounds = area.getCaretBounds().get();
    	assertEquals(beforeBounds.getMinY(), afterBounds.getMinY(), 6.1);
    	assertEquals(5, area.getCurrentParagraph());
    }

    @Test
    public void page_up_leaves_caret_at_TOP_of_viewport_when_FIRST_line_IS_visible() {
        interact(() -> {
            insert( 0, 1, " page_up_leaves_caret_at_TOP_of_viewport" );
            area.moveTo(4, 0);
            area.requestFollowCaret();
        });

        type(PAGE_UP);

        assertEquals(0, area.getCurrentParagraph());
    }

    @Test
    public void page_down_leaves_caret_at_TOP_of_viewport_when_LAST_line_NOT_visible() throws Exception {
        interact(() -> {
        	insert( 0, 1, " page_down_leaves_caret_at_TOP_of_viewport" );
            insert( 2, 1, " page_down_leaves_caret_at_TOP_of_viewport" );
            area.moveTo(0);
            area.requestFollowCaret();
        });

        Bounds beforeBounds = area.getCaretBounds().get();

        //WaitForAsyncUtils.waitForFxEvents(250);
        type(PAGE_DOWN);
        //WaitForAsyncUtils.waitForFxEvents(250);

    	Bounds afterBounds = area.getCaretBounds().get();
    	assertEquals(beforeBounds.getMinY(), afterBounds.getMinY(), 6.1);
    	assertEquals(2, area.getCurrentParagraph());
    }

    @Test
    public void page_down_leaves_caret_at_BOTTOM_of_viewport_when_LAST_line_IS_visible() throws Exception {
        interact(() -> {
            insert( 7, 1, " page_down_leaves_caret_at_BOTTOM_of_viewport" );
            area.showParagraphAtTop(3);
            area.moveTo(3, 0);
        });

        type(PAGE_DOWN);

        assertEquals(7, area.getCurrentParagraph());
        assertEquals(area.getLength(), area.getCaretPosition());
    }

    @Test
    public void shift_page_up_leaves_caret_at_bottom_of_viewport_and_makes_selection() {
        interact(() -> {
            insert( 5, 1, " SHIFT_page_up_SELECTS_leaving_caret_at_BOTTOM_of_viewport" );
            insert( 7, 1, " SHIFT_page_up_SELECTS_leaving_caret_at_BOTTOM_of_viewport" );
            area.moveTo(7, 0);
            area.requestFollowCaret();
        });

        Bounds beforeBounds = area.getCaretBounds().get();

        //WaitForAsyncUtils.waitForFxEvents(250);
        press(SHIFT).type(PAGE_UP).release(SHIFT);
        //WaitForAsyncUtils.waitForFxEvents(250);

    	Bounds afterBounds = area.getCaretBounds().get();
    	assertEquals(beforeBounds.getMinY(), afterBounds.getMinY(), 6.1);
    	assertEquals(area.getText(5, 0, 7, 0), area.getSelectedText());
    	assertEquals(10, area.getCaretPosition());
    }

    @Test
    public void shift_page_down_leaves_caret_at_top_of_viewport_and_makes_selection() {
        interact(() -> {
            insert( 2, 1, " SHIFT_page_down_SELECTS_leaving_caret_at_TOP_of_viewport" );
            insert( 0, 1, " SHIFT_page_down_SELECTS_leaving_caret_at_TOP_of_viewport" );
            area.requestFollowCaret();
        });

        assertTrue(area.getSelectedText().isEmpty());
        Bounds beforeBounds = area.getCaretBounds().get();

        //WaitForAsyncUtils.waitForFxEvents(250);
        press(SHIFT).type(PAGE_DOWN).release(SHIFT);
        //WaitForAsyncUtils.waitForFxEvents(250);
        
    	Bounds afterBounds = area.getCaretBounds().get();
    	//assertEquals(beforeBounds.getMinY(), afterBounds.getMinY(), 6.1);
    	assertEquals(area.getText(1, -1, 3, -1), area.getSelectedText());
    	assertEquals(119, area.getCaretPosition());
    }


    private void insert( int p, int col, String text ) {
    	area.insert( p, col, ReadOnlyStyledDocument.fromString( text, "", "", area.getSegOps() ) );
    }
    
}

