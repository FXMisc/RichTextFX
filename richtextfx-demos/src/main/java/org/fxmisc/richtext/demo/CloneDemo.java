package org.fxmisc.richtext.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.richtext.AreaFactory;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.StyledTextArea;

public class CloneDemo extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        String text = "Edit this area and watch the below area update itself.";
        InlineCssTextArea area = AreaFactory.inlineCssTextArea(text);
        StyledTextArea<String, String> clone = AreaFactory.cloneInlineCssTextArea(area);

        VBox vbox = new VBox(area, clone);
        vbox.setSpacing(10);

        // set up labels displaying caret position
        String caret = "Caret: ";
        Label areaCaret = new Label(caret);
        area.caretPositionProperty().addListener((observable, oldValue, newValue) -> {
            areaCaret.setText(caret + String.valueOf(newValue));
        });
        Label cloneCaret = new Label(caret);
        clone.caretPositionProperty().addListener((observable, oldValue, newValue) -> {
            cloneCaret.setText(caret + String.valueOf(newValue));
        });

        // set up label's displaying selection position
        String selText = "Selected Text: ";
        Label areaSelection = new Label(selText);
        area.selectedTextProperty().addListener(((observable, oldValue, newValue) -> {
            areaSelection.setText(selText + newValue);
        }));
        Label cloneSelection = new Label(selText);
        clone.selectedTextProperty().addListener(((observable, oldValue, newValue) -> {
            cloneSelection.setText(selText + newValue);
        }));

        // set up Label's distinguishing which labels belong to which area.
        Label areaLabel = new Label("Original Area: ");
        Label cloneLabel = new Label("Cloned Area: ");

        // set up Buttons that programmatically change area but not clone
        Button deleteLastThreeChars = new Button("Click to Delete the previous 3 chars.");
        deleteLastThreeChars.setOnAction((ae) -> {
            for (int i = 0; i <= 2; i++) {
                area.deletePreviousChar();
            }
        });

        // finish GUI
        GridPane grid = new GridPane();
        grid.add(areaLabel, 0, 0);
        grid.add(areaCaret, 1, 0);
        grid.add(areaSelection, 2, 0);
        grid.add(cloneLabel, 0, 1);
        grid.add(cloneCaret, 1, 1);
        grid.add(cloneSelection, 2, 1);
        grid.setHgap(10);
        grid.setVgap(4);

        BorderPane pane = new BorderPane();
        pane.setCenter(vbox);
        pane.setTop(grid);
        pane.setBottom(deleteLastThreeChars);

        Scene scene = new Scene(pane, 800, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
