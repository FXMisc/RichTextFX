package org.fxmisc.richtext;

import java.util.function.BiConsumer;

import javafx.beans.NamedArg;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.text.TextFlow;

import org.fxmisc.richtext.model.EditableStyledDocument;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.SimpleEditableStyledDocument;
import org.fxmisc.richtext.model.StyledText;

/**
 * Text editing control. Accepts user input (keyboard, mouse) and
 * provides API to assign style to text ranges. It is suitable for
 * syntax highlighting and rich-text editors.
 *
 * <p>Subclassing is allowed to define the type of style, e.g. inline
 * style or style classes.</p>
 *
 * <p>Note: Scroll bars no longer appear when the content spans outside
 * of the viewport. To add scroll bars, the area needs to be wrapped in
 * a {@link org.fxmisc.flowless.VirtualizedScrollPane}. For example, </p>
 * <pre>
 * {@code
 * // shows area without scroll bars
 * InlineCssTextArea area = new InlineCssTextArea();
 *
 * // add scroll bars that will display as needed
 * VirtualizedScrollPane<InlineCssTextArea> vsPane = new VirtualizedScrollPane(area);
 *
 * Parent parent = //;
 * parent.getChildren().add(vsPane)
 * }
 * </pre>
 *
 * <h3>Overriding keyboard shortcuts</h3>
 *
 * {@code StyledTextArea} uses {@code KEY_TYPED} handler to handle ordinary
 * character input and {@code KEY_PRESSED} handler to handle control key
 * combinations (including Enter and Tab). To add or override some keyboard
 * shortcuts, while keeping the rest in place, you would combine the default
 * event handler with a new one that adds or overrides some of the default
 * key combinations. This is how to bind {@code Ctrl+S} to the {@code save()}
 * operation:
 * <pre>
 * {@code
 * import static javafx.scene.input.KeyCode.*;
 * import static javafx.scene.input.KeyCombination.*;
 * import static org.fxmisc.wellbehaved.event.EventPattern.*;
 * import static org.fxmisc.wellbehaved.event.InputMap.*;
 *
 * import org.fxmisc.wellbehaved.event.Nodes;
 *
 * Nodes.addInputMap(area, consume(keyPressed(S, CONTROL_DOWN), event -> save()));
 * }
 * </pre>
 *
 * @param <S> type of style that can be applied to text.
 */
public class StyledTextArea<PS, S> extends GenericStyledArea<PS, StyledText<S>, S> {

    public StyledTextArea(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
                          @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
                          @NamedArg("initialTextStyle")      S initialTextStyle,
                          @NamedArg("applyStyle")            BiConsumer<? super TextExt, S> applyStyle,
                          @NamedArg("document")              EditableStyledDocument<PS, StyledText<S>, S> document,
                          @NamedArg("preserveStyle")         boolean preserveStyle) {
        super(initialParagraphStyle, applyParagraphStyle,
              initialTextStyle,
              document, StyledText.textOps(), preserveStyle,
              seg -> createStyledTextNode(seg, StyledText.textOps(), applyStyle));
    }

    public StyledTextArea(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
                          @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
                          @NamedArg("initialTextStyle")      S initialTextStyle,
                          @NamedArg("applyStyle")            BiConsumer<? super TextExt, S> applyStyle,
                          @NamedArg("document")              EditableStyledDocument<PS, StyledText<S>, S> document) {
        this(initialParagraphStyle, applyParagraphStyle,
             initialTextStyle, applyStyle, document, true);
    }

    public StyledTextArea(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
                          @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
                          @NamedArg("initialTextStyle")      S initialTextStyle,
                          @NamedArg("applyStyle")            BiConsumer<? super TextExt, S> applyStyle,
                          @NamedArg("preserveStyle")         boolean preserveStyle) {
        this(
                initialParagraphStyle,
                applyParagraphStyle,
                initialTextStyle,
                applyStyle,
                new SimpleEditableStyledDocument<>(initialParagraphStyle, initialTextStyle),
                preserveStyle);
    }

    public StyledTextArea(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
                          @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
                          @NamedArg("initialTextStyle")      S initialTextStyle,
                          @NamedArg("applyStyle")            BiConsumer<? super TextExt, S> applyStyle) {
        this(initialParagraphStyle, applyParagraphStyle,
             initialTextStyle, applyStyle, true);
    }

    public static <S> Node createStyledTextNode(StyledText<S> seg, SegmentOps<StyledText<S>, S> segOps,
            BiConsumer<? super TextExt, S> applyStyle) {

        TextExt t = new TextExt(segOps.getText(seg));
        t.setTextOrigin(VPos.TOP);
        t.getStyleClass().add("text");
        applyStyle.accept(t, segOps.getStyle(seg));

        // XXX: binding selectionFill to textFill,
        // see the note at highlightTextFill
        t.impl_selectionFillProperty().bind(t.fillProperty());
        return t;
    }
}
