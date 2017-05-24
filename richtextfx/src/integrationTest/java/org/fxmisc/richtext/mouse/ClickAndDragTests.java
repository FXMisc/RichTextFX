package org.fxmisc.richtext.mouse;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

@RunWith(NestedRunner.class)
public class ClickAndDragTests {

    public class WhenAreaIsDisabled extends InlineCssTextAreaAppTest {

        @Before
        public void setup() {
            interact(() -> {
                area.setDisable(true);
                area.replaceText("When Area Is Disabled Test: Some text goes here");
                area.moveTo(0);
            });
        }

        @Test
        public void shiftClickingAreaDoesNothing() {
            moveTo(firstLineOfArea())
                    .moveBy(20, 0)
                    .press(KeyCode.SHIFT)
                    .press(MouseButton.PRIMARY)
                    .release(KeyCode.SHIFT);

            assertFalse(area.isFocused());

            // cleanup
            release(MouseButton.PRIMARY);
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
                    .press(MouseButton.PRIMARY)
                    .moveBy(20, 0);

            assertTrue(area.getSelectedText().isEmpty());

            // cleanup
            drop();
        }

        @Test
        public void releasingTheMouseAfterDragDoesNothing() {
            assertEquals(0, area.getCaretPosition());

            moveTo(firstLineOfArea())
                    .press(MouseButton.PRIMARY)
                    .dropBy(20, 0);

            assertEquals(0, area.getCaretPosition());

        }

    }

    public class WhenAreaIsEnabled {

        public class AndTextIsNotSelected extends InlineCssTextAreaAppTest {

            private String firstWord = "Some";
            private String firstParagraph = firstWord + " text goes here";

            @Before
            public void setup() {
                interact(() -> {
                    area.replaceText(firstParagraph);
                    area.moveTo(0);
                });
            }

            @Test
            public void singleClickingAreaMovesCaretToThatPosition() {
                assertEquals(0, area.getCaretPosition());

                moveTo(firstLineOfArea())
                        .moveBy(20, 0)
                        .clickOn(MouseButton.PRIMARY);

                assertTrue(0 != area.getCaretPosition());
                // TODO: check that caret is moved exactly to where it should be
            }

            @Test
            public void doubleClickingTextInAreaSelectsClosestWord() {
                moveTo(firstLineOfArea())
                        .doubleClickOn(MouseButton.PRIMARY);

                assertEquals(firstWord, area.getSelectedText());
            }

            @Test
            public void tripleClickingLineInAreaSelectsParagraph() {
                moveTo(firstLineOfArea())
                        .clickOn(MouseButton.PRIMARY)
                        .doubleClickOn(MouseButton.PRIMARY);

                assertEquals(firstParagraph, area.getSelectedText());
            }

            @Test
            public void pressingMouseOverTextAndDraggingMouseSelectsText() {
                moveTo(firstLineOfArea())
                        .press(MouseButton.PRIMARY)
                        .moveBy(20, 0);

                assertFalse(area.getSelectedText().isEmpty());
            }

        }

        public class AndTextIsSelected extends InlineCssTextAreaAppTest {

            private String firstWord = "Some";
            private String firstParagraph = firstWord + " text goes here";

            @Test
            public void singleClickingWithinSelectedTextMovesCaretToThatPosition() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph);
                    area.selectAll();
                });

                moveTo(firstLineOfArea())
                        .moveBy(20, 0)
                        .clickOn(MouseButton.PRIMARY);

                assertTrue(0 != area.getCaretPosition());
            }

            @Test
            public void doubleClickingWithinSelectedTextSelectsClosestWord() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph);
                    area.selectAll();
                });

                moveTo(firstLineOfArea())
                        .doubleClickOn(MouseButton.PRIMARY);

                assertEquals(firstWord, area.getSelectedText());
            }

            @Test
            public void tripleClickingWithinSelectedTextSelectsParagraph() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph);
                    area.selectAll();
                });

                moveTo(firstLineOfArea())
                        .clickOn(MouseButton.PRIMARY)
                        .doubleClickOn(MouseButton.PRIMARY);

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
                        .clickOn(MouseButton.PRIMARY);

                assertTrue(caretPos != area.getCaretPosition());
                // TODO: check that caret is moved exactly to where it should be

            }

            @Test
            public void doubleClickingOutsideOfSelectedTextSelectsClosestWord() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + "this is the selected text");
                    area.selectRange(1, 0, 2, -1);
                });

                moveTo(firstLineOfArea())
                        .doubleClickOn(MouseButton.PRIMARY);

                assertEquals(firstWord, area.getSelectedText());
            }

            @Test
            public void tripleClickingOutsideOfSelectedTextSelectsParagraph() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + "this is the selected text");
                    area.selectRange(1, 0, 2, -1);
                });

                moveTo(firstLineOfArea())
                        .clickOn(MouseButton.PRIMARY)
                        .doubleClickOn(MouseButton.PRIMARY);

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
                        .press(MouseButton.PRIMARY)
                        .moveBy(20, 0);

                assertFalse(originalSelection.equals(area.getSelectedText()));
            }

            @Test
            public void pressingMouseOnSelectionAndDraggingDisplacesCaret() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + "This is extra text");
                    area.selectRange(0, firstWord.length());
                });

                String selText = area.getSelectedText();
                int caretPos = area.getCaretPosition();

                moveTo(firstLineOfArea())
                        .press(MouseButton.PRIMARY)
                        .moveBy(0, 14);

                assertTrue(caretPos != area.getCaretPosition());
                assertEquals(selText, area.getSelectedText());
            }

            @Test
            public void pressingMouseOnSelectionAndDraggingAndReleasingMovesSelectedTextToThatPosition() {
                // setup
                interact(() -> {
                    area.replaceText(firstParagraph + "\n" + "This is extra text");
                    area.selectRange(0, firstWord.length());
                });

                String selText = area.getSelectedText();
                int caretPos = area.getCaretPosition();

                moveTo(firstLineOfArea())
                        .press(MouseButton.PRIMARY)
                        .dropBy(0, 14);

                assertTrue(caretPos != area.getCaretPosition());
                assertTrue(area.getSelectedText().isEmpty());
                // TODO: should check that move is exact
            }

        }

    }

}
