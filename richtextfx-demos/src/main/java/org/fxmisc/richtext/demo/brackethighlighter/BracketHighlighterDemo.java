package org.fxmisc.richtext.demo.brackethighlighter;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;

public class BracketHighlighterDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        // create a new code area
        CustomCodeArea codeArea = new CustomCodeArea();

        // highlight brackets
        BracketHighlighter bracketHighlighter = new BracketHighlighter(codeArea);

        // initialize and show scene
        Scene scene = new Scene(new StackPane(new VirtualizedScrollPane<>(codeArea)), 600, 400);
        scene.getStylesheets().add(BracketHighlighterDemo.class.getResource("bracket-highlighter.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Bracket Highlighter Demo");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
