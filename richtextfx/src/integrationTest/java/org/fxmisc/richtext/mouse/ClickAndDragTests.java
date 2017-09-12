package org.fxmisc.richtext.mouse;

import com.nitorcreations.junit.runners.NestedRunner;
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

    public class WhenAreaIsDisabled extends InlineCssTextAreaAppTest {

        @Override
        public void start(Stage stage) throws Exception {
            super.start(stage);
            area.setDisable(true);
            area.replaceText("When Area Is Disabled Test: Some text goes here");
            area.moveTo(0);
        }

        @Test
        public void shiftClickingAreaDoesNothing() {
            moveTo(firstLineOfArea())
                    .moveBy(20, 0)
                    .press(SHIFT)
                    .press(PRIMARY);

            assertFalse(area.isFocused());
        }

        @Test
        public void singleClickingAreaDoesNothing() {
            leftClickOnFirstLine();

            assertFalse(area.isFocused());
        }

        @Test
        public void doubleClickingAreaDoesNothing() {
            doubleClickOnFirstLine();

            assertFalse(area.isFocused());
        }

        @Test
        public void tripleClickingAreaDoesNothing() {
            tripleClickOnFirstLine();

            assertFalse(area.isFocused());
        }

        @Test
        public void draggingTheMouseDoesNotSelectText() {
            moveTo(firstLineOfArea())
                    .press(PRIMARY)
                    .moveBy(20, 0);

            assertTrue(area.getSelectedText().isEmpty());
        }

        @Test
        public void releasingTheMouseAfterDragDoesNothing() {
            assertEquals(0, area.getCaretPosition());

            moveTo(firstLineOfArea())
                    .press(PRIMARY)
                    .dropBy(20, 0);

            assertEquals(0, area.getCaretPosition());
        }

    }

    public class WhenAreaIsEnabled {

        public class AndTextIsNotSelected extends InlineCssTextAreaAppTest {

            private String firstWord = "Some";
            private String firstParagraph = firstWord + " text goes here";

            @Override
            public void start(Stage stage) throws Exception {
                super.start(stage);
                area.replaceText(firstParagraph);
                area.moveTo(0);
            }

            @Test
            public void singleClickingAreaMovesCaretToThatPosition() {
                assertEquals(0, area.getCaretPosition());

                moveTo(firstLineOfArea())
                        .moveBy(20, 0)
                        .clickOn(PRIMARY);

                assertTrue(0 != area.getCaretPosition());
            }

            @Test
            public void doubleClickingTextInAreaSelectsClosestWord() {
                doubleClickOnFirstLine();

                assertEquals(firstWord, area.getSelectedText());
            }

            @Test
            public void tripleClickingLineInAreaSelectsParagraph() {
                tripleClickOnFirstLine();

                assertEquals(firstParagraph, area.getSelectedText());
            }

            @Test
            public void pressingMouseOverTextAndDraggingMouseSelectsText() {
                moveTo(firstLineOfArea())
                        .press(PRIMARY)
                        .moveBy(20, 0);

                assertFalse(area.getSelectedText().isEmpty());
            }

        }

        public class AndTextIsSelected extends InlineCssTextAreaAppTest {

            private String firstWord = "Some";
            private String firstParagraph = firstWord + " text goes here";
            private String extraText = "This is extra text";

            @Test
            public void singleClickingWithinSelectedTextMovesCaretToThatPosition() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph);
                    area.selectAll();
                });

                moveTo(firstLineOfArea())
                        .moveBy(20, 0)
                        .clickOn(PRIMARY);

                assertTrue(0 != area.getCaretPosition());
            }

            @Test
            public void doubleClickingWithinSelectedTextSelectsClosestWord() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph);
                    area.selectAll();
                });

                doubleClickOnFirstLine();

                assertEquals(firstWord, area.getSelectedText());
            }

            @Test
            public void tripleClickingWithinSelectedTextSelectsParagraph() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph);
                    area.selectAll();
                });

                tripleClickOnFirstLine();

                assertEquals(firstParagraph, area.getSelectedText());
            }

            @Test
            public void singleClickingOutsideOfSelectedTextMovesCaretToThatPosition() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + "this is the selected text");
                    area.selectRange(1, 0, 2, -1);
                });

                int caretPos = area.getCaretPosition();

                moveTo(firstLineOfArea())
                        .moveBy(20, 0)
                        .clickOn(PRIMARY);

                assertTrue(caretPos != area.getCaretPosition());
            }

            @Test
            public void doubleClickingOutsideOfSelectedTextSelectsClosestWord() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + "this is the selected text");
                    area.selectRange(1, 0, 2, -1);
                });

                doubleClickOnFirstLine();

                assertEquals(firstWord, area.getSelectedText());
            }

            @Test
            public void tripleClickingOutsideOfSelectedTextSelectsParagraph() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + "this is the selected text");
                    area.selectRange(1, 0, 2, -1);
                });

                tripleClickOnFirstLine();

                assertEquals(firstParagraph, area.getSelectedText());
            }

            @Test
            public void pressingMouseOnUnselectedTextAndDraggingMakesNewSelection() {
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
            public void pressingMouseOnSelectionAndDraggingDisplacesCaret() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + extraText);
                    area.selectRange(0, firstWord.length());
                });

                String selText = area.getSelectedText();

                moveTo(firstLineOfArea())
                        .press(PRIMARY)
                        .moveBy(0, 22);

                assertEquals(firstParagraph.length() + 1, area.getCaretPosition());
                assertEquals(selText, area.getSelectedText());
            }

            @Test
            public void pressingMouseOnSelectionAndDraggingAndReleasingMovesSelectedTextToThatPosition() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + extraText);
                    area.selectRange(0, firstWord.length());
                });

                String selText = area.getSelectedText();

                moveTo(firstLineOfArea())
                        .press(PRIMARY)
                        .dropBy(0, 22);

                String expectedText = firstParagraph.substring(firstWord.length())
                    + "\n" + firstWord + extraText;

                assertEquals(firstParagraph.length() + 1, area.getCaretPosition());
                assertEquals(selText, area.getSelectedText());
                assertEquals(expectedText, area.getText());
            }

        }

    }
}