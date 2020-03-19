package org.fxmisc.richtext.api;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.RichTextFXTestBase;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.model.SimpleEditableStyledDocument;
import org.fxmisc.richtext.model.RichTextChange;
import org.fxmisc.richtext.model.TextChange;
import org.fxmisc.richtext.util.UndoUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactfx.SuspendableYes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(NestedRunner.class)
public class UndoManagerTests {

    public class UsingInlineCssTextArea extends InlineCssTextAreaAppTest {

        @Test
        public void incoming_change_is_not_merged_after_period_of_user_inactivity() {
            String text1 = "text1";
            String text2 = "text2";

            long periodOfUserInactivity = UndoUtils.DEFAULT_PREVENT_MERGE_DELAY.toMillis() + 300L;

            write(text1);
            sleep(periodOfUserInactivity);
            write(text2);

            interact(area::undo);
            assertEquals(text1, area.getText());

            interact(area::undo);
            assertEquals("", area.getText());
        }

        @Test  // After undo, text insertion point jumps to the start of the text area #780
               // After undo, text insertion point jumps to the end of the text area #912
        public void undo_leaves_correct_insertion_point() {

            write("abc mno");
            interact(() -> {
                area.insertText(3," def");
                area.appendText(" xyz");
            });

            assertEquals("abc def mno xyz",area.getText());

            interact(area::undo); // removes " xyz"
            assertEquals("abc def mno",area.getText());
            //                       ^
            assertEquals( area.getCaretPosition(), area.getSelection().getStart() );
            assertEquals( 11, area.getSelection().getStart() );

            interact(area::undo); // removes " def"
            assertEquals("abc mno",area.getText());
            //               ^
            assertEquals( area.getCaretPosition(), area.getSelection().getStart() );
            assertEquals( 3, area.getSelection().getStart() );

            interact(area::redo); // restore " def"
            assertEquals("abc def mno",area.getText());
            //                   ^
            assertEquals( area.getCaretPosition(), area.getSelection().getStart() );
            assertEquals( 7, area.getSelection().getStart() );

            interact(area::undo); // removes " def"
            interact(() -> area.insertText(area.getCaretPosition()," ?"));
            assertEquals("abc ? mno",area.getText());
        }

        @Test
        public void testUndoWithWinNewlines() {
            String text1 = "abc\r\ndef";
            String text2 = "A\r\nB\r\nC";

            interact(() -> {
                area.replaceText(text1);
                area.getUndoManager().forgetHistory();
                area.insertText(0, text2);
                assertEquals("A\nB\nCabc\ndef", area.getText());

                area.undo();
                assertEquals("abc\ndef", area.getText());
            });
        }

        @Test
        public void multiChange_undo_and_redo_works() {
            interact(() -> {
                String text = "text";
                String wrappedText = "(" + text + ")";
                area.replaceText(wrappedText);
                area.getUndoManager().forgetHistory();

                // Text:     |(|t|e|x|t|)|
                // Position: 0 1 2 3 4 5 6
                area.createMultiChange(2)
                        // delete parenthesis
                        .deleteText(0, 1)
                        .deleteText(5, 6)
                        .commit();

                area.undo();
                assertEquals(wrappedText, area.getText());

                area.redo();
                assertEquals(text, area.getText());
            });
        }

        @Test
        public void multiChange_merge_works() {
            interact(() -> {
                String initialText = "123456";
                area.replaceText(initialText);
                area.getUndoManager().forgetHistory();

                int firstCount = 0;
                int secondCount = 3;

                // Text:     |1|2|3|4|5|6|
                // Position: 0 1 2 3 4 5 6
                area.createMultiChange(2)
                        // replace '1' with 'a'
                        .replaceText(firstCount, ++firstCount, "a")
                        // replace '4' with 'c'
                        .replaceText(secondCount, ++secondCount, "c")
                        .commit();

                // Text:     |a|2|3|c|5|6|
                // Position: 0 1 2 3 4 5 6
                area.createMultiChange(2)
                        // replace '2' with 'b'
                        .replaceText(firstCount, ++firstCount, "b")
                        // replace '5' with 'd'
                        .replaceText(secondCount, ++secondCount, "d")
                        .commit();

                String finalText = "ab3cd6";

                area.undo();
                assertFalse(area.getUndoManager().isUndoAvailable());
                assertEquals(initialText, area.getText());

                area.redo();
                assertFalse(area.getUndoManager().isRedoAvailable());
                assertEquals(finalText, area.getText());
            });
        }

        @Test
        public void identity_change_works() {
            interact(() -> {
               area.replaceText("ttttt");

               SimpleIntegerProperty richEmissions = new SimpleIntegerProperty(0);
               SimpleIntegerProperty plainEmissions = new SimpleIntegerProperty(0);
               area.multiRichChanges()
                       .hook(list -> richEmissions.set(richEmissions.get() + 1))
                       .filter(list -> !list.stream().allMatch(TextChange::isIdentity))
                       .subscribe(list -> plainEmissions.set(plainEmissions.get() + 1));


               int position = 0;
               area.createMultiChange(4)
                       .replaceText(position, ++position, "t")
                       .replaceText(position, ++position, "t")
                       .replaceText(position, ++position, "t")
                       .replaceText(position, ++position, "t")
                       .commit();

               assertEquals(1, richEmissions.get());
               assertEquals(0, plainEmissions.get());
            });
        }
        
        @Test
        public void testForBug904() {
        	String firstLine = "some text\n";
        	write( firstLine );
        	interact( () -> area.setStyle( 5, 9, "-fx-font-weight: bold;" ) );
        	write( "new line" );
        	area.getUndoManager().preventMerge();
            interact( () -> area.append( area.getContent().subSequence( firstLine.length()-1, area.getLength() ) ) );
            interact( area::undo ); // should not throw Unexpected change received exception 
        }

        @Test
        public void suspendable_UndoManager_skips_style_check() {
        	
            SuspendableYes suspendUndo = new SuspendableYes();
            area.setUndoManager( UndoUtils.richTextSuspendableUndoManager( area, suspendUndo ) );
            write( "some text\n" );
            interact( () -> suspendUndo.suspendWhile( () -> area.setStyle( 5, 9, "-fx-font-weight: bold;" ) ) );
            write( "new line" );
            interact( area::undo ); // should not throw Unexpected change received exception
            
            area.setUndoManager( UndoUtils.defaultUndoManager( area ) );
            RichTextChange.skipStyleComparison( false );
        }

    }

    public class UsingStyledTextArea extends RichTextFXTestBase {

        @Override
        public void start(Stage stage) throws Exception {
            stage.setScene(new Scene(new Label("Ignore me..."), 400, 400));
            stage.show();
        }

        @Test
        public void testForBug216() {
            interact(() -> {
                // set up area with some styled text content
                boolean initialStyle = false;
                StyledTextArea<String, Boolean> area = new StyledTextArea<>(
                        "", (t, s) -> {},
                        initialStyle, (t, s) -> {},
                        new SimpleEditableStyledDocument<>("", initialStyle), true
                );
                area.replaceText("testtest");
                area.setStyle(0, 8, true);

                // add a space styled by initialStyle
                area.setUseInitialStyleForInsertion(true);
                area.insertText(4, " ");

                // add another space
                area.insertText(5, " ");

                // testing that undo/redo don't throw an exception
                area.undo();
                area.redo();
            });
        }

    }
}
