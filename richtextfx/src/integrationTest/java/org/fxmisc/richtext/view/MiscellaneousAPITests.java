package org.fxmisc.richtext.view;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.ViewActions.CaretVisibility;
import org.fxmisc.richtext.model.NavigationActions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testfx.service.query.PointQuery;
import org.testfx.util.WaitForAsyncUtils;

import java.util.function.Consumer;

import static javafx.scene.input.MouseButton.PRIMARY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(NestedRunner.class)
public class MiscellaneousAPITests {

    public class CharacterBoundsTest extends InlineCssTextAreaAppTest {

        @Override
        public void start(Stage stage) throws Exception {
            super.start(stage);
            area.replaceText("a line of sample text");
        }

        @Test
        public void selectionBoundsUnchangedWhenCallGetCharacterBounds() {
            area.selectAll();
            Bounds bounds = area.getSelectionBounds().get();

            // getCharacterBoundsOnScreen() uses the selection shape to calculate the bounds
            // so insure it doesn't affect the selection shape if something is selected
            // before it gets called
            area.getCharacterBoundsOnScreen(0, area.getLength() - 1);

            assertEquals(bounds, area.getSelectionBounds().get());
        }

    }

    public class ParagraphLinesCountTests extends InlineCssTextAreaAppTest {

        @Test
        public void multiLineReturnsCorrectCount() {
            String[] lines = {
                    "01 02 03 04 05",
                    "11 12 13 14 15",
                    "21 22 23 24 25"
            };
            interact(() -> {
                area.setWrapText(true);
                stage.setWidth(120);
                area.replaceText(String.join(" ", lines));
            });

            assertEquals(3, area.getParagraphLinesCount(0));
        }

        @Test
        public void singleLineReturnsOne() {
            interact(() -> area.replaceText("some text"));
            assertFalse(area.isWrapText());

            assertEquals(1, area.getParagraphLinesCount(0));
        }

    }

    public class MoveCaretTests extends InlineCssTextAreaAppTest {

        @Override
        public void start(Stage stage) throws Exception {
            super.start(stage);

            // insure caret is always visible
            area.setShowCaret(CaretVisibility.ON);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                sb.append(i).append("\n");
            }
            area.replaceText(sb.toString());
            area.moveTo(0);
            area.showParagraphAtTop(0);
        }

        @Test
        public void testMoveCaretAndFollowIt() {
            assertTrue(area.getCaretBounds().isPresent());

            // move caret outside of viewport
            area.moveTo(area.getLength());
            area.requestFollowCaret();

            // needed for test to pass
            WaitForAsyncUtils.waitForFxEvents();

            // viewport should update itself so caret is visible again
            assertTrue(area.getCaretBounds().isPresent());
        }

        @Test
        public void testMoveCaretWithoutFollowingIt() {
            assertTrue(area.getCaretBounds().isPresent());

            // move caret outside of viewport
            area.moveTo(area.getLength());

            // caret should not be visible
            assertFalse(area.getCaretBounds().isPresent());
        }
    }

    public class HitTests extends InlineCssTextAreaAppTest {

        private PointQuery position(Pos position, double offsetX, double offsetY) {
            return point(area).atPosition(position).atOffset(offsetX, offsetY);
        }

        private void moveCaretToAreaEnd() {
            area.moveTo(area.getLength());
        }

        public class WhenAreaIsPadded {

            double paddingAmount = 20;

            public class AndHitsOccurOutsideArea {

                String text = "text";
                String fullText = text + "\n" + text;

                @Before
                public void setup() {
                    interact(() -> area.replaceText(fullText));
                }

                @Test
                public void clickingInTopPaddingMovesCaretToTopLine() {
                    interact(() -> area.setPadding(new Insets(paddingAmount, 0, 0, 0)));

                    moveCaretToAreaEnd();
                    moveTo(position(Pos.TOP_LEFT, 1, 2)).clickOn(PRIMARY);
                    assertEquals(0, area.getCurrentParagraph());

                    moveCaretToAreaEnd();
                    moveTo(position(Pos.TOP_CENTER, 0, 0)).clickOn(PRIMARY);
                    assertEquals(0, area.getCurrentParagraph());
                }

                @Test
                public void clickingInLeftPaddingMovesCaretToBeginningOfLineOnSingleLineParagraph() {
                    interact(() -> area.setPadding(new Insets(0, 0, 0, paddingAmount)));

                    moveCaretToAreaEnd();
                    moveTo(position(Pos.TOP_LEFT, 1, 1)).clickOn(PRIMARY);
                    assertEquals(0, area.getCaretColumn());
                }

                @Test
                public void clickingInRightPaddingMovesCaretToEndOfLineOnSingleLineParagraph() {
                    interact(() -> {
                        area.setPadding(new Insets(0, paddingAmount, 0, 0));
                        area.moveTo(0);

                        // insure we're scrolled all the way to the right
                        area.scrollBy(new Point2D(100, 0));
                    });

                    moveTo(position(Pos.TOP_RIGHT, -1, 1)).clickOn(PRIMARY);
                    assertEquals(area.getParagraphLenth(0), area.getCaretColumn());
                }

                @Test
                public void clickingInBottomPaddingMovesCaretToBottomLine() {
                    interact(() -> {
                        area.setPadding(new Insets(0, 0, paddingAmount, 0));
                        area.moveTo(0);

                        // insure we're scrolled all the way to the right
                        area.scrollBy(new Point2D(0, 100));
                    });

                    moveTo(position(Pos.BOTTOM_CENTER, 0, -2)).clickOn(PRIMARY);
                    assertEquals(1, area.getCurrentParagraph());
                }

            }

            public class AndHitsOccurInsideArea {

                String text = "abcdefghijklmnopqrstuvwxyz";
                String fullText;

                {
                    int totalPars = 50;
                    int indexLimit = totalPars - 1;
                    StringBuilder sb = new StringBuilder();
                    Consumer<Integer> appendParagraph = i -> sb.append("Par #").append(i).append(" ").append(text);
                    for (int i = 0; i < indexLimit; i++) {
                        appendParagraph.accept(i);
                        sb.append("\n");
                    }
                    appendParagraph.accept(indexLimit);
                    fullText = sb.toString();
                }

                @Before
                public void setup() {
                    interact(() -> {
                        area.replaceText(fullText);
                        area.setPadding(new Insets(paddingAmount));
                        area.setStyle("-fx-font-family: monospace; -fx-font-size: 12pt;");
                    });
                }

                @Test
                public void clickingCharacterShouldMoveCaretToThatPosition() {
                    int start = area.getAbsolutePosition(3, 8);
                    Bounds b = area.getCharacterBoundsOnScreen(start, start + 1).get();
                    moveTo(b).clickOn(PRIMARY);
                    assertEquals(start, area.getCaretPosition());
                }

                @Test
                public void prevPageMovesCaretToTopOfPage() {
                    area.showParagraphAtBottom(area.getParagraphs().size() - 1);
                    // move to last line, column 0
                    area.moveTo(area.getParagraphs().size() - 1, 0);

                    interact(() -> {
                        // hit is called here
                        area.prevPage(NavigationActions.SelectionPolicy.CLEAR);
                    });

                    assertEquals(0, area.getCaretColumn());
                    assertEquals(32, area.getCurrentParagraph());
                }

                @Test
                public void nextPageMovesCaretToBottomOfPage() {
                    area.showParagraphAtTop(0);
                    area.moveTo(0);

                    interact(() -> {
                        // hit is called here
                        area.nextPage(NavigationActions.SelectionPolicy.CLEAR);
                    });

                    assertEquals(0, area.getCaretColumn());
                    assertEquals(17, area.getCurrentParagraph());
                }

            }

        }

    }

}
