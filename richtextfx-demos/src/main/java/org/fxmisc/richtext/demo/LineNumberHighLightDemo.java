package org.fxmisc.richtext.demo;

import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.reactfx.value.Val;

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
        }
        @Override
        public Node apply(int lineIndex) {

            Label after = new Label();
            after.setText(String.valueOf(lineIndex+1));

            // While this works and is the method used in demo.lineindicator.ArrowFactory it isn't
            // very efficient as this will execute for EVERY line / label whenever the current
            // paragraph property changes. The next commit will show how this can be done better.
            ObservableValue<Boolean> visible = Val.map(shownLines, sl -> {
                boolean contains = sl.equals(lineIndex);

                if (contains) {
                	after.setStyle("-fx-text-fill: red");
                } else {
                	after.setStyle("-fx-text-fill: blue");
                }
                return true;
            });
            after.visibleProperty().bind(
                    Val.flatMap(after.sceneProperty(), scene -> {
                        if (scene != null) {
                            return visible;
                        }
                        return Val.constant(false);
                    }));

            return after;
        }
    }
}
