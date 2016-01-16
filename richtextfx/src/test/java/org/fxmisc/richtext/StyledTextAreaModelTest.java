package org.fxmisc.richtext;

import org.junit.Test;

public class StyledTextAreaModelTest {

    class BoldUnderline {
        boolean styled;
        String style = "-fx-font-weight: bold; -fx-underline: true;";
        String toCss() { return (styled) ? style : ""; }

        BoldUnderline(boolean styled) {
            this.styled = styled;
        }
    }

    @Test
    public void testForBug216() {
        BoldUnderline initialStyle = new BoldUnderline(false);
        StyledTextAreaModel<BoldUnderline, String> model = new StyledTextAreaModel<BoldUnderline, String>(
                initialStyle, (text, style) -> text.setStyle(style.toCss()),
                "", (paragraph, style) -> paragraph.setStyle(""),
                new EditableStyledDocument<>(initialStyle, ""),
                true
        );
        // set up text
        model.replaceText(0, 0, "testtest");

        // style first and second strings
        model.setStyle(0, 4, new BoldUnderline(true));
        model.setStyle(5, 8, new BoldUnderline(true));

        // add a space styled by initialStyle
        model.positionCaret("test".length());
        model.setUseInitialStyleForInsertion(true);
        model.replaceText(model.getCaretPosition(), model.getCaretPosition(), " ");
        model.setUseInitialStyleForInsertion(false);

        // add more spaces
        model.insertText(model.getCaretPosition(), " ");
        model.insertText(model.getCaretPosition(), " ");

        // undo. Redo should throw IllegalArgumentException
        model.undo();
        model.redo();
    }
}
