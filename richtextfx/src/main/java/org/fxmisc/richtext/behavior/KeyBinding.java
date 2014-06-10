/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates and Tomas Mikula.
 * All rights reserved.
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

package org.fxmisc.richtext.behavior;

import java.util.function.Function;

import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import com.sun.javafx.tk.Toolkit;

/**
 * KeyBindings are used to describe which action should occur based on some
 * KeyEvent state and Control state. These bindings are used to populate the
 * keyBindings variable on BehaviorBase. The KeyBinding can be subclassed to
 * add additional matching criteria. A match in a subclass should always have
 * a specificity that is 1 greater than its superclass in the case of a match,
 * or 0 in the case where there is no match.
 *
 * Note that this API is, at present, quite odd in that you use a constructor
 * and then use shift(), ctrl(), alt(), or meta() separately. It gave me an
 * object-literal like approach but isn't ideal. We will want some builder
 * approach here (similar as in other places).
 */
class KeyBinding<A> {
    private final KeyCode code;
    private final String character;
    private final EventType<KeyEvent> eventType;
    private final Function<KeyEvent, A> actionFactory;
    private Boolean shift = false;
    private Boolean ctrl = false;
    private Boolean alt = false;
    private Boolean meta = false;

    public KeyBinding(KeyCode code, A action) {
        this(code, KeyEvent.KEY_PRESSED, e -> action);
    }

    public KeyBinding(KeyCode code, Function<KeyEvent, A> actionFactory) {
        this(code, KeyEvent.KEY_PRESSED, actionFactory);
    }

    public KeyBinding(KeyCode code, EventType<KeyEvent> type, A action) {
        this(code, type, e -> action);
    }

    public KeyBinding(KeyCode code, EventType<KeyEvent> type, Function<KeyEvent, A> actionFactory) {
        this.code = code;
        this.character = null;
        this.eventType = type;
        this.actionFactory = actionFactory;
    }

    public KeyBinding(String character, EventType<KeyEvent> type, A action) {
        this(character, type, e -> action);
    }

    public KeyBinding(String character, EventType<KeyEvent> type, Function<KeyEvent, A> actionFactory) {
        this.code = null;
        this.character = character;
        this.eventType = type;
        this.actionFactory = actionFactory;
    }

    public KeyBinding<A> shift() {
        return shift(true);
    }

    public KeyBinding<A> shift(Boolean value) {
        shift = value;
        return this;
    }

    public KeyBinding<A> ctrl() {
        return ctrl(true);
    }

    public KeyBinding<A> ctrl(Boolean value) {
        ctrl = value;
        return this;
    }

    public KeyBinding<A> alt() {
        return alt(true);
    }

    public KeyBinding<A> alt(Boolean value) {
        alt = value;
        return this;
    }

    public KeyBinding<A> meta() {
        return meta(true);
    }

    public KeyBinding<A> meta(Boolean value) {
        meta = value;
        return this;
    }

    public KeyBinding<A> shortcut() {
        switch (Toolkit.getToolkit().getPlatformShortcutKey()) {
            case SHIFT:
                return shift();

            case CONTROL:
                return ctrl();

            case ALT:
                return alt();

            case META:
                return meta();

            default:
                return this;
        }
    }

    public final KeyCode getCode() { return code; }
    public final EventType<KeyEvent> getType() { return eventType; }
    public final A getAction(KeyEvent event) { return actionFactory.apply(event); }
    public final Boolean getShift() { return shift; }
    public final Boolean getCtrl() { return ctrl; }
    public final Boolean getAlt() { return alt; }
    public final Boolean getMeta() { return meta; }

    public int getSpecificity(KeyEvent event) {
        int s = 0;
        if(character != null) if(character.equals(event.getCharacter())) s++; else return 0;
        if(code != null) if(code == event.getCode()) s++; else return 0;
        if(shift != null) if(shift == event.isShiftDown()) s++; else return 0;
        if(ctrl != null) if(ctrl == event.isControlDown()) s++; else return 0;
        if(alt != null) if(alt == event.isAltDown()) s++; else return 0;
        if(meta != null) if(meta == event.isMetaDown()) s++; else return 0;
        if(eventType != null) if(eventType == event.getEventType()) s++; else return 0;
        return s;
    }

    @Override
    public String toString() {
        return "KeyBinding [code=" + code + ", shift=" + shift +
                ", ctrl=" + ctrl + ", alt=" + alt +
                ", meta=" + meta + ", type=" + eventType + "]";
    }
}
