package org.fxmisc.richtext.demo.richtext;

import javafx.scene.Node;
import org.fxmisc.richtext.model.Codec;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface LinkedImage<S> {
    static <S> Codec<LinkedImage<S>> codec(Codec<S> styleCodec) {
        return new Codec<LinkedImage<S>>() {

            @Override
            public String getName() {
                return "LinkedImage<" + styleCodec.getName() + ">";
            }

            @Override
            public void encode(DataOutputStream os, LinkedImage<S> i) throws IOException {
                // don't encode EmptyLinkedImage objects
                if (i.getStyle() != null) {
                    // external path rep should use forward slashes only
                    String externalPath = i.getImagePath().replace("\\", "/");
                    Codec.STRING_CODEC.encode(os, externalPath);
                    styleCodec.encode(os, i.getStyle());
                }
            }

            @Override
            public RealLinkedImage<S> decode(DataInputStream is) throws IOException {
                // Sanitize path - make sure that forward slashes only are used
                String imagePath = Codec.STRING_CODEC.decode(is);
                imagePath = imagePath.replace("\\",  "/");
                S style = styleCodec.decode(is);
                return new RealLinkedImage<>(imagePath, style);
            }

        };
    }

    LinkedImage<S> setStyle(S style);

    S getStyle();

    /**
     * @return The path of the image to render.
     */
    String getImagePath();

    Node createNode();
}
