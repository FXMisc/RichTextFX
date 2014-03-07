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

package org.fxmisc.richtext;


public class StyledText<S> implements CharSequence {
    private final String text;
    private S style;

    public StyledText(String text, S style) {
        this.text = text;
        this.style= style;
    }

    @Override
    public int length() {
        return text.length();
    }

    @Override
    public char charAt(int index) {
        return text.charAt(index);
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public StyledText<S> subSequence(int start, int end) {
        return new StyledText<S>(text.substring(start, end), style);
    }

    public StyledText<S> subSequence(int start) {
        return new StyledText<S>(text.substring(start), style);
    }

    public StyledText<S> concat(CharSequence str) {
        return new StyledText<S>(text + str, style);
    }

    public StyledText<S> spliced(int from, int to, CharSequence replacement) {
        String left = text.substring(0, from);
        String right = text.substring(to);
        return new StyledText<S>(left + replacement + right, style);
    }

    public S getStyle() {
        return style;
    }

    public void setStyle(S style) {
        this.style = style;
    }
}