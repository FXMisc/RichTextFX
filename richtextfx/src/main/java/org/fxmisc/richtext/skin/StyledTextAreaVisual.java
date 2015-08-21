package org.fxmisc.richtext.skin;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.wellbehaved.skin.SimpleVisualBase;

import java.util.List;
import java.util.function.BiConsumer;

public class StyledTextAreaVisual<S, PS> extends SimpleVisualBase<StyledTextArea<S, PS>> {
    private final StyledTextAreaView<S, PS> node;

    public StyledTextAreaVisual(StyledTextArea<S, PS> control,
                                BiConsumer<Text, S> applyStyle,
                                PS initialParagraphStyle,
                                BiConsumer<TextFlow, PS> applyParagraphStyle) {
        super(control);
        this.node = new StyledTextAreaView<>(control, applyStyle, initialParagraphStyle, applyParagraphStyle);
    }

    @Override
    public void dispose() {
        node.dispose();
    }

    @Override
    public StyledTextAreaView<S, PS> getNode() {
        return node;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return node.getCssMetaData();
    }
}