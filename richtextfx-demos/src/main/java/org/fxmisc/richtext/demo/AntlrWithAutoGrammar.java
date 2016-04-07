package org.fxmisc.richtext.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.antlr.ErrorUnderlineHighlighter;
import org.fxmisc.richtext.antlr.LexicalBracketCountingHighlighter;
import org.fxmisc.richtext.antlr.StructuredTextArea;

/**
 * Created by Geoff on 3/30/2016.
 */
public class AntlrWithAutoGrammar extends Application {

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
        codeArea.setImplicitTerminalStyle(true);
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getLexerListeners().add(new LexicalBracketCountingHighlighter());
        codeArea.getSemanticListeners().add(new ErrorUnderlineHighlighter());

        codeArea.replaceText(0, 0, initialText);
        codeArea.setPrefHeight(200);
        codeArea.setPrefWidth(600);

        Scene scene = new Scene(codeArea);
        scene.getStylesheets().add(getClass().getResource("javish-math-auto.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("ANTLR Grammar Demo");
        primaryStage.show();
    }

}
