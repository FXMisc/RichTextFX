package org.fxmisc.richtext.model;

/**
 * Simple POJO for object specific meta data.
 */
public class ObjectData {

    private int type;       // The type will be used to get the registered renderer
    private String data;    // The data will be used by the renderer (TODO: Make generic)

    public ObjectData(int type, String data) {
        this.type = type;
        this.data = data;
    }

    public String getData() {
        return data;
    }


    @Override
    public String toString() {
        return String.format("ObjectData[type=%s, data=[%s]]", type, data);
    }
}
