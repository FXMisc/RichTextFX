package org.fxmisc.richtext.demo;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;

import java.util.function.Consumer;
import java.util.function.Function;

public class ShowLineDemo extends Application {

    public static final int MIN_LINE_INDEX = 0;

    private class NumbersOnlyField extends TextField {

        NumbersOnlyField() {
            super();
            setOnKeyTyped(ke -> {
                String keyEventText = ke.getCharacter();
                if (Character.isDigit(keyEventText.charAt(0))) {
                    if (getSelectedText().isEmpty()) {
                        appendText(keyEventText);
                    } else {
                        replaceText(getSelection(), keyEventText);
                    }
                }
                ke.consume();
            });
        }

        public int getTextAsInt() {
            String text = getText();
            if (text.isEmpty()) {
                setText(String.valueOf(MIN_LINE_INDEX));
                return MIN_LINE_INDEX;
            } else {
                return Integer.parseInt(text);
            }
        }

    }

    private final NumbersOnlyField field = new NumbersOnlyField();
    {
        field.setPromptText("Input a number to indicate which line you want to show");
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        StringBuilder sb = new StringBuilder();
        int max = 100;
        for (int i = 0; i < max; i++) {
            sb.append("Line Index: ").append(i).append("\n");
        }
        sb.append("Line Index: ").append(max);
        InlineCssTextArea area = new InlineCssTextArea(sb.toString());
        VirtualizedScrollPane<InlineCssTextArea> vsPane = new VirtualizedScrollPane<>(area);

        Function<Integer, Integer> clamp = i -> Math.max(0, Math.min(i, area.getLength() - 1));
        Button showInViewportButton = createButton("Show line somewhere in Viewport", ae -> {
            area.showParagraphInViewport(clamp.apply(field.getTextAsInt()));
        });
        Button showAtViewportTopButton = createButton("Show line at top of viewport", ae -> {
            area.showParagraphAtTop(clamp.apply(field.getTextAsInt()));
        });
        Button showAtViewportBottomButton = createButton("Show line at bottom of viewport", ae -> {
            area.showParagraphAtBottom(clamp.apply(field.getTextAsInt()));
        });

        VBox vbox = new VBox(field, showInViewportButton, showAtViewportTopButton, showAtViewportBottomButton);
        vbox.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setCenter(vsPane);
        root.setBottom(vbox);

        Scene scene = new Scene(root, 700, 500);
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    private Button createButton(String text, Consumer<ActionEvent> handler) {
        Button b = new Button(text);
        b.setOnAction(ae -> {
            handler.accept(ae);
            field.requestFocus();
        });
        return b;
    }

}
