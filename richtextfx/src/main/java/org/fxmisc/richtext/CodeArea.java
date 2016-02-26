package org.fxmisc.richtext;


/**
 * A convenience subclass of {@link StyleClassedTextArea}
 * with fixed-width font and an undo manager that observes
 * only plain text changes (not styled changes).
 *
 * <p>Note: either {@link CodeAreaModel} or {@link StyleClassedTextAreaModel} can be used with this view.
 * {@code CodeAreaModel} only provides some extra constructors to use in {@link AreaFactory}, such as
 * {@link AreaFactory#codeArea(String)}</p>.
 */
public class CodeArea<Model extends StyleClassedTextAreaModel> extends StyleClassedTextArea<Model> {

    {
        getStyleClass().add("code-area");

        // load the default style that defines a fixed-width font
        getStylesheets().add(CodeArea.class.getResource("code-area.css").toExternalForm());

        // don't apply preceding style to typed text
        setUseInitialStyleForInsertion(true);
    }

    public CodeArea(Model model) {
        super(model);
    }

}
