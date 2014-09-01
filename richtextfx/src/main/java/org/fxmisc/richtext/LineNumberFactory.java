package org.fxmisc.richtext;

import java.util.function.IntFunction;

import javafx.scene.Node;
import javafx.scene.control.Label;

import org.reactfx.EventStream;
import org.reactfx.EventStreams;

public class LineNumberFactory implements IntFunction<Node> {

    private static final String STYLESHEET = LineNumberFactory.class.getResource("lineno.css").toExternalForm();

    public static IntFunction<Node> get(StyledTextArea<?> area) {
        return new LineNumberFactory(area);
    }

    private final EventStream<Integer> nParagraphs;

    private LineNumberFactory(StyledTextArea<?> area) {
        nParagraphs = EventStreams.sizeOf(area.getParagraphs());
    }

    @Override
    public Node apply(int idx) {
        Label lineNo = new Label();
        lineNo.getStyleClass().add("lineno");
        lineNo.getStylesheets().add(STYLESHEET);

        // When removed from the scene, stay subscribed to never(), which is
        // a fake subscription that consumes no resources, instead of staying
        // subscribed to area's paragraphs.
        EventStreams.valuesOf(lineNo.sceneProperty())
                .flatMap(scene -> scene != null
                        ? nParagraphs.map(n -> format(idx+1, n))
                        : EventStreams.<String>never())
                .feedTo(lineNo.textProperty());
        return lineNo;
    }

    private String format(int x, int max) {
        int digits = (int) Math.floor(Math.log10(max)) + 1;
        return String.format(" %" + digits + "d ", x);
    }
}
