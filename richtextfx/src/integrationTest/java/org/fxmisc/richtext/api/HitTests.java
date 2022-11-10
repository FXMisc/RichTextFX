package org.fxmisc.richtext.api;

import com.nitorcreations.junit.runners.NestedRunner;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.NavigationActions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static javafx.scene.input.MouseButton.PRIMARY;
import static org.junit.Assert.assertEquals;
import static org.testfx.util.WaitForAsyncUtils.asyncFx;

@RunWith(NestedRunner.class)
public class HitTests extends InlineCssTextAreaAppTest {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    private static final String FIFTY_PARS;
    private static final double PADDING_AMOUNT = 20;

    static {
        int totalPars = 50;
        int indexLimit = totalPars - 1;
        StringBuilder sb = new StringBuilder();
        Consumer<Integer> appendParagraph = i -> sb.append("Par #").append(i).append(" ").append(ALPHABET);
        for (int i = 0; i < indexLimit; i++) {
            appendParagraph.accept(i);
            sb.append("\n");
        }
        appendParagraph.accept(indexLimit);
        FIFTY_PARS = sb.toString();
    }

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);

        // insure stage width doesn't change irregardless of changes in superclass' start method
        stage.setWidth(400);
        stage.setHeight(400);
    }

    private void moveCaretToAreaEnd() {
        area.moveTo(area.getLength());
    }

    public class When_Area_Is_Padded {

        public class And_Hits_Occur_Outside_Area {

            String text = "text";
            String fullText = text + "\n" + text;

            @Before
            public void setup() {
                interact(() -> area.replaceText(fullText));
            }

            @Test
            public void clicking_in_top_padding_moves_caret_to_top_line() {
                interact(() -> {
                    area.setPadding(new Insets(PADDING_AMOUNT, 0, 0, 0));
                    moveCaretToAreaEnd();
                });
                moveTo(position(Pos.TOP_LEFT, 1, 2)).clickOn(PRIMARY);
                assertEquals(0, area.getCurrentParagraph());

                interact(() -> moveCaretToAreaEnd());
                moveTo(position(Pos.TOP_CENTER, 0, 0)).clickOn(PRIMARY);
                assertEquals(0, area.getCurrentParagraph());
            }

            @Test
            public void clicking_in_left_padding_moves_caret_to_beginning_of_line_on_single_line_paragraph() {
                interact(() -> area.setPadding(new Insets(0, 0, 0, PADDING_AMOUNT)));

                moveCaretToAreaEnd();
                moveTo(position(Pos.TOP_LEFT, 1, 1)).clickOn(PRIMARY);
                assertEquals(0, area.getCaretColumn());
            }

            @Test
            public void clicking_in_right_padding_moves_caret_to_end_of_line_on_single_line_paragraph() {
                interact(() -> {
                    area.setPadding(new Insets(0, PADDING_AMOUNT, 0, 0));
                    area.moveTo(0);

                    // insure we're scrolled all the way to the right
                    area.scrollBy(new Point2D(100, 0));
                });

                moveTo(position(Pos.TOP_RIGHT, -1, 1)).clickOn(PRIMARY);
                assertEquals(area.getParagraphLength(0), area.getCaretColumn());
            }

            @Test
            public void clicking_in_bottom_padding_moves_caret_to_bottom_line() {
                interact(() -> {
                    area.setPadding(new Insets(0, 0, PADDING_AMOUNT, 0));
                    area.moveTo(0);

                    // insure we're scrolled all the way to the bottom
                    area.scrollBy(new Point2D(0, 100));
                });

                moveTo(position(Pos.BOTTOM_CENTER, 0, -2)).clickOn(PRIMARY);
                assertEquals(1, area.getCurrentParagraph());
            }

        }

        public class And_Hits_Occur_Inside_Area {

            @Before
            public void setup() {
                interact(() -> {
                    area.replaceText(FIFTY_PARS);
                    area.setPadding(new Insets(PADDING_AMOUNT));
                    area.setStyle("-fx-font-family: monospace; -fx-font-size: 12pt;");
                });
            }

            @Test
            public void clicking_character_should_move_caret_to_that_position()
                    throws InterruptedException, ExecutionException {
                int start = area.getAbsolutePosition(3, 8);
                Bounds b = asyncFx(
                        () -> area.getCharacterBoundsOnScreen(start, start + 1).get())
                        .get();
                moveTo(b).clickOn(PRIMARY);
                assertEquals(start, area.getCaretPosition());
            }

            @Test
            public void prev_page_leaves_caret_at_bottom_of_page() {
                area.showParagraphAtBottom(area.getParagraphs().size() - 1);
                // move to last line, column 0
                area.moveTo(area.getParagraphs().size() - 1, 0);

                interact(() -> {
                    // hit is called here
                    area.prevPage(NavigationActions.SelectionPolicy.CLEAR);
                });

                assertEquals(0, area.getCaretColumn());
                assertEquals(area.lastVisibleParToAllParIndex(), area.getCurrentParagraph());
            }

            @Test
            public void next_page_leaves_caret_at_top_of_page() {
                area.showParagraphAtTop(0);

                interact(() -> {
                    area.moveTo(0);
                    // hit is called here
                    area.nextPage(NavigationActions.SelectionPolicy.CLEAR);
                });

                assertEquals(0, area.getCaretColumn());
                assertEquals(area.firstVisibleParToAllParIndex(), area.getCurrentParagraph());
            }

        }

    }

    public class When_ParagraphBox_Is_Padded {

        @Before
        public void setup() {
            interact(() -> {
                area.replaceText(FIFTY_PARS);
                area.setStyle("-fx-font-family: monospace; -fx-font-size: 12pt;");
                scene.getStylesheets().add(HitTests.class.getResource("padded-paragraph-box.css").toExternalForm());
            });
        }

        private void runTest() throws InterruptedException, ExecutionException {
            int start = area.getAbsolutePosition(3, 8);
            Bounds b = asyncFx(
                    () -> area.getCharacterBoundsOnScreen(start, start + 1).get())
                    .get();
            moveTo(b).clickOn(PRIMARY);
            assertEquals(start, area.getCaretPosition());
        }

        public class And_Area_Is_Padded {

            @Test
            public void clicking_character_should_move_caret_to_that_position()
                    throws InterruptedException, ExecutionException {
                interact(() -> area.setPadding(new Insets(PADDING_AMOUNT)));

                runTest();
            }
        }

        public class And_Area_Is_Not_Padded {

            @Test
            public void clicking_character_should_move_caret_to_that_position()
                    throws InterruptedException, ExecutionException {
                runTest();
            }

        }

    }

}
