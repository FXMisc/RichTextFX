package org.fxmisc.richtext.demo.richtext;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.fxmisc.richtext.model.Codec;


/**
 * A custom object which contains a file path to an image file.
 * When rendered in the rich text editor, the image is loaded from the
 * specified file.
 */
public class LinkedImage<S> {

    public static <S> Codec<LinkedImage<S>> codec(Codec<S> styleCodec) {
        return new Codec<LinkedImage<S>>() {

            @Override
            public String getName() {
                return "LinkedImage<" + styleCodec.getName() + ">";
            }

            @Override
            public void encode(DataOutputStream os, LinkedImage<S> i) throws IOException {
                // external path rep should use forward slashes only
                String externalPath = i.imagePath.replace("\\", "/");
                Codec.STRING_CODEC.encode(os, externalPath);
                styleCodec.encode(os, i.style);
            }

            @Override
            public LinkedImage<S> decode(DataInputStream is) throws IOException {
                // Sanitize path - make sure that forward slashes only are used
                String imagePath = Codec.STRING_CODEC.decode(is);
                imagePath = imagePath.replace("\\",  "/");
                S style = styleCodec.decode(is);
                return new LinkedImage<>(imagePath, style);
            }

        };
    }

    private final String imagePath;
    private final S style;

    /**
     * Creates a new linked image object.
     *
     * @param imagePath The path to the image file.
     * @param style The text style to apply to the corresponding segment.
     */
    public LinkedImage(String imagePath, S style) {

        // if the image is below the current working directory,
        // then store as relative path name.
        String currentDir = System.getProperty("user.dir") + File.separatorChar;
        if (imagePath.startsWith(currentDir)) {
            imagePath = imagePath.substring(currentDir.length());
        }

        this.imagePath = imagePath;
        this.style = style;
    }

    public LinkedImage<S> setStyle(S style) {
        return new LinkedImage<>(imagePath, style);
    }


    /**
     * @return The path of the image to render.
     */
    public String getImagePath() {
        return imagePath;
    }

    public S getStyle() {
        return style;
    }


    @Override
    public String toString() {
        return String.format("LinkedImage[path=%s]", imagePath);
    }

    public Node createNode() {
        Image image = new Image("file:" + imagePath); // XXX: No need to create new Image objects each time -
                                                      // could be cached in the model layer
        ImageView result = new ImageView(image);
        return result;
    }
}
