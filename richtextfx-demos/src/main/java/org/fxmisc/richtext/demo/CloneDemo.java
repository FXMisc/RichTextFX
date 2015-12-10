package org.fxmisc.richtext.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
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
        StyledTextArea<String, String> clone = area.createClone();

        printAreaCarePosition("area", area);
        printAreaCarePosition("clone", clone);

        area.positionCaret(area.getLength());
        printAreaCarePosition("area", area);
        printAreaCarePosition("clone", clone);

        clone.replaceText(0, clone.getLength(), "");
        printAreaCarePosition("area", area);
        printAreaCarePosition("clone", clone);

        area.replaceText(0, 0, text);
        printAreaCarePosition("area", area);
        printAreaCarePosition("clone", clone);

        VBox vbox = new VBox(area, clone);
        vbox.setSpacing(10);
        StackPane pane = new StackPane(vbox);
        Scene scene = new Scene(pane, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    void printAreaCarePosition(String areaName, StyledTextArea area) {
        System.out.println("Caret position of " + areaName + ": " + String.valueOf(area.getCaretPosition()));
    }

}
