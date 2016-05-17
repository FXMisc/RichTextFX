package org.fxmisc.richtext.demo.customobject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.CustomObject;

import javafx.scene.Node;
import javafx.scene.shape.Circle;

/**
 * A custom object which represents a circle.
 */
public class CircleObject extends CustomObject<Collection<String>> {

    private double radius;

    public CircleObject() {}

    public CircleObject(double radius) {
        super(new ArrayList<String>());
        this.radius = radius;
    }

    public double getRadius() {
        return radius;
    }

    @Override
    public void encode(DataOutputStream os) throws IOException {
        Codec.STRING_CODEC.encode(os, Double.toString(radius));
    }

    @Override
    public void decode(DataInputStream is) throws IOException {
        try {
            radius = Double.parseDouble(Codec.STRING_CODEC.decode(is));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Node createNode() {
        Circle result = new Circle(getRadius());
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("CircleObject[radius=%s]", radius);
    }

}