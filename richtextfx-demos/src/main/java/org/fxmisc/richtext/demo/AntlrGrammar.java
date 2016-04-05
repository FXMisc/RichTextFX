package org.fxmisc.richtext.demo;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.fxmisc.richtext.ContextualHighlight;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StructuredTextArea;

/**
 * Created by Geoff on 3/30/2016.
 */
public class AntlrGrammar extends Application {

    private static final String initialText =
            "// notice that with ANTLR we can differentiate on a variables context\n" +
            "// declaration gets one style (underline), usage gets another (red)\n" +
            "var x = 2 + x ^ 3\n" +
            "var another = v -> 2 + 3 * v + x;\n" +
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
        ObservableList<ContextualHighlight> highlights = codeArea.getHighlights();

        highlights.add(new ContextualHighlight("Statement", "", "var", "var"));
        highlights.add(new ContextualHighlight("Variable", "Expr", "", "variable-use"));
        highlights.add(new ContextualHighlight("Variable", "Statement", "", "variable-decl"));
        highlights.add(new ContextualHighlight("Comment", "", "", "comment"));

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
