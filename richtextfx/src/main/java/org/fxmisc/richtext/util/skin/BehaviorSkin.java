package org.fxmisc.richtext.util.skin;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.control.Control;
import javafx.scene.control.SkinBase;

public final class BehaviorSkin<C extends Control, V extends Visual> extends SkinBase<C> {
    private final V visual;
    private final Behavior behavior;

    public BehaviorSkin(
            C control,
            Function<? super C, ? extends V> visualFactory,
            BiFunction<? super C, ? super V, Behavior> behaviorFactory) {
        super(control);
        this.visual = visualFactory.apply(control);
        this.behavior = behaviorFactory.apply(control, visual);
        getChildren().add(visual.getNode());
    }

    @Override
    public void dispose() {
        behavior.dispose();
        visual.dispose();
    }


    @Override
    public final List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return visual.getCssMetaData();
    }
}
