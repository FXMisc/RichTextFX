package org.fxmisc.richtext.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javafx.scene.paint.Color;


/**
 * The interface to codecs which encode/decode objects into/from a binary 
 * representation.
 * A Codec essentially has a name, a method to encode a given object into 
 * a specific stream, and another method to decode a specific stream to 
 * recreate the corresponding object.
 * This interface also contains a couple of static fields with implementations
 * of the Codec interface for various standard types, such as String, Color,
 * or List&lt;T&gt;. 
 *
 * @param <T> The type of the object to encode/decode.
 */
public interface Codec<T> {

    /**
     * @return The name of this codec.
     */
    String getName();

    /**
     * Encodes an object of type T into an output stream.   
     *
     * @param os The destination stream.
     * @param t  The object to encode.
     *
     * @throws IOException if an error occurs when writing to the output stream.
     */
    void encode(DataOutputStream os, T t) throws IOException;
    
    /**
     * Decodes an object of type T from an input stream.
     * 
     * @param is The source stream
     *  
     * @return The object which was decoded from the input stream.
     * 
     * @throws IOException if an error occurs when reading from the input stream.
     */
    T decode(DataInputStream is) throws IOException;


/***************************************
 * Default Codecs for standard types   *
 ***************************************/

    static final Codec<String> STRING_CODEC = new Codec<String>() {

        @Override
        public String getName() {
            return "string";
        }

        @Override
        public void encode(DataOutputStream os, String s) throws IOException {
            os.writeUTF(s);
        }

        @Override
        public String decode(DataInputStream is) throws IOException {
            return is.readUTF();
        }
    };

    static final Codec<Color> COLOR_CODEC = new Codec<Color>() {

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

    static <T> Codec<List<T>> listCodec(Codec<T> elemCodec) {
        return SuperCodec.collectionListCodec(elemCodec);
    }

    static <T> Codec<Collection<T>> collectionCodec(Codec<T> elemCodec) {
        return SuperCodec.upCast(SuperCodec.collectionListCodec(elemCodec));
    }

    static <T> Codec<Optional<T>> optionalCodec(Codec<T> elemCodec) {
        return new Codec<Optional<T>>() {

            @Override
            public String getName() {
                return "optional<" + elemCodec.getName() + ">";
            }

            @Override
            public void encode(DataOutputStream os, Optional<T> ot) throws IOException {
                if(ot.isPresent()) {
                    os.writeBoolean(true);
                    elemCodec.encode(os, ot.get());
                } else {
                    os.writeBoolean(false);
                }
            }

            @Override
            public Optional<T> decode(DataInputStream is) throws IOException {
                return is.readBoolean()
                        ? Optional.of(elemCodec.decode(is))
                        : Optional.empty();
            }

        };
    }

    static <E extends Enum<E>> Codec<E> enumCodec(Class<E> enumType) {
        return new Codec<E>() {

            @Override
            public String getName() {
                return enumType.getSimpleName();
            }

            @Override
            public void encode(DataOutputStream os, E e) throws IOException {
                os.writeInt(e.ordinal());
            }

            @Override
            public E decode(DataInputStream is) throws IOException {
                int ord = is.readInt();
                return enumType.getEnumConstants()[ord];
            }

        };
    }
}