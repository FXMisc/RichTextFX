package codearea.control;

import javafx.scene.control.Control;

/**
 * Transient common superclass for {@link StyledTextArea} and
 * {@link StyledTextArea2}, which will stay in place only until refactoring
 * is complete.
 */
public abstract class StyledTextAreaBase<S> extends Control
implements TextEditingArea<S>, EditActions<S>, ClipboardActions<S>, NavigationActions<S>, UndoActions, TwoDimensional {

}
