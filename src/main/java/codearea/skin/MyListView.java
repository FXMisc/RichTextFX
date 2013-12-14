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

import java.util.function.Consumer;
import java.util.function.Function;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListView;

import com.sun.javafx.scene.control.skin.VirtualFlow;

class MyListView<T> extends ListView<T> {

    public MyListView(ObservableList<T> items) {
        super(items);
    }

    public void show(int index) {
        withFlow(flow -> flow.show(index));
    }

    public void showAsFirst(int index) {
        withFlow(flow -> showAsFirst(flow, index));
    }

    public void showAsLast(int index) {
        withFlow(flow -> showAsLast(flow, index));
    }

    public int getFirstVisibleIndex() {
        return withFlow(f -> f.getFirstVisibleCell().getIndex(), -1);
    }

    public int getLastVisibleIndex() {
        return withFlow(f -> f.getLastVisibleCell().getIndex(), -1);
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

    private void withFlow(Consumer<VirtualFlow<?>> f) {
        VirtualFlow<?> flow = getFlow();
        if(flow != null)
            f.accept(flow);
    }

    private <R> R withFlow(Function<VirtualFlow<?>, R> f, R orElse) {
        VirtualFlow<?> flow = getFlow();
        return flow != null ? f.apply(flow) : orElse;
    }

    private VirtualFlow<?> getFlow() {
        for(Node child: getChildren()) {
            if(child instanceof VirtualFlow)
                return (VirtualFlow<?>) child;
        }
        return null;
    }
}
