package codearea.control;

/**
 * A convenience subclass of {@link StyleClassedTextArea}
 * with fixed-width font by default.
 */
public class CodeArea extends StyleClassedTextArea {

    {
        getStyleClass().add("code-area");

        // load the default style that defines a fixed-width font
        getStylesheets().add(CodeArea.class.getResource("code-area.css").toExternalForm());
    }

    public CodeArea() {
        super();
    }

    public CodeArea(String text) {
        super(text);
    }

}
