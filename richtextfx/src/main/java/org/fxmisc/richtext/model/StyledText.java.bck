package org.fxmisc.richtext.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

import javafx.scene.Node;

public class StyledText<S> implements Segment<S> {
    private String text;
    private S style;

    StyledText() {}

    public StyledText(String text, S style) {
        this.text = text;
        this.style = style;
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
    public String getText() {
        return text;
    }

    @Override
    public StyledText<S> subSequence(int start, int end) {
        return new StyledText<>(text.substring(start, end), style);
    }

    @Override
    public StyledText<S> subSequence(int start) {
        return new StyledText<>(text.substring(start), style);
    }

    @Override
    public StyledText<S> append(String str) {
        return new StyledText<>(text + str, style);
    }

    @Override
    public StyledText<S> spliced(int from, int to, CharSequence replacement) {
        String left = text.substring(0, from);
        String right = text.substring(to);
        return new StyledText<>(left + replacement + right, style);
    }

    @Override
    public S getStyle() {
        return style;
    }

    @Override
    public void setStyle(S style) {
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


    @Override
    public boolean canJoin(Segment<S> right) {

        if (right instanceof StyledText) {
            return Objects.equals(getStyle(), right.getStyle());
        }

        return false;
    }

    
    @Override
    public void encode(DataOutputStream os, Codec<S> styleCodec) throws IOException {
        Codec.STRING_CODEC.encode(os, getText());
        styleCodec.encode(os, style);
    }

    @Override
    public void decode(DataInputStream is, Codec<S> styleCodec) throws IOException {
        text = Codec.STRING_CODEC.decode(is);
        style = styleCodec.decode(is);
    }

    @SuppressWarnings("rawtypes")
    private static Function<StyledText, Node> nodeFactory;

    @Override
    public Node createNode() {
        return nodeFactory.apply(this);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <S> void setNodeFactory(Function<StyledText<S>, Node> nodeFactory) {
        StyledText.nodeFactory = (Function<StyledText, Node>) (Object) nodeFactory;
    }

}
