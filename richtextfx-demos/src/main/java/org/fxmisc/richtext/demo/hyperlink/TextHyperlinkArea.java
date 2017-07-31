package org.fxmisc.richtext.demo.hyperlink;

import javafx.geometry.VPos;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.StyledText;
import org.fxmisc.richtext.model.TextOps;
import org.reactfx.util.Either;

import java.util.function.Consumer;

public class TextHyperlinkArea extends GenericStyledArea<Void, Either<StyledText<TextStyle>, Hyperlink<TextStyle>>, TextStyle> {

    private static final TextOps<StyledText<TextStyle>, TextStyle> STYLED_TEXT_OPS = StyledText.textOps();
    private static final HyperlinkOps<TextStyle> HYPERLINK_OPS = new HyperlinkOps<>();
    private static final TextOps<Either<StyledText<TextStyle>, Hyperlink<TextStyle>>, TextStyle> EITHER_OPS = STYLED_TEXT_OPS._or(HYPERLINK_OPS);

    public TextHyperlinkArea(Consumer<String> showLink) {
        super(
                null,
                (t, p) -> {},
                TextStyle.EMPTY,
                EITHER_OPS,
                e -> e.unify(
                        styledText ->
                            createStyledTextNode(t -> {
                                t.setText(styledText.getText());
                                t.setStyle(styledText.getStyle().toCss());
                        }),
                        hyperlink ->
                            createStyledTextNode(t -> {
                                if (hyperlink.isReal()) {
                                    t.setText(hyperlink.getDisplayedText());
                                    t.getStyleClass().add("hyperlink");
                                    t.setOnMouseClicked(ae -> showLink.accept(hyperlink.getLink()));
                                }
                        })
                )
        );

        getStyleClass().add("text-hyperlink-area");
        getStylesheets().add(TextHyperlinkArea.class.getResource("text-hyperlink-area.css").toExternalForm());
    }

    public void appendWithLink(String displayedText, String link) {
        replaceWithLink(getLength(), getLength(), displayedText, link);
    }

    public void replaceWithLink(int start, int end, String displayedText, String link) {
        replace(start, end, ReadOnlyStyledDocument.fromSegment(
                Either.right(new Hyperlink<>(displayedText, displayedText, TextStyle.EMPTY, link)),
                null,
                TextStyle.EMPTY,
                EITHER_OPS
        ));
    }

    public static TextExt createStyledTextNode(Consumer<TextExt> applySegment) {
        TextExt t = new TextExt();
        t.setTextOrigin(VPos.TOP);
        applySegment.accept(t);

        // XXX: binding selectionFill to textFill,
        // see the note at highlightTextFill
        t.impl_selectionFillProperty().bind(t.fillProperty());
        return t;
    }
}
