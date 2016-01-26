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

public class CloneDemo extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        String selectedText = "selection";
        String text = "Edit the top area (original)\nand watch the (clone) bottom area's displayed text change and its " +
                "selected text [" + selectedText + "] update itself accordingly.";
        InlineCssTextArea area = AreaFactory.inlineCssTextArea();
        InlineCssTextArea clone = AreaFactory.cloneInlineCssTextArea(area);
        area.insertText(0, text);

        VBox vbox = new VBox(area, clone);
        vbox.setSpacing(10);

        // set up labels displaying caret position
        String caret = "Caret: ";
        Label areaCaret = new Label(caret);
        area.caretPositionProperty().addListener((observable, oldValue, newValue) -> areaCaret.setText(caret + String.valueOf(newValue)));
        Label cloneCaret = new Label(caret);
        clone.caretPositionProperty().addListener((observable, oldValue, newValue) -> cloneCaret.setText(caret + String.valueOf(newValue)));

        // set up label's displaying selection position
        String selection = "Selected Text: ";
        Label areaSelection = new Label(selection);
        area.selectedTextProperty().addListener(((observable, oldValue, newValue) -> areaSelection.setText(selection + newValue)));
        Label cloneSelection = new Label(selection);
        clone.selectedTextProperty().addListener(((observable, oldValue, newValue) -> cloneSelection.setText(selection + newValue)));

        // now that listeners are set up update the selection for clone
        int selectionStart = text.indexOf(selectedText);
        int selectionEnd   = selectionStart +  selectedText.length();
        clone.selectRange(selectionStart, selectionEnd);

        // set up Label's distinguishing which labels belong to which area.
        Label areaLabel = new Label("Original Area: ");
        Label cloneLabel = new Label("Cloned Area: ");

        // finish GUI
        GridPane grid = new GridPane();
        // add area content to first row
        grid.add(areaLabel, 0, 0);
        grid.add(areaCaret, 1, 0);
        grid.add(areaSelection, 2, 0);

        // add clone content to second row
        grid.add(cloneLabel, 0, 1);
        grid.add(cloneCaret, 1, 1);
        grid.add(cloneSelection, 2, 1);

        grid.setHgap(10);
        grid.setVgap(4);


        // set up Buttons that programmatically alter content area but not clone
        Button areaDelPrevChar = new Button("Area::deletePreviousChar");
        areaDelPrevChar.setOnAction((ae) -> area.deletePreviousChar() );

        Button areaUndo = new Button("Area::undo");
        areaUndo.setOnAction((ae) -> area.undo());
        Button areaRedo = new Button("Area::redo");
        areaRedo.setOnAction((ae) -> area.redo());

        Button cloneUndo = new Button("Clone::undo");
        cloneUndo.setOnAction((ae) -> clone.undo());
        Button cloneRedo = new Button("Clone::redo");
        cloneRedo.setOnAction((ae) -> clone.redo());

        HBox buttonBox = new HBox(areaDelPrevChar, areaUndo, areaRedo, cloneUndo, cloneRedo);

        BorderPane pane = new BorderPane();
        pane.setCenter(vbox);
        pane.setTop(grid);
        pane.setBottom(buttonBox);

        Scene scene = new Scene(pane, 800, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
