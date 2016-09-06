package org.fxmisc.richtext.model;

import java.io.DataOutputStream;
import java.io.IOException;

public class InlineImage<S> extends CustomObject<S> {

    private String imagePath;

    public InlineImage(S style, String imagePath) {
        super(style, DefaultSegmentTypes.INLINE_IMAGE);
        this.imagePath = imagePath;
    }
   

    @Override
    public String toString() {
        return String.format("InlineImage[path=%s]", imagePath);
    }


    @Override
    public void encode(DataOutputStream os) throws IOException {
        Codec.STRING_CODEC.encode(os, imagePath);
    }


    public String getImagePath() {
        return imagePath;
    }
}
