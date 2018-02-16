package org.fxmisc.richtext.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxmisc.richtext.CaretNode;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.Selection;
import org.fxmisc.richtext.SelectionImpl;
import org.fxmisc.richtext.SelectionPath;

public class MultiCaretAndSelectionDemo extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(i).append(" :").append(alphabet).append("\n");
        }
        InlineCssTextArea area = new InlineCssTextArea(sb.toString());

        // create CSS styles
        String background = "-rtfx-background-color: red; ";
        String underline = "-rtfx-underline-color: blue; " +
                           "-rtfx-underline-width: 2.0;";
        String border = "-rtfx-border-stroke-color: green; " +
                        "-rtfx-border-stroke-width: 3.0;";

        // set all of them at once on a give line to insure they display properly
        area.setStyle(0, alphabet.length(), background + underline + border);

        // set each one over a given segment
        area.setStyle(1, 0, 3, background);
        area.setStyle(1, 4, 7, underline);
        area.setStyle(1, 8, 11, border);

        // add another caret
        CaretNode extraCaret = new CaretNode("another caret", area);
        if (!area.addCaret(extraCaret)) {
            throw new IllegalStateException("caret was not added to area");
        }
        extraCaret.moveTo(3, 8);

        // since the properties are re-set when it applies the CSS from files
        // remove the style class so that properties set above are not changed
        extraCaret.getStyleClass().remove("caret");

        extraCaret.setStrokeWidth(10.0);
        extraCaret.setStroke(Color.BROWN);
        extraCaret.setBlinkRate(Duration.millis(200));

        // add another selection
        Selection<String, String, String> extraSelection = new SelectionImpl<>("another selection", area,
                range -> {
                    SelectionPath p = new SelectionPath(range);
                    p.setStrokeWidth(0);
                    p.setFill(Color.YELLOW);

                    // since the properties are re-set when it applies the CSS from files
                    // remove the style class so that properties set above are not changed
                    p.getStyleClass().remove("selection");
                    return p;
                }
        );
        if (!area.addSelection(extraSelection)) {
            throw new IllegalStateException("selection was not added to area");
        }
        extraSelection.selectRange(9, 2, 9, 8);

        // select some other range with the regular caret/selection before showing area
        area.selectRange(2, 0, 2, 4);

        primaryStage.setScene(new Scene(area, 400, 400));
        primaryStage.show();

        // request focus so carets blink
        area.requestFocus();
    }
}
