package org.fxmisc.flowless;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.fxmisc.flowless.VirtualFlow.HitInfo;

public class VirtualFlow<T, C extends Cell<T, ?>> extends Region {
    // Children of a VirtualFlow are cells. All children are unmanaged.
    // Children correspond to a sublist of items. Not all children are
    // visible. Visible children form a continuous subrange of all children.
    // Invisible children have CSS applied, but are not sized and positioned.


    public static <T, C extends Cell<T, ?>> VirtualFlow<T, C> createHorizontal(
            ObservableList<T> items, BiFunction<Integer, T, C> cellFactory) {
        return new VirtualFlow<>(items, cellFactory, new HorizontalFlowMetrics());
    }

    public static <T, C extends Cell<T, ?>> VirtualFlow<T, C> createVertical(
            ObservableList<T> items, BiFunction<Integer, T, C> cellFactory) {
        return new VirtualFlow<>(items, cellFactory, new VerticalFlowMetrics());
    }

    public static abstract class HitInfo<C extends Cell<?, ?>> {

        static <C extends Cell<?, ?>> HitInfo<C> cellHit(
                int cellIndex, C cell, double cellOffset) {
            return new CellHit<>(cellIndex, cell, cellOffset);
        }

        static <C extends Cell<?, ?>> HitInfo<C> hitBeforeCells(double offset) {
            return new HitBeforeCells<>(offset);
        }

        static <C extends Cell<?, ?>> HitInfo<C> hitAfterCells(double offset) {
            return new HitAfterCells<>(offset);
        }

        // private constructor to prevent subclassing
        private HitInfo() {}

        public abstract boolean isCellHit();
        public abstract boolean isBeforeCells();
        public abstract boolean isAfterCells();

        public abstract int getCellIndex();
        public abstract C getCell();
        public abstract double getCellOffset();

        public abstract double getOffsetBeforeCells();
        public abstract double getOffsetAfterCells();

        private static class CellHit<C extends Cell<?, ?>> extends HitInfo<C> {
            private final int cellIdx;
            private final C cell;
            private final double cellOffset;

            CellHit(int cellIdx, C cell, double cellOffset) {
                this.cellIdx = cellIdx;
                this.cell = cell;
                this.cellOffset = cellOffset;
            }

            @Override public boolean isCellHit() { return true; }
            @Override public boolean isBeforeCells() { return false; }
            @Override public boolean isAfterCells() { return false; }
            @Override public int getCellIndex() { return cellIdx; }
            @Override public C getCell() { return cell; }
            @Override public double getCellOffset() { return cellOffset; }

            @Override
            public double getOffsetBeforeCells() {
                throw new UnsupportedOperationException();
            }

            @Override
            public double getOffsetAfterCells() {
                throw new UnsupportedOperationException();
            }
        }

        private static class HitBeforeCells<C extends Cell<?, ?>> extends HitInfo<C> {
            private final double offset;

            HitBeforeCells(double offset) {
                this.offset = offset;
            }

            @Override public boolean isCellHit() { return false; }
            @Override public boolean isBeforeCells() { return true; }
            @Override public boolean isAfterCells() { return false; }

            @Override public int getCellIndex() {
                throw new UnsupportedOperationException();
            }

            @Override public C getCell() {
                throw new UnsupportedOperationException();
            }

            @Override public double getCellOffset() {
                throw new UnsupportedOperationException();
            }

            @Override public double getOffsetBeforeCells() {
                return offset;
            }

            @Override public double getOffsetAfterCells() {
                throw new UnsupportedOperationException();
            }
        }

        private static class HitAfterCells<C extends Cell<?, ?>> extends HitInfo<C> {
            private final double offset;

            HitAfterCells(double offset) {
                this.offset = offset;
            }

            @Override public boolean isCellHit() { return false; }
            @Override public boolean isBeforeCells() { return false; }
            @Override public boolean isAfterCells() { return true; }

            @Override public int getCellIndex() {
                throw new UnsupportedOperationException();
            }

            @Override public C getCell() {
                throw new UnsupportedOperationException();
            }

            @Override public double getCellOffset() {
                throw new UnsupportedOperationException();
            }

            @Override public double getOffsetBeforeCells() {
                throw new UnsupportedOperationException();
            }

            @Override public double getOffsetAfterCells() {
                return offset;
            }
        }
    }

    private final ScrollBar hbar;
    private final ScrollBar vbar;
    private final VirtualFlowContent<T, C> content;


    private VirtualFlow(
            ObservableList<T> items,
            BiFunction<Integer, T, C> cellFactory,
            Metrics metrics) {
        this.getStyleClass().add("virtual-flow");
        this.content = new VirtualFlowContent<>(items, cellFactory, metrics);

        // create scrollbars
        hbar = new ScrollBar();
        vbar = new ScrollBar();
        vbar.setOrientation(Orientation.VERTICAL);

        // scrollbar ranges
        hbar.setMin(0);
        vbar.setMin(0);
        hbar.maxProperty().bind(metrics.widthEstimateProperty(content));
        vbar.maxProperty().bind(metrics.heightEstimateProperty(content));

        // scrollbar increments
        setupUnitIncrement(hbar);
        setupUnitIncrement(vbar);
        hbar.blockIncrementProperty().bind(hbar.visibleAmountProperty());
        vbar.blockIncrementProperty().bind(vbar.visibleAmountProperty());

        // scrollbar positions
        hbar.setValue(metrics.getHorizontalPosition(content));
        vbar.setValue(metrics.getVerticalPosition(content));
        metrics.horizontalPositionProperty(content).addListener(
                obs -> hbar.setValue(metrics.getHorizontalPosition(content)));
        metrics.verticalPositionProperty(content).addListener(
                obs -> vbar.setValue(metrics.getVerticalPosition(content)));

        // scroll content by scrollbars
        hbar.valueProperty().addListener((obs, old, pos) ->
                metrics.setHorizontalPosition(content, pos.doubleValue()));
        vbar.valueProperty().addListener((obs, old, pos) ->
                metrics.setVerticalPosition(content, pos.doubleValue()));

        // scroll content by mouse scroll
        this.addEventHandler(ScrollEvent.SCROLL, se -> {
            double dx = se.getDeltaX();
            double dy = se.getDeltaY();
            metrics.scrollVertically(content, dy);
            metrics.scrollHorizontally(content, dx);
            se.consume();
        });

        // scrollbar visibility
        DoubleBinding layoutWidth = Bindings.createDoubleBinding(
                () -> getLayoutBounds().getWidth(),
                layoutBoundsProperty());
        DoubleBinding layoutHeight = Bindings.createDoubleBinding(
                () -> getLayoutBounds().getHeight(),
                layoutBoundsProperty());
        BooleanBinding needsHBar0 = Bindings.greaterThan(
                metrics.widthEstimateProperty(content),
                layoutWidth);
        BooleanBinding needsVBar0 = Bindings.greaterThan(
                metrics.heightEstimateProperty(content),
                layoutHeight);
        BooleanBinding needsHBar = needsHBar0.or(needsVBar0.and(
                Bindings.greaterThan(
                        Bindings.add(metrics.widthEstimateProperty(content), vbar.widthProperty()),
                        layoutWidth)));
        BooleanBinding needsVBar = needsVBar0.or(needsHBar0.and(
                Bindings.greaterThan(
                        Bindings.add(metrics.heightEstimateProperty(content), hbar.heightProperty()),
                        layoutHeight)));
        hbar.visibleProperty().bind(needsHBar);
        vbar.visibleProperty().bind(needsVBar);

        // request layout later, because if currently in layout, the request is ignored
        hbar.visibleProperty().addListener(obs -> Platform.runLater(() -> requestLayout()));
        vbar.visibleProperty().addListener(obs -> Platform.runLater(() -> requestLayout()));

        getChildren().addAll(content, hbar, vbar);
    }

    public void dispose() {
        content.dispose();
    }

    @Override
    public Orientation getContentBias() {
        return content.getContentBias();
    }

    public void show(int index) {
        content.show(index);
    }

    public void showAsFirst(int itemIndex) {
        content.showAsFirst(itemIndex);
    }

    public void showAsLast(int itemIndex) {
        content.showAsLast(itemIndex);
    }

    public void show(C cell, Rectangle region) {
        content.showRegion(cell, region);
    }

    public C getCell(int itemIndex) {
        return content.paveToItem(itemIndex);
    }

    public Optional<C> getCellIfVisible(int itemIndex) {
        return content.tryGetVisibleCell(itemIndex);
    }

    public Stream<C> visibleCells() {
        return content.visibleCells();
    }

    public IndexRange getVisibleRange() {
        return content.firstVisibleRange();
    }

    public HitInfo<C> hit(double offset) {
        return content.hit(offset);
    }

    @Override
    protected double computePrefWidth(double height) {
        return content.prefWidth(height)
                + (vbar.isVisible() ? vbar.prefWidth(-1) : 0);
    }

    @Override
    protected double computePrefHeight(double width) {
        return content.prefHeight(width)
                + (hbar.isVisible() ? hbar.prefHeight(-1) : 0);
    }

    @Override
    protected double computeMinWidth(double height) {
        return content.minWidth(height)
                + (vbar.isVisible() ? vbar.minWidth(-1) : 0);
    }

    @Override
    protected double computeMinHeight(double width) {
        return content.minHeight(width)
                + (hbar.isVisible() ? hbar.minHeight(-1) : 0);
    }

    @Override
    protected double computeMaxWidth(double height) {
        return content.maxWidth(height)
                + (vbar.isVisible() ? vbar.maxWidth(-1) : 0);
    }

    @Override
    protected double computeMaxHeight(double width) {
        return content.maxHeight(width)
                + (hbar.isVisible() ? hbar.maxHeight(-1) : 0);
    }

    @Override
    protected void layoutChildren() {
        double layoutWidth = getLayoutBounds().getWidth();
        double layoutHeight = getLayoutBounds().getHeight();
        boolean vbarVisible = vbar.isVisible();
        boolean hbarVisible = hbar.isVisible();
        double vbarWidth = vbarVisible ? vbar.prefWidth(-1) : 0;
        double hbarHeight = hbarVisible ? hbar.prefHeight(-1) : 0;

        double w = layoutWidth - vbarWidth;
        double h = layoutHeight - hbarHeight;

        content.resizeRelocate(0, 0, w, h);

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


class VirtualFlowContent<T, C extends Cell<T, ?>> extends Region {
    private final ObservableList<T> items;
    private final ObservableList<C> cells;
    private final Metrics metrics;
    private final BreadthTracker breadthTracker;
    private final CellPool<T, C> cellPool;

    private final ListChangeListener<? super T> itemsListener = ch -> {
        while(ch.next()) {
            int pos = ch.getFrom();
            int removedSize;
            int addedSize;
            if(ch.wasPermutated()) {
                addedSize = removedSize = ch.getTo() - pos;
            } else {
                removedSize = ch.getRemovedSize();
                addedSize = ch.getAddedSize();
            }
            itemsReplaced(pos, removedSize, addedSize);
        }
    };

    // hold the subscription of list binding to prevent it from being garbage collected
    @SuppressWarnings("unused")
    private final Subscription childrenBinding;

    private final IntegerProperty prefCellCount = new SimpleIntegerProperty(20);

    private int renderedFrom = 0; // index of the first item that has a cell
    private Optional<IndexRange> hole = Optional.empty();

    // offset of the content in breadth axis, <= 0
    private double breadthOffset = 0;

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

    VirtualFlowContent(
            ObservableList<T> items,
            BiFunction<Integer, T, C> cellFactory,
            Metrics metrics) {
        this.getStyleClass().add("virtual-flow-content");
        this.items = items;
        this.cellPool = new CellPool<>(cellFactory);
        this.metrics = metrics;
        this.breadthTracker = new BreadthTracker(items.size());
        this.cells = FXCollections.observableArrayList();

        ObservableList<Node> cellNodes = EasyBind.map(cells, c -> c.getNode());
        this.childrenBinding = EasyBind.listBind(getChildren(), cellNodes);

        Rectangle clipRect = new Rectangle();
        setClip(clipRect);

        layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            clipRect.setWidth(newBounds.getWidth());
            clipRect.setHeight(newBounds.getHeight());
            layoutBoundsChanged(oldBounds, newBounds);
        });

        items.addListener(itemsListener);


        // set up bindings

        totalBreadthEstimate = new DoubleBinding() {
            @Override
            protected double computeValue() {
                return maxKnownBreadth();
            }
        };

        totalLengthEstimate = new DoubleBinding() {
            @Override
            protected double computeValue() {
                return hasVisibleCells()
                        ? averageLength() * items.size()
                        : 0;
            }
        };

        breadthPositionEstimate = new DoubleBinding() {
            @Override
            protected double computeValue() {
                if(items.isEmpty()) {
                    return 0;
                }

                double total = maxKnownBreadth();
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

                if(totalLengthEstimate.get() <= length()) {
                    return 0;
                }

                double avgLen = averageLength();
                double beforeVisible = firstVisibleRange().getStart() * avgLen;
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

    public void dispose() {
        items.removeListener(itemsListener);
        dropCellsFrom(0);
        cellPool.dispose();
    }

    @Override
    protected void layoutChildren() {
        // do nothing
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
        return getChildren().stream()
                .mapToDouble(metrics::prefBreadth)
                .reduce(0, (a, b) -> Math.max(a, b));
    }

    private double computePrefLength(double breadth) {
        int n = prefCellCount.get();
        ensureRenderedCells(n);
        return getChildren().stream().limit(n)
                .mapToDouble(cell -> metrics.prefLength(cell, breadth))
                .sum();
    }

    @Override
    public final Orientation getContentBias() {
        return metrics.getContentBias();
    }

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

    private void ensureRenderedCells(int n) {
        for(int i = cells.size(); i < n; ++i) {
            if(hole.isPresent()) {
                render(hole.get().getStart());
            } else if(renderedFrom > 0) {
                render(renderedFrom - 1);
            } else if(renderedFrom + cells.size() < items.size()) {
                render(renderedFrom + cells.size());
            } else {
                break;
            }
        }
    }

    private C renderInitial(int index) {
        if(!cells.isEmpty()) {
            throw new IllegalStateException("There are some rendered cells already");
        }

        renderedFrom = index;
        return render(index, 0);
    }

    private C render(int index, int childInsertionPos) {
        T item = items.get(index);
        C cell = cellPool.getCell(index, item);
        cell.getNode().setVisible(false);
        cells.add(childInsertionPos, cell);
        cell.getNode().applyCss();
        return cell;
    }

    private C render(int index) {
        int renderedTo = renderedFrom + cells.size() + hole.map(IndexRange::getLength).orElse(0);
        if(index < renderedFrom - 1) {
            throw new IllegalArgumentException("Cannot render " + index + ". Rendered cells start at " + renderedFrom);
        } else if(index == renderedFrom - 1) {
            C cell = render(index, 0);
            renderedFrom -= 1;
            return cell;
        } else if(index == renderedTo) {
            return render(index, cells.size());
        } else if(index > renderedTo) {
            throw new IllegalArgumentException("Cannot render " + index + ". Rendered cells end at " + renderedTo);
        } else if(hole.isPresent()) {
            IndexRange hole = this.hole.get();
            if(index < hole.getStart()) {
                return cells.get(index - renderedFrom);
            } else if(index >= hole.getEnd()) {
                return cells.get(index - renderedFrom - hole.getLength());
            } else if(index == hole.getStart()) {
                C cell = render(index, index - renderedFrom);
                this.hole = hole.getLength() == 1
                        ? Optional.empty()
                        : Optional.of(new IndexRange(index + 1, hole.getEnd()));
                return cell;
            } else if(index == hole.getEnd() - 1) {
                C cell = render(index, hole.getStart() - renderedFrom);
                this.hole = Optional.of(new IndexRange(hole.getStart(), hole.getEnd() - 1));
                return cell;
            } else {
                throw new IllegalArgumentException("Cannot render " + index + " inside hole " + hole);
            }
        } else {
            return cells.get(index - renderedFrom);
        }
    }

    private void cullFrom(int pos) {
        if(hole.isPresent()) {
            IndexRange hole = this.hole.get();
            if(pos >= hole.getEnd()) {
                dropCellsFrom(pos - hole.getLength() - renderedFrom);
            } else if(pos > hole.getStart()) {
                dropCellsFrom(hole.getStart() - renderedFrom);
                this.hole = Optional.of(new IndexRange(hole.getStart(), pos));
            } else {
                dropCellsFrom(pos - renderedFrom);
                this.hole = Optional.empty();
            }
        } else {
            dropCellsFrom(pos - renderedFrom);
        }
    }

    private void cullBefore(int pos) {
        if(hole.isPresent()) {
            IndexRange hole = this.hole.get();
            if(pos <= hole.getStart()) {
                dropCellsBefore(pos - renderedFrom);
            } else if(pos < hole.getEnd()) {
                dropCellsBefore(hole.getStart() - renderedFrom);
                this.hole = Optional.of(new IndexRange(pos, hole.getEnd()));
            } else {
                dropCellsBefore(pos - hole.getLength() - renderedFrom);
                this.hole = Optional.empty();
            }
        } else {
            dropCellsBefore(pos - renderedFrom);
        }

        renderedFrom = pos;
    }

    private void dropCellsFrom(int cellIdx) {
        dropCellRange(cellIdx, cells.size());
    }

    private void dropCellsBefore(int cellIdx) {
        dropCellRange(0, cellIdx);
    }

    private void dropCellRange(int from, int to) {
        List<C> toDrop = cells.subList(from, to);
        toDrop.forEach(cellPool::acceptCell);
        toDrop.clear();
    }

    private void layoutBoundsChanged(Bounds oldBounds, Bounds newBounds) {
        double oldBreadth = metrics.breadth(oldBounds);
        double newBreadth = metrics.breadth(newBounds);
        double minBreadth = maxKnownBreadth();
        double breadth = Math.max(minBreadth, newBreadth);

        // adjust breadth of visible cells
        if(oldBreadth != newBreadth) {
            if(oldBreadth <= minBreadth && newBreadth <= minBreadth) {
                // do nothing
            } else {
                resizeVisibleCells(breadth);
            }
        }

        if(breadth + breadthOffset < newBreadth) { // empty space on the right
            shiftVisibleCellsByBreadth(newBreadth - (breadth + breadthOffset));
        }

        // fill current screen
        fillViewport(0);

        totalBreadthEstimate.invalidate();
        totalLengthEstimate.invalidate();
        breadthPositionEstimate.invalidate();
        lengthOffsetEstimate.invalidate();
    }

    private void itemsReplaced(int pos, int removedSize, int addedSize) {
        if(hole.isPresent()) {
            throw new IllegalStateException("change in items before hole was closed");
        }

        breadthTracker.itemsReplaced(pos, removedSize, addedSize);

        if(pos >= renderedFrom + cells.size()) {
            // does not affect any cells, do nothing
        } else if(pos + removedSize <= renderedFrom) {
            // change before rendered cells, just update indices
            int delta = addedSize - removedSize;
            renderedFrom += delta;
            for(int i = 0; i < cells.size(); ++i) {
                cells.get(i).updateIndex(renderedFrom + i);
            }
        } else if(pos > renderedFrom && pos + removedSize < renderedFrom + cells.size()) {
            // change within rendered cells,
            // at least one cell retained on both sides
            dropCellRange(pos - renderedFrom, pos + removedSize - renderedFrom);
            for(int i = pos - renderedFrom; i < cells.size(); ++i) {
                cells.get(i).updateIndex(renderedFrom + addedSize + i);
            }
            if(addedSize > 0) {
                // creating a hole in rendered cells
                hole = Optional.of(new IndexRange(pos, pos + addedSize));
            }
        } else if(pos > renderedFrom) {
            dropCellsFrom(pos - renderedFrom);
        } else if(pos + removedSize >= renderedFrom + cells.size()) {
            // all rendered items removed
            dropCellsFrom(0);
            renderedFrom = 0;
        } else {
            dropCellsBefore(pos + removedSize - renderedFrom);
            renderedFrom = pos + addedSize;
            for(int i = 0; i < cells.size(); ++i) {
                cells.get(i).updateIndex(renderedFrom + i);
            }
        }

        fillViewport(pos);

        totalBreadthEstimate.invalidate();
        totalLengthEstimate.invalidate();
        breadthPositionEstimate.invalidate();
        lengthOffsetEstimate.invalidate();
    }

    private void fillViewport(int ifEmptyStartWith) {
        if(!hasVisibleCells()) {
            if(hole.isPresent()) {
                // There is a hole in rendered cells.
                // Place its first item at the start of the viewport.
                shrinkHoleFromLeft(0.0);
            } else if(!cells.isEmpty()) {
                // There are at least some rendered cells.
                // Place the first one at 0.
                placeAt(renderedFrom, cells.get(0), 0.0);
            } else if(!items.isEmpty()) {
                // use the hint
                int idx = ifEmptyStartWith < 0 ? 0
                        : ifEmptyStartWith >= items.size() ? items.size() - 1
                        : ifEmptyStartWith;
                placeInitialAtStart(idx);
            } else {
                return;
            }
        }

        fillViewport();
    }

    private void fillViewport() {
        if(!hasVisibleCells()) {
            throw new IllegalStateException("need a visible cell to start from");
        }

        double breadth = Math.max(maxKnownBreadth(), breadth());

        boolean repeat = true;
        while(repeat) {
            fillViewportOnce();

            if(maxKnownBreadth() > breadth) { // broader cell encountered
                breadth = maxKnownBreadth();
                resizeVisibleCells(breadth);
            } else {
                repeat = false;
            }
        }

        // cull, but first eliminate the hole
        if(hole.isPresent()) {
            IndexRange hole = this.hole.get();
            if(hole.getStart() > renderedFrom) {
                Node cellBeforeHole = cells.get(hole.getStart() - 1 - renderedFrom).getNode();
                if(!cellBeforeHole.isVisible() || metrics.maxY(cellBeforeHole) <= 0) {
                    cullBefore(hole.getEnd());
                } else {
                    cullFrom(hole.getStart());
                }
            } else {
                cullBefore(hole.getEnd());
            }
        }
        cullBeforeViewport();
        cullAfterViewport();
    }

    private void fillViewportOnce() {
        // expand the visible range
        IndexRange visibleRange = firstVisibleRange();
        int firstVisible = visibleRange.getStart();
        int lastVisible = visibleRange.getEnd() - 1;

        // fill backward until 0 is covered
        firstVisible = paveBackwardTo(0.0, firstVisible);
        double minY = metrics.minY(getVisibleCell(firstVisible));
        if(minY > 0) {
            shiftVisibleCellsByLength(-minY);
            minY = 0;
        }

        // fill forward until end of viewport is covered
        double length = length();
        lastVisible = paveForwardTo(length, lastVisible);
        double maxY = metrics.maxY(getVisibleCell(lastVisible));

        double leftToFill = length - maxY;
        if(leftToFill > 0) {
            firstVisible = paveBackwardTo(-leftToFill, firstVisible);
            minY = metrics.minY(getVisibleCell(firstVisible));
            double shift = Math.min(-minY, leftToFill);
            shiftVisibleCellsByLength(shift);
        }
    }

    private int paveForwardTo(double y, int startAfter) {
        int i = startAfter;
        C cell = getVisibleCell(i);
        double maxY = metrics.maxY(cell);
        while(maxY < y && i < items.size() - 1) {
            cell = placeAt(++i, maxY);
            maxY = metrics.maxY(cell);
        }
        return i;
    }

    private C paveForwardToItem(int itemIdx, int startAfter) {
        C cell = getVisibleCell(startAfter);
        double maxY = metrics.maxY(cell);
        for(int i = startAfter + 1; i <= itemIdx; ++i) {
            cell = placeAt(i, maxY);
            maxY = metrics.maxY(cell);
        }
        return cell;
    }

    private int paveBackwardTo(double y, int startBefore) {
        int i = startBefore;
        C cell = getVisibleCell(i);
        double minY = metrics.minY(cell);
        while(minY > y && i > 0) {
            cell = placeEndAt(--i, minY);
            minY = metrics.minY(cell);
        }
        return i;
    }

    private C paveBackwardToItem(int itemIdx, int startBefore) {
        C cell = getVisibleCell(startBefore);
        double minY = metrics.minY(cell);
        for(int i = startBefore - 1; i >= itemIdx; --i) {
            cell = placeEndAt(i, minY);
            minY = metrics.minY(cell);
        }
        return cell;
    }

    C paveToItem(int itemIdx) {
        if(hasVisibleCells()) {
            IndexRange rng = firstVisibleRange();
            if(itemIdx < rng.getStart()) {
                return paveBackwardToItem(itemIdx, rng.getStart());
            } else if(itemIdx >= rng.getEnd()) {
                return paveForwardToItem(itemIdx, rng.getEnd() - 1);
            } else {
                return getVisibleCell(itemIdx);
            }
        } else {
            return jumpToItem(itemIdx);
        }
    }

    int paveTo(double offset) {
        if(!hasVisibleCells()) {
            throw new IllegalStateException("No visible cells to offset from");
        }

        IndexRange rng = firstVisibleRange();
        double minY = metrics.minY(getVisibleCell(rng.getStart()));
        double maxY = metrics.maxY(getVisibleCell(rng.getEnd() - 1));

        if(offset < minY) {
            return paveBackwardTo(offset, rng.getStart());
        } else if(offset > maxY) {
            return paveForwardTo(offset, rng.getEnd() - 1);
        } else {
            for(int i = rng.getStart(); i < rng.getEnd(); ++i) {
                if(metrics.maxY(getVisibleCell(i)) >= offset) {
                    return i;
                }
            }
            throw new AssertionError("unreachable code");
        }
    }

    HitInfo<C> hit(double offset) {
        int idx = paveTo(offset);
        C cell = getVisibleCell(idx);
        double minY = metrics.minY(cell);
        double maxY = metrics.maxY(cell);
        if(offset < minY) {
            return HitInfo.hitBeforeCells(offset - minY);
        } else if(offset > maxY) {
            return HitInfo.hitAfterCells(offset - maxY);
        } else {
            return HitInfo.cellHit(idx, cell, offset - minY);
        }
    }

    void show(int itemIdx) {
        if(hasVisibleCells()) {
            IndexRange rng = firstVisibleRange();
            if(itemIdx < rng.getStart()) {
                showStartAtBeforeVisibleRange(itemIdx, 0.0, rng.getStart());
            } else if(itemIdx >= rng.getEnd()) {
                showEndAtAfterVisibleRange(itemIdx, 0.0, rng.getEnd() - 1);
            } else { // already visible
                C cell = getVisibleCell(itemIdx);
                showLengthRegion(cell, 0.0, metrics.length(cell.getNode()));
            }
        } else {
            jumpToItem(itemIdx);
        }
    }

    private void showLengthRegion(C cell, double fromY, double toY) {
        double minY = metrics.minY(cell);
        double spaceBefore = minY + fromY;
        double spaceAfter = length() - (minY + toY);
        if(spaceBefore < 0 && spaceAfter > 0) {
            double shift = Math.min(-spaceBefore, spaceAfter);
            shiftVisibleCellsByLength(shift);
        } else if(spaceAfter < 0 && spaceBefore > 0) {
            double shift = Math.max(spaceAfter, -spaceBefore);
            shiftVisibleCellsByLength(shift);
        }
    }

    private void showBreadthRegion(C cell, double fromX, double toX) {
        double spaceBefore = fromX + breadthOffset;
        double spaceAfter = breadth() - toX - breadthOffset;
        if(spaceBefore < 0 && spaceAfter > 0) {
            double shift = Math.min(-spaceBefore, spaceAfter);
            shiftVisibleCellsByBreadth(shift);
        } else if(spaceAfter < 0 && spaceBefore > 0) {
            double shift = Math.max(spaceAfter, -spaceBefore);
            shiftVisibleCellsByBreadth(shift);
        }
    }

    void showRegion(C cell, Rectangle region) {
        showLengthRegion(cell, metrics.minY(region), metrics.maxY(region));
        showBreadthRegion(cell, metrics.minX(region), metrics.maxX(region));
    }

    void showAsFirst(int itemIdx) {
        showStartAt(itemIdx, 0.0);
    }

    void showAsLast(int itemIdx) {
        showEndAt(itemIdx, 0.0);
    }

    private void showStartAt(int itemIdx, double offset) {
        if(hasVisibleCells()) {
            IndexRange rng = firstVisibleRange();
            if(itemIdx < rng.getStart()) {
                showStartAtBeforeVisibleRange(itemIdx, offset, rng.getStart());
            } else if(itemIdx >= rng.getEnd()) {
                jumpToItem(itemIdx, offset);
            } else {
                Node cell = getVisibleCell(itemIdx).getNode();
                double minY = metrics.minY(cell);
                if(minY != offset) {
                    shiftVisibleCellsByLength(offset - minY);
                    fillViewport();
                }
            }
        } else {
            jumpToItem(itemIdx, offset);
        }
    }

    private void showEndAt(int itemIdx, double offsetFromEnd) {
        if(hasVisibleCells()) {
            IndexRange rng = firstVisibleRange();
            if(itemIdx < rng.getStart()) {
                jumpToEndOfItem(itemIdx, offsetFromEnd);
            } else if(itemIdx >= rng.getEnd()) {
                showEndAtAfterVisibleRange(itemIdx, offsetFromEnd, rng.getEnd());
            } else {
                Node cell = getVisibleCell(itemIdx).getNode();
                double maxY = metrics.maxY(cell);
                double targetMaxY = length() + offsetFromEnd;
                if(maxY != targetMaxY) {
                    shiftVisibleCellsByLength(targetMaxY - maxY);
                    fillViewport();
                }
            }
        } else {
            jumpToEndOfItem(itemIdx, offsetFromEnd);
        }
    }

    private void showStartAtBeforeVisibleRange(int itemIdx, double offset, int firstVisible) {
        double distance = averageLength() * (firstVisible - itemIdx);
        if(distance > length()) {
            jumpToItem(itemIdx, offset);
        } else {
            Node cell = paveBackwardToItem(itemIdx, firstVisible).getNode();
            double minY = metrics.minY(cell);
            if(minY != offset) {
                shiftVisibleCellsByLength(offset - minY);
                fillViewport();
            }
        }
    }

    private void showEndAtAfterVisibleRange(int itemIdx, double offsetFromEnd, int lastVisible) {
        double distance = averageLength() * (itemIdx - lastVisible);
        if(distance > length()) {
            jumpToEndOfItem(itemIdx, offsetFromEnd);
        } else {
            Node cell = paveForwardToItem(itemIdx, lastVisible).getNode();
            double maxY = metrics.maxY(cell);
            double targetMaxY = length() + offsetFromEnd;
            if(maxY != targetMaxY) {
                shiftVisibleCellsByLength(targetMaxY - maxY);
                fillViewport();
            }
        }
    }

    private void cullBeforeViewport() {
        if(hole.isPresent()) {
            throw new IllegalStateException("unexpected hole");
        }

        // find first in the viewport
        int i = 0;
        for(; i < cells.size(); ++i) {
            Node cell = cells.get(i).getNode();
            if(cell.isVisible() && metrics.maxY(cell) > 0) {
                break;
            }
        }

        cullBefore(renderedFrom + i);
    }

    private void cullAfterViewport() {
        if(hole.isPresent()) {
            throw new IllegalStateException("unexpected hole");
        }

        // find first after the viewport
        int i = 0;
        for(; i < cells.size(); ++i) {
            Node cell = cells.get(i).getNode();
            if(!cell.isVisible() || metrics.minY(cell) >= length()) {
                break;
            }
        }

        cullFrom(renderedFrom + i);
    }

    private Optional<Integer> itemToCellIndex(int itemIdx) {
        if(itemIdx < renderedFrom) {
            return Optional.empty();
        } else if(hole.isPresent()) {
            IndexRange hole = this.hole.get();
            if(itemIdx < hole.getStart()) {
                return Optional.of(itemIdx - renderedFrom);
            } else if(itemIdx < hole.getEnd()) {
                return Optional.empty();
            } else if(itemIdx < renderedFrom + hole.getLength() + cells.size()) {
                return Optional.of(itemIdx - hole.getLength() - renderedFrom);
            } else {
                return Optional.empty();
            }
        } else if(itemIdx >= renderedFrom + cells.size()) {
            return Optional.empty();
        } else {
            return Optional.of(itemIdx - renderedFrom);
        }
    }

    Optional<C> tryGetVisibleCell(int itemIdx) {
        return itemToCellIndex(itemIdx).flatMap(cellIdx -> {
            C cell = cells.get(cellIdx);
            return cell.getNode().isVisible()
                    ? Optional.of(cell)
                    : Optional.empty();
        });
    }

    private C getVisibleCell(int itemIdx) {
        Optional<C> cell = tryGetVisibleCell(itemIdx);
        if(cell.isPresent()) {
            return cell.get();
        } else {
            throw new IllegalArgumentException("Item " + itemIdx + " is not visible");
        }
    }

    private boolean hasVisibleCells() {
        return getChildren().stream().anyMatch(Node::isVisible);
    }

    private void shrinkHoleFromLeft(double placeAtY) {
        int itemIdx = hole.get().getStart();
        int cellIdx = itemIdx - renderedFrom;
        C cell = render(itemIdx, cellIdx);
        placeAt(itemIdx, cell, placeAtY);
        hole = hole.get().getLength() == 1
                ? Optional.empty()
                : Optional.of(new IndexRange(itemIdx + 1, hole.get().getEnd()));
    }

    private void placeAt(int itemIdx, C cell, double y) {
        double minBreadth = metrics.minBreadth(cell.getNode());
        breadthTracker.reportBreadth(itemIdx, minBreadth);
        double breadth = Math.max(maxKnownBreadth(), breadth());
        double length = metrics.prefLength(cell.getNode(), breadth);
        layoutCell(cell.getNode(), y, breadth, length);
    }

    private void placeEndAt(int itemIdx, C cell, double endY) {
        double minBreadth = metrics.minBreadth(cell.getNode());
        breadthTracker.reportBreadth(itemIdx, minBreadth);
        double breadth = Math.max(maxKnownBreadth(), breadth());
        double length = metrics.prefLength(cell.getNode(), breadth);
        layoutCell(cell.getNode(), endY - length, breadth, length);
    }

    private C placeAt(int itemIdx, double y) {
        C cell = render(itemIdx);
        placeAt(itemIdx, cell, y);
        return cell;
    }

    private C placeEndAt(int itemIdx, double endY) {
        C cell = render(itemIdx);
        placeEndAt(itemIdx, cell, endY);
        return cell;
    }

    private C placeInitialAt(int itemIdx, double y) {
        C cell = renderInitial(itemIdx);
        placeAt(itemIdx, cell, y);
        return cell;
    }

    private void placeInitialAtStart(int itemIdx) {
        placeInitialAt(itemIdx, 0.0);
    }

    private C placeInitialFromEnd(int itemIdx, double offsetFromEnd) {
        C cell = renderInitial(itemIdx);
        double maxY = length() + offsetFromEnd;
        placeAt(itemIdx, cell, maxY - metrics.length(cell.getNode()));
        return cell;
    }

    private void layoutCell(Node cell, double l0, double breadth, double length) {
        cell.setVisible(true);
        metrics.resizeRelocate(cell, breadthOffset, l0, breadth, length);
    }

    private void shiftVisibleCellsByLength(double shift) {
        visibleNodes().forEach(cell -> {
            metrics.relocate(cell, breadthOffset, metrics.minY(cell) + shift);
        });
    }

    private void shiftVisibleCellsByBreadth(double shift) {
        breadthOffset += shift;
        visibleNodes().forEach(cell -> {
            metrics.relocate(cell, breadthOffset, metrics.minY(cell));
        });
    }

    private void resizeVisibleCells(double breadth) {
        if(hole.isPresent()) {
            throw new IllegalStateException("unexpected hole in rendered cells");
        }

        double y = visibleCellsMinY();
        for(C cell: cells) {
            if(cell.getNode().isVisible()) {
                double length = metrics.prefLength(cell.getNode(), breadth);
                layoutCell(cell.getNode(), y, breadth, length);
                y += length;
            }
        }
    }

    private Stream<Node> visibleNodes() {
        return getChildren().stream().filter(Node::isVisible);
    }

    Stream<C> visibleCells() {
        return cells.stream().filter(c -> c.getNode().isVisible());
    }

    private double maxKnownBreadth() {
        return breadthTracker.maxKnownBreadth();
    }

    private double length() {
        return metrics.length(this);
    }

    private double breadth() {
        return metrics.breadth(this);
    }

    private double visibleCellsMinY() {
        return visibleNodes().findFirst().map(metrics::minY).orElse(0.0);
    }

    private double averageLength() {
        int n = 0;
        double lengthSum = 0.0;
        for(Node cell: getChildren()) {
            if(cell.isVisible()) {
                n += 1;
                lengthSum += metrics.length(cell);
            }
        }
        return n == 0 ? 0 : lengthSum / n;
    }

    IndexRange firstVisibleRange() {
        if(cells.isEmpty()) {
            throw new IllegalStateException("no rendered cells");
        }

        if(hole.isPresent()) {
            IndexRange rng = visibleRangeIn(0, hole.get().getStart() - renderedFrom);
            if(rng != null) {
                return new IndexRange(rng.getStart() + renderedFrom, rng.getEnd() + renderedFrom);
            } else if((rng = visibleRangeIn(hole.get().getStart() - renderedFrom, cells.size())) != null) {
                return new IndexRange(rng.getStart() + hole.get().getLength() + renderedFrom, rng.getEnd() + hole.get().getLength() + renderedFrom);
            } else {
                throw new IllegalStateException("no visible cells");
            }
        } else {
            IndexRange rng = visibleRangeIn(0, cells.size());
            if(rng != null) {
                return new IndexRange(rng.getStart() + renderedFrom, rng.getEnd() + renderedFrom);
            } else {
                throw new IllegalStateException("no visible cells");
            }
        }
    }

    private IndexRange visibleRangeIn(int from, int to) {
        int a;
        for(a = from; a < to; ++a) {
            if(cells.get(a).getNode().isVisible()) {
                break;
            }
        }
        if(a < to) {
            int b;
            for(b = a + 1; b < to; ++b) {
                if(!cells.get(b).getNode().isVisible()) {
                    break;
                }
            }
            return new IndexRange(a, b);
        } else {
            return null;
        }
    }

    private void setLengthOffset(double pixels) {
        double total = totalLengthEstimate.get();
        double length = length();
        double max = Math.max(total - length, 0);
        double current = lengthOffsetEstimate.get();

        if(pixels > max) pixels = max;
        if(pixels < 0) pixels = 0;

        double diff = pixels - current;
        if(Math.abs(diff) < length) { // distance less than one screen
            shiftVisibleCellsByLength(-diff);
            fillViewport(0);
        } else {
            jumpToAbsolutePosition(pixels);
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

    private void jumpToAbsolutePosition(double pixels) {
        if(items.isEmpty()) {
            return;
        }

        // guess the first visible cell and its offset in the viewport
        double avgLen = averageLength();
        if(avgLen == 0) return;
        int first = (int) Math.floor(pixels / avgLen);
        double firstOffset = -(pixels % avgLen);

        if(first < items.size()) {
            jumpToItem(first, firstOffset);
        } else {
            jumpToEndOfItem(items.size()-1);
        }
    }

    private C jumpToItem(int itemIdx) {
        return jumpToItem(itemIdx, 0.0);
    }

    private C jumpToItem(int itemIdx, double itemOffset) {
        // remove all cells
        cullFrom(renderedFrom);

        C cell = placeInitialAt(itemIdx, itemOffset);
        fillViewport();

        return cell;
    }

    private C jumpToEndOfItem(int itemIdx) {
        return jumpToEndOfItem(itemIdx, 0.0);
    }

    private C jumpToEndOfItem(int itemIdx, double offsetFromEnd) {
        // remove all cells
        cullFrom(renderedFrom);

        C cell = placeInitialFromEnd(itemIdx, offsetFromEnd);
        fillViewport();

        return cell;
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
}

final class CellPool<T, C extends Cell<T, ?>> {
    private final BiFunction<Integer, T, C> cellFactory;
    private final Queue<C> pool = new LinkedList<>();

    public CellPool(BiFunction<Integer, T, C> cellFactory) {
        this.cellFactory = cellFactory;
    }

    public C getCell(int index, T item) {
        C cell = pool.poll();
        if(cell != null) {
            cell.updateItem(index, item);
        } else {
            cell = cellFactory.apply(index, item);
        }
        cell.getNode().setManaged(false);
        return cell;
    }

    public void acceptCell(C cell) {
        cell.reset();
        if(cell.isReusable()) {
            pool.add(cell);
        } else {
            cell.dispose();
        }
    }

    public void dispose() {
        for(C cell: pool) {
            cell.dispose();
        }

        pool.clear();
    }
}

final class BreadthTracker {
    private final List<Double> breadths; // NaN means not known
    private double maxKnownBreadth = 0; // NaN means needs recomputing

    BreadthTracker(int initSize) {
        breadths = new ArrayList<>(initSize);
        for(int i = 0; i < initSize; ++i) {
            breadths.add(Double.NaN);
        }
    }

    void reportBreadth(int itemIdx, double breadth) {
        breadths.set(itemIdx, breadth);
        if(!Double.isNaN(maxKnownBreadth) && breadth > maxKnownBreadth) {
            maxKnownBreadth = breadth;
        }
    }

    void itemsReplaced(int pos, int removedSize, int addedSize) {
        List<Double> remBreadths = breadths.subList(pos, pos + removedSize);
        for(double b: remBreadths) {
            if(b == maxKnownBreadth) {
                maxKnownBreadth = Double.NaN;
                break;
            }
        }
        remBreadths.clear();
        for(int i = 0; i < addedSize; ++i) {
            remBreadths.add(Double.NaN);
        }
    }

    double maxKnownBreadth() {
        if(Double.isNaN(maxKnownBreadth)) {
            maxKnownBreadth = breadths.stream()
                    .filter(x -> !Double.isNaN(x))
                    .mapToDouble(x -> x)
                    .reduce(0, (a, b) -> Math.max(a, b));
        }
        return maxKnownBreadth;
    }
}

interface Metrics {
    Orientation getContentBias();
    double length(Bounds bounds);
    double breadth(Bounds bounds);
    double minX(Bounds bounds);
    double minY(Bounds bounds);
    double layoutX(Node node);
    double layoutY(Node node);
    default double length(Node node) { return length(node.getLayoutBounds()); }
    default double breadth(Node node) { return breadth(node.getLayoutBounds()); }
    default double minY(Node node) { return layoutY(node) + minY(node.getLayoutBounds()); }
    default double maxY(Node node) { return minY(node) + length(node); }
    default double minX(Node node) { return layoutX(node) + minX(node.getLayoutBounds()); }
    default double maxX(Node node) { return minX(node) + breadth(node); }
    default double minY(Cell<?, ?> cell) { return minY(cell.getNode()); }
    default double maxY(Cell<?, ?> cell) { return maxY(cell.getNode()); }
    double minBreadth(Node cell);
    double prefBreadth(Node cell);
    double prefLength(Node cell, double breadth);
    void resizeRelocate(Node cell, double b0, double l0, double breadth, double length);
    void relocate(Node cell, double b0, double l0);

    ObservableDoubleValue widthEstimateProperty(VirtualFlowContent<?, ?> content);
    ObservableDoubleValue heightEstimateProperty(VirtualFlowContent<?, ?> content);
    ObservableDoubleValue horizontalPositionProperty(VirtualFlowContent<?, ?> content);
    ObservableDoubleValue verticalPositionProperty(VirtualFlowContent<?, ?> content);
    void setHorizontalPosition(VirtualFlowContent<?, ?> content, double pos);
    void setVerticalPosition(VirtualFlowContent<?, ?> content, double pos);
    void scrollHorizontally(VirtualFlowContent<?, ?> content, double dx);
    void scrollVertically(VirtualFlowContent<?, ?> content, double dy);

    default double getHorizontalPosition(VirtualFlowContent<?, ?> content) {
        return horizontalPositionProperty(content).get();
    }
    default double getVerticalPosition(VirtualFlowContent<?, ?> content) {
        return verticalPositionProperty(content).get();
    }
}

final class HorizontalFlowMetrics implements Metrics {

    @Override
    public Orientation getContentBias() {
        return Orientation.VERTICAL;
    }

    @Override
    public double minBreadth(Node cell) {
        return cell.minHeight(-1);
    }

    @Override
    public double prefBreadth(Node cell) {
        return cell.prefHeight(-1);
    }

    @Override
    public double prefLength(Node cell, double breadth) {
        return cell.prefWidth(breadth);
    }

    @Override
    public double breadth(Bounds bounds) {
        return bounds.getHeight();
    }

    @Override
    public double length(Bounds bounds) {
        return bounds.getWidth();
    }

    @Override
    public double minX(Bounds bounds) {
        return bounds.getMinY();
    }

    @Override
    public double minY(Bounds bounds) {
        return bounds.getMinX();
    }

    @Override
    public double layoutX(Node node) {
        return node.getLayoutY();
    }

    @Override
    public double layoutY(Node node) {
        return node.getLayoutX();
    }

    @Override
    public void resizeRelocate(
            Node cell, double b0, double l0, double breadth, double length) {
        cell.resizeRelocate(l0, b0, length, breadth);
    }

    @Override
    public void relocate(Node cell, double b0, double l0) {
        cell.relocate(l0, b0);
    }

    @Override
    public ObservableDoubleValue widthEstimateProperty(
            VirtualFlowContent<?, ?> content) {
        return content.totalLengthEstimateProperty();
    }

    @Override
    public ObservableDoubleValue heightEstimateProperty(
            VirtualFlowContent<?, ?> content) {
        return content.totalBreadthEstimateProperty();
    }

    @Override
    public ObservableDoubleValue horizontalPositionProperty(
            VirtualFlowContent<?, ?> content) {
        return content.lengthPositionEstimateProperty();
    }

    @Override
    public ObservableDoubleValue verticalPositionProperty(
            VirtualFlowContent<?, ?> content) {
        return content.breadthPositionEstimateProperty();
    }

    @Override
    public void setHorizontalPosition(VirtualFlowContent<?, ?> content,
            double pos) {
        content.setLengthPosition(pos);
    }

    @Override
    public void setVerticalPosition(VirtualFlowContent<?, ?> content, double pos) {
        content.setBreadthPosition(pos);
    }

    @Override
    public void scrollHorizontally(VirtualFlowContent<?, ?> content, double dx) {
        content.scrollLength(dx);
    }

    @Override
    public void scrollVertically(VirtualFlowContent<?, ?> content, double dy) {
        content.scrollBreadth(dy);
    }
}

final class VerticalFlowMetrics implements Metrics {

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    public double minBreadth(Node cell) {
        return cell.minWidth(-1);
    }

    @Override
    public double prefBreadth(Node cell) {
        return cell.prefWidth(-1);
    }

    @Override
    public double prefLength(Node cell, double breadth) {
        return cell.prefHeight(breadth);
    }

    @Override
    public double breadth(Bounds bounds) {
        return bounds.getWidth();
    }

    @Override
    public double length(Bounds bounds) {
        return bounds.getHeight();
    }

    @Override
    public double minX(Bounds bounds) {
        return bounds.getMinX();
    }

    @Override
    public double minY(Bounds bounds) {
        return bounds.getMinY();
    }

    @Override
    public double layoutX(Node node) {
        return node.getLayoutX();
    }

    @Override
    public double layoutY(Node node) {
        return node.getLayoutY();
    }

    @Override
    public void resizeRelocate(
            Node cell, double b0, double l0, double breadth, double length) {
        cell.resizeRelocate(b0, l0, breadth, length);
    }

    @Override
    public void relocate(Node cell, double b0, double l0) {
        cell.relocate(b0, l0);
    }

    @Override
    public ObservableDoubleValue widthEstimateProperty(
            VirtualFlowContent<?, ?> content) {
        return content.totalBreadthEstimateProperty();
    }

    @Override
    public ObservableDoubleValue heightEstimateProperty(
            VirtualFlowContent<?, ?> content) {
        return content.totalLengthEstimateProperty();
    }

    @Override
    public ObservableDoubleValue horizontalPositionProperty(
            VirtualFlowContent<?, ?> content) {
        return content.breadthPositionEstimateProperty();
    }

    @Override
    public ObservableDoubleValue verticalPositionProperty(
            VirtualFlowContent<?, ?> content) {
        return content.lengthPositionEstimateProperty();
    }

    @Override
    public void setHorizontalPosition(VirtualFlowContent<?, ?> content,
            double pos) {
        content.setBreadthPosition(pos);
    }

    @Override
    public void setVerticalPosition(VirtualFlowContent<?, ?> content, double pos) {
        content.setLengthPosition(pos);
    }

    @Override
    public void scrollHorizontally(VirtualFlowContent<?, ?> content, double dx) {
        content.scrollBreadth(dx);
    }

    @Override
    public void scrollVertically(VirtualFlowContent<?, ?> content, double dy) {
        content.scrollLength(dy);
    }
}