/**
 * This package contains scaffolding for {@link javafx.scene.control.Skin}
 * implementations that separates the view and controller aspects of the skin.
 * The view is represented by {@link Visual} and the controller is represented
 * by {@link Behavior}. The architecture is designed such that the Visual (view)
 * may only hold a reference to the Control (model), while the Behavior
 * (controller) may hold a reference to both the Control and the Visual. In
 * other words, the view can observe only the model, while the controller can
 * observe both the model and the view, and can modify the model.
 *
 * <p>Once you have the implementation of the Visual and the Behavior, you use
 * one of the factory methods from {@link Skins} to create a Skin instance.
 */
package org.fxmisc.wellbehaved.skin;