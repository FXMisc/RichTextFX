package com.test.chatgpt;

import javafx.application.Application;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.value.Val;

import java.util.function.IntFunction;

public class LineNumberHighLightApplication extends Application {
    CodeArea codeArea;
    public static final ObservableList<Integer> olistValue = FXCollections.observableArrayList();
    public static final ListProperty<Integer> listValue = new SimpleListProperty<Integer>(olistValue);


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        codeArea = new CodeArea();
        codeArea.replaceText(0,0,"\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        codeArea.setPrefHeight(600);
        olistValue.add(codeArea.getCurrentParagraph() + 1);
        IntFunction<Node> numberFactory = LineNumberFactory.get(codeArea);
        IntFunction<Node> arrowFactory = new LineNumberHighLightFactory(listValue, numberFactory);

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

        private final ListProperty<Integer> shownLines;

        IntFunction<Node> numberFactory;

        public LineNumberHighLightFactory(ListProperty<Integer> shownLine, IntFunction<Node> numberFactory) {
            this.numberFactory = numberFactory;
            this.shownLines = shownLine;
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

            codeArea.currentParagraphProperty().addListener(new ChangeListener<Integer>() {
                @Override
                public void changed(ObservableValue<? extends Integer> observableValue, Integer integer, Integer t1) {
                    olistValue.clear();
                    olistValue.add(t1+1);
                }
            });
            stackPane.getChildren().addAll(before, after);
            ObservableValue<Boolean> visible = Val.map(shownLines, sl -> {
                boolean contains = sl.contains(lineIndex + 1);

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
