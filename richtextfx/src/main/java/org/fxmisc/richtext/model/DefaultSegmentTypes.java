package org.fxmisc.richtext.model;

/**
 * The default segment types supported by the core RichTextFX component.
 * Whenever one if those enums is used as parameter, the SegmentType interface
 * should be used instead so that users can extend the segment types accordingly. 
 */
public enum DefaultSegmentTypes implements SegmentType {
    STYLED_TEXT("StyledText"), LINKED_IMAGE("LinkedImage");

    private String theName;

    private DefaultSegmentTypes(String typeName) {
        theName = typeName;
    }

    @Override
    public String getName() {
        return theName;
    }
}
