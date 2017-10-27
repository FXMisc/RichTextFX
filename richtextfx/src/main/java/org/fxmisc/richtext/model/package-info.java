/**
 * Defines the support classes and operations related to {@link org.fxmisc.richtext.model.EditableStyledDocument},
 * the immutable model of rich-text content that can be rendered and edited.
 *
 * <p>
 *     An {@link org.fxmisc.richtext.model.EditableStyledDocument} acts as an immutable model for rich-text content
 *     that will be rendered by an object implementing the {@link org.fxmisc.richtext.TextEditingArea} interface.
 *     A {@link org.fxmisc.richtext.model.StyledDocument} is composed of a list of
 *     {@link org.fxmisc.richtext.model.Paragraph}s. Paragraphs are nothing more than an
 *     object containing a paragraph style (type {@code PS}), a list of a generic segments (type {@code SEG}), and a
 *     list of generic styles (type {@code S}) that can apply to a segment. Most of the time, either
 *     {@link org.fxmisc.richtext.model.EditableStyledDocument} or
 *     {@link org.fxmisc.richtext.model.ReadOnlyStyledDocument} are being used to implement that interface.
 * </p>
 * <p>
 *     The document can include more than just text; thus, the segment generic
 *     can be specified as regular text ({@link java.lang.String}) or as an {@link org.reactfx.util.Either} (e.g.
 *     {@code Either<String, Image>} or as a nested Either (e.g.
 *     {@code Either<String, Either<Image, Either<Circle, Square>}) if one wanted to have four different kinds of segments
 *     (ways to specify a segment generic in a way that still makes the code easy to read are not described here).
 * </p>
 * <p>
 *     To allow these generics, one must supply a {@link org.fxmisc.richtext.model.SegmentOps} object that can
 *     correctly operate on the generic segments and their generic styles. In addition, a
 *     {@link org.fxmisc.richtext.model.TextOps} adds one more method to its base interface by adding a method
 *     that maps a {@link java.lang.String} to a given segment. For text-based custom segments, one should use
 *     {@link org.fxmisc.richtext.model.SegmentOpsBase} and for node-based custom segments, one should use
 *     {@link org.fxmisc.richtext.model.NodeSegmentOpsBase}.
 * </p>
 * <p>
 *     The document also uses {@link org.fxmisc.richtext.model.StyleSpans} to store styles in a memory-efficient way.
 *     To construct one, use {@link org.fxmisc.richtext.model.StyleSpans#singleton(org.fxmisc.richtext.model.StyleSpan)}
 *     or {@link org.fxmisc.richtext.model.StyleSpansBuilder}.
 * </p>
 * <p>
 *     To navigate throughout the document, read through the javadoc of
 *     {@link org.fxmisc.richtext.model.TwoDimensional} and {@link org.fxmisc.richtext.model.TwoDimensional.Bias}.
 *     Also, read the difference between "position" and "index" in
 *     {@link org.fxmisc.richtext.model.StyledDocument#getAbsolutePosition(int, int)}.
 * </p>
 * <p>To serialize things correctly, see {@link org.fxmisc.richtext.model.Codec} and its static factory methods.
 * </p>
 * <p>
 *     Lastly, the {@link org.fxmisc.richtext.model.EditableStyledDocument} can emit
 *     {@link org.fxmisc.richtext.model.PlainTextChange}s or {@link org.fxmisc.richtext.model.RichTextChange}s
 *     that can be used to undo/redo various changes.
 * </p>
 *
 * @see org.fxmisc.richtext.model.EditableStyledDocument
 * @see org.fxmisc.richtext.model.Paragraph
 * @see org.fxmisc.richtext.model.SegmentOps
 * @see org.fxmisc.richtext.model.TwoDimensional
 * @see org.fxmisc.richtext.model.TwoDimensional.Bias
 */
package org.fxmisc.richtext.model;