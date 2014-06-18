package org.fxmisc.richtext.skin;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicBinding;
import org.fxmisc.richtext.Paragraph;

class ParagraphBox<S> extends Region {
    private final ParagraphText<S> text;

    private final ObjectProperty<Supplier<? extends Node>> graphicFactory
            = new SimpleObjectProperty<>(null);
    public ObjectProperty<Supplier<? extends Node>> graphicFactoryProperty() {
        return graphicFactory;
    }

    private final MonadicBinding<Node> graphic
            = EasyBind.map(graphicFactory, f -> f != null ? f.get() : null);

    private final DoubleProperty wrapWidth = new SimpleDoubleProperty(Region.USE_COMPUTED_SIZE);
    public DoubleProperty wrapWidthProperty() { return wrapWidth; }
    {
        wrapWidth.addListener((obs, old, w) -> requestLayout());
    }

    public ParagraphBox(Paragraph<S> par, BiConsumer<Text, S> applyStyle) {
        this.text = new ParagraphText<>(par, applyStyle);
        getChildren().add(text);
        graphic.addListener((obs, oldG, newG) -> {
            if(oldG != null) {
                getChildren().remove(oldG);
            }
            if(newG != null) {
                getChildren().add(newG);
            }
        });
    }

    public ParagraphText<S> getText() {
        return text;
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets insets = getInsets();
        return wrapWidth.get() == Region.USE_COMPUTED_SIZE
                ? graphicWidth() + text.prefWidth(-1) + insets.getLeft() + insets.getRight()
                : 0; // return 0, ListView will size it to its width anyway
    }

    @Override
    protected double computePrefHeight(double ignoredWidth) {
        Insets insets = getInsets();
        double textHeight = wrapWidth.get() == Region.USE_COMPUTED_SIZE
                ? text.prefHeight(-1)
                : text.prefHeight(wrapWidth.get() - insets.getLeft() - insets.getRight() - graphicWidth());
        return textHeight + insets.getTop() + insets.getBottom();
    }

    private double graphicWidth() {
        return graphic.getOpt().map(g -> g.prefWidth(-1)).orElse(0.0);
    }

    @Override
    protected
    void layoutChildren() {
        Bounds bounds = getLayoutBounds();
        double w = bounds.getWidth();
        double h = bounds.getHeight();
        double graphicWidth = graphicWidth();
        text.resizeRelocate(graphicWidth, 0, w - graphicWidth, h);

        graphic.ifPresent(g -> {
            g.resizeRelocate(0, 0, graphicWidth, h);
        });
    }
}
