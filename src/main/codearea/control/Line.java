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

package codearea.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.IndexRange;

public final class Line {
	private final List<StyledString> segments = new ArrayList<>();

	// selection proprety
    private final ReadOnlyObjectWrapper<IndexRange> selection = new ReadOnlyObjectWrapper<IndexRange>(this, "selection", new IndexRange(0, 0));
    public final IndexRange getSelection() { return selection.get(); }
    public final ReadOnlyObjectProperty<IndexRange> selectionProperty() { return selection.getReadOnlyProperty(); }

    // caret position property
    private final ReadOnlyIntegerWrapper caretPosition = new ReadOnlyIntegerWrapper(0);
    public final int getCaretPosition() { return caretPosition.get(); }
    public final void setCaretPosition(int pos) {
    	if(pos < 0 || pos > length())
    		throw new IndexOutOfBoundsException();
    	caretPosition.set(pos);
    }
    public final ReadOnlyIntegerProperty caretPositionProperty() { return caretPosition.getReadOnlyProperty(); }


	public Line() {
		this("");
	}

	public Line(String text) {
		segments.add(new StyledString(text));
	}

	private Line(List<StyledString> segments) {
		if(segments.isEmpty())
			this.segments.add(new StyledString(""));
		else
			this.segments.addAll(segments);
	}

	public List<StyledString> getSegments() {
		return Collections.unmodifiableList(segments);
	}

	void setSelection(int start, int end) {
		selection.set(new IndexRange(start, end));
	}

	public int length() {
		return segments.stream().mapToInt(new ToIntFunction<StyledString>(){
			@Override
			public int applyAsInt(StyledString t) {
				return t.length();
			}
		}).sum();
	}

	public char charAt(int index) {
		if(index < 0)
			throw new IndexOutOfBoundsException();

		for(StyledString seg: segments)
			if(seg.length() > index)
				return seg.charAt(index);
			else
				index -= seg.length();

		throw new IndexOutOfBoundsException();
	}

	public void appendFrom(Line line) {
		if(line.length() == 0)
			return;

		if(length() == 0)
			segments.clear();

		int oldSegCount = segments.size();
		segments.addAll(line.segments);
		tryMergeAtSeg(oldSegCount);
	}

	public void append(CharSequence str) {
		int lastSegIdx = segments.size() - 1;
		StyledString lastSegment = segments.get(lastSegIdx);
		StyledString replacement = lastSegment.concat(str);
		segments.set(lastSegIdx, replacement);
	}

	public void insert(int offset, CharSequence str) {
		if(offset < 0)
			throw new IndexOutOfBoundsException();

		for(int i=0, n=segments.size(); i < n; ++i) {
			StyledString seg = segments.get(i);
			if(seg.length() >= offset) {
				segments.set(i, seg.spliced(offset, offset, str));
				return;
			}
			else
				offset -= seg.length();
		}

		throw new IndexOutOfBoundsException();
	}

	public void delete(int start, int end) {
		int i=0, offset=0;
		int seglen = segments.get(i).length();
		while(start > offset+seglen) {
			offset += seglen;
			seglen = segments.get(++i).length();
		}
		int firstSegIdx = i;
		int firstSegStart = start-offset;
		while(end > offset+seglen) {
			offset += seglen;
			seglen = segments.get(++i).length();
		}
		int lastSegIdx = i;
		int lastSegEnd = end-offset;

		if(firstSegIdx == lastSegIdx) {
			StyledString seg = segments.get(firstSegIdx);
			if(firstSegStart == 0 && lastSegEnd == seg.length() && segments.size() > 1)
				segments.remove(firstSegIdx);
			else
				segments.set(firstSegIdx, seg.spliced(firstSegStart, lastSegEnd, ""));
		}
		else {
			StyledString lastSeg = segments.get(lastSegIdx);
			if(lastSegEnd == lastSeg.length())
				segments.remove(lastSegIdx);
			else
				segments.set(lastSegIdx, lastSeg.spliced(0, lastSegEnd, ""));

			if(firstSegStart == 0 && segments.size() > 1)
				segments.remove(firstSegIdx);
			else {
				StyledString firstSeg = segments.get(firstSegIdx);
				segments.set(firstSegIdx, firstSeg.spliced(firstSegStart, firstSeg.length(), ""));
			}
		}

		caretPosition.set(Math.min(caretPosition.get(), length()));
	}

	public void setStyleClasses(Set<String> classes) {
		for(StyledString t: segments)
			t.setStyleClasses(classes);
	}

	public void setStyleClasses(int from, int to, Set<String> classes) {
	    if(from == to)
	        return;

		int fromSeg = splitAt(from);
		int toSeg = splitAt(to);

		if(toSeg - fromSeg > 1) {
			// merge segments into one
			StringBuilder sb = new StringBuilder(to - from);
			for(int i = fromSeg; i < toSeg; ++i)
				sb.append(segments.get(i));
			segments.subList(fromSeg, toSeg).clear();
			segments.add(fromSeg, new StyledString(sb.toString(), classes));
		}
		else {
			segments.get(fromSeg).setStyleClasses(classes);
		}

		tryMergeAtSeg(fromSeg+1);
		tryMergeAtSeg(fromSeg);
	}

	private int splitAt(int pos) {
		return splitAt(0, pos);
	}

	private int splitAt(int segIdx, int pos) {
	    if(pos == 0)
	        return segIdx;

		StyledString segment = segments.get(segIdx);
		if(segment.length() < pos) {
			return splitAt(segIdx+1, pos - segment.length());
		}
		else {
			if(segment.length() > pos) {
				StyledString left = segment.subSequence(0, pos);
				StyledString right = segment.subSequence(pos, segment.length());
				segments.set(segIdx, left);
				segments.add(segIdx+1, right);
			}
			return segIdx+1;
		}
	}

	/**
	 * Splits this line at the given position.
	 * Returns an array of 2 new Lines, corresponding to the left
	 * and right side of the split position.
	 * After return, the content of this line is undefined.
	 */
	public Line[] split(int pos) {
		int segIdx = splitAt(pos);
		Line left = new Line(segments.subList(0, segIdx));
		Line right = new Line(segments.subList(segIdx, segments.size()));
		return new Line[] { left, right };
	}

	private void tryMergeAtSeg(int segIdx) {
		if(segIdx == 0 || segIdx == segments.size())
			return;
		if(segIdx < 0 || segIdx > segments.size())
			throw new IndexOutOfBoundsException("index: " + segIdx + ", size: " + segments.size());

		StyledString left = segments.get(segIdx-1);
		StyledString right = segments.get(segIdx);
		if(left.getStyleClasses().equals(right.getStyleClasses())) {
			StyledString segment = new StyledString(left.toString()+right.toString(), left.getStyleClasses());
			segments.remove(segIdx);
			segments.set(segIdx-1, segment);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(length());
		for(StyledString seg: segments)
			sb.append(seg);
		return sb.toString();
	}
}