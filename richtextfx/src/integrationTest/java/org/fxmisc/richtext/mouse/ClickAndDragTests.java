package org.fxmisc.richtext.mouse;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.geometry.Bounds;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.MouseButton.*;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

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

            moveTo(firstLineOfArea())
                    .press(PRIMARY)
                    .dropBy(20, 0);

            assertEquals(0, area.getCaretPosition());
        }

    }

    public class When_Area_Is_Enabled {

        public class And_Text_Is_Not_Selected extends InlineCssTextAreaAppTest {

            private String firstWord = "Some";
            private String firstParagraph = firstWord + " text goes here";

            @Override
            public void start(Stage stage) throws Exception {
                super.start(stage);
                area.replaceText(firstParagraph);
                area.moveTo(0);
            }

            @Test
            public void single_clicking_area_moves_caret_to_that_position() {
                assertEquals(0, area.getCaretPosition());

                Bounds bounds = area.getCharacterBoundsOnScreen(
                        firstWord.length(), firstWord.length() + 1).get();

                moveTo(bounds).clickOn(PRIMARY);

                assertEquals(firstWord.length(), area.getCaretPosition());
            }

            @Test
            public void double_clicking_text_in_area_selects_closest_word() {
                doubleClickOnFirstLine();

                assertEquals(firstWord, area.getSelectedText());
            }

            @Test
            public void triple_clicking_line_in_area_selects_paragraph() {
                tripleClickOnFirstLine();

                assertEquals(firstParagraph, area.getSelectedText());
            }

            @Test
            public void pressing_mouse_over_text_and_dragging_mouse_selects_text() {
                moveTo(firstLineOfArea())
                        .press(PRIMARY)
                        .moveBy(20, 0);

                assertFalse(area.getSelectedText().isEmpty());
            }

        }

        public class And_Text_Is_Selected extends InlineCssTextAreaAppTest {

            private String firstWord = "Some";
            private String firstParagraph = firstWord + " text goes here";
            private String extraText = "This is extra text";

            @Test
            public void single_clicking_within_selected_text_moves_caret_to_that_position() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph);
                    area.selectAll();
                });

                Bounds bounds = area.getCharacterBoundsOnScreen(
                        firstWord.length(), firstWord.length() + 1).get();

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
            public void single_clicking_outside_of_selected_text_moves_caret_to_that_position() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + "this is the selected text");
                    area.selectRange(1, 0, 2, -1);
                });

                Bounds bounds = area.getCharacterBoundsOnScreen(
                        firstWord.length(), firstWord.length() + 1).get();

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
            public void pressing_mouse_on_selection_and_dragging_displaces_caret() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + extraText);
                    area.selectRange(0, firstParagraph.length());
                });

                String selText = area.getSelectedText();

                Bounds firstLetterBounds = area.getCharacterBoundsOnScreen(1, 2).get();
                Bounds firstWordEndBounds = area.getCharacterBoundsOnScreen(
                        firstWord.length(), firstWord.length() + 1).get();

                moveTo(firstLetterBounds)
                        .press(PRIMARY)
                        .moveTo(firstWordEndBounds);

                assertEquals(firstWord.length(), area.getCaretPosition());
                assertEquals(selText, area.getSelectedText());
            }

            @Test
            public void pressing_mouse_on_selection_and_dragging_and_releasing_moves_selected_text_to_that_position() {
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

                Bounds letterInFirstWord = area.getCharacterBoundsOnScreen(1, 2).get();

                int insertionPosition = firstParagraph.length() + 2;
                Bounds insertionBounds = area.getCharacterBoundsOnScreen(insertionPosition, insertionPosition + 1).get();

                moveTo(letterInFirstWord)
                        .press(PRIMARY)
                        .dropTo(insertionBounds);

                String expectedText = firstParagraph.substring(firstWord.length())
                    + "\n" + " " + firstWord + " " + extraText;

                assertEquals(insertionPosition, area.getCaretPosition());
                assertEquals(selText, area.getSelectedText());
                assertEquals(expectedText, area.getText());
            }

        }

    }
}