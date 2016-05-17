package org.fxmisc.richtext.demo.customobject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.CustomObject;

import javafx.scene.Node;
import javafx.scene.shape.Rectangle;

/**
 * A custom object which represents a rectangle.
 */
public class RectangleObject extends CustomObject<Collection<String>> {

    private double width;
    private double height;

    public RectangleObject() {}

    public RectangleObject(double width, double height) {
        super(new ArrayList<String>());
        this.width = width;
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    @Override
    public void encode(DataOutputStream os) throws IOException {
        Codec.STRING_CODEC.encode(os, Double.toString(width));
        Codec.STRING_CODEC.encode(os, Double.toString(height));
    }

    @Override
    public void decode(DataInputStream is) throws IOException {
        try {
            width = Double.parseDouble(Codec.STRING_CODEC.decode(is));
            height = Double.parseDouble(Codec.STRING_CODEC.decode(is));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Node createNode() {
        Rectangle result = new Rectangle(getWidth(), getHeight());
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("RectangleObject[width=%s, height=%s]", width, height);
    }
}
