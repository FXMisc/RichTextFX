package org.fxmisc.richtext.demo;

import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.MouseOverTextEvent;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.antlr.StructuredTextArea;

import java.time.Duration;

public class AntlrWithErrorHoverOver extends Application {

    private static final String initialText =
            "// notice that with ANTLR we can differentiate on a variables context\n" +
                    "// declaration gets one style (underline), usage gets another (red)\n" +
                    "var x = 2 + x ^ (3 + (10 + 12));\n" +
                    "var another = v -> 2 + 3 * v + x;\n" +
                    "var err + x;\n" +
                    "// and of course, with arbitrary text like comments, we can set the style.";

    public static void main(String[] args) {
        launch(args);
    }

    @Override public void start(Stage primaryStage) throws Exception {

        StructuredTextArea codeArea = new StructuredTextArea(
                "org.fxmisc.richtext.parser.JavishMathParser",
                "org.fxmisc.richtext.parser.JavishMathLexer",
                "block"
        );
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.setImplicitTerminalStyle(true);
        codeArea.getStylesheets().add(getClass().getResource("antlrd-area.css").toExternalForm());
        codeArea.getStyleClass().add("code-area");

        Popup popup = new Popup();
        Label popupMsg = new Label();
        popupMsg.setStyle(
                "-fx-background-color: black;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 5;");
        popup.getContent().add(popupMsg);

        codeArea.setMouseOverTextDelay(Duration.ofMillis(200));
        codeArea.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN, e -> {
            int chIdx = e.getCharacterIndex();
            StructuredTextArea.ParseError errorAtIndex = codeArea.getErrorsByCharIndex().get(chIdx);
            if(errorAtIndex == null){ return; }

            Point2D pos = e.getScreenPosition();
            popupMsg.setText(errorAtIndex.getMessage());
            popup.show(codeArea, pos.getX(), pos.getY() + 10);
        });
        codeArea.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_END, e -> {
            popup.hide();
        });


        codeArea.replaceText(0, 0, initialText);
        codeArea.setPrefHeight(200);
        codeArea.setPrefWidth(600);

        Scene scene = new Scene(codeArea);
        scene.getStylesheets().add(getClass().getResource("javish-math.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("ANTLR Grammar Demo");
        primaryStage.show();
    }
}