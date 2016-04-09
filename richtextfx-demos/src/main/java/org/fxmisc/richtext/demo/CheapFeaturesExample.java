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
import org.fxmisc.richtext.antlr.LexicalBracketCountingHighlighter;
import org.fxmisc.richtext.antlr.StructuredTextArea;

import java.time.Duration;

public class CheapFeaturesExample extends Application {

    private static final String initialText =
            "// This demo shows some of the ~free features of this library\n" +
            "\n" +
            "var x = 2 + x ^ (3 + (10 + 12)); //click on the brackets\n" +
            "// note the corresponding closing bracket is highlighted\n" +
            "var another = v -> 2 + 3 * v + x;\n " +
            "// note here variables are highlighted in purple\n" +
            "var err + x; // error highlights!\n" +
            "// try mousing-over the red text\n" +
            "\n" +
            "// and of course, with arbitrary text like comments, we can set the style.\n" +
            "// these features all operate with fairly basic ANTLR facilities, (lexer & error handlers)\n" +
            "// substantially more elaborate features can be added with parser highlighters!\n" +
            "\n" +
            "// many thanks to Tomas Mikula for his excellent library\n" +
            "// and ofc Terrance Parr and Sam Harwell wrote the superb ANTLR!\n";

    public static void main(String[] args) {
        launch(args);
    }

    @Override public void start(Stage primaryStage) throws Exception {

        StructuredTextArea codeArea = new StructuredTextArea(
                "org.fxmisc.richtext.parser.JavishMathParser",
                "org.fxmisc.richtext.parser.JavishMathLexer",
                "block"
        );
        codeArea.getStylesheets().add(getClass().getResource("antlrd-area.css").toExternalForm());
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        //implicitly map from token names to css styles
        codeArea.setImplicitTerminalStyle(true);

        //implicitly highlight parser errors with a red-underline.
        codeArea.setImplicitErrorStyle(true);

        //add bracket highlighter
        codeArea.getLexerListeners().add(new LexicalBracketCountingHighlighter("(", ")", "bracket"));

        //hover over with ANTLR generated error message
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
        codeArea.setPrefHeight(230);
        codeArea.setPrefWidth(600);

        Scene scene = new Scene(codeArea);
        scene.getStylesheets().add(getClass().getResource("javish-math.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("ANTLR Grammar Demo");
        primaryStage.show();
    }
}