package org.fxmisc.richtext.demo.richtext;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.util.Objects;
import java.util.Optional;

import javafx.scene.paint.Color;

import org.fxmisc.richtext.Codec;

class StyleInfo {

    public static final StyleInfo EMPTY = new StyleInfo();

    public static final Codec<StyleInfo> CODEC = new Codec<StyleInfo>() {

        private final Codec<Color> COLOR_CODEC = new Codec<Color>() {

            @Override
            public String getName() {
                return "color";
            }

            @Override
            public void encode(DataOutputStream os, Color c)
                    throws IOException {
                os.writeDouble(c.getRed());
                os.writeDouble(c.getGreen());
                os.writeDouble(c.getBlue());
                os.writeDouble(c.getOpacity());
            }

            @Override
            public Color decode(DataInputStream is) throws IOException {
                return Color.color(
                        is.readDouble(),
                        is.readDouble(),
                        is.readDouble(),
                        is.readDouble());
            }

        };

        @Override
        public String getName() {
            return "style-info";
        }

        @Override
        public void encode(DataOutputStream os, StyleInfo s)
                throws IOException {
            os.writeByte(encodeBoldItalicUnderlineStrikethrough(s));
            os.writeInt(encodeOptionalUint(s.fontSize));
            encodeOptional(os, s.fontFamily, Codec.STRING_CODEC);
            encodeOptional(os, s.textColor, COLOR_CODEC);
            encodeOptional(os, s.backgroundColor, COLOR_CODEC);
        }

        @Override
        public StyleInfo decode(DataInputStream is) throws IOException {
            byte bius = is.readByte();
            Optional<Integer> fontSize = decodeOptionalUint(is.readInt());
            Optional<String> fontFamily = decodeOptional(is, Codec.STRING_CODEC);
            Optional<Color> textColor = decodeOptional(is, COLOR_CODEC);
            Optional<Color> bgrColor = decodeOptional(is, COLOR_CODEC);
            return new StyleInfo(
                    bold(bius), italic(bius), underline(bius), strikethrough(bius),
                    fontSize, fontFamily, textColor, bgrColor);
        }

        private int encodeBoldItalicUnderlineStrikethrough(StyleInfo s) {
            return encodeOptionalBoolean(s.bold) << 6 |
                   encodeOptionalBoolean(s.italic) << 4 |
                   encodeOptionalBoolean(s.underline) << 2 |
                   encodeOptionalBoolean(s.strikethrough);
        }

        private Optional<Boolean> bold(byte bius) throws IOException {
            return decodeOptionalBoolean((bius >> 6) & 3);
        }

        private Optional<Boolean> italic(byte bius) throws IOException {
            return decodeOptionalBoolean((bius >> 4) & 3);
        }

        private Optional<Boolean> underline(byte bius) throws IOException {
            return decodeOptionalBoolean((bius >> 2) & 3);
        }

        private Optional<Boolean> strikethrough(byte bius) throws IOException {
            return decodeOptionalBoolean((bius >> 0) & 3);
        }

        private int encodeOptionalBoolean(Optional<Boolean> ob) {
            return ob.map(b -> 2 + (b ? 1 : 0)).orElse(0);
        }

        private Optional<Boolean> decodeOptionalBoolean(int i) throws IOException {
            switch(i) {
                case 0: return Optional.empty();
                case 2: return Optional.of(false);
                case 3: return Optional.of(true);
            }
            throw new MalformedInputException(0);
        }

        private int encodeOptionalUint(Optional<Integer> oi) {
            return oi.orElse(-1);
        }

        private Optional<Integer> decodeOptionalUint(int i) {
            return (i < 0) ? Optional.empty() : Optional.of(i);
        }

        private <T> void encodeOptional(DataOutputStream os, Optional<T> ot, Codec<T> codec) throws IOException {
            if(ot.isPresent()) {
                os.writeBoolean(true);
                codec.encode(os, ot.get());
            } else {
                os.writeBoolean(false);
            }
        }

        private <T> Optional<T> decodeOptional(DataInputStream is, Codec<T> codec) throws IOException {
            return is.readBoolean()
                    ? Optional.of(codec.decode(is))
                    : Optional.empty();
        }
    };

    public static StyleInfo fontSize(int fontSize) { return EMPTY.updateFontSize(fontSize); }
    public static StyleInfo fontFamily(String family) { return EMPTY.updateFontFamily(family); }
    public static StyleInfo textColor(Color color) { return EMPTY.updateTextColor(color); }
    public static StyleInfo backgroundColor(Color color) { return EMPTY.updateBackgroundColor(color); }

    private static String cssColor(Color color) {
        int red = (int) (color.getRed() * 255);
        int green = (int) (color.getGreen() * 255);
        int blue = (int) (color.getBlue() * 255);
        return "rgb(" + red + ", " + green + ", " + blue + ")";
    }

    final Optional<Boolean> bold;
    final Optional<Boolean> italic;
    final Optional<Boolean> underline;
    final Optional<Boolean> strikethrough;
    final Optional<Integer> fontSize;
    final Optional<String> fontFamily;
    final Optional<Color> textColor;
    final Optional<Color> backgroundColor;

    public StyleInfo() {
        this(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }

    public StyleInfo(
            Optional<Boolean> bold,
            Optional<Boolean> italic,
            Optional<Boolean> underline,
            Optional<Boolean> strikethrough,
            Optional<Integer> fontSize,
            Optional<String> fontFamily,
            Optional<Color> textColor,
            Optional<Color> backgroundColor) {
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strikethrough = strikethrough;
        this.fontSize = fontSize;
        this.fontFamily = fontFamily;
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                bold, italic, underline, strikethrough,
                fontSize, fontFamily, textColor, backgroundColor);
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof StyleInfo) {
            StyleInfo that = (StyleInfo) other;
            return Objects.equals(this.bold,            that.bold) &&
                   Objects.equals(this.italic,          that.italic) &&
                   Objects.equals(this.underline,       that.underline) &&
                   Objects.equals(this.strikethrough,   that.strikethrough) &&
                   Objects.equals(this.fontSize,        that.fontSize) &&
                   Objects.equals(this.fontFamily,      that.fontFamily) &&
                   Objects.equals(this.textColor,       that.textColor) &&
                   Objects.equals(this.backgroundColor, that.backgroundColor);
        } else {
            return false;
        }
    }

    public String toCss() {
        StringBuilder sb = new StringBuilder();

        if(bold.isPresent()) {
            if(bold.get()) {
                sb.append("-fx-font-weight: bold;");
            } else {
                sb.append("-fx-font-weight: normal;");
            }
        }

        if(italic.isPresent()) {
            if(italic.get()) {
                sb.append("-fx-font-style: italic;");
            } else {
                sb.append("-fx-font-style: normal;");
            }
        }

        if(underline.isPresent()) {
            if(underline.get()) {
                sb.append("-fx-underline: true;");
            } else {
                sb.append("-fx-underline: false;");
            }
        }

        if(strikethrough.isPresent()) {
            if(strikethrough.get()) {
                sb.append("-fx-strikethrough: true;");
            } else {
                sb.append("-fx-strikethrough: false;");
            }
        }

        if(fontSize.isPresent()) {
            sb.append("-fx-font-size: " + fontSize.get() + "pt;");
        }

        if(fontFamily.isPresent()) {
            sb.append("-fx-font-family: " + fontFamily.get() + ";");
        }

        if(textColor.isPresent()) {
            Color color = textColor.get();
            sb.append("-fx-fill: " + cssColor(color) + ";");
        }

        if(backgroundColor.isPresent()) {
            Color color = backgroundColor.get();
            sb.append("-fx-background-fill: " + cssColor(color) + ";");
        }

        return sb.toString();
    }

    public StyleInfo updateWith(StyleInfo mixin) {
        return new StyleInfo(
                mixin.bold.isPresent() ? mixin.bold : bold,
                mixin.italic.isPresent() ? mixin.italic : italic,
                mixin.underline.isPresent() ? mixin.underline : underline,
                mixin.strikethrough.isPresent() ? mixin.strikethrough : strikethrough,
                mixin.fontSize.isPresent() ? mixin.fontSize : fontSize,
                mixin.fontFamily.isPresent() ? mixin.fontFamily : fontFamily,
                mixin.textColor.isPresent() ? mixin.textColor : textColor,
                mixin.backgroundColor.isPresent() ? mixin.backgroundColor : backgroundColor);
    }

    public StyleInfo updateBold(boolean bold) {
        return new StyleInfo(Optional.of(bold), italic, underline, strikethrough, fontSize, fontFamily, textColor, backgroundColor);
    }

    public StyleInfo updateItalic(boolean italic) {
        return new StyleInfo(bold, Optional.of(italic), underline, strikethrough, fontSize, fontFamily, textColor, backgroundColor);
    }

    public StyleInfo updateUnderline(boolean underline) {
        return new StyleInfo(bold, italic, Optional.of(underline), strikethrough, fontSize, fontFamily, textColor, backgroundColor);
    }

    public StyleInfo updateStrikethrough(boolean strikethrough) {
        return new StyleInfo(bold, italic, underline, Optional.of(strikethrough), fontSize, fontFamily, textColor, backgroundColor);
    }

    public StyleInfo updateFontSize(int fontSize) {
        return new StyleInfo(bold, italic, underline, strikethrough, Optional.of(fontSize), fontFamily, textColor, backgroundColor);
    }

    public StyleInfo updateFontFamily(String fontFamily) {
        return new StyleInfo(bold, italic, underline, strikethrough, fontSize, Optional.of(fontFamily), textColor, backgroundColor);
    }

    public StyleInfo updateTextColor(Color textColor) {
        return new StyleInfo(bold, italic, underline, strikethrough, fontSize, fontFamily, Optional.of(textColor), backgroundColor);
    }

    public StyleInfo updateBackgroundColor(Color backgroundColor) {
        return new StyleInfo(bold, italic, underline, strikethrough, fontSize, fontFamily, textColor, Optional.of(backgroundColor));
    }
}