package org.fxmisc.richtext.demo.richtext;

import static javafx.scene.text.TextAlignment.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import org.fxmisc.richtext.Codec;

/**
 * Holds information about the style of a paragraph.
 */
class ParStyle {

    public static final ParStyle EMPTY = new ParStyle();

    public static final Codec<ParStyle> CODEC = new Codec<ParStyle>() {

        private final Codec<Optional<TextAlignment>> OPT_ALIGNMENT_CODEC =
                Codec.optionalCodec(Codec.enumCodec(TextAlignment.class));
        private final Codec<Optional<Color>> OPT_COLOR_CODEC =
                Codec.optionalCodec(Codec.COLOR_CODEC);

        @Override
        public String getName() {
            return "par-style";
        }

        @Override
        public void encode(DataOutputStream os, ParStyle t) throws IOException {
            OPT_ALIGNMENT_CODEC.encode(os, t.alignment);
            OPT_COLOR_CODEC.encode(os, t.backgroundColor);
        }

        @Override
        public ParStyle decode(DataInputStream is) throws IOException {
            return new ParStyle(
                    OPT_ALIGNMENT_CODEC.decode(is),
                    OPT_COLOR_CODEC.decode(is));
        }

    };

    public static ParStyle alignLeft() { return EMPTY.updateAlignment(LEFT); }
    public static ParStyle alignCenter() { return EMPTY.updateAlignment(CENTER); }
    public static ParStyle alignRight() { return EMPTY.updateAlignment(RIGHT); }
    public static ParStyle alignJustify() { return EMPTY.updateAlignment(JUSTIFY); }
    public static ParStyle backgroundColor(Color color) { return EMPTY.updateBackgroundColor(color); }

    final Optional<TextAlignment> alignment;
    final Optional<Color> backgroundColor;

    public ParStyle() {
        this(Optional.empty(), Optional.empty());
    }

    public ParStyle(Optional<TextAlignment> alignment, Optional<Color> backgroundColor) {
        this.alignment = alignment;
        this.backgroundColor = backgroundColor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(alignment, backgroundColor);
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof ParStyle) {
            ParStyle that = (ParStyle) other;
            return Objects.equals(this.alignment, that.alignment) &&
                   Objects.equals(this.backgroundColor, that.backgroundColor);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return toCss();
    }

    public String toCss() {
        StringBuilder sb = new StringBuilder();

        alignment.ifPresent(al -> {
            String cssAlignment;
            switch(al) {
                case LEFT:    cssAlignment = "left";    break;
                case CENTER:  cssAlignment = "center";  break;
                case RIGHT:   cssAlignment = "right";   break;
                case JUSTIFY: cssAlignment = "justify"; break;
                default: throw new AssertionError("unreachable code");
            }
            sb.append("-fx-text-alignment: " + cssAlignment + ";");
        });

        backgroundColor.ifPresent(color -> {
            sb.append("-fx-background-color: " + TextStyle.cssColor(color) + ";");
        });

        return sb.toString();
    }

    public ParStyle updateWith(ParStyle mixin) {
        return new ParStyle(
                mixin.alignment.isPresent() ? mixin.alignment : alignment,
                mixin.backgroundColor.isPresent() ? mixin.backgroundColor : backgroundColor);
    }

    public ParStyle updateAlignment(TextAlignment alignment) {
        return new ParStyle(Optional.of(alignment), backgroundColor);
    }

    public ParStyle updateBackgroundColor(Color backgroundColor) {
        return new ParStyle(alignment, Optional.of(backgroundColor));
    }

    /**
     * Parses a CSS declaration into a ParStyle object.
     * 
     * @param style The CSS style declaration to parse.
     * @return A ParStyle which represents the parsed CSS style declaration.
     */
    public static ParStyle fromCss(String style) {
        Optional<TextAlignment> alignment = Optional.empty();
        Optional<Color> backgroundColor = Optional.empty();

        if (style.length() > 0) {
            for (String property : style.split(";", 0)) {
                String[] pair = property.split(":");
                String propName = pair[0].trim();
                String propValue = pair[1].trim();

                switch(propName) {
                    case "-fx-text-alignment"   :
                        alignment = Optional.of(TextAlignment.valueOf(propValue.toUpperCase()));
                        break;
    
                    case "-fx-background-color" :
                        backgroundColor = Optional.of(TextStyle.cssColor(propValue));
                        break;
    
                    default:
                        throw new IllegalArgumentException("Unknown CSS property: " + property);
                }
            }
        }

        return new ParStyle(alignment, backgroundColor); 
    }

}
