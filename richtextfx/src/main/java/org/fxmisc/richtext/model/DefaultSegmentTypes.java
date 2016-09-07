package org.fxmisc.richtext.model;

/**
 * The default segment types supported by the core RichTextFX component.
 * TODO: Since this needs to be extendible, an enum is probably not the
 * best solution. Simply use a String instead as the key.  
 */
public enum DefaultSegmentTypes implements SegmentType {
    STYLED_TEXT("StyledText"), INLINE_IMAGE("InlineImage"), INLINE_TABLE("InlineTable");

    private String theName;

    private DefaultSegmentTypes(String typeName) {
        theName = typeName;
    }

    @Override
    public String getName() {
        return theName;
    }
}
