package org.fxmisc.richtext.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Codec that can serialize an object and its super class; helpful when using generics and captures.
 *
 * @param <S> the super class type
 * @param <T> the regular class type
 */
interface SuperCodec<S, T extends S> extends Codec<T> {
    void encodeSuper(DataOutputStream os, S s) throws IOException;

    @Override
    default void encode(DataOutputStream os, T t) throws IOException {
        encodeSuper(os, t);
    }

    /**
     * Returns a codec that can serialize {@code sc}'s type's super class
     */
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