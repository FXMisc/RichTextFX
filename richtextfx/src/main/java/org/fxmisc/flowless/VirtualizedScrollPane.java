package org.fxmisc.flowless;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;

public class VirtualizedScrollPane extends Region {
    private final Node content;
    private final ScrollBar hbar;
    private final ScrollBar vbar;

    public <C extends Node & Virtualized> VirtualizedScrollPane(C content) {
        this.content = content;

        // create scrollbars
        hbar = new ScrollBar();
        vbar = new ScrollBar();
        vbar.setOrientation(Orientation.VERTICAL);

        // scrollbar ranges
        hbar.setMin(0);
        vbar.setMin(0);
        hbar.maxProperty().bind(content.totalWidthEstimateProperty());
        vbar.maxProperty().bind(content.totalHeightEstimateProperty());

        // scrollbar increments
        setupUnitIncrement(hbar);
        setupUnitIncrement(vbar);
        hbar.blockIncrementProperty().bind(hbar.visibleAmountProperty());
        vbar.blockIncrementProperty().bind(vbar.visibleAmountProperty());

        // scrollbar positions
        hbar.setValue(content.getHorizontalPosition());
        vbar.setValue(content.getVerticalPosition());
        content.horizontalPositionProperty().addListener(
                obs -> hbar.setValue(content.getHorizontalPosition()));
        content.verticalPositionProperty().addListener(
                obs -> vbar.setValue(content.getVerticalPosition()));

        // scroll content by scrollbars
        hbar.valueProperty().addListener((obs, old, pos) ->
                content.setHorizontalPosition(pos.doubleValue()));
        vbar.valueProperty().addListener((obs, old, pos) ->
                content.setVerticalPosition(pos.doubleValue()));

        // scroll content by mouse scroll
        this.addEventHandler(ScrollEvent.SCROLL, se -> {
            double dx = se.getDeltaX();
            double dy = se.getDeltaY();
            content.scrollVertically(dy);
            content.scrollHorizontally(dx);
            se.consume();
        });

        DoubleBinding layoutWidth = Bindings.createDoubleBinding(
                () -> getLayoutBounds().getWidth(),
                layoutBoundsProperty());
        DoubleBinding layoutHeight = Bindings.createDoubleBinding(
                () -> getLayoutBounds().getHeight(),
                layoutBoundsProperty());

        // scrollbar visibility
        hbar.visibleProperty().bind(Bindings.greaterThan(
                content.totalWidthEstimateProperty(),
                layoutWidth));
        vbar.visibleProperty().bind(Bindings.greaterThan(
                content.totalHeightEstimateProperty(),
                layoutHeight));

        hbar.visibleProperty().addListener(obs -> requestLayout());
        vbar.visibleProperty().addListener(obs -> requestLayout());

        getChildren().addAll(content, hbar, vbar);
    }

    @Override
    public Orientation getContentBias() {
        return content.getContentBias();
    }

    @Override
    public double computePrefWidth(double height) {
        return content.prefWidth(height);
    }

    @Override
    public double computePrefHeight(double width) {
        return content.prefHeight(width);
    }

    @Override
    public double computeMinWidth(double height) {
        return content.minWidth(height);
    }

    @Override
    public double computeMinHeight(double width) {
        return content.minHeight(width);
    }

    @Override
    public double computeMaxWidth(double height) {
        return content.maxWidth(height);
    }

    @Override
    public double computeMaxHeight(double width) {
        return content.maxHeight(width);
    }

    @Override
    protected void layoutChildren() {
        // allow 3 iterations:
        // - the first might result in need of one scrollbar
        // - the second might result in need of the other scrollbar,
        //   as a result of limited space due to the first one
        // - the third iteration should lead to a fixed point
        layoutChildren(3);
    }

    private void layoutChildren(int limit) {
        double layoutWidth = getLayoutBounds().getWidth();
        double layoutHeight = getLayoutBounds().getHeight();
        boolean vbarVisible = vbar.isVisible();
        boolean hbarVisible = hbar.isVisible();
        double vbarWidth = vbarVisible ? vbar.prefWidth(-1) : 0;
        double hbarHeight = hbarVisible ? hbar.prefHeight(-1) : 0;

        double w = layoutWidth - vbarWidth;
        double h = layoutHeight - hbarHeight;

        content.resizeRelocate(0, 0, w, h);

        if(vbar.isVisible() != vbarVisible || hbar.isVisible() != hbarVisible) {
            // the need for scrollbars changed, start over
            if(limit > 1) {
                layoutChildren(limit - 1);
                return;
            } else {
                // layout didn't settle after 3 iterations
            }
        }

        hbar.setVisibleAmount(w);
        vbar.setVisibleAmount(h);

        if(vbarVisible) {
            vbar.resizeRelocate(layoutWidth - vbarWidth, 0, vbarWidth, h);
        }

        if(hbarVisible) {
            hbar.resizeRelocate(0, layoutHeight - hbarHeight, w, hbarHeight);
        }
    }

    private static void setupUnitIncrement(ScrollBar bar) {
        bar.unitIncrementProperty().bind(new DoubleBinding() {
            { bind(bar.maxProperty(), bar.visibleAmountProperty()); }

            @Override
            protected double computeValue() {
                double max = bar.getMax();
                double visible = bar.getVisibleAmount();
                return max > visible
                        ? 13 / (max - visible) * max
                        : 0;
            }
        });
    }
}
