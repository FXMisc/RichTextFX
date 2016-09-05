package org.fxmisc.richtext.model;

/**
 * Simple POJO for object specific meta data.
 */
public class ObjectData {

    private SegmentType type;       // The type will be used to get the registered renderer
    private String data;    // The data will be used by the renderer (TODO: Make generic)

    public ObjectData(SegmentType type, String data) {
        this.type = type;
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public SegmentType getType() {
        return type;
    }



    @Override
    public String toString() {
        return String.format("ObjectData[type=%s, data=[%s]]", type, data);
    }
}
