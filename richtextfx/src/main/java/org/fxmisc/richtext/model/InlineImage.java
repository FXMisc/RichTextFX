package org.fxmisc.richtext.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class InlineImage<S> extends CustomObject<S> {

    private String imagePath;

    public InlineImage(String imagePath, S style) {
        super(style, DefaultSegmentTypes.INLINE_IMAGE);
        this.imagePath = imagePath;
    }
   

    public String getImagePath() {
        return imagePath;
    }


    @Override
    public void encode(DataOutputStream os) throws IOException {
        Codec.STRING_CODEC.encode(os, imagePath);
    }


    public static <S> Segment<S> decode(DataInputStream is, Codec<S> styleCodec) throws IOException {
        String path = Codec.STRING_CODEC.decode(is);
        S style = styleCodec.decode(is);
        return new InlineImage<>(path, style);
    }
    

    @Override
    public String toString() {
        return String.format("InlineImage[path=%s]", imagePath);
    }
}
