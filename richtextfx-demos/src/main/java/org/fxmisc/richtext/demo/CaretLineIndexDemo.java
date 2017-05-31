package org.fxmisc.richtext.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.value.Val;

import java.util.Objects;

public class CaretLineIndexDemo extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 15; i++) {  // lines
            for (int j = 0; j < 100; j++) { // content on each line
                sb.append(j).append(" ");
            }
            sb.append("\n");
        }
        InlineCssTextArea area = new InlineCssTextArea(sb.toString());
        area.setWrapText(true);
        area.setParagraphGraphicFactory(LineNumberFactory.get(area));
        VirtualizedScrollPane<InlineCssTextArea> vspane = new VirtualizedScrollPane<>(area);

        Label inParagraphLabel = new Label("");
        Val.create(area::getCaretLineIndexInParagraph, area.caretPositionProperty())
                .values()
                .filter(Objects::nonNull)
                .map(v -> "Line Index in Paragraph is: " + String.valueOf(v))
                .feedTo(inParagraphLabel.textProperty());

        Label inViewportLabel = new Label("");
        Val.create(area::getCaretLineIndexInViewport, area.caretPositionProperty())
                .values()
                .filter(Objects::nonNull)
                .map(v -> "Line Index in Viewport is: " + String.valueOf(v))
                .feedTo(inViewportLabel.textProperty());

        Label instructions = new Label("Move the caret around with your mouse or keyboard arrows and see the values update.");
        instructions.setWrapText(true);

        BorderPane root = new BorderPane();
        root.setTop(instructions);
        root.setCenter(vspane);
        root.setBottom(new VBox(inParagraphLabel, inViewportLabel));

        Scene scene = new Scene(root, 500, 500);
        primaryStage.setScene(scene);
        primaryStage.show();

        area.moveTo(0);
        area.requestFollowCaret();
    }
}
