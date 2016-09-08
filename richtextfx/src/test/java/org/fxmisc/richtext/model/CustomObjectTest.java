package org.fxmisc.richtext.model;

import static org.junit.Assert.*;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

public class CustomObjectTest {

    @Test
    public void testLinkedImageCreation() {
        SimpleEditableStyledDocument<Boolean, String> doc = 
                new SimpleEditableStyledDocument<>(true, "");
        ReadOnlyStyledDocument<Boolean, String> customObj = 
                ReadOnlyStyledDocument.createObject(new LinkedImage<String>("", "sample.png"), true, "");
        doc.replace(0, 0, customObj);
        assertEquals(1, doc.getLength());

        Paragraph<Boolean, String> para = doc.getParagraphs().get(0);
        Object x = para.getSegments().get(0);
        assertThat(x, instanceOf(LinkedImage.class));
    }

}
