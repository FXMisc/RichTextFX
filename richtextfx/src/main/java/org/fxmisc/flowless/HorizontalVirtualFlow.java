package org.fxmisc.flowless;

import javafx.beans.value.ObservableDoubleValue;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;

public final class HorizontalVirtualFlow<T, C extends Node> extends VirtualFlow<T, C> {

    HorizontalVirtualFlow(ObservableList<T> items, CellFactory<T, C> cellFactory) {
        super(items, cellFactory);
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.VERTICAL;
    }

    @Override
    public ObservableDoubleValue totalWidthEstimateProperty() {
        return totalLengthEstimateProperty();
    }

    @Override
    public ObservableDoubleValue totalHeightEstimateProperty() {
        return totalBreadthEstimateProperty();
    }

    @Override
    public ObservableDoubleValue horizontalPositionProperty() {
        return lengthPositionEstimateProperty();
    }

    @Override
    public ObservableDoubleValue verticalPositionProperty() {
        return breadthPositionEstimateProperty();
    }

    @Override
    public void setHorizontalPosition(double pos) {
        setLengthPosition(pos);
    }

    @Override
    public void setVerticalPosition(double pos) {
        setBreadthPosition(pos);
    }

    @Override
    public void scrollHorizontally(double deltaX) {
        scrollLength(deltaX);
    }

    @Override
    public void scrollVertically(double deltaY) {
        scrollBreadth(deltaY);
    }

    @Override
    protected double minBreadth(Node cell) {
        return cell.minHeight(-1);
    }

    @Override
    protected double prefBreadth(Node cell) {
        return cell.prefHeight(-1);
    }

    @Override
    protected double prefLength(Node cell, double breadth) {
        return cell.prefWidth(breadth);
    }

    @Override
    protected double breadth(Bounds bounds) {
        return bounds.getHeight();
    }

    @Override
    protected double length(Bounds bounds) {
        return bounds.getWidth();
    }

    @Override
    protected double maxY(Bounds bounds) {
        return bounds.getMaxX();
    }

    @Override
    protected double minY(Bounds bounds) {
        return bounds.getMinX();
    }

    @Override
    protected void resizeRelocate(
            Node cell, double b0, double l0, double breadth, double length) {
        cell.resizeRelocate(l0, b0, length, breadth);
    }

    @Override
    protected void relocate(Node cell, double b0, double l0) {
        cell.relocate(l0, b0);
    }
}
