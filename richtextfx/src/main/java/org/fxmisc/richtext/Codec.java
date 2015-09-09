package org.fxmisc.richtext;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface Codec<T> {

    String getName();
    void encode(DataOutputStream os, T t) throws IOException;
    T decode(DataInputStream is) throws IOException;


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

    static <T> Codec<List<T>> listCodec(Codec<T> elemCodec) {
        return SuperCodec.collectionListCodec(elemCodec);
    }
}

interface SuperCodec<S, T extends S> extends Codec<T> {
    void encodeSuper(DataOutputStream os, S s) throws IOException;

    @Override
    default void encode(DataOutputStream os, T t) throws IOException {
        encodeSuper(os, t);
    }

    @SuppressWarnings("unchecked")
    static <S, U extends S, T extends U> SuperCodec<S, U> upCast(SuperCodec<S, T> sc) {
        return (SuperCodec<S, U>) sc;
    }

    static <T> SuperCodec<Collection<T>, List<T>> collectionListCodec(Codec<T> elemCodec) {
        return new SuperCodec<Collection<T>, List<T>>() {

            @Override
            public String getName() {
                return "list<" + elemCodec.getName() + ">";
            }

            @Override
            public void encodeSuper(DataOutputStream os, Collection<T> col) throws IOException {
                os.writeInt(col.size());
                for(T t: col) {
                    elemCodec.encode(os, t);
                }
            }

            @Override
            public List<T> decode(DataInputStream is) throws IOException {
                int size = is.readInt();
                List<T> elems = new ArrayList<>(size);
                for(int i = 0; i < size; ++i) {
                    T t = elemCodec.decode(is);
                    elems.add(t);
                }
                return elems;
            }
        };
    }
}