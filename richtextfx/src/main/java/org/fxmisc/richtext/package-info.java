/**
 * Defines the view-related classes for rendering and editing an
 * {@link org.fxmisc.richtext.model.EditableStyledDocument EditableStyledDocument}.
 *
 * <p>
 *     The base area is {@link org.fxmisc.richtext.GenericStyledArea}. Those unfamiliar with this
 *     project should read through its javadoc. This class should be used for custom segments (e.g. text and images
 *     in the same area). {@link org.fxmisc.richtext.StyledTextArea} uses {@link java.lang.String}-only segments,
 *     and styling them are already supported in the two most common ways via
 *     {@link org.fxmisc.richtext.StyleClassedTextArea} and {@link org.fxmisc.richtext.InlineCssTextArea}.
 *     For those looking to use a base for a code editor, see {@link org.fxmisc.richtext.CodeArea}.
 * </p>
 * <p>
 *     For text fields there is {@link org.fxmisc.richtext.StyledTextField} using {@link java.lang.String}-only segments,
 *     and styling them are also already supported in the two most common ways via
 *     {@link org.fxmisc.richtext.StyleClassedTextField} and {@link org.fxmisc.richtext.InlineCssTextField}.
 * </p>
 *
 * @see org.fxmisc.richtext.model.EditableStyledDocument
 * @see org.fxmisc.richtext.model.TwoDimensional
 * @see org.fxmisc.richtext.model.TwoDimensional.Bias
 * @see org.fxmisc.richtext.GenericStyledArea
 * @see org.fxmisc.richtext.TextEditingArea
 * @see org.fxmisc.richtext.Caret
 * @see org.fxmisc.richtext.Selection
 */
package org.fxmisc.richtext;