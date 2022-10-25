package org.fxmisc.richtext.mouse;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.MouseButton.*;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.testfx.util.WaitForAsyncUtils.asyncFx;

@RunWith(NestedRunner.class)
public class ClickAndDragTests {

    public class When_Area_Is_Disabled extends InlineCssTextAreaAppTest {

        @Override
        public void start(Stage stage) throws Exception {
            super.start(stage);
            area.setDisable(true);
            area.replaceText("When Area Is Disabled Test: Some text goes here");
            area.moveTo(0);
        }

        @Test
        public void shift_clicking_area_does_nothing() {
            moveTo(firstLineOfArea())
                    .moveBy(20, 0)
                    .press(SHIFT)
                    .press(PRIMARY);

            assertFalse(area.isFocused());
        }

        @Test
        public void single_clicking_area_does_nothing() {
            leftClickOnFirstLine();

            assertFalse(area.isFocused());
        }

        @Test
        public void double_clicking_area_does_nothing() {
            doubleClickOnFirstLine();

            assertFalse(area.isFocused());
        }

        @Test
        public void triple_clicking_area_does_nothing() {
            tripleClickOnFirstLine();

            assertFalse(area.isFocused());
        }

        @Test
        public void dragging_the_mouse_does_not_select_text() {
            moveTo(firstLineOfArea())
                    .press(PRIMARY)
                    .moveBy(20, 0);

            assertTrue(area.getSelectedText().isEmpty());
        }

        @Test
        public void releasing_the_mouse_after_drag_does_nothing() {
            assertEquals(0, area.getCaretPosition());

            SimpleIntegerProperty i = new SimpleIntegerProperty(0);
            area.setOnNewSelectionDragFinished(e -> i.set(1));

            moveTo(firstLineOfArea())
                    .press(PRIMARY)
                    .dropBy(20, 0);

            assertEquals(0, area.getCaretPosition());
            assertEquals(0, i.get());
        }

    }

    public class When_Area_Is_Enabled {

        public class And_Text_Is_Not_Selected extends InlineCssTextAreaAppTest {

            private String firstWord = "Some";
            private String firstParagraph = firstWord + " text goes here";
            private String secondWord = "More";
            private String secondParagraph = secondWord + " text goes here";

            @Override
            public void start(Stage stage) throws Exception {
                super.start(stage);
                area.replaceText(firstParagraph + "\n" + secondParagraph);
                area.moveTo(0);
            }

            @Test
            public void single_clicking_area_moves_caret_to_that_position()
                    throws InterruptedException, ExecutionException {
                assertEquals(0, area.getCaretPosition());

                Bounds bounds = asyncFx(
                        () -> area.getCharacterBoundsOnScreen(firstWord.length(), firstWord.length() + 1).get())
                        .get();

                moveTo(bounds).clickOn(PRIMARY);

                assertEquals(firstWord.length(), area.getCaretPosition());
            }

            @Test
            public void single_clicking_area_beyond_text_moves_caret_to_end_position()
                    throws InterruptedException, ExecutionException {

                int position = firstParagraph.length() + secondWord.length();
                interact( () -> area.getStylesheets ().add("org/fxmisc/richtext/mouse/padtest.css") );
                assertEquals(0, area.getCaretPosition());
                clickOn( area );

                Bounds b = asyncFx(
                        () -> area.getCharacterBoundsOnScreen(position, position + 1).get())
                        .get();

                moveTo(new Point2D( b.getMaxX(), b.getMaxY()+25 )).clickOn(PRIMARY);

                assertEquals(area.getLength(), area.getCaretPosition());
            }

            @Test
            public void double_clicking_text_in_area_selects_closest_word() {
                doubleClickOnFirstLine();

                assertEquals(firstWord, area.getSelectedText());
            }

            @Test
            public void triple_clicking_line_in_area_selects_paragraph()
                    throws InterruptedException, ExecutionException {

                int wordStart = firstParagraph.length() + 1;
                Bounds bounds = asyncFx(
                        () -> area.getCharacterBoundsOnScreen(wordStart, wordStart + 1).get())
                        .get();

                moveTo(bounds).doubleClickOn(PRIMARY).clickOn(PRIMARY);

                assertEquals(secondParagraph, area.getSelectedText());
            }

            @Test
            public void pressing_mouse_over_text_and_dragging_mouse_selects_text() {
                moveTo(firstLineOfArea())
                        .press(PRIMARY)
                        .moveBy(20, 0);

                assertFalse(area.getSelectedText().isEmpty());
            }

            @Test
            public void pressing_mouse_over_text_and_dragging_and_releasing_mouse_triggers_new_selection_finished() {
                // Doesn't work on Mac builds; works on Linux & Windows
                skip_if_on_mac();

                SimpleIntegerProperty i = new SimpleIntegerProperty(0);
                area.setOnNewSelectionDragFinished(e -> i.set(1));
                moveTo(firstLineOfArea())
                        .press(PRIMARY)
                        .moveBy(20, 0)
                        .release(PRIMARY);

                assertFalse(area.getSelectedText().isEmpty());
                assertEquals(1, i.get());
            }

        }

        public class And_Text_Is_Selected extends InlineCssTextAreaAppTest {

            private String firstWord = "Some";
            private String firstParagraph = firstWord + " text goes here";
            private String extraText = "This is extra text";

            @Test
            public void single_clicking_within_selected_text_moves_caret_to_that_position()
                    throws InterruptedException, ExecutionException {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph);
                    area.selectAll();
                });

                Bounds bounds = asyncFx(
                        () -> area.getCharacterBoundsOnScreen(
                                firstWord.length(), firstWord.length() + 1).get())
                        .get();

                moveTo(bounds).clickOn(PRIMARY);

                assertEquals(firstWord.length(), area.getCaretPosition());
            }

            @Test
            public void double_clicking_within_selected_text_selects_closest_word() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph);
                    area.selectAll();
                });

                doubleClickOnFirstLine();

                assertEquals(firstWord, area.getSelectedText());
            }

            @Test
            public void triple_clicking_within_selected_text_selects_paragraph() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph);
                    area.selectAll();
                });

                tripleClickOnFirstLine();

                assertEquals(firstParagraph, area.getSelectedText());
            }

            @Test
            public void single_clicking_within_selected_text_does_not_trigger_new_selection_finished()
                    throws InterruptedException, ExecutionException {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph);
                    area.selectAll();
                });

                SimpleIntegerProperty i = new SimpleIntegerProperty(0);
                area.setOnNewSelectionDragFinished(e -> i.set(1));

                Bounds bounds = asyncFx(
                        () -> area.getCharacterBoundsOnScreen(
                                firstWord.length(), firstWord.length() + 1).get())
                        .get();

                moveTo(bounds).clickOn(PRIMARY);

                assertEquals(0, i.get());
            }

            @Test
            public void single_clicking_outside_of_selected_text_moves_caret_to_that_position()
                    throws InterruptedException, ExecutionException {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + "this is the selected text");
                    area.selectRange(1, 0, 2, -1);
                });

                Bounds bounds = asyncFx(
                        () -> area.getCharacterBoundsOnScreen(
                                firstWord.length(), firstWord.length() + 1).get())
                        .get();
                moveTo(bounds).clickOn(PRIMARY);

                assertEquals(firstWord.length(), area.getCaretPosition());
            }

            @Test
            public void double_clicking_outside_of_selected_text_selects_closest_word() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + "this is the selected text");
                    area.selectRange(1, 0, 2, -1);
                });

                doubleClickOnFirstLine();

                assertEquals(firstWord, area.getSelectedText());
            }

            @Test
            public void triple_clicking_outside_of_selected_text_selects_paragraph() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + "this is the selected text");
                    area.selectRange(1, 0, 2, -1);
                });

                tripleClickOnFirstLine();

                assertEquals(firstParagraph, area.getSelectedText());
            }

            @Test
            public void single_clicking_outside_of_selected_text_does_not_trigger_new_selection_finished()
                    throws InterruptedException, ExecutionException {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + "this is the selected text");
                    area.selectRange(1, 0, 2, -1);
                });

                SimpleIntegerProperty i = new SimpleIntegerProperty(0);
                area.setOnNewSelectionDragFinished(e -> i.set(1));

                Bounds bounds = asyncFx(
                        () -> area.getCharacterBoundsOnScreen(firstWord.length(), firstWord.length() + 1).get())
                        .get();

                moveTo(bounds).clickOn(PRIMARY);

                assertEquals(0, i.get());
            }

            @Test
            public void pressing_mouse_on_unselected_text_and_dragging_makes_new_selection() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + "this is the selected text");
                    area.selectRange(1, 0, 2, -1);
                });

                String originalSelection = area.getSelectedText();

                moveTo(firstLineOfArea())
                        .press(PRIMARY)
                        .moveBy(20, 0);

                assertFalse(originalSelection.equals(area.getSelectedText()));
            }

            @Test
            public void pressing_mouse_on_selection_and_dragging_displaces_caret()
                    throws InterruptedException, ExecutionException {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + extraText);
                    area.selectRange(0, firstParagraph.length());
                });

                String selText = area.getSelectedText();

                Bounds firstLetterBounds = asyncFx(
                        () -> area.getCharacterBoundsOnScreen(1, 2).get())
                        .get();
                Bounds firstWordEndBounds = asyncFx(
                        () -> area.getCharacterBoundsOnScreen(firstWord.length(), firstWord.length() + 1).get())
                        .get();

                moveTo(firstLetterBounds)
                        .press(PRIMARY)
                        .moveTo(firstWordEndBounds);

                assertEquals(firstWord.length(), area.getCaretPosition());
                assertEquals(selText, area.getSelectedText());
            }

            @Test
            public void pressing_mouse_on_selection_and_dragging_and_releasing_moves_selected_text_to_that_position()
                    throws InterruptedException, ExecutionException {
                // Linux passes; Mac fails at "assertEquals(selText, area.getSelectedText())"; Windows is untested
                // so only run on Linux
                // TODO: update test to see if it works on Windows
                run_only_on_linux();

                // setup
                String twoSpaces = "  ";
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + twoSpaces + extraText);
                    area.selectRange(0, firstWord.length());
                });

                String selText = area.getSelectedText();

                Bounds letterInFirstWord =
                        asyncFx(() -> area.getCharacterBoundsOnScreen(1, 2).get())
                        .get();
                int insertionPosition = firstParagraph.length() + 2;
                Bounds insertionBounds =
                        asyncFx(() -> area.getCharacterBoundsOnScreen(insertionPosition, insertionPosition + 1).get())
                        .get();
                moveTo(letterInFirstWord)
                        .press(PRIMARY)
                        .dropTo(insertionBounds);

                String expectedText = firstParagraph.substring(firstWord.length())
                    + "\n" + " " + firstWord + " " + extraText;

                assertEquals(insertionPosition, area.getCaretPosition());
                assertEquals(selText, area.getSelectedText());
                assertEquals(expectedText, area.getText());
            }

            @Test
            public void pressing_mouse_on_selection_and_dragging_and_releasing_does_not_trigger_new_selection_finished()
                    throws InterruptedException, ExecutionException {
                // Linux passes; Mac/Windows uncertain
                // TODO: update test to see if it works on Mac & Windows
                run_only_on_linux();

                // setup
                String twoSpaces = "  ";
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + twoSpaces + extraText);
                    area.selectRange(0, firstWord.length());
                });

                SimpleIntegerProperty i = new SimpleIntegerProperty(0);
                area.setOnNewSelectionDragFinished(e -> i.set(1));

                Bounds letterInFirstWord = asyncFx(
                        () -> area.getCharacterBoundsOnScreen(1, 2).get())
                        .get();
                final int insertionPosition = firstParagraph.length() + 2;
                Bounds insertionBounds = asyncFx(
                        () -> area.getCharacterBoundsOnScreen(insertionPosition, insertionPosition + 1).get())
                        .get();

                moveTo(letterInFirstWord)
                        .press(PRIMARY)
                        .dropTo(insertionBounds);

                assertEquals(0, i.get());
            }

        }

    }
}