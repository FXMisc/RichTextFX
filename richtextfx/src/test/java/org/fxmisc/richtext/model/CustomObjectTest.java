package org.fxmisc.richtext.model;

import static org.junit.Assert.*;
import org.junit.Test;

public class CustomObjectTest {

    @Test
    public void testCustomObjectCreation() {
        SimpleEditableStyledDocument<Boolean, String> doc = 
                new SimpleEditableStyledDocument<>(true, "");
        ReadOnlyStyledDocument<Boolean, String> customObj = 
                ReadOnlyStyledDocument.createObject(new ObjectData(DefaultSegmentTypes.INLINE_IMAGE, "sample.png"), true, "");
        doc.replace(0, 0, customObj);
        assertEquals(1, doc.getLength());

        Paragraph<Boolean, String> para = doc.getParagraphs().get(0);
        Object x = para.getSegments().get(0);
        assertTrue(x instanceof CustomObject);
    }

}
