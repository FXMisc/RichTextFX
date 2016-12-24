package org.fxmisc.richtext.model;


public class CustomObject<S> extends StyledText<S> {

    public CustomObject(S style) {
        super("\ufffc", style);
    }
    
   
    private ObjectData objectData;
    public void setObjectData(ObjectData data) {
        this.objectData = data;
    }

    public ObjectData getObjectData() {
        return objectData;
    }

    @Override
    public String toString() {
        return String.format("CustomObject[objectData=%s", objectData);
    }

    @Override
    public StyledText<S> subSequence(int start, int end) {
        if (start == 0 && end == 1) {
            return this;
        }
        return new StyledText<>("", getStyle());
    }

    @Override
    public StyledText<S> subSequence(int start) {
        if (start == 1) {
            return new StyledText<>("", getStyle());
        }
        return this;
    }

    @Override
    public StyledText<S> append(String str) {
        throw new UnsupportedOperationException();
        // return new StyledText<>(text + str, style);
    }

    @Override
    public StyledText<S> spliced(int from, int to, CharSequence replacement) {
        throw new UnsupportedOperationException();
/*        String left = text.substring(0, from);
        String right = text.substring(to);
        return new StyledText<>(left + replacement + right, style);*/
    }
}
