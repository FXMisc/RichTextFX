package org.fxmisc.richtext.demo;

import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

import java.util.function.IntFunction;

public class LineNumberHighLightDemo extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        CodeArea codeArea = new CodeArea();
        codeArea.replaceText(0,0,"\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        codeArea.setPrefHeight(600);
        IntFunction<Node> arrowFactory = new LineNumberHighLightFactory( codeArea );

        codeArea.setParagraphGraphicFactory(arrowFactory);
        VBox vbox = new VBox();

        vbox.getChildren().addAll(codeArea);

        Scene scene = new Scene(vbox, 600, 600);
        primaryStage.setTitle("Line number highLight Demo");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    class LineNumberHighLightFactory implements IntFunction<Node> {

        private final ObservableValue<Integer> shownLines;

        public LineNumberHighLightFactory( CodeArea codeArea ) {
        	shownLines = codeArea.currentParagraphProperty();
            shownLines.addListener( (ob,oldValue,newValue) ->
            {
                codeArea.getParagraphGraphic( oldValue ).setStyle("-fx-text-fill: blue");
                codeArea.getParagraphGraphic( newValue ).setStyle("-fx-text-fill: red");
            });
        }
        @Override
        public Node apply(int lineIndex) {

            Label after = new Label();
            after.setText(String.valueOf(lineIndex+1));

            if ( lineIndex != shownLines.getValue() ) {
                after.setStyle("-fx-text-fill: blue");
            }
            else after.setStyle("-fx-text-fill: red");

            return after;
        }
    }
}
