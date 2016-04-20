package org.fxmisc.richtext;

import static org.fxmisc.richtext.ReadOnlyStyledDocument.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class ReadOnlyStyledDocumentTest {

    @Test
    public void testUndo() {
        ReadOnlyStyledDocument<String, String> doc0 = fromString("", "X", "X");

        doc0.replace(0, 0, fromString("abcd", "Y", "Y")).exec((doc1, chng1, pchng1) -> {
            // undo chng1
            doc1.replace(chng1.getPosition(), chng1.getInsertionEnd(), from(chng1.getRemoved())).exec((doc2, chng2, pchng2) -> {
                // we should have arrived at the original document
                assertEquals(doc0, doc2);

                // chng2 should be the inverse of chng1
                assertEquals(chng1.invert(), chng2);
            });
        });
    }

}
