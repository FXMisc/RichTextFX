package org.fxmisc.wellbehaved.skin;

import java.util.List;
import java.util.function.Function;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;

/**
 * Provides factory methods to wire a {@link Visual} and a {@link Behavior} to
 * form a {@link Skin}. The factory methods are meant to be used to implement
 * the {@link Control#createDefaultSkin()} method.
 */
public final class Skins {

    /**
     * Creates a skin whose visual consists of a single node. The returned skin
     * attaches that node to the control and takes care of removing it from the
     * control on disposal.
     *
     * The intended usage is
     *
     * <pre>
     * {@code
     * protected Skin<?> createDefaultSkin() {
     *     return Skins.createSimpleSkin(
     *             this,
     *             control -> new FooVisual<>(control),
     *             (control, visual) -> new FooBehavior(control, visual));
     * }
     * }
     * </pre>
     *
     * or, more concisely
     *
     * <pre>
     * {@code
     * protected Skin<?> createDefaultSkin() {
     *     return Skins.createSimpleSkin(this, FooVisual::new, FooBehavior::new);
     * }
     * }
     * </pre>
     *
     * @param control control for which the skin is going to be created.
     * @param visualFactory function to create the Visual, given the control.
     * @param behaviorFactory function to create the Behavior, given the Visual.
     * @return a Skin that delegates the view aspect to the Visual and the
     * controller aspect to the Behavior.
     */
    public static <C extends Control, V extends SimpleVisualBase<? super C>> Skin<C> createSimpleSkin(
            C control,
            Function<? super C, ? extends V> visualFactory,
            Function<? super V, ? extends Behavior> behaviorFactory) {

        return new SkinBase<C>(control) {
            private final V visual = visualFactory.apply(control);
            private final Behavior behavior = behaviorFactory.apply(visual);
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

    /**
     * Creates a skin whose visual is in direct control of managing the
     * control's child list.
     *
     * The intended usage is
     *
     * <pre>
     * {@code
     * protected Skin<?> createDefaultSkin() {
     *     return Skins.createComplexSkin(
     *             this,
     *             control -> new FooVisual<>(control),
     *             (control, visual) -> new FooBehavior(control, visual));
     * }
     * }
     * </pre>
     *
     * or, more concisely
     *
     * <pre>
     * {@code
     * protected Skin<?> createDefaultSkin() {
     *     return Skins.createComplexSkin(this, FooVisual::new, FooBehavior::new);
     * }
     * }
     * </pre>
     *
     * @param control control for which the skin is going to be created.
     * @param visualFactory function to create the Visual, given the control.
     * @param behaviorFactory function to create the Behavior, given the Visual.
     * @return a Skin that delegates the view aspect to the Visual and the
     * controller aspect to the Behavior.
     */
    public static <C extends Control, V extends ComplexVisualBase<C>> Skin<C> createComplexSkin(
            C control,
            Function<? super C, ? extends V> visualFactory,
            Function<? super V, ? extends Behavior> behaviorFactory) {

        return new SkinBase<C>(control) {
            private final V visual = visualFactory.apply(control);
            private final Behavior behavior = behaviorFactory.apply(visual);

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
