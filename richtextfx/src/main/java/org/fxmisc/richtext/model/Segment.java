package org.fxmisc.richtext.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javafx.scene.Node;

/**
 * An interface to segment types, like StyledText or LinkedImage.
 */
public interface Segment<S> {

    int length();

    char charAt(int index);

    String getText();   // each segment has a string associated with it - for custom objects
                        // this is the replacement character \ufffc 

    Segment<S> subSequence(int start, int end);

    Segment<S> subSequence(int start);

    Segment<S> append(String str);

    Segment<S> spliced(int from, int to, CharSequence replacement);

    S getStyle();

    void encode(DataOutputStream os, Codec<S> styleCodec) throws IOException;

    void decode(DataInputStream is, Codec<S> styleCodec) throws IOException;

    Node createNode();

    boolean canJoin(Segment<S> right);

    void setStyle(S style);
}
