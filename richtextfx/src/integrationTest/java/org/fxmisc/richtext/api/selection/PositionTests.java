package org.fxmisc.richtext.api.selection;

import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.Selection;
import org.fxmisc.richtext.SelectionImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PositionTests extends InlineCssTextAreaAppTest {

    private String leftText = "left";
    private String rightText = "right";
    private String fullText = leftText + rightText;

    private Selection<String, String, String> selection;

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        area.replaceText(fullText);
        selection = new SelectionImpl<>("extra selection", area);
        area.addSelection(selection);
    }

    private void selectLeft() {
        selection.selectRange(0, leftText.length());
    }

    private void selectRight() {
        selection.selectRange(leftText.length(), area.getLength());
    }
    
    @Test
    public void initial_range_specified_via_constructor_is_honored() {
        interact(() -> {
            area.appendText( "\n"+ fullText );
            area.appendText( "\n"+ fullText );
        });

    	int paragraphOne = 1;
    	int start = area.getAbsolutePosition( paragraphOne, leftText.length() );
    	int end = start + rightText.length();

    	Selection s0 = new SelectionImpl<>( "constructor", area, start, end );
    	assertEquals( leftText.length(), s0.getStartColumnPosition() );
    	assertEquals( paragraphOne, s0.getStartParagraphIndex() );

    	interact( () -> area.replaceText(fullText) );
    }

    @Test
    public void start_position_is_correct_when_change_occurs_before_position() {
        interact(() -> {
            selectLeft();
            int pos = selection.getStartPosition();

            String append = "some";

            // add test
            area.insertText(0, append);
            assertEquals(pos + append.length(), selection.getStartPosition());

            // delete test
            area.deleteText(0, append.length());
            assertEquals(pos, selection.getStartPosition());
        });
    }

    @Test
    public void start_position_is_correct_when_change_occurs_before_position_and_deletes_carets_position() {
        interact(() -> {
            selection.selectRange(2, leftText.length() + 1);

            area.deleteText(0, leftText.length());
            assertEquals(0, selection.getStartPosition());
        });
    }

    @Test
    public void start_position_is_correct_when_change_occurs_at_position() {
        interact(() -> {
            selectRight();
            int pos = selection.getStartPosition();

            String append = "some";
            // add test
            area.insertText(leftText.length(), append);
            assertEquals(pos + append.length(), selection.getStartPosition());

            // reset
            selection.updateStartTo(pos);

            // delete test
            area.deleteText(pos, append.length());
            assertEquals(pos, selection.getStartPosition());
        });
    }

    @Test
    public void start_position_is_correct_when_change_occurs_after_position() {
        interact(() -> {
            selectLeft();

            // add test
            String append = "some";
            area.appendText(append);
            assertEquals(0, selection.getStartPosition());

            // delete test
            int length = area.getLength();
            area.deleteText(length - append.length(), length);
            assertEquals(0, selection.getStartPosition());
        });
    }

    @Test
    public void end_position_is_correct_when_change_occurs_before_position() {
        interact(() -> {
            selectRight();
            int pos = selection.getEndPosition();

            String append = "some";

            // add test
            area.insertText(0, append);
            assertEquals(pos + append.length(), selection.getEndPosition());

            // delete test
            area.deleteText(0, append.length());
            assertEquals(pos, selection.getEndPosition());
        });
    }

    @Test
    public void end_position_is_correct_when_change_occurs_before_position_and_deletes_carets_position() {
        interact(() -> {
            selection.selectRange(leftText.length() - 1, area.getLength());

            area.deleteText(leftText.length(), area.getLength());
            assertEquals(leftText.length(), selection.getEndPosition());
        });
    }

    @Test
    public void end_position_is_correct_when_change_occurs_at_position() {
        interact(() -> {
            selectLeft();
            int pos = selection.getEndPosition();

            String append = "some";
            // add test
            area.insertText(leftText.length(), append);
            assertEquals(pos, selection.getEndPosition());

            // delete test
            area.deleteText(pos, area.getLength());
            assertEquals(pos, selection.getEndPosition());
        });
    }

    @Test
    public void end_position_is_correct_when_change_occurs_after_position() {
        interact(() -> {
            selectLeft();

            // add test
            String append = "some";
            area.appendText(append);
            assertEquals(leftText.length(), selection.getEndPosition());

            // delete test
            int length = area.getLength();
            area.deleteText(length - append.length(), length);
            assertEquals(leftText.length(), selection.getEndPosition());
        });
    }

    @Test
    public void deletion_which_includes_selection_and_which_occurs_at_end_of_area_moves_selection_to_new_area_end() {
        interact(() -> {
           selection.selectRange(area.getLength(), area.getLength());
           area.deleteText(leftText.length(), area.getLength());
           assertEquals(area.getLength(), selection.getStartPosition());
           assertEquals(area.getLength(), selection.getEndPosition());
        });
    }
    
    @Test
    public void anchor_updates_correctly_with_listener_attached() {
        interact(() -> {
            area.clear();
            area.anchorProperty().addListener( (ob,ov,nv) -> nv++ );
            area.appendText("asdf");
            area.selectRange(1,2);
            assertEquals("s",area.getSelectedText());
            assertEquals(1,area.getAnchor());
         });
    }
}
