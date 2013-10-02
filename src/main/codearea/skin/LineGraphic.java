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

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextFlow;
import codearea.control.Line;
import codearea.control.StyledString;

import com.sun.javafx.scene.text.HitInfo;

public class LineGraphic extends TextFlow {

	// FIXME: changing it currently has not effect, because
	// Text.impl_selectionFillProperty().set(newFill) doesn't work
	// properly for Text node inside a TextFlow (as of JDK8-b100).
	public final ObjectProperty<Paint> highlightTextFill = new SimpleObjectProperty<Paint>(Color.WHITE);

	private final Line line;

    private final Path caret = new Path();
    private final Rectangle selectionHighlightRect = new Rectangle();

    private final ChangeListener<Number> updateCaretPosition = new ChangeListener<Number>() {
		@Override
		public void changed(ObservableValue<? extends Number> observable,
				Number oldPos, Number newPos) {
			setCaretPosition(newPos.intValue());
		}
	};

	private final ChangeListener<IndexRange> updateSelection = new ChangeListener<IndexRange>() {
		@Override
		public void changed(ObservableValue<? extends IndexRange> observable,
				IndexRange oldRange, IndexRange newRange) {
			setSelection(newRange);
		}
	};

	private PumpedUpText currentTextNode;

	public LineGraphic(Line line) {
		this.line = line;

		setPrefWidth(Region.USE_COMPUTED_SIZE); // no wrapping

        // selection highlight
        selectionHighlightRect.setManaged(false);
        selectionHighlightRect.setVisible(true);
        selectionHighlightRect.setFill(Color.DODGERBLUE);
        getChildren().add(selectionHighlightRect);

        // caret
        caret.setManaged(false);
        caret.setStrokeWidth(1);
        getChildren().add(caret);

        // XXX: see the note at highlightTextFill
//        highlightTextFill.addListener(new ChangeListener<Paint>() {
//			@Override
//			public void changed(ObservableValue<? extends Paint> observable,
//					Paint oldFill, Paint newFill) {
//				for(PumpedUpText text: textNodes())
//					text.impl_selectionFillProperty().set(newFill);
//			}
//        });

        // populate with text nodes
		for(StyledString segment: line.getSegments()) {
			PumpedUpText t = new PumpedUpText(segment.toString());
	        t.setTextOrigin(VPos.TOP);
			t.getStyleClass().add("text");
			t.getStyleClass().addAll(segment.getStyleClasses());

	        // XXX: binding selectionFill to textFill,
			// see the note at highlightTextFill
			t.impl_selectionFillProperty().bind(t.fillProperty());

			// keep the caret graphic up to date
			InvalidationListener updateCaret = new InvalidationListener() {
				@Override
				public void invalidated(Observable boundsProperty) {
					updateCaretPath();
				}
			};
			t.caretShapeProperty().addListener(updateCaret);
			t.layoutXProperty().addListener(updateCaret);

			// keep the selection graphic up to date
			InvalidationListener updateSelectionHighlight = new InvalidationListener() {
				@Override
				public void invalidated(Observable boundsProperty) {
					updateSelectionHighlight();
				}
			};
			t.selectionBoundsProperty().addListener(updateSelectionHighlight);
			t.layoutXProperty().addListener(updateSelectionHighlight);

			getChildren().add(t);
		}
		this.currentTextNode = (PumpedUpText) getChildren().get(2); // skip caret and selection highlight

		// keep caret position up to date
		line.caretPositionProperty().addListener(updateCaretPosition);
		setCaretPosition(line.getCaretPosition());

		// keep selection up to date
		line.selectionProperty().addListener(updateSelection);
		setSelection(line.getSelection());
	}

	public void dispose() {
		line.caretPositionProperty().removeListener(updateCaretPosition);
		line.selectionProperty().removeListener(updateSelection);
	}

	public BooleanProperty caretVisibleProperty() {
		return caret.visibleProperty();
	}

	public ObjectProperty<Paint> highlightFillProperty() {
		return selectionHighlightRect.fillProperty();
	}

	public HitInfo hit(double x) {
		// compute y as the vertical center of the text flow
		Bounds bounds = getBoundsInLocal();
		double y = (bounds.getMinY() + bounds.getMaxY()) / 2;

		// hit the text node
		// XXX Hitting the first text node works because of implementation details in JDK8-b100,
		// where hit is delegated to TextLayout
		HitInfo hit = textNodes().iterator().next().impl_hitTestChar(new Point2D(x, y));

		if(hit.getCharIndex() == line.length()) // clicked beyond the end of line
			hit.setLeading(true); // prevent going to the start of the next line

		return hit;
	}

	private void setCaretPosition(int pos) {
		if(pos < 0)
			throw new IllegalArgumentException();

		if(pos > line.length())
			pos = line.length();

		for(PumpedUpText text: textNodes()) {
			if(pos <= text.getText().length()) {
				currentTextNode = text;
				text.setCaretPosition(pos);
				updateCaretPath(); // in case the above line didn't trigger this
				break;
			}
			else {
				pos -= text.getText().length();
			}
		}
	}

	private void updateCaretPath() {
		caret.getElements().setAll(currentTextNode.caretShapeProperty().get());
		caret.setTranslateX(currentTextNode.getLayoutX());
		caret.setTranslateY(currentTextNode.getLayoutY());
	}

	private void setSelection(IndexRange range) {
		int start = range.getStart();
		int end = range.getEnd();

		int offset = 0;
		for(PumpedUpText text: textNodes()) {
			int len = text.getText().length();
			if(start < offset + len && end > offset) {
				int a = Math.max(start - offset, 0);
				int b = Math.min(end - offset, len);
				text.setSelection(a, b);
			}
			else {
				text.setSelection(-1, -1);
			}
			offset += len;
		}
	}

	private void updateSelectionHighlight() {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

		this.layout(); // make sure text nodes are laid out even if the text flow is not displayed yet
        for(PumpedUpText text: textNodes()) {
        	Bounds bounds = text.selectionBoundsProperty().get();
        	if(!bounds.isEmpty()) {
        		minX = Math.min(minX, text.getLayoutX() + bounds.getMinX());
        		minY = Math.min(minY, text.getLayoutY() + bounds.getMinY());
        		maxX = Math.max(maxX, text.getLayoutX() + bounds.getMaxX());
        		maxY = Math.max(maxY, text.getLayoutY() + bounds.getMaxY());
        	}
        }

        double width = maxX - minX;
        double height = maxY - minY;

        selectionHighlightRect.setX(minX);
        selectionHighlightRect.setY(minY);
        selectionHighlightRect.setWidth(width);
        selectionHighlightRect.setHeight(height);
	}

	/**
	 * Utility method to iterate children of type Text.
	 *
	 * TODO: can be much shorter using subList(), stream() and map().
	 */
	private Iterable<PumpedUpText> textNodes() {
		return new Iterable<PumpedUpText>() {

			@Override
			public Iterator<PumpedUpText> iterator() {
				return new Iterator<PumpedUpText>() {
					private final Iterator<Node> it = getChildren().iterator();
					{
						it.next(); // skip caret
						it.next(); // skip selection highlight
					}

					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public PumpedUpText next() {
						return (PumpedUpText) it.next();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}

					@Override
					public void forEachRemaining(Consumer<? super PumpedUpText> action) {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public void forEach(Consumer<? super PumpedUpText> action) {
				throw new UnsupportedOperationException();

			}

			@Override
			public Spliterator<PumpedUpText> spliterator() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
