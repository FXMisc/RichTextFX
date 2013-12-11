/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates and Tomas Mikula.
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

package codearea.behavior;

import static javafx.scene.input.KeyCode.A;
import static javafx.scene.input.KeyCode.BACK_SLASH;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.C;
import static javafx.scene.input.KeyCode.COPY;
import static javafx.scene.input.KeyCode.CUT;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.END;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.F10;
import static javafx.scene.input.KeyCode.HOME;
import static javafx.scene.input.KeyCode.INSERT;
import static javafx.scene.input.KeyCode.KP_DOWN;
import static javafx.scene.input.KeyCode.KP_LEFT;
import static javafx.scene.input.KeyCode.KP_RIGHT;
import static javafx.scene.input.KeyCode.KP_UP;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.PAGE_DOWN;
import static javafx.scene.input.KeyCode.PAGE_UP;
import static javafx.scene.input.KeyCode.PASTE;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.TAB;
import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyCode.V;
import static javafx.scene.input.KeyCode.X;
import static javafx.scene.input.KeyCode.Y;
import static javafx.scene.input.KeyCode.Z;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_TYPED;

import java.util.ArrayList;
import java.util.List;

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.scene.control.behavior.KeyBinding;
import com.sun.javafx.scene.control.behavior.OptionalBoolean;

enum CodeAreaAction {
    Left,
    Right,
    LeftWord,
    RightWord,
    LineStart,
    LineEnd,
    PreviousLine,
    NextLine,
    PreviousPage,
    NextPage,
    TextStart,
    TextEnd,

    SelectLeft,
    SelectRight,
    SelectLeftWord,
    SelectLeftWordExtend,
    SelectRightWord,
    SelectRightWordExtend,
    SelectLineStart,
    SelectLineStartExtend,
    SelectLineEnd,
    SelectLineEndExtend,
    SelectPreviousLine,
    SelectNextLine,
    SelectPreviousPage,
    SelectNextPage,
    SelectTextStart,
    SelectTextStartExtend,
    SelectTextEnd,
    SelectTextEndExtend,
    SelectAll,
    Unselect,

    InsertNewLine(true),
    InsertTab(true),
    InputCharacter(true),

    DeletePreviousChar(true),
    DeleteNextChar(true),
    DeleteNextWord(true),
    DeletePreviousWord(true),

    Cut(true),
    Copy,
    Paste(true),

    Undo(true),
    Redo(true),

    ToParent;


    private final boolean isEdit;

    CodeAreaAction() {
        this(false);
    }

    CodeAreaAction(boolean isEdit) {
        this.isEdit = isEdit;
    }

    public boolean isEditAction() {
        return isEdit;
    }
}

public class CodeAreaBindings {
    static final List<KeyBinding> BINDINGS = new ArrayList<KeyBinding>();
    static {
        // caret movement
        BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,       CodeAreaAction.Right.name()));
        BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,    CodeAreaAction.Right.name()));
        BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,        CodeAreaAction.Left.name()));
        BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,     CodeAreaAction.Left.name()));
        BINDINGS.add(new KeyBinding(HOME, KEY_PRESSED,        CodeAreaAction.LineStart.name()));
        BINDINGS.add(new KeyBinding(END, KEY_PRESSED,         CodeAreaAction.LineEnd.name()));
        BINDINGS.add(new KeyBinding(UP, KEY_PRESSED,          CodeAreaAction.PreviousLine.name()));
        BINDINGS.add(new KeyBinding(KP_UP, KEY_PRESSED,       CodeAreaAction.PreviousLine.name()));
        BINDINGS.add(new KeyBinding(DOWN, KEY_PRESSED,        CodeAreaAction.NextLine.name()));
        BINDINGS.add(new KeyBinding(KP_DOWN, KEY_PRESSED,     CodeAreaAction.NextLine.name()));
        BINDINGS.add(new KeyBinding(PAGE_UP, KEY_PRESSED,     CodeAreaAction.PreviousPage.name()));
        BINDINGS.add(new KeyBinding(PAGE_DOWN, KEY_PRESSED,   CodeAreaAction.NextPage.name()));
        // deletion
        BINDINGS.add(new KeyBinding(BACK_SPACE, KEY_PRESSED,  CodeAreaAction.DeletePreviousChar.name()));
        BINDINGS.add(new KeyBinding(DELETE, KEY_PRESSED,      CodeAreaAction.DeleteNextChar.name()));
        // cut/copy/paste
        BINDINGS.add(new KeyBinding(CUT, KEY_PRESSED,         CodeAreaAction.Cut.name()));
        BINDINGS.add(new KeyBinding(DELETE, KEY_PRESSED,      CodeAreaAction.Cut.name()).shift());
        BINDINGS.add(new KeyBinding(COPY, KEY_PRESSED,        CodeAreaAction.Copy.name()));
        BINDINGS.add(new KeyBinding(PASTE, KEY_PRESSED,       CodeAreaAction.Paste.name()));
        BINDINGS.add(new KeyBinding(INSERT, KEY_PRESSED,      CodeAreaAction.Paste.name()).shift());// does this belong on mac?
        // selection
        BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,       CodeAreaAction.SelectRight.name()).shift());
        BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,    CodeAreaAction.SelectRight.name()).shift());
        BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,        CodeAreaAction.SelectLeft.name()).shift());
        BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,     CodeAreaAction.SelectLeft.name()).shift());
        BINDINGS.add(new KeyBinding(UP, KEY_PRESSED,          CodeAreaAction.SelectPreviousLine.name()).shift());
        BINDINGS.add(new KeyBinding(KP_UP, KEY_PRESSED,       CodeAreaAction.SelectPreviousLine.name()).shift());
        BINDINGS.add(new KeyBinding(DOWN, KEY_PRESSED,        CodeAreaAction.SelectNextLine.name()).shift());
        BINDINGS.add(new KeyBinding(KP_DOWN, KEY_PRESSED,     CodeAreaAction.SelectNextLine.name()).shift());
        BINDINGS.add(new KeyBinding(PAGE_UP, KEY_PRESSED,     CodeAreaAction.SelectPreviousPage.name()).shift());
        BINDINGS.add(new KeyBinding(PAGE_DOWN, KEY_PRESSED,   CodeAreaAction.SelectNextPage.name()).shift());
        // tab & newline
        BINDINGS.add(new KeyBinding(ENTER, KEY_PRESSED,       CodeAreaAction.InsertNewLine.name()));
        BINDINGS.add(new KeyBinding(TAB, KEY_PRESSED,         CodeAreaAction.InsertTab.name()));


        // platform specific settings
        if (PlatformUtil.isMac()) {
            BINDINGS.add(new KeyBinding(HOME, KEY_PRESSED,       CodeAreaAction.SelectLineStartExtend.name()).shift());
            BINDINGS.add(new KeyBinding(END, KEY_PRESSED,        CodeAreaAction.SelectLineEndExtend.name()).shift());
            BINDINGS.add(new KeyBinding(HOME, KEY_PRESSED,       CodeAreaAction.TextStart.name()).shortcut());
            BINDINGS.add(new KeyBinding(END, KEY_PRESSED,        CodeAreaAction.TextEnd.name()).shortcut());
            BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,       CodeAreaAction.LineStart.name()).shortcut());
            BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,    CodeAreaAction.LineStart.name()).shortcut());
            BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,      CodeAreaAction.LineEnd.name()).shortcut());
            BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,   CodeAreaAction.LineEnd.name()).shortcut());
            BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,       CodeAreaAction.LeftWord.name()).alt());
            BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,    CodeAreaAction.LeftWord.name()).alt());
            BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,      CodeAreaAction.RightWord.name()).alt());
            BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,   CodeAreaAction.RightWord.name()).alt());
            BINDINGS.add(new KeyBinding(DELETE, KEY_PRESSED,     CodeAreaAction.DeleteNextWord.name()).shortcut());
            BINDINGS.add(new KeyBinding(BACK_SPACE, KEY_PRESSED, CodeAreaAction.DeletePreviousWord.name()).shortcut());
            BINDINGS.add(new KeyBinding(X, KEY_PRESSED,          CodeAreaAction.Cut.name()).shortcut());
            BINDINGS.add(new KeyBinding(C, KEY_PRESSED,          CodeAreaAction.Copy.name()).shortcut());
            BINDINGS.add(new KeyBinding(INSERT, KEY_PRESSED,     CodeAreaAction.Copy.name()).shortcut());
            BINDINGS.add(new KeyBinding(V, KEY_PRESSED,          CodeAreaAction.Paste.name()).shortcut());
            BINDINGS.add(new KeyBinding(HOME, KEY_PRESSED,       CodeAreaAction.SelectTextStartExtend.name()).shift().shortcut());
            BINDINGS.add(new KeyBinding(END, KEY_PRESSED,        CodeAreaAction.SelectTextEndExtend.name()).shift().shortcut());
            BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,       CodeAreaAction.SelectLineStartExtend.name()).shift().shortcut());
            BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,    CodeAreaAction.SelectLineStartExtend.name()).shift().shortcut());
            BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,      CodeAreaAction.SelectLineEndExtend.name()).shift().shortcut());
            BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,   CodeAreaAction.SelectLineEndExtend.name()).shift().shortcut());
            BINDINGS.add(new KeyBinding(A, KEY_PRESSED,          CodeAreaAction.SelectAll.name()).shortcut());
            BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,       CodeAreaAction.SelectLeftWordExtend.name()).shift().alt());
            BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,    CodeAreaAction.SelectLeftWordExtend.name()).shift().alt());
            BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,      CodeAreaAction.SelectRightWordExtend.name()).shift().alt());
            BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,   CodeAreaAction.SelectRightWordExtend.name()).shift().alt());
            BINDINGS.add(new KeyBinding(Z, KEY_PRESSED,          CodeAreaAction.Undo.name()).shortcut());
            BINDINGS.add(new KeyBinding(Z, KEY_PRESSED,          CodeAreaAction.Redo.name()).shift().shortcut());
        } else {
            BINDINGS.add(new KeyBinding(HOME, KEY_PRESSED,       CodeAreaAction.SelectLineStart.name()).shift());
            BINDINGS.add(new KeyBinding(END, KEY_PRESSED,        CodeAreaAction.SelectLineEnd.name()).shift());
            BINDINGS.add(new KeyBinding(HOME, KEY_PRESSED,       CodeAreaAction.TextStart.name()).ctrl());
            BINDINGS.add(new KeyBinding(END, KEY_PRESSED,        CodeAreaAction.TextEnd.name()).ctrl());
            BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,       CodeAreaAction.LeftWord.name()).ctrl());
            BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,    CodeAreaAction.LeftWord.name()).ctrl());
            BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,      CodeAreaAction.RightWord.name()).ctrl());
            BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,   CodeAreaAction.RightWord.name()).ctrl());
            BINDINGS.add(new KeyBinding(DELETE, KEY_PRESSED,     CodeAreaAction.DeleteNextWord.name()).ctrl());
            BINDINGS.add(new KeyBinding(BACK_SPACE, KEY_PRESSED, CodeAreaAction.DeletePreviousWord.name()).ctrl());
            BINDINGS.add(new KeyBinding(X, KEY_PRESSED,          CodeAreaAction.Cut.name()).ctrl());
            BINDINGS.add(new KeyBinding(C, KEY_PRESSED,          CodeAreaAction.Copy.name()).ctrl());
            BINDINGS.add(new KeyBinding(INSERT, KEY_PRESSED,     CodeAreaAction.Copy.name()).ctrl());
            BINDINGS.add(new KeyBinding(V, KEY_PRESSED,          CodeAreaAction.Paste.name()).ctrl());
            BINDINGS.add(new KeyBinding(HOME, KEY_PRESSED,       CodeAreaAction.SelectTextStart.name()).ctrl().shift());
            BINDINGS.add(new KeyBinding(END, KEY_PRESSED,        CodeAreaAction.SelectTextEnd.name()).ctrl().shift());
            BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,       CodeAreaAction.SelectLeftWord.name()).ctrl().shift());
            BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,    CodeAreaAction.SelectLeftWord.name()).ctrl().shift());
            BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,      CodeAreaAction.SelectRightWord.name()).ctrl().shift());
            BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,   CodeAreaAction.SelectRightWord.name()).ctrl().shift());
            BINDINGS.add(new KeyBinding(A, KEY_PRESSED,          CodeAreaAction.SelectAll.name()).ctrl());
            BINDINGS.add(new KeyBinding(BACK_SLASH, KEY_PRESSED, CodeAreaAction.Unselect.name()).ctrl());
            BINDINGS.add(new KeyBinding(Z, KEY_PRESSED,          CodeAreaAction.Undo.name()).ctrl());
            BINDINGS.add(new KeyBinding(Z, KEY_PRESSED,          CodeAreaAction.Redo.name()).ctrl().shift());
            BINDINGS.add(new KeyBinding(Y, KEY_PRESSED,          CodeAreaAction.Redo.name()).ctrl());
        }
        // Any other key press first goes to normal text input
        // Note this is KEY_TYPED because otherwise the character is not available in the event.
        BINDINGS.add(new KeyBinding(null, KEY_TYPED, CodeAreaAction.InputCharacter.name())
                .alt(OptionalBoolean.ANY)
                .shift(OptionalBoolean.ANY)
                .ctrl(OptionalBoolean.ANY)
                .meta(OptionalBoolean.ANY));

        // The following keys are forwarded to the parent container
        BINDINGS.add(new KeyBinding(ESCAPE, CodeAreaAction.ToParent.name()));
        BINDINGS.add(new KeyBinding(F10, CodeAreaAction.ToParent.name()));
    }
}
