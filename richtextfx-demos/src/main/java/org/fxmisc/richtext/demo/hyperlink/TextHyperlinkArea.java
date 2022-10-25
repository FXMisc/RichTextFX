package org.fxmisc.richtext.demo.hyperlink;

import javafx.geometry.VPos;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.TextOps;
import org.reactfx.util.Either;

import java.util.Optional;
import java.util.function.Consumer;

public class TextHyperlinkArea extends GenericStyledArea<Void, Either<String, Hyperlink>, TextStyle> {

    private static final TextOps<String, TextStyle> STYLED_TEXT_OPS = SegmentOps.styledTextOps();
    private static final HyperlinkOps<TextStyle> HYPERLINK_OPS = new HyperlinkOps<>();

    private static final TextOps<Either<String, Hyperlink>, TextStyle> EITHER_OPS = STYLED_TEXT_OPS._or(HYPERLINK_OPS, (s1, s2) -> Optional.empty());

    public TextHyperlinkArea(Consumer<String> showLink) {
        super(
                null,
                (t, p) -> {},
                TextStyle.EMPTY,
                EITHER_OPS,
                e -> e.getSegment().unify(
                        text ->
                            createStyledTextNode(t -> {
                                t.setText(text);
                                t.setStyle(e.getStyle().toCss());
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
                Either.right(new Hyperlink(displayedText, displayedText, link)),
                null,
                TextStyle.EMPTY,
                EITHER_OPS
        ));
    }

    public static TextExt createStyledTextNode(Consumer<TextExt> applySegment) {
        TextExt t = new TextExt();
        t.setTextOrigin(VPos.TOP);
        applySegment.accept(t);
        return t;
    }
}
