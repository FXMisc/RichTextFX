package org.fxmisc.flowless;

import javafx.beans.value.ObservableDoubleValue;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;

public final class VerticalVirtualFlow<T, C extends Node> extends VirtualFlow<T, C> {

    VerticalVirtualFlow(ObservableList<T> items, CellFactory<T, C> cellFactory) {
        super(items, cellFactory);
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    public ObservableDoubleValue totalWidthEstimateProperty() {
        return totalBreadthEstimateProperty();
    }

    @Override
    public ObservableDoubleValue totalHeightEstimateProperty() {
        return totalLengthEstimateProperty();
    }

    @Override
    public ObservableDoubleValue horizontalPositionProperty() {
        return breadthPositionEstimateProperty();
    }

    @Override
    public ObservableDoubleValue verticalPositionProperty() {
        return lengthPositionEstimateProperty();
    }

    @Override
    public void setHorizontalPosition(double pos) {
        setBreadthPosition(pos);
    }

    @Override
    public void setVerticalPosition(double pos) {
        setLengthPosition(pos);
    }

    @Override
    public void scrollHorizontally(double deltaX) {
        scrollBreadth(deltaX);
    }

    @Override
    public void scrollVertically(double deltaY) {
        scrollLength(deltaY);
    }

    @Override
    protected double minBreadth(Node cell) {
        return cell.minWidth(-1);
    }

    @Override
    protected double prefBreadth(Node cell) {
        return cell.prefWidth(-1);
    }

    @Override
    protected double prefLength(Node cell, double breadth) {
        return cell.prefHeight(breadth);
    }

    @Override
    protected double breadth(Bounds bounds) {
        return bounds.getWidth();
    }

    @Override
    protected double length(Bounds bounds) {
        return bounds.getHeight();
    }

    @Override
    protected double maxY(Bounds bounds) {
        return bounds.getMaxY();
    }

    @Override
    protected double minY(Bounds bounds) {
        return bounds.getMinY();
    }

    @Override
    protected void resizeRelocate(
            Node cell, double b0, double l0, double breadth, double length) {
        cell.resizeRelocate(b0, l0, breadth, length);
    }

    @Override
    protected void relocate(Node cell, double b0, double l0) {
        cell.relocate(b0, l0);
    }
}
