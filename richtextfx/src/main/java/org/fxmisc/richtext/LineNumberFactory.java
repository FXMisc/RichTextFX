package org.fxmisc.richtext;

import java.util.function.IntFunction;

import javafx.scene.Node;
import javafx.scene.control.Label;

import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;

public class LineNumberFactory implements IntFunction<Node> {

    private static final String STYLESHEET =
            LineNumberFactory.class.getResource("lineno.css").toExternalForm();

    public static IntFunction<Node> get(StyledTextArea<?> area) {
        return get(area, STYLESHEET);
    }

    public static IntFunction<Node> get(
            StyledTextArea<?> area,
            String customStylesheet) {
        return get(area, digits -> "%0" + digits + "d", customStylesheet);
    }

    public static IntFunction<Node> get(
            StyledTextArea<?> area,
            IntFunction<String> format) {
        return get(area, format, STYLESHEET);
    }

    public static IntFunction<Node> get(
            StyledTextArea<?> area,
            IntFunction<String> format,
            String customStylesheet) {
        return new LineNumberFactory(area, format, customStylesheet);
    }

    private final Val<Integer> nParagraphs;
    private final String stylesheet;
    private final IntFunction<String> format;

    private LineNumberFactory(
            StyledTextArea<?> area,
            IntFunction<String> format,
            String stylesheet) {
        nParagraphs = LiveList.sizeOf(area.getParagraphs());
        this.format = format;
        this.stylesheet = stylesheet;
    }

    @Override
    public Node apply(int idx) {
        Label lineNo = new Label();
        lineNo.getStyleClass().add("lineno");
        lineNo.getStylesheets().add(stylesheet);

        // When removed from the scene, bind label's text to constant "",
        // which is a fake binding that consumes no resources, instead of
        // staying bound to area's paragraphs.
        lineNo.textProperty().bind(Val.flatMap(
                lineNo.sceneProperty(),
                scene -> scene != null
                        ? nParagraphs.map(n -> format(idx+1, n))
                        : Val.constant("")));
        return lineNo;
    }

    private String format(int x, int max) {
        int digits = (int) Math.floor(Math.log10(max)) + 1;
        return String.format(format.apply(digits), x);
    }
}
