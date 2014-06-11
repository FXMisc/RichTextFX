package org.fxmisc.richtext.util.skin;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;

public final class Skins {

    public static <C extends Control, V extends SimpleVisual> Skin<C> simpleSkin(
            C control,
            Function<? super C, ? extends V> visualFactory,
            BiFunction<? super C, ? super V, ? extends Behavior> behaviorFactory) {

        return new SkinBase<C>(control) {
            private final V visual = visualFactory.apply(control);
            private final Behavior behavior = behaviorFactory.apply(control, visual);
            private final Node node = visual.getNode();
            {
                getChildren().add(node);
            }

            @Override
            public void dispose() {
                behavior.dispose();
                visual.dispose();
                getChildren().remove(node);
            }


            @Override
            public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
                return visual.getCssMetaData();
            }
        };
    }

    public static <C extends Control, V extends ComplexVisualBase<C>> Skin<C> complexSkin(
            C control,
            Function<? super C, ? extends V> visualFactory,
            BiFunction<? super C, ? super V, ? extends Behavior> behaviorFactory) {

        return new SkinBase<C>(control) {
            private final V visual = visualFactory.apply(control);
            private final Behavior behavior = behaviorFactory.apply(control, visual);

            @Override
            public void dispose() {
                behavior.dispose();
                visual.dispose();
            }

            @Override
            public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
                return visual.getCssMetaData();
            }
        };
    }
}
