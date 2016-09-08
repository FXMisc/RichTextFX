package org.fxmisc.richtext.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A custom object which contains a file path to an image file.
 * When rendered in the rich text editor, the image is loaded from the 
 * specified file.  
 */
public class LinkedImage<S> extends CustomObject<S> {

    private String imagePath;

    /**
     * Creates a new linked image object.
     *
     * @param imagePath The path to the image file.
     * @param style The text style to apply to the corresponding segment.
     */
    public LinkedImage(String imagePath, S style) {
        super(style, DefaultSegmentTypes.LINKED_IMAGE);
        this.imagePath = imagePath;
    }


    /**
     * @return The path of the image to render.
     */
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
        return new LinkedImage<>(path, style);
    }

    @Override
    public String toString() {
        return String.format("LinkedImage[path=%s]", imagePath);
    }
}
