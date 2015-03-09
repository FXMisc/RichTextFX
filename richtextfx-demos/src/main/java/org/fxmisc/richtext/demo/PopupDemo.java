package org.fxmisc.richtext.demo;

import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;

import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.PopupAlignment;

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
        area.setPopupWindow(popup);
        area.setPopupAlignment(PopupAlignment.SELECTION_BOTTOM_CENTER);
        area.setPopupAnchorOffset(new Point2D(4, 4));

        Button toggle = new Button("Show/Hide popup");
        toggle.setOnAction(ae -> {
            if(popup.isShowing()) {
                popup.hide();
            } else {
                popup.show(primaryStage);
            }
        });

        VBox.setVgrow(area, Priority.ALWAYS);

        primaryStage.setScene(new Scene(new VBox(toggle, area), 200, 200));
        primaryStage.setTitle("Popup Demo");
        primaryStage.show();
        popup.show(primaryStage);
    }
}
