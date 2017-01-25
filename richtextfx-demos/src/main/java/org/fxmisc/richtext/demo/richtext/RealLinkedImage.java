package org.fxmisc.richtext.demo.richtext;

import java.io.File;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


/**
 * A custom object which contains a file path to an image file.
 * When rendered in the rich text editor, the image is loaded from the
 * specified file.
 */
public class RealLinkedImage<S> implements LinkedImage<S> {

    private final String imagePath;
    private final S style;

    /**
     * Creates a new linked image object.
     *
     * @param imagePath The path to the image file.
     * @param style The text style to apply to the corresponding segment.
     */
    public RealLinkedImage(String imagePath, S style) {

        // if the image is below the current working directory,
        // then store as relative path name.
        String currentDir = System.getProperty("user.dir") + File.separatorChar;
        if (imagePath.startsWith(currentDir)) {
            imagePath = imagePath.substring(currentDir.length());
        }

        this.imagePath = imagePath;
        this.style = style;
    }

    @Override
    public RealLinkedImage<S> setStyle(S style) {
        return new RealLinkedImage<>(imagePath, style);
    }


    @Override
    public String getImagePath() {
        return imagePath;
    }

    @Override
    public S getStyle() {
        return style;
    }


    @Override
    public String toString() {
        return String.format("RealLinkedImage[path=%s]", imagePath);
    }

    @Override
    public Node createNode() {
        Image image = new Image("file:" + imagePath); // XXX: No need to create new Image objects each time -
                                                      // could be cached in the model layer
        ImageView result = new ImageView(image);
        return result;
    }
}
