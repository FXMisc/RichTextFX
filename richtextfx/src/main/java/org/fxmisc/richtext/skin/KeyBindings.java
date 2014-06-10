package org.fxmisc.richtext.skin;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.scene.input.KeyCode;

import org.fxmisc.richtext.skin.StyledTextAreaBehavior.Actions;

import com.sun.javafx.PlatformUtil;

class KeyBindings {
    static final List<KeyBinding<? extends Action>> BINDINGS;
    static {
        if(PlatformUtil.isMac()) {
            BINDINGS = new ArrayList<>(CommonBindings.BINDINGS.size() + MacBindings.BINDINGS.size());
            BINDINGS.addAll(CommonBindings.BINDINGS);
            BINDINGS.addAll(MacBindings.BINDINGS);
        } else {
            BINDINGS = new ArrayList<>(CommonBindings.BINDINGS.size() + PcBindings.BINDINGS.size());
            BINDINGS.addAll(CommonBindings.BINDINGS);
            BINDINGS.addAll(PcBindings.BINDINGS);
        }
    }
}

class CommonBindings {
    static final List<KeyBinding<? extends Action>> BINDINGS = Arrays.asList(
            // caret movement
            new KeyBinding<>(RIGHT,      Actions.Right),
            new KeyBinding<>(KP_RIGHT,   Actions.Right),
            new KeyBinding<>(LEFT,       Actions.Left),
            new KeyBinding<>(KP_LEFT,    Actions.Left),
            new KeyBinding<>(HOME,       Actions.LineStart),
            new KeyBinding<>(END,        Actions.LineEnd),
            new KeyBinding<>(UP,         Actions.PreviousLine),
            new KeyBinding<>(KP_UP,      Actions.PreviousLine),
            new KeyBinding<>(DOWN,       Actions.NextLine),
            new KeyBinding<>(KP_DOWN,    Actions.NextLine),
            new KeyBinding<>(PAGE_UP,    Actions.PreviousPage),
            new KeyBinding<>(PAGE_DOWN,  Actions.NextPage),
            // deletion
            new KeyBinding<>(BACK_SPACE, Actions.DeletePreviousChar),
            new KeyBinding<>(DELETE,     Actions.DeleteNextChar),
            // cut/copy/paste
            new KeyBinding<>(CUT,        Actions.Cut),
            new KeyBinding<>(DELETE,     Actions.Cut).shift(),
            new KeyBinding<>(COPY,       Actions.Copy),
            new KeyBinding<>(PASTE,      Actions.Paste),
            new KeyBinding<>(INSERT,     Actions.Paste).shift(),
            // selection
            new KeyBinding<>(RIGHT,      Actions.SelectRight).shift(),
            new KeyBinding<>(KP_RIGHT,   Actions.SelectRight).shift(),
            new KeyBinding<>(LEFT,       Actions.SelectLeft).shift(),
            new KeyBinding<>(KP_LEFT,    Actions.SelectLeft).shift(),
            new KeyBinding<>(UP,         Actions.SelectPreviousLine).shift(),
            new KeyBinding<>(KP_UP,      Actions.SelectPreviousLine).shift(),
            new KeyBinding<>(DOWN,       Actions.SelectNextLine).shift(),
            new KeyBinding<>(KP_DOWN,    Actions.SelectNextLine).shift(),
            new KeyBinding<>(PAGE_UP,    Actions.SelectPreviousPage).shift(),
            new KeyBinding<>(PAGE_DOWN,  Actions.SelectNextPage).shift(),

            // tab & newline
            new KeyBinding<>(ENTER, Actions.InsertNewLine),
            new KeyBinding<>(TAB,   Actions.InsertTab),

            // Consume KEY_TYPED events for Enter and Tab,
            // because they are already handled as KEY_PRESSED
            new KeyBinding<>("\t", KEY_TYPED, Actions.Consume).alt(null).shift(null).ctrl(null).meta(null),
            new KeyBinding<>("\n", KEY_TYPED, Actions.Consume).alt(null).shift(null).ctrl(null).meta(null),
            new KeyBinding<>("\r", KEY_TYPED, Actions.Consume).alt(null).shift(null).ctrl(null).meta(null),
            new KeyBinding<>("\r\n", KEY_TYPED, Actions.Consume).alt(null).shift(null).ctrl(null).meta(null),

            // Note this is KEY_TYPED because otherwise the character is not available in the event.
            new KeyBinding<>((KeyCode) null, KEY_TYPED, Actions.InputCharacter)
                    .alt(null).shift(null).ctrl(null).meta(null));
}

class MacBindings {
    static final List<KeyBinding<? extends Action>> BINDINGS = Arrays.asList(
            new KeyBinding<>(HOME,       Actions.SelectLineStartExtend).shift(),
            new KeyBinding<>(END,        Actions.SelectLineEndExtend).shift(),
            new KeyBinding<>(HOME,       Actions.TextStart).shortcut(),
            new KeyBinding<>(END,        Actions.TextEnd).shortcut(),
            new KeyBinding<>(LEFT,       Actions.LineStart).shortcut(),
            new KeyBinding<>(KP_LEFT,    Actions.LineStart).shortcut(),
            new KeyBinding<>(RIGHT,      Actions.LineEnd).shortcut(),
            new KeyBinding<>(KP_RIGHT,   Actions.LineEnd).shortcut(),
            new KeyBinding<>(LEFT,       Actions.LeftWord).alt(),
            new KeyBinding<>(KP_LEFT,    Actions.LeftWord).alt(),
            new KeyBinding<>(RIGHT,      Actions.RightWord).alt(),
            new KeyBinding<>(KP_RIGHT,   Actions.RightWord).alt(),
            new KeyBinding<>(DELETE,     Actions.DeleteNextWord).shortcut(),
            new KeyBinding<>(BACK_SPACE, Actions.DeletePreviousWord).shortcut(),
            new KeyBinding<>(X,          Actions.Cut).shortcut(),
            new KeyBinding<>(C,          Actions.Copy).shortcut(),
            new KeyBinding<>(INSERT,     Actions.Copy).shortcut(),
            new KeyBinding<>(V,          Actions.Paste).shortcut(),
            new KeyBinding<>(HOME,       Actions.SelectTextStartExtend).shift().shortcut(),
            new KeyBinding<>(END,        Actions.SelectTextEndExtend).shift().shortcut(),
            new KeyBinding<>(LEFT,       Actions.SelectLineStartExtend).shift().shortcut(),
            new KeyBinding<>(KP_LEFT,    Actions.SelectLineStartExtend).shift().shortcut(),
            new KeyBinding<>(RIGHT,      Actions.SelectLineEndExtend).shift().shortcut(),
            new KeyBinding<>(KP_RIGHT,   Actions.SelectLineEndExtend).shift().shortcut(),
            new KeyBinding<>(A,          Actions.SelectAll).shortcut(),
            new KeyBinding<>(LEFT,       Actions.SelectLeftWordExtend).shift().alt(),
            new KeyBinding<>(KP_LEFT,    Actions.SelectLeftWordExtend).shift().alt(),
            new KeyBinding<>(RIGHT,      Actions.SelectRightWordExtend).shift().alt(),
            new KeyBinding<>(KP_RIGHT,   Actions.SelectRightWordExtend).shift().alt(),
            new KeyBinding<>(Z,          Actions.Undo).shortcut(),
            new KeyBinding<>(Z,          Actions.Redo).shift().shortcut());
}

class PcBindings {
    static final List<KeyBinding<? extends Action>> BINDINGS = Arrays.asList(
            new KeyBinding<>(HOME,       Actions.SelectLineStart).shift(),
            new KeyBinding<>(END,        Actions.SelectLineEnd).shift(),
            new KeyBinding<>(HOME,       Actions.TextStart).ctrl(),
            new KeyBinding<>(END,        Actions.TextEnd).ctrl(),
            new KeyBinding<>(LEFT,       Actions.LeftWord).ctrl(),
            new KeyBinding<>(KP_LEFT,    Actions.LeftWord).ctrl(),
            new KeyBinding<>(RIGHT,      Actions.RightWord).ctrl(),
            new KeyBinding<>(KP_RIGHT,   Actions.RightWord).ctrl(),
            new KeyBinding<>(DELETE,     Actions.DeleteNextWord).ctrl(),
            new KeyBinding<>(BACK_SPACE, Actions.DeletePreviousWord).ctrl(),
            new KeyBinding<>(X,          Actions.Cut).ctrl(),
            new KeyBinding<>(C,          Actions.Copy).ctrl(),
            new KeyBinding<>(INSERT,     Actions.Copy).ctrl(),
            new KeyBinding<>(V,          Actions.Paste).ctrl(),
            new KeyBinding<>(HOME,       Actions.SelectTextStart).ctrl().shift(),
            new KeyBinding<>(END,        Actions.SelectTextEnd).ctrl().shift(),
            new KeyBinding<>(LEFT,       Actions.SelectLeftWord).ctrl().shift(),
            new KeyBinding<>(KP_LEFT,    Actions.SelectLeftWord).ctrl().shift(),
            new KeyBinding<>(RIGHT,      Actions.SelectRightWord).ctrl().shift(),
            new KeyBinding<>(KP_RIGHT,   Actions.SelectRightWord).ctrl().shift(),
            new KeyBinding<>(A,          Actions.SelectAll).ctrl(),
            new KeyBinding<>(BACK_SLASH, Actions.Unselect).ctrl(),
            new KeyBinding<>(Z,          Actions.Undo).ctrl(),
            new KeyBinding<>(Z,          Actions.Redo).ctrl().shift(),
            new KeyBinding<>(Y,          Actions.Redo).ctrl());
}