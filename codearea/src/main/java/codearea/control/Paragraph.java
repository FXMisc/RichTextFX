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

import static codearea.control.TwoDimensional.Bias.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import codearea.control.TwoDimensional.Position;

public final class Paragraph<S> implements CharSequence {
    private final List<StyledText<S>> segments;
    private final TwoLevelNavigator navigator;

    public Paragraph(String text, S style) {
        this(new StyledText<S>(text, style));
    }

    public Paragraph(StyledText<S> text) {
        this(Arrays.asList(text));
    }

    private Paragraph(List<StyledText<S>> segments) {
        assert !segments.isEmpty();
        this.segments = segments;
        navigator = new TwoLevelNavigator(
                () -> segments.size(),
                i -> segments.get(i).length());
    }

    public List<StyledText<S>> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    private int length = -1;
    @Override
    public int length() {
        if(length == -1) {
            length = segments.stream().mapToInt(s -> s.length()).sum();
        }
        return length;
    }

    @Override
    public char charAt(int index) {
        Position pos = navigator.offsetToPosition(index, Forward);
        return segments.get(pos.getMajor()).charAt(pos.getMinor());
    }

    public String substring(int from, int to) {
        return toString().substring(from, Math.min(to, length()));
    }

    public String substring(int from) {
        return toString().substring(from);
    }

    public Paragraph<S> append(Paragraph<S> p) {
        if(p.length() == 0)
            return this;

        if(length() == 0)
            return p;

        StyledText<S> left = segments.get(segments.size()-1);
        StyledText<S> right = p.segments.get(0);
        if(Objects.equals(left.getStyle(), right.getStyle())) {
            StyledText<S> segment = left.concat(right);
            List<StyledText<S>> segs = new ArrayList<>(segments.size() + p.segments.size() - 1);
            segs.addAll(segments.subList(0, segments.size()-1));
            segs.add(segment);
            segs.addAll(p.segments.subList(1, p.segments.size()));
            return new Paragraph<S>(segs);
        } else {
            List<StyledText<S>> segs = new ArrayList<>(segments.size() + p.segments.size());
            segs.addAll(segments);
            segs.addAll(p.segments);
            return new Paragraph<S>(segs);
        }
    }

    public Paragraph<S> append(CharSequence str) {
        List<StyledText<S>> segs = new ArrayList<>(segments);
        int lastIdx = segments.size() - 1;
        segs.set(lastIdx, segments.get(lastIdx).concat(str));
        return new Paragraph<S>(segs);
    }

    public Paragraph<S> insert(int offset, CharSequence str) {
        if(offset < 0 || offset > length())
            throw new IndexOutOfBoundsException(String.valueOf(offset));

        Position pos = navigator.offsetToPosition(offset, Backward);
        int segIdx = pos.getMajor();
        int segPos = pos.getMinor();
        StyledText<S> seg = segments.get(segIdx);
        StyledText<S> replacement = seg.spliced(segPos, segPos, str);
        List<StyledText<S>> segs = new ArrayList<>(segments);
        segs.set(segIdx, replacement);
        return new Paragraph<S>(segs);
    }

    @Override
    public Paragraph<S> subSequence(int start, int end) {
        return trim(end).subSequence(start);
    }

    public Paragraph<S> trim(int length) {
        if(length < length()) {
            Position pos = navigator.offsetToPosition(length, Backward);
            int segIdx = pos.getMajor();
            List<StyledText<S>> segs = new ArrayList<>(segIdx + 1);
            segs.addAll(segments.subList(0, segIdx));
            segs.add(segments.get(segIdx).subSequence(0, pos.getMinor()));
            return new Paragraph<S>(segs);
        } else {
            return this;
        }
    }

    public Paragraph<S> subSequence(int start) {
        if(start > 0) {
            Position pos = navigator.offsetToPosition(start, Forward);
            int segIdx = pos.getMajor();
            List<StyledText<S>> segs = new ArrayList<>(segments.size() - segIdx);
            segs.add(segments.get(segIdx).subSequence(pos.getMinor()));
            segs.addAll(segments.subList(segIdx + 1, segments.size()));
            return new Paragraph<S>(segs);
        } else {
            return this;
        }
    }

    public Paragraph<S> delete(int start, int end) {
        return trim(start).append(subSequence(end));
    }

    public Paragraph<S> restyle(S style) {
        return new Paragraph<S>(toString(), style);
    }

    public Paragraph<S> restyle(int from, int to, S style) {
        Paragraph<S> left = subSequence(0, from);
        Paragraph<S> middle = new Paragraph<S>(substring(from, to), style);
        Paragraph<S> right = subSequence(to);
        return left.append(middle).append(right);
    }

    /**
     * Returns style at the given character position.
     * If {@code charIdx < 0}, returns the style at the beginning of this line.
     * If {@code charIdx >= this.length()}, returns the style at the end of this line.
     */
    public S getStyleAt(int charIdx) {
        if(charIdx < 0)
            return segments.get(0).getStyle();

        Position pos = navigator.offsetToPosition(charIdx, Forward);
        return segments.get(pos.getMajor()).getStyle();
    }

    /**
     * Returns the string content of this paragraph.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(length());
        for(StyledText<S> seg: segments)
            sb.append(seg);
        return sb.toString();
    }
}