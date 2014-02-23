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
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import com.sun.javafx.scene.control.skin.VirtualFlow;

class MyListView<T> extends ListView<T> {

    public MyListView(ObservableList<T> items) {
        super(items);
    }

    public void show(int index) {
        getFlow().ifPresent(flow -> flow.show(index));
    }

    public void show(int index, Consumer<ListCell<T>> whenShown) {
        getFlow().ifPresent(flow -> {
            flow.show(index);
            Platform.runLater(() -> { // runLater to allow layout after show()
                ListCell<T> cell = flow.getCell(index);
                if(cell.getIndex() == index) { // true only if cell was cached
                    whenShown.accept(cell);
                }
            });
        });
    }

    public void showAsFirst(int index) {
        getFlow().ifPresent(flow -> showAsFirst(flow, index));
    }

    public void showAsLast(int index) {
        getFlow().ifPresent(flow -> showAsLast(flow, index));
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
    public Optional<ListCell<T>> getCell(int index) {
        return getFlow().map(flow -> flow.getCell(index));
    }

    private static <C extends IndexedCell<?>> void showAsFirst(VirtualFlow<C> flow, int index) {
        flow.show(index);
        C cell = flow.getVisibleCell(index);
        assert cell != null;
        flow.showAsFirst(cell);
    }

    private static <C extends IndexedCell<?>> void showAsLast(VirtualFlow<C> flow, int index) {
        flow.show(index);
        C cell = flow.getVisibleCell(index);
        assert cell != null;
        flow.showAsLast(cell);
    }

    @SuppressWarnings("unchecked")
    private Optional<VirtualFlow<? extends ListCell<T>>> getFlow() {
        for(Node child: getChildren()) {
            if(child instanceof VirtualFlow)
                return Optional.of((VirtualFlow<? extends ListCell<T>>) child);
        }
        return Optional.empty();
    }
}
