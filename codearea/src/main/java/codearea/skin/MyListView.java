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

package codearea.skin;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import com.sun.javafx.scene.control.skin.VirtualFlow;

class MyListView<T, C extends ListCell<T>> extends ListView<T> {

    @SuppressWarnings("unchecked")
    public MyListView(ObservableList<T> items, Function<? super MyListView<T, C>, C> cellFactory) {
        super(items);
        super.setCellFactory(lv -> cellFactory.apply((MyListView<T, C>) lv));
    }

    public void show(int index) {
        getFlow().ifPresent(flow -> flow.show(index));
    }

    public void show(int index, Consumer<C> whenShown) {
        show(index, (flow, cell) -> whenShown.accept(cell));
    }

    public void show(int index, BiConsumer<VirtualFlow<C>, C> whenShown) {
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
     * Returns the cell for the given index.
     * The returned cell shall be used read-only
     * (for measurement purposes) and shall not be stored.
     */
    public Optional<C> getCell(int index) {
        // If the cell for the given index is not available (~visible),
        // VirtualFlow may return some arbitrary cell. Therefore we
        // check that the returned cell has the desired index.
        return getFlow()
                .map(flow -> flow.getCell(index))
                .filter(cell -> cell.getIndex() == index);
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
