package org.fxmisc.richtext.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;

import org.fxmisc.richtext.InlineCssTextArea;

public class PopupDemo extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        InlineCssTextArea area = new InlineCssTextArea("Hello popup!");
        area.setWrapText(true);

        Popup popup = new Popup();
        popup.getContent().add(new Button("I am a popup button!"));
        area.setPopupAtCaret(popup);

        primaryStage.setScene(new Scene(new StackPane(area), 200, 200));
        primaryStage.show();
        popup.show(primaryStage);
    }
}
