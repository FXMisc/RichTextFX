package org.fxmisc.richtext.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * A String with a single style of type S.
 *
 * This is a simple class suitable for use as the SEG type in the other classes
 * such as {@link Paragraph}, {@link org.fxmisc.richtext.GenericStyledArea}, {@link StyledDocument}, etc.
 *
 * This class is immutable.
 *
 * @param <S> The type of the style of the text.
 */
public class StyledText<S>  {

    /**
     * An implementation of TextOps for StyledText.  Useful for passing to the constructor
     * of {@link org.fxmisc.richtext.GenericStyledArea} and similar classes if you are using this class as the SEG type.
     */
    public static <S> TextOps<StyledText<S>, S> textOps() {
        return new TextOps<StyledText<S>, S>() {

            private final StyledText<S> emptySeg = new StyledText<>("", null);

            @Override
            public int length(StyledText<S> styledText) {
                return styledText.getText().length();
            }

            @Override
            public char charAt(StyledText<S> styledText, int index) {
                return styledText == emptySeg ? '\0' : styledText.getText().charAt(index);
            }

            @Override
            public String getText(StyledText<S> styledText) {
                return styledText.getText();
            }

            @Override
            public StyledText<S> subSequence(StyledText<S> styledText, int start, int end) {
                return styledText == emptySeg ? emptySeg : new StyledText<>(styledText.getText().substring(start, end), styledText.getStyle());
            }

            @Override
            public StyledText<S> subSequence(StyledText<S> styledText, int start) {
                return styledText == emptySeg ? emptySeg : new StyledText<>(styledText.getText().substring(start), styledText.getStyle());
            }

            @Override
            public S getStyle(StyledText<S> styledText) {
                return styledText.getStyle();
            }

            @Override
            public StyledText<S> setStyle(StyledText<S> seg, S style) {
                return seg == emptySeg ? emptySeg : seg.setStyle(style);
            }

            @Override
            public Optional<StyledText<S>> join(StyledText<S> left, StyledText<S> right) {
                return Objects.equals(left.getStyle(), right.getStyle())
                        ? Optional.of(new StyledText<>(left.getText() + right.getText(), left.getStyle()))
                        : Optional.empty();
            }

            @Override
            public StyledText<S> createEmpty() {
                return emptySeg;
            }

            @Override
            public StyledText<S> create(String text, S style) {
                return new StyledText<>(text, style);
            }
        };
    }

    /**
     * A codec which allows serialisation of this class to/from a data stream.
     *
     * Because S may be any type, you must pass a codec for it.  If your style
     * is String or Color, you can use {@link Codec#STRING_CODEC}/{@link Codec#COLOR_CODEC} respectively.
     */
    public static <S> Codec<StyledText<S>> codec(Codec<S> styleCodec) {
        return new Codec<StyledText<S>>() {

            @Override
            public String getName() {
                return "styled-text";
            }

            @Override
            public void encode(DataOutputStream os, StyledText<S> t) throws IOException {
                Codec.STRING_CODEC.encode(os, t.text);
                styleCodec.encode(os, t.style);
            }

            @Override
            public StyledText<S> decode(DataInputStream is) throws IOException {
                String text = Codec.STRING_CODEC.decode(is);
                S style = styleCodec.decode(is);
                return new StyledText<>(text, style);
            }

        };
    }


    private final String text;

    /**
     * The text content of this piece of styled text.
     */
    public String getText() { return text; }

    private final S style;

    /**
     * The style of this piece of styled text.
     */
    public S getStyle() { return style; }

    /**
     * Creates a new StyledText with the same content but the given style.
     */
    public StyledText<S> setStyle(S style) {
        return new StyledText<>(text, style);
    }

    /**
     * Creates a new StyledText with the given text content, and style.
     */
    public StyledText(String text, S style) {
        this.text = text;
        this.style = style;
    }

    @Override
    public String toString() {
        return String.format("StyledText[text=\"%s\", style=%s]", text, style);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof StyledText) {
            StyledText<?> that = (StyledText<?>) obj;
            return Objects.equals(this.text, that.text)
                && Objects.equals(this.style, that.style);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, style);
    }

}
