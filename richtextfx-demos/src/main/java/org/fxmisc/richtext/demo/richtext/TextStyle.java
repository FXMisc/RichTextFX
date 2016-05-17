package org.fxmisc.richtext.demo.richtext;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.paint.Color;

import org.fxmisc.richtext.Codec;

/**
 * Holds information about the style of a text fragment.
 */
class TextStyle {

    public static final TextStyle EMPTY = new TextStyle();

    public static final Codec<TextStyle> CODEC = new Codec<TextStyle>() {

        private final Codec<Optional<String>> OPT_STRING_CODEC =
                Codec.optionalCodec(Codec.STRING_CODEC);
        private final Codec<Optional<Color>> OPT_COLOR_CODEC =
                Codec.optionalCodec(Codec.COLOR_CODEC);

        @Override
        public String getName() {
            return "text-style";
        }

        @Override
        public void encode(DataOutputStream os, TextStyle s)
                throws IOException {
            os.writeByte(encodeBoldItalicUnderlineStrikethrough(s));
            os.writeInt(encodeOptionalUint(s.fontSize));
            OPT_STRING_CODEC.encode(os, s.fontFamily);
            OPT_COLOR_CODEC.encode(os, s.textColor);
            OPT_COLOR_CODEC.encode(os, s.backgroundColor);
        }

        @Override
        public TextStyle decode(DataInputStream is) throws IOException {
            byte bius = is.readByte();
            Optional<Integer> fontSize = decodeOptionalUint(is.readInt());
            Optional<String> fontFamily = OPT_STRING_CODEC.decode(is);
            Optional<Color> textColor = OPT_COLOR_CODEC.decode(is);
            Optional<Color> bgrColor = OPT_COLOR_CODEC.decode(is);
            return new TextStyle(
                    bold(bius), italic(bius), underline(bius), strikethrough(bius),
                    fontSize, fontFamily, textColor, bgrColor);
        }

        private int encodeBoldItalicUnderlineStrikethrough(TextStyle s) {
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
    };

    public static TextStyle bold(boolean bold) { return EMPTY.updateBold(bold); }
    public static TextStyle italic(boolean italic) { return EMPTY.updateItalic(italic); }
    public static TextStyle underline(boolean underline) { return EMPTY.updateUnderline(underline); }
    public static TextStyle strikethrough(boolean strikethrough) { return EMPTY.updateStrikethrough(strikethrough); }
    public static TextStyle fontSize(int fontSize) { return EMPTY.updateFontSize(fontSize); }
    public static TextStyle fontFamily(String family) { return EMPTY.updateFontFamily(family); }
    public static TextStyle textColor(Color color) { return EMPTY.updateTextColor(color); }
    public static TextStyle backgroundColor(Color color) { return EMPTY.updateBackgroundColor(color); }

    static String cssColor(Color color) {
        int red = (int) (color.getRed() * 255);
        int green = (int) (color.getGreen() * 255);
        int blue = (int) (color.getBlue() * 255);
        return "rgb(" + red + ", " + green + ", " + blue + ")";
    }

    /**
     * Parses a String representation of a Color object.
     *
     * @param color A string representation of a color object, in the form "rgb(red, green, blue)"
     * @return A Color object for the given string representation.
     */
    static Color cssColor(String color) {
        Pattern pattern = Pattern.compile("rgb\\((\\d*),\\s*(\\d*),\\s*(\\d*)\\)");
        Matcher matcher = pattern.matcher(color);
        matcher.find();
        int red = Integer.parseInt(matcher.group(1));
        int green = Integer.parseInt(matcher.group(2));
        int blue = Integer.parseInt(matcher.group(3));
        return new Color(red / 255.0, green / 255.0, blue / 255.0, 1.0);
    }

    final Optional<Boolean> bold;
    final Optional<Boolean> italic;
    final Optional<Boolean> underline;
    final Optional<Boolean> strikethrough;
    final Optional<Integer> fontSize;
    final Optional<String> fontFamily;
    final Optional<Color> textColor;
    final Optional<Color> backgroundColor;

    public TextStyle() {
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

    public TextStyle(
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
        if(other instanceof TextStyle) {
            TextStyle that = (TextStyle) other;
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


    /**
     * Parses a CSS declaration into a TextStyle object.
     * 
     * @param style The CSS style declaration to parse.
     * @return A TextStyle which represents the parsed CSS style declaration.
     */
    public static TextStyle fromCss(String style) {
        Optional<Boolean> bold = Optional.empty();
        Optional<Boolean> italic = Optional.empty();
        Optional<Boolean> underline = Optional.empty();
        Optional<Boolean> strikethrough = Optional.empty();
        Optional<Integer> fontSize = Optional.empty();
        Optional<String> fontFamily = Optional.empty();
        Optional<Color> textColor = Optional.empty();
        Optional<Color> backgroundColor = Optional.empty();

        if (style.length() > 0) {
            for (String property : style.split(";")) {
                String[] pair = property.split(":");
                String propName = pair[0].trim();
                String propValue = pair[1].trim();
    
                switch(propName) {
                    case "-fx-font-size"   : 
                        Pattern pattern = Pattern.compile("(\\d*)pt");
                        Matcher matcher = pattern.matcher(propValue);
                        matcher.find();
                        int sizePt = Integer.parseInt(matcher.group(1));
                        fontSize = Optional.of(sizePt);
                        break;
    
                    case "-fx-font-family" :
                        fontFamily = Optional.of(propValue);
                        break;
    
                    case "-fx-fill" :
                        textColor = Optional.of(cssColor(propValue));
                        break;

                    case "-fx-font-weight" :
                        bold = Optional.of("bold".equals(propValue)); 
                        break;
    
                    case "-fx-font-style" :
                        italic = Optional.of("italic".equals(propValue)); 
                        break;
    
                    case "-fx-underline" :
                        underline = Optional.of("true".equals(propValue));
                        break;

                    case "-fx-strikethrough" :
                        strikethrough = Optional.of("true".equals(propValue));
                        break;

                    case "-fx-background-fill" :
                        backgroundColor = Optional.of(cssColor(propValue));
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown CSS property: " + property);
                }
            }
        }

        return new TextStyle(bold, italic, underline, strikethrough, fontSize, fontFamily, textColor, backgroundColor);
    }

    @Override
    public String toString() {
        List<String> styles = new ArrayList<>();

        bold           .ifPresent(b -> styles.add(b.toString()));
        italic         .ifPresent(i -> styles.add(i.toString()));
        underline      .ifPresent(u -> styles.add(u.toString()));
        strikethrough  .ifPresent(s -> styles.add(s.toString()));
        fontSize       .ifPresent(s -> styles.add(s.toString()));
        fontFamily     .ifPresent(f -> styles.add(f.toString()));
        textColor      .ifPresent(c -> styles.add(c.toString()));
        backgroundColor.ifPresent(b -> styles.add(b.toString()));

        return String.join(",", styles);
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

    public TextStyle updateWith(TextStyle mixin) {
        return new TextStyle(
                mixin.bold.isPresent() ? mixin.bold : bold,
                mixin.italic.isPresent() ? mixin.italic : italic,
                mixin.underline.isPresent() ? mixin.underline : underline,
                mixin.strikethrough.isPresent() ? mixin.strikethrough : strikethrough,
                mixin.fontSize.isPresent() ? mixin.fontSize : fontSize,
                mixin.fontFamily.isPresent() ? mixin.fontFamily : fontFamily,
                mixin.textColor.isPresent() ? mixin.textColor : textColor,
                mixin.backgroundColor.isPresent() ? mixin.backgroundColor : backgroundColor);
    }

    public TextStyle updateBold(boolean bold) {
        return new TextStyle(Optional.of(bold), italic, underline, strikethrough, fontSize, fontFamily, textColor, backgroundColor);
    }

    public TextStyle updateItalic(boolean italic) {
        return new TextStyle(bold, Optional.of(italic), underline, strikethrough, fontSize, fontFamily, textColor, backgroundColor);
    }

    public TextStyle updateUnderline(boolean underline) {
        return new TextStyle(bold, italic, Optional.of(underline), strikethrough, fontSize, fontFamily, textColor, backgroundColor);
    }

    public TextStyle updateStrikethrough(boolean strikethrough) {
        return new TextStyle(bold, italic, underline, Optional.of(strikethrough), fontSize, fontFamily, textColor, backgroundColor);
    }

    public TextStyle updateFontSize(int fontSize) {
        return new TextStyle(bold, italic, underline, strikethrough, Optional.of(fontSize), fontFamily, textColor, backgroundColor);
    }

    public TextStyle updateFontFamily(String fontFamily) {
        return new TextStyle(bold, italic, underline, strikethrough, fontSize, Optional.of(fontFamily), textColor, backgroundColor);
    }

    public TextStyle updateTextColor(Color textColor) {
        return new TextStyle(bold, italic, underline, strikethrough, fontSize, fontFamily, Optional.of(textColor), backgroundColor);
    }

    public TextStyle updateBackgroundColor(Color backgroundColor) {
        return new TextStyle(bold, italic, underline, strikethrough, fontSize, fontFamily, textColor, Optional.of(backgroundColor));
    }
}