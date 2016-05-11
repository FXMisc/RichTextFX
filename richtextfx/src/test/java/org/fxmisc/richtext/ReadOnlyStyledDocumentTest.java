package org.fxmisc.richtext;

import static org.fxmisc.richtext.model.ReadOnlyStyledDocument.*;
import static org.junit.Assert.*;

import java.util.List;

import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
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

    @Test
    public void deleteNewlineTest() {
        ReadOnlyStyledDocument<Void, Void> doc0 = fromString("Foo\nBar", null, null);
        doc0.replace(3, 4, fromString("", null, null)).exec((doc1, ch, pch) -> {
            List<? extends Paragraph<Void, Void>> removed = pch.getRemoved();
            List<? extends Paragraph<Void, Void>> added = pch.getAdded();
            assertEquals(2, removed.size());
            assertEquals(new Paragraph<Void, Void>(null, "Foo", null), removed.get(0));
            assertEquals(new Paragraph<Void, Void>(null, "Bar", null), removed.get(1));
            assertEquals(1, added.size());
            assertEquals(new Paragraph<Void, Void>(null, "FooBar", null), added.get(0));
        });
    }

}
