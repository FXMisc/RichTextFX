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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ChangeListener;
import javafx.util.Duration;

import com.sun.javafx.binding.ExpressionHelper;

final class BooleanPulse extends BooleanExpression {

    private ExpressionHelper<Boolean> listenerHelper;
    private final Timeline timeline;
    private boolean currentValue;

    public BooleanPulse(Duration duration) {
        this(duration, true);
    }

    public BooleanPulse(Duration duration, boolean initialValue) {
        currentValue = initialValue;

        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(new KeyFrame(duration, event -> toggle()));
    }

    public void start() {
        timeline.play();
    }

    public void start(boolean initialValue) {
        if(currentValue != initialValue)
            toggle();
        timeline.play();
    }

    public void stop() {
        timeline.stop();
    }

    public void stop(boolean terminalValue) {
        timeline.stop();
        if(currentValue != terminalValue)
            toggle();
    }

    private void toggle() {
        currentValue = !currentValue;
        ExpressionHelper.fireValueChangedEvent(listenerHelper);
    }

    @Override
    public boolean get() {
        return currentValue;
    }

    @Override
    public void addListener(ChangeListener<? super Boolean> listener) {
        listenerHelper = ExpressionHelper.addListener(listenerHelper, this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super Boolean> listener) {
        listenerHelper = ExpressionHelper.removeListener(listenerHelper, listener);
    }

    @Override
    public void addListener(InvalidationListener listener) {
        listenerHelper = ExpressionHelper.addListener(listenerHelper, this, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        listenerHelper = ExpressionHelper.removeListener(listenerHelper, listener);
    }
}