package org.fxmisc.richtext.demo.hyperlink;

public class Hyperlink<S> {

    private final String originalDisplayedText;
    private final String displayedText;
    private final S style;
    private final String link;

    Hyperlink(String originalDisplayedText, String displayedText, S style, String link) {
        this.originalDisplayedText = originalDisplayedText;
        this.displayedText = displayedText;
        this.style = style;
        this.link = link;
    }

    public boolean isEmpty() {
        return length() == 0;
    }

    public boolean isReal() {
        return length() > 0;
    }

    public boolean shareSameAncestor(Hyperlink<S> other) {
        return link.equals(other.link) && originalDisplayedText.equals(other.originalDisplayedText);
    }

    public int length() {
        return displayedText.length();
    }

    public char charAt(int index) {
        return isEmpty() ? '\0' : displayedText.charAt(index);
    }

    public String getOriginalDisplayedText() { return originalDisplayedText; }

    public String getDisplayedText() {
        return displayedText;
    }

    public String getLink() {
        return link;
    }

    public Hyperlink<S> subSequence(int start, int end) {
        return new Hyperlink<>(originalDisplayedText, displayedText.substring(start, end), style, link);
    }

    public Hyperlink<S> subSequence(int start) {
        return new Hyperlink<>(originalDisplayedText, displayedText.substring(start), style, link);
    }

    public S getStyle() {
        return style;
    }

    public Hyperlink<S> setStyle(S style) {
        return new Hyperlink<>(originalDisplayedText, displayedText, style, link);
    }

    public Hyperlink<S> mapDisplayedText(String text) {
        return new Hyperlink<>(originalDisplayedText, text, style, link);
    }

    @Override
    public String toString() {
        return isEmpty()
                ? String.format("EmptyHyperlink[original=%s style=%s link=%s]", originalDisplayedText, style, link)
                : String.format("RealHyperlink[original=%s displayedText=%s, style=%s, link=%s]",
                                    originalDisplayedText, displayedText, style, link);
    }

}
