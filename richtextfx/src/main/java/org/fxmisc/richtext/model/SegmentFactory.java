package org.fxmisc.richtext.model;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Temporary helper class to overcome the "static hell" of Codec ...
 * Allows to register factory classes so that they can be retrieved later on.
 * Requires some unusual casts and should be replaced with a better approach.
 */
public class SegmentFactory  {

    private static Map<String, BiFunction<DataInputStream, Codec, Segment>> factories = new HashMap<>();

    public static <S> void registerFactory(String id, 
                                           BiFunction<DataInputStream, Codec<S>, Segment<S>> factory) {
       factories.put(id, ((BiFunction<DataInputStream, Codec, Segment>) (Object) factory));
    }


    public static <S> Segment<S> decode(DataInputStream is, Codec<S> styleCodec) throws IOException {
        String segType = Codec.STRING_CODEC.decode(is);
        BiFunction<DataInputStream, Codec, Segment> fac = factories.get(segType);
        return (Segment<S>) fac.apply(is, styleCodec);
    }

}
