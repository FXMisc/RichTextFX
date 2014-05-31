/*
 * Copyright (c) 2013, Tomas Mikula. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.fxmisc.richtext.skin;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import com.sun.javafx.scene.control.skin.VirtualFlow;

class MyListView<T, C extends ListCell<T>> extends ListView<T> {
    private final Function<? super MyListView<T, C>, C> cellFactory;

    @SuppressWarnings("unchecked")
    public MyListView(ObservableList<T> items, Function<? super MyListView<T, C>, C> cellFactory) {
        super(items);
        this.cellFactory = cellFactory;
        super.setCellFactory(lv -> cellFactory.apply((MyListView<T, C>) lv));
    }

    public void show(int index) {
        getFlow().ifPresent(flow -> flow.show(index));
    }

    public void show(int index, Consumer<T> whenShown) {
        show(index, (flow, cell) -> whenShown.accept(cell.getItem()));
    }

    private void show(int index, BiConsumer<VirtualFlow<C>, C> whenShown) {
        getFlow().ifPresent(flow -> {
            flow.show(index);
            Platform.runLater(() -> { // runLater to allow layout after show()
                C cell = flow.getCell(index);
                if(cell.getIndex() == index) { // true only if cell was cached
                    whenShown.accept(flow, cell);
                }
            });
        });
    }

    public void showAsFirst(int index) {
        show(index, (flow, cell) -> flow.showAsFirst(cell));
    }

    public void showAsLast(int index) {
        show(index, (flow, cell) -> flow.showAsLast(cell));
    }

    public int getFirstVisibleIndex() {
        Optional<Integer> index = getFlow().map(f -> {
            ListCell<T> cell = f.getFirstVisibleCell();
            return cell != null ? cell.getIndex() : -1;
        });
        return index.orElse(-1);
    }

    public int getLastVisibleIndex() {
        Optional<Integer> index = getFlow().map(f -> {
            ListCell<T> cell = f.getLastVisibleCell();
            return cell != null ? cell.getIndex() : -1;
        });
        return index.orElse(-1);
    }

    public IndexRange getVisibleRange() {
        Optional<IndexRange> range = getFlow().map(f -> {
            ListCell<T> first = f.getFirstVisibleCell();
            ListCell<T> last = f.getLastVisibleCell();
            if(first != null && last != null) {
                return new IndexRange(first.getIndex(), last.getIndex() + 1);
            } else {
                return new IndexRange(0, 0);
            }
        });
        return range.orElse(new IndexRange(0, 0));
    }

    /**
     * Returns the cell for the given index, if visible.
     * The returned cell shall be used read-only
     * (for measurement purposes) and shall not be stored.
     */
    public Optional<C> getVisibleCell(int index) {
        // If the cell for the given index is not available (~visible),
        // VirtualFlow may return some arbitrary cell. Therefore we
        // check that the returned cell has the desired index.
        return getFlow()
                .map(flow -> flow.getCell(index))
                .filter(cell -> cell.getIndex() == index);
    }

    /**
     * Performs an action on the cell for the given index.
     * If the cell with the given index is visible, the actual cell displayed
     * in the ListView is used. If there is no visible cell with the given
     * index, an artificial cell is created, laid out and used.
     * {@code action} shall use the cell read-only (for measurement purposes)
     * and shall not store it.
     */
    public void withCell(int index, Consumer<C> action) {
        mapCell(index, cell -> { action.accept(cell); return null; });
    }

    /**
     * Invokes the given function on the cell with the given index.
     * If the cell with the given index is visible, the actual cell displayed
     * in the ListView is used. If there is no visible cell with the given
     * index, an artificial cell is created, laid out and used.
     * {@code f} shall use the cell read-only (for measurement purposes)
     * and shall not store it.
     */
    public <U> U mapCell(int index, Function<C, U> f) {
        Optional<C> optCell = getVisibleCell(index);
        if(optCell.isPresent()) {
            return f.apply(optCell.get());
        } else {
            C cell = cellFactory.apply(this);

            // assign cell's item
            cell.updateListView(this);
            cell.updateIndex(index);

            // layout the cell
            double cellWidth, cellHeight;
            if(getOrientation() == Orientation.VERTICAL) {
                cellWidth = getWidth() - snappedLeftInset() - snappedRightInset();
                cellHeight = cell.prefHeight(cellWidth);
            } else {
                cellHeight = getHeight() - snappedTopInset() - snappedBottomInset();
                cellWidth = cell.prefWidth(cellHeight);
            }
            cell.resize(cellWidth, cellHeight);
            cell.layout();

            // perform action
            U res = f.apply(cell);

            // clean up cell
            cell.updateIndex(-1);

            return res;
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<VirtualFlow<C>> getFlow() {
        for(Node child: getChildren()) {
            if(child instanceof VirtualFlow)
                return Optional.of((VirtualFlow<C>) child);
        }
        return Optional.empty();
    }
}
