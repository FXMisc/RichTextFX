package org.fxmisc.flowless;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiConsumer;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Region;

public abstract class VirtualFlow<T, C extends Node> extends Region implements Virtualized {
    // Children of a VirtualFlow are cells. All children are unmanaged.
    // Children correspond to a sublist of items. Not all children are
    // visible. Visible children form a continuous subrange of all children.
    // Invisible children have CSS applied, but are not sized and positioned.

    private final IntegerProperty prefCellCount = new SimpleIntegerProperty(20);

    private final Queue<C> cellPool = new LinkedList<>();

    private final ObservableList<T> items;
    private final CellFactory<T, C> cellFactory;
    private final ObservableList<C> cells;

    private final List<Double> minBreadths; // NaN means not known
    private double maxKnownMinBreadth = 0; // NaN means needs recomputing

    // offset of the content in breadth axis, <= 0
    private double breadthOffset = 0;

    private double visibleLength = 0; // total length of all visible cells
    private int renderedFrom = 0; // index of the first item that has a cell
    private int visibleFrom = 0; // index of the first item that has a visible cell
    private int visibleTo = 0; // 1 + index of the last item that has a visible cell


    private final DoubleBinding totalBreadthEstimate;
    public ObservableDoubleValue totalBreadthEstimateProperty() {
        return totalBreadthEstimate;
    }

    private final DoubleBinding totalLengthEstimate;
    public ObservableDoubleValue totalLengthEstimateProperty() {
        return totalLengthEstimate;
    }

    private final DoubleBinding breadthPositionEstimate;
    public ObservableDoubleValue breadthPositionEstimateProperty() {
        return breadthPositionEstimate;
    }

    private final DoubleBinding lengthOffsetEstimate;

    private final DoubleBinding lengthPositionEstimate;
    public ObservableDoubleValue lengthPositionEstimateProperty() {
        return lengthPositionEstimate;
    }

    VirtualFlow(ObservableList<T> items, CellFactory<T, C> cellFactory) {
        this.items = items;
        this.cellFactory = cellFactory;

        @SuppressWarnings("unchecked")
        ObservableList<C> cells = (ObservableList<C>) getChildren();
        this.cells = cells;

        minBreadths = new ArrayList<>(items.size());
        for(int i = 0; i < items.size(); ++i) {
            minBreadths.add(Double.NaN);
        }

        layoutBoundsProperty().addListener((obs, oldBounds, newBounds) ->
                layoutBoundsChanged(oldBounds, newBounds));


        // set up bindings

        totalBreadthEstimate = new DoubleBinding() {
            @Override
            protected double computeValue() {
                return maxKnownMinBreadth();
            }
        };

        totalLengthEstimate = new DoubleBinding() {
            @Override
            protected double computeValue() {
                return visibleCells().isEmpty()
                        ? 0
                        : visibleLength / visibleCells().size() * items.size();
            }
        };

        breadthPositionEstimate = new DoubleBinding() {
            @Override
            protected double computeValue() {
                if(items.isEmpty()) {
                    return 0;
                }

                double total = totalBreadthEstimate.get();
                if(total <= breadth()) {
                    return 0;
                }

                return breadthPixelsToPosition(-breadthOffset);
            }
        };

        lengthOffsetEstimate = new DoubleBinding() {
            @Override
            protected double computeValue() {
                if(items.isEmpty()) {
                    return 0;
                }

                double total = totalLengthEstimate.get();
                if(total <= length()) {
                    return 0;
                }

                double avgLen = total / items.size();
                double beforeVisible = visibleFrom * avgLen;
                return beforeVisible - visibleCellsMinY();
            }
        };

        lengthPositionEstimate = new DoubleBinding() {
            { bind(lengthOffsetEstimate); }

            @Override
            protected double computeValue() {
                return pixelsToPosition(lengthOffsetEstimate.get());
            }
        };
    }

    @Override
    public Orientation getContentBias() {
        throw new AssertionError("must override getContentBias() in a subclass");
    }

    @Override
    protected void layoutChildren() {
        // do nothing
    }

    protected abstract double minBreadth(C cell);
    protected abstract double prefBreadth(C cell);
    protected abstract double prefLength(C cell, double breadth);
    protected abstract double breadth(Bounds bounds);
    protected abstract double length(Bounds bounds);
    protected abstract double maxY(Bounds bounds);
    protected abstract double minY(Bounds bounds);
    protected abstract void resizeRelocate(C cell, double b0, double l0, double breadth, double length);
    protected abstract void relocate(C cell, double b0, double l0);

    protected final void setLengthPosition(double pos) {
        setLengthOffset(positionToPixels(pos));
    }

    protected final void setBreadthPosition(double pos) {
        setBreadthOffset(breadthPositionToPixels(pos));
    }

    protected final void scrollLength(double deltaLength) {
        setLengthOffset(lengthOffsetEstimate.get() - deltaLength);
    }

    protected final void scrollBreadth(double deltaBreadth) {
        setBreadthOffset(breadthOffset - deltaBreadth);
    }

    @Override
    protected final double computePrefWidth(double height) {
        switch(getContentBias()) {
            case HORIZONTAL: // vertical flow
                return computePrefBreadth();
            case VERTICAL: // horizontal flow
                return computePrefLength(height);
            default:
                throw new AssertionError("Unreachable code");
        }
    }

    @Override
    protected final double computePrefHeight(double width) {
        switch(getContentBias()) {
            case HORIZONTAL: // vertical flow
                return computePrefLength(width);
            case VERTICAL: // horizontal flow
                return computePrefBreadth();
            default:
                throw new AssertionError("Unreachable code");
        }
    }

    private double computePrefBreadth() {
        // take maximum of all rendered cells,
        // but first ensure there are at least 10 rendered cells
        ensureRenderedCells(10);
        return cells.stream()
                .mapToDouble(this::prefBreadth)
                .reduce(0, (a, b) -> Math.max(a, b));
    }

    private double computePrefLength(double breadth) {
        int n = prefCellCount.get();
        ensureRenderedCells(n);
        return cells.stream().limit(n)
                .mapToDouble(cell -> prefLength(cell, breadth))
                .sum();
    }

    private void ensureRenderedCells(int n) {
        for(int i = cells.size(); i < n; ++i) {
            renderOneMore();
        }
    }

    private void renderOneMore() {
        if(renderedFrom > 0) {
            renderPrevious();
        } else if(renderedFrom + cells.size() < items.size()) {
            renderNext();
        }
    }

    private C renderInitial(int index) {
        if(!cells.isEmpty()) {
            throw new AssertionError("not an initial cell");
        }

        T item = items.get(index);
        C cell = createCell(index, item);
        cell.setVisible(false);
        cells.add(cell);

        renderedFrom = index;
        visibleFrom = index;
        visibleTo = index;
        visibleLength = 0;

        return cell;
    }

    private C renderPrevious() {
        --renderedFrom;
        T item = items.get(renderedFrom);
        C cell = createCell(renderedFrom, item);
        cell.setVisible(false);
        cells.add(0, cell);
        cell.applyCss();
        return cell;
    }

    private C renderNext() {
        int index = renderedFrom + cells.size();
        T item = items.get(index);
        C cell = createCell(index, item);
        cell.setVisible(false);
        cells.add(cell);
        cell.applyCss();
        return cell;
    }

    private C createCell(int index, T item) {
        C cell;
        C cachedCell = getFromPool();
        if(cachedCell != null) {
            cell = cellFactory.createCell(index, item, cachedCell);
            if(cell != cachedCell) {
                cellFactory.disposeCell(cachedCell);
            }
        } else {
            cell = cellFactory.createCell(index, item);
        }
        cell.setManaged(false);
        return cell;
    }

    private C getFromPool() {
        return cellPool.poll();
    }

    private void addToPool(C cell) {
        if(cell.isVisible()) {
            visibleLength -= length(cell);
        }
        cellFactory.resetCell(cell);
        cellPool.add(cell);
    }

    private List<C> visibleCells() {
        return cells.subList(
                visibleFrom - renderedFrom,
                visibleTo - renderedFrom);
    }

    private double visibleCellsMinY() {
        List<C> visibleCells = visibleCells();
        return visibleCells.isEmpty()
                ? 0.0
                : minY(visibleCells.get(0));
    }

    private double visibleCellsMaxY() {
        List<C> visibleCells = visibleCells();
        return visibleCells.isEmpty()
                ? 0.0
                : maxY(visibleCells.get(visibleCells.size()-1));
    }

    private void layoutBoundsChanged(Bounds oldBounds, Bounds newBounds) {
        double oldBreadth = breadth(oldBounds);
        double newBreadth = breadth(newBounds);
        double minBreadth = maxKnownMinBreadth();
        double breadth = Math.max(minBreadth, newBreadth);

        // adjust breadth of visible cells
        if(oldBreadth == newBreadth) {
            // do nothing
        } else if(oldBreadth <= minBreadth && newBreadth <= minBreadth) {
            // do nothing
        } else {
            resizeVisibleCells(breadth);
        }

        // fill current screen
        forwardToRel(0);

        cull();

        if(breadth + breadthOffset < newBreadth) { // empty space on the right
            shiftVisibleCellsByBreadth(newBreadth - (breadth + breadthOffset));
        }

        // TODO: invalidate only if changed (record the value at the start of this method
        totalBreadthEstimate.invalidate();
        totalLengthEstimate.invalidate();

        breadthPositionEstimate.invalidate();
        lengthOffsetEstimate.invalidate();
    }

    private void resizeVisibleCells(double breadth) {
        double y = visibleCellsMinY();
        for(C cell: visibleCells()) {
            double length = prefLength(cell, breadth);
            layoutCell(cell, y, breadth, length);
            y += length;
        }
    }

    private void forwardToRel(double l0) {
        toRel(l0, (l, breadth) -> fillLengthForwardTo(l + length(), breadth));
    }

    private void backwardToRel(double l0) {
        toRel(l0, this::fillLengthBackwardTo);
    }

    private void toRel(double l0, BiConsumer<Double, Double> fillTo) {
        double breadth = Math.max(maxKnownMinBreadth(), breadth());
        double length = length();

        boolean repeat = true;
        while(repeat) {
            fillTo.accept(l0, breadth);

            double shift;
            if(visibleLength < length) {
                shift = -visibleCellsMinY();
            } else if(visibleCellsMaxY() - l0 >= length) {
                shift = -l0;
            } else {
                shift = -(visibleCellsMaxY() - length);
            }

            if(shift != 0) {
                shiftVisibleCellsByLength(shift);
            }

            if(maxKnownMinBreadth() > breadth) { // broader cell encountered
                breadth = maxKnownMinBreadth();
                resizeVisibleCells(breadth);
            } else {
                repeat = false;
            }
        }
    }

    private void fillLengthForwardTo(double l, double breadth) {
        double length = length();

        while(visibleCellsMaxY() < l && visibleTo < items.size()) {
            placeNext(breadth);
        }

        while(visibleLength < length && visibleFrom > 0) {
            placePrevious(breadth);
        }
    }

    private void fillLengthBackwardTo(double l, double breadth) {
        double length = length();

        while(visibleCellsMinY() > l && visibleFrom > 0) {
            placePrevious(breadth);
        }

        while(visibleLength < length && visibleTo < items.size()) {
            placeNext(breadth);
        }
    }

    private void placeNext(double breadth) {
        C cell;
        if(cells.size() > visibleTo - renderedFrom) {
            cell = cells.get(visibleTo - renderedFrom);
        } else {
            cell = renderNext();
        }

        double minBreadth = minBreadth(cell);
        minBreadths.set(visibleTo, minBreadth);
        if(minBreadth > maxKnownMinBreadth()) {
            maxKnownMinBreadth = minBreadth;
            breadth = Math.max(breadth, minBreadth);
        }

        double length = prefLength(cell, breadth);
        layoutCell(cell, visibleCellsMaxY(), breadth, length);
        visibleTo += 1;
    }

    private void placePrevious(double breadth) {
        C cell;
        if(visibleFrom > renderedFrom) {
            cell = cells.get(visibleFrom - 1 - renderedFrom);
        } else {
            cell = renderPrevious();
        }

        double minBreadth = minBreadth(cell);
        minBreadths.set(visibleFrom - 1, minBreadth);
        if(minBreadth > maxKnownMinBreadth()) {
            maxKnownMinBreadth = minBreadth;
            breadth = Math.max(breadth, minBreadth);
        }

        double length = prefLength(cell, breadth);
        layoutCell(cell, visibleCellsMinY() - length, breadth, length);
        visibleFrom -= 1;
    }

    private void placeInitial(int idx, double offset) {
        double breadth = Math.max(maxKnownMinBreadth(), breadth());
        C cell = renderInitial(idx);

        double minBreadth = minBreadth(cell);
        minBreadths.set(idx, minBreadth);
        if(minBreadth > maxKnownMinBreadth()) {
            maxKnownMinBreadth = minBreadth;
            breadth = Math.max(breadth, minBreadth);
        }

        double length = prefLength(cell, breadth);
        layoutCell(cell, offset, breadth, length);

        visibleFrom = idx;
        visibleTo = idx + 1;
    }

    private void placeInitialFromEnd(int idx, double offsetFromEnd) {
        double breadth = Math.max(maxKnownMinBreadth(), breadth());
        C cell = renderInitial(idx);

        double minBreadth = minBreadth(cell);
        minBreadths.set(idx, minBreadth);
        if(minBreadth > maxKnownMinBreadth()) {
            maxKnownMinBreadth = minBreadth;
            breadth = Math.max(breadth, minBreadth);
        }

        double length = prefLength(cell, breadth);
        layoutCell(cell, length() - length + offsetFromEnd, breadth, length);

        visibleFrom = idx;
        visibleTo = idx + 1;
    }

    private void layoutCell(C cell, double l0, double breadth, double length) {
        if(cell.isVisible()) {
            visibleLength -= length(cell);
        } else {
            cell.setVisible(true);
        }
        visibleLength += length;
        resizeRelocate(cell, breadthOffset, l0, breadth, length);
    }

    private void shiftVisibleCellsByLength(double shift) {
        for(C cell: visibleCells()) {
            relocate(cell, breadthOffset, minY(cell) + shift);
        }
    }

    private void shiftVisibleCellsByBreadth(double shift) {
        breadthOffset += shift;
        for(C cell: visibleCells()) {
            relocate(cell, breadthOffset, minY(cell));
        }
    }

    private void cull() {
        cullBeforeViewport();
        cullAfterViewport();
    }

    private void cullBeforeViewport() {
        // find first in the viewport
        int i = 0;
        for(; i < cells.size(); ++i) {
            C cell = cells.get(i);
            if(cell.isVisible() && maxY(cell) > 0) {
                break;
            }
        }

        renderedFrom += i;
        visibleFrom = renderedFrom;
        List<C> toCull = cells.subList(0, i);
        toCull.forEach(this::addToPool);
        toCull.clear();
    }

    private void cullAfterViewport() {
        // find first after the viewport
        int i = 0;
        for(; i < cells.size(); ++i) {
            C cell = cells.get(i);
            if(!cell.isVisible() || minY(cell) >= length()) {
                break;
            }
        }

        visibleTo = visibleFrom + i;
        List<C> toCull = cells.subList(i, cells.size());
        toCull.forEach(this::addToPool);
        toCull.clear();
    }

    private double maxKnownMinBreadth() {
        if(Double.isNaN(maxKnownMinBreadth)) {
            maxKnownMinBreadth = minBreadths.stream()
                    .filter(x -> !Double.isNaN(x))
                    .mapToDouble(x -> x)
                    .reduce(0, (a, b) -> Math.max(a, b));
        }
        return maxKnownMinBreadth;
    }

    private double pixelsToPosition(double pixels) {
        double total = totalLengthEstimate.get();
        double length = length();
        return total > length
                ? pixels / (total - length) * total
                : 0;
    }

    private double positionToPixels(double pos) {
        double total = totalLengthEstimate.get();
        double length = length();
        return total > 0 && total > length
                ? pos / total * (total - length())
                : 0;
    }

    private double breadthPixelsToPosition(double pixels) {
        double total = totalBreadthEstimate.get();
        double breadth = breadth();
        return total > breadth
                ? pixels / (total - breadth) * total
                : 0;
    }

    private double breadthPositionToPixels(double pos) {
        double total = totalBreadthEstimate.get();
        double breadth = breadth();
        return total > 0 && total > breadth
                ? pos / total * (total - breadth)
                : 0;
    }

    private double breadth() {
        return breadth(getLayoutBounds());
    }

    private double length() {
        return length(getLayoutBounds());
    }

    private double length(C cell) {
        return length(cell.getLayoutBounds());
    }

    private double minY(C cell) {
        return minY(cell.getBoundsInParent());
    }

    private double maxY(C cell) {
        return maxY(cell.getBoundsInParent());
    }

    private void setLengthOffset(double pixels) {
        double total = totalLengthEstimate.get();
        double length = length();
        double max = Math.max(total - length, 0);
        double current = lengthOffsetEstimate.get();

        if(pixels > max) pixels = max;
        if(pixels < 0) pixels = 0;

        if(pixels > current) {
            double diff = pixels - current;
            if(diff < length) { // distance less than one screen
                double localPixels = pixels - lengthOffsetEstimate.get();
                forwardToRel(localPixels);
            } else {
                toAbs(pixels);
            }
        } else if(pixels < current) {
            double diff = current - pixels;
            if(diff < length) { // distance less than one screen
                double localPixels = pixels - lengthOffsetEstimate.get();
                backwardToRel(localPixels);
            } else {
                toAbs(pixels);
            }
        }

        totalBreadthEstimate.invalidate();
        totalLengthEstimate.invalidate();
        lengthOffsetEstimate.invalidate();
    }

    private void setBreadthOffset(double pixels) {
        double total = totalBreadthEstimate.get();
        double breadth = breadth();
        double max = Math.max(total - breadth, 0);
        double current = -breadthOffset;

        if(pixels > max) pixels = max;
        if(pixels < 0) pixels = 0;

        if(pixels != current) {
            shiftVisibleCellsByBreadth(current - pixels);
            breadthPositionEstimate.invalidate();
        }
    }

    private void toAbs(double pixels) {
        if(items.isEmpty()) {
            return;
        }

        // guess the first visible cell and its offset in the viewport
        double total = totalLengthEstimate.get();
        double avgLen = total / items.size();
        if(avgLen == 0) return;
        int first = (int) Math.floor(pixels / avgLen);
        double firstOffset = -(pixels % avgLen);

        // remove all cells
        cells.forEach(this::addToPool);
        cells.clear();

        if(first < items.size()) {
            placeInitial(first, firstOffset);
            forwardToRel(0);
        } else {
            placeInitialFromEnd(items.size()-1, 0);
            backwardToRel(0);
        }
    }
}
