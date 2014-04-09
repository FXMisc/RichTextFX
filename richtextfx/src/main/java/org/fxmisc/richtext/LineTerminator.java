package org.fxmisc.richtext;

import java.util.regex.Pattern;

// For multi-character line terminators, it is important that any subsequence
// is also a valid line terminator, because we support splicing text at any
// position, even within line terminators.

public enum LineTerminator {
    CR("\r"),
    LF("\n"),
    CRLF("\r\n");

    public static LineTerminator from(String s) {
        for(LineTerminator t: values()) {
            if(t.asString().equals(s)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Not a line terminator: " + s);
    }

    public static boolean isLineTerminatorChar(char c) {
        return c == '\r' || c == '\n';
    }

    private static final Pattern regex = Pattern.compile("\r\n|\r|\n");
    public static Pattern regex() {
        return regex;
    }

    private final String s;

    private LineTerminator(String s) {
        this.s = s;
    }

    public String asString() { return s; }

    public int length() { return s.length(); }

    public LineTerminator trim(int length) {
        return from(s.substring(0, length));
    }

    public LineTerminator subSequence(int start) {
        return from(s.substring(start));
    }
}
