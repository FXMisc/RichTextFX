package org.fxmisc.richtext.demo;

import javafx.application.Application;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.reactfx.value.Val;

import java.util.function.IntFunction;

public class LineNumberHighLightDemo extends Application {
    CodeArea codeArea;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        codeArea = new CodeArea();
        codeArea.replaceText(0,0,"\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        codeArea.setPrefHeight(600);
        IntFunction<Node> arrowFactory = new LineNumberHighLightFactory();

        IntFunction<Node> graphicFactory = line -> {
            HBox hbox = new HBox(arrowFactory.apply(line));
            hbox.setAlignment(Pos.CENTER_LEFT);

            return hbox;
        };
        codeArea.setParagraphGraphicFactory(graphicFactory);
        VBox vbox = new VBox();

        vbox.getChildren().addAll(codeArea);

        Scene scene = new Scene(vbox, 600, 600);
        primaryStage.setTitle("Line number highLight Demo");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    class LineNumberHighLightFactory implements IntFunction<Node> {

        private final IntegerProperty shownLines = new SimpleIntegerProperty();

        public LineNumberHighLightFactory() {
        	shownLines.set( codeArea.getCurrentParagraph() + 1 );
	        codeArea.currentParagraphProperty().addListener( 
	            (observableValue, integer, t1) -> shownLines.set(t1+1)
            );
        }
        @Override
        public Node apply(int lineIndex) {
            StackPane stackPane = new StackPane();

            Label before =  new Label();
            before.setText(String.valueOf(lineIndex));
            before.setStyle("-fx-text-fill: blue");
            Label after = new Label();
            after.setText(String.valueOf(lineIndex));
            after.setStyle("-fx-text-fill: red");

            stackPane.getChildren().addAll(before, after);
            ObservableValue<Boolean> visible = Val.map(shownLines, sl -> {
                boolean contains = sl.equals(lineIndex + 1);

                if (contains) {
                    before.setVisible(false);
                } else {
                    before.setVisible(true);
                }
                return contains;
            });
            after.visibleProperty().bind(
                    Val.flatMap(after.sceneProperty(), scene -> {
                        if (scene != null) {
                            return visible;
                        }
                        return Val.constant(false);
                    }));

            return stackPane;
        }
    }
}
