package org.fxmisc.richtext.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

// TODO SMA UT caret in all cases
public class PlainTextChangeTest {
    private void checkContent(PlainTextChange textChange, int position, String removed, String inserted) {
        assertEquals("Incorrect position", position, textChange.getPosition());
        assertEquals("Incorrect inserted text", new PlainTextChangeData(inserted), textChange.getInserted());
        assertEquals("Incorrect removed text", new PlainTextChangeData(removed), textChange.getRemoved());
    }

    private void checkCaret(PlainTextChange textChange, int before, int after) {
        checkCaret(textChange.getCaretChange(), before, after);
        checkCaret(textChange.invert().getCaretChange(), after, before);
    }

    private void checkCaret(CaretChange caretChange, int before, int after) {
        assertEquals("Incorrect caret position before", before, caretChange.before());
        assertEquals("Incorrect caret position after", after, caretChange.after());
    }

    @Test
    public void caret_at_the_end_of_selection() {
        checkCaret(new PlainTextChange(2, 2, "test", "one"), 2, 5);
        checkCaret(new PlainTextChange(2, 2, "test", "three"), 2, 7);
        checkCaret(new PlainTextChange(2, 2, "test", ""), 2, 2);
        checkCaret(new PlainTextChange(2, 2, "", "one"), 2, 5);
    }

    @Test
    public void caret_at_the_start_of_selection() {
        checkCaret(new PlainTextChange(6, 2, "test", "one"), 6, 5);
        checkCaret(new PlainTextChange(6, 2, "test", "three"), 6, 7);
        checkCaret(new PlainTextChange(6, 2, "test", ""), 6, 2);
    }

    @Test
    public void remove_what_it_adds_is_identity() {
        assertTrue(new PlainTextChange(2, 2, "test", "test").isIdentity());
        assertTrue(new PlainTextChange(6, 2, "test", "test").isIdentity());
        assertFalse(new PlainTextChange(6, 2, "", "test").isIdentity());
        assertFalse(new PlainTextChange(2, 2, "test", "").isIdentity());
        assertTrue(new PlainTextChange(2, 2, "", "").isIdentity());
    }

    @Test
    public void equality_if_all_equals() {
        PlainTextChange change = new PlainTextChange(5, 2, "art", "umbl");
        assertEquals(1, change.getNetLength());
        assertEquals(5, change.getRemovalEnd());
        assertEquals(6, change.getInsertionEnd());
        assertEquals(new PlainTextChange(2, 2, "art", "umbl"), change);
        assertNotEquals(new PlainTextChange(2, 2, "art", ""), change);
        assertNotEquals(new PlainTextChange(2, 2, "", "umbl"), change);
        assertNotEquals(new PlainTextChange(1, 1, "art", "umbl"), change);
        assertNotEquals(null, change);
        assertNotEquals("test", change);
    }

    @Test
    public void invert_text_change() {
        // Invert removal : st[art]ing => sting
        checkContent(new PlainTextChange(2, 2, "art", "").invert(), 2, "", "art");

        // Invert replacement : st[art]ing => stumbling
        checkContent(new PlainTextChange(2, 2, "art", "umbl").invert(), 2, "umbl", "art");

        // Invert addition : sting => starting
        checkContent(new PlainTextChange(2, 2, "", "art").invert(), 2, "art", "");
    }

    @Test
    public void merge_addition_followed_by_removal_that_starts_before() {
        for(int caretPosition : new int[] { 1, 4 }) {
            PlainTextChange former = new PlainTextChange(2, 2, "", "CD"); // ABE => ABCDE
            PlainTextChange latter = new PlainTextChange(caretPosition, 1, "BCD", ""); // ABCDE => AE
            PlainTextChange merged = former.mergeWith(latter).orElseThrow();
            checkContent(merged, 1, "B", ""); // ABE => AE
            checkCaret(merged, 2, 1);
            assertTrue(latter.mergeWith(former).isEmpty());
        }
    }

    @Test
    public void merge_replace_followed_by_removal_that_starts_before() {
        // ABFFE => ABCDE
        PlainTextChange[] former = new PlainTextChange[] {
                new PlainTextChange(2, 2, "FF", "CD"),
                new PlainTextChange(4, 2, "FF", "CD")
        };
        PlainTextChange latter = new PlainTextChange(1, 1, "BCD", ""); // ABCDE => AE
        PlainTextChange[] merged = new PlainTextChange[]{
                former[0].mergeWith(latter).orElseThrow(),
                former[1].mergeWith(latter).orElseThrow()
        };
        // ABFFE => AE
        checkContent(merged[0], 1, "BFF", "");
        checkContent(merged[1], 1, "BFF", "");
        checkCaret(former[0], 2, 4);
        checkCaret(former[1], 4, 4);
        checkCaret(latter, 1, 1);
        checkCaret(merged[0], 1, 1);
        checkCaret(merged[1], 4, 1);
        assertTrue(latter.mergeWith(former[0]).isEmpty());
        assertTrue(latter.mergeWith(former[1]).isEmpty());
    }

    @Test
    public void merge_replace_followed_by_greater_replace_finishing_at_the_same_index() {
        PlainTextChange former = new PlainTextChange(2, 2, "CD", "FG"); // ABCDE => ABFGE
        PlainTextChange latter = new PlainTextChange(1, 1, "BFG", "HIJ"); // ABFGE => AHIJE
        PlainTextChange merged = former.mergeWith(latter).orElseThrow();
        checkContent(merged, 1, "BCD", "HIJ"); // ABCDE => AHIJE
        checkCaret(merged, 1, 4);
        // Not a correct case because strings are different, but that is how the previous code behaved
        checkContent(latter.mergeWith(former).orElseThrow(), 1, "BFG", "HFG"); // ABFGE => AHFGE
    }

    @Test
    public void merge_replace_followed_by_replace_starting_and_finishing_before() {
        PlainTextChange former = new PlainTextChange(2, 2, "CD", "FG"); // ABCDE => ABFGE
        PlainTextChange latter = new PlainTextChange(1, 1, "BFGE", "P"); // ABFGE => AP
        // This one could have been merged (ABCDE => AP) but the previous code didn't
        assertTrue(former.mergeWith(latter).isEmpty());
        checkContent(latter.mergeWith(former).orElseThrow(), 1, "BFGECD", "PFG"); // ABFGECD => APCD => APFG
    }

    @Test
    public void merge_addition_followed_by_removal_of_the_addition() {
        PlainTextChange former = new PlainTextChange(2, 2, "", "CD"); // ABE => ABCDE
        PlainTextChange latter = new PlainTextChange(2, 2, "CD", ""); // ABCDE => ABE
        PlainTextChange merged = former.mergeWith(latter).orElseThrow();
        checkContent(merged, 2, "", ""); // ABE => ABE
        checkCaret(merged, 2, 2);
        checkContent(latter.mergeWith(former).orElseThrow(), 2, "CD", "CD");
    }

    @Test
    public void merge_replace_followed_by_removal_of_the_addition() {
        for(int caretBefore : new int[] {2, 4}) {
            PlainTextChange former = new PlainTextChange(caretBefore, 2, "FF", "CD"); // ABFFE => ABCDE
            PlainTextChange latter = new PlainTextChange(6 - caretBefore, 2, "CD", ""); // ABCDE => ABE
            PlainTextChange merged = former.mergeWith(latter).orElseThrow();
            checkContent(merged, 2, "FF", ""); // ABFFE => ABE
            checkCaret(merged, caretBefore, 2);
            checkContent(latter.mergeWith(former).orElseThrow(), 2, "CDFF", "CD"); // ABCDFFE => ABCDE
        }
    }

    @Test
    public void merge_removal_followed_by_addition_for_different_but_same_size() {
        PlainTextChange former = new PlainTextChange(2, 2, "CD", ""); // ABCDE => ABE
        PlainTextChange latter = new PlainTextChange(2, 2, "", "FG"); // ABE => ABFGE
        checkContent(former.mergeWith(latter).orElseThrow(), 2, "CD", "FG"); // ABCDE => ABFGE
        // I wrote tests to match the existing behaviour, but that is a wrong scenario. You cannot merge insert
        // "FG" at index 2 and then remove "CD" at index 2 because "FG" is at index 2. Now, that is how the old code
        // behaved.
        checkContent(latter.mergeWith(former).orElseThrow(), 2, "", ""); // ABCDE => ABFGE
    }

    @Test
    public void merge_removal_followed_by_addition_at_next_index() {
        PlainTextChange former = new PlainTextChange(1, 1, "BCD", ""); // ABCDE => AE
        PlainTextChange latter = new PlainTextChange(2, 2, "", "CD"); // AE => AECD
        assertTrue(former.mergeWith(latter).isEmpty());
        checkContent(latter.mergeWith(former).orElseThrow(), 1, "B", ""); // ABE => AE
    }

    @Test
    public void merge_removal_followed_by_addition_at_previous_index() {
        PlainTextChange former = new PlainTextChange(2, 2, "CD", ""); // ABCDE => ABE
        PlainTextChange latter = new PlainTextChange(1, 1, "", "CCC"); // ABE => ACCCBE
        assertTrue(former.mergeWith(latter).isEmpty());
        // This is a wrong case because the reverse should have been +CCC and -CC. This is how the previous code behaved
        // as it didn't match on the content.
        checkContent(latter.mergeWith(former).orElseThrow(), 1, "", "C");
    }

    @Test
    public void merge_consecutive_replace_then_backspace() {
        PlainTextChange former = new PlainTextChange(0, 0, "reig", "seve"); // reign => seven
        PlainTextChange latter = new PlainTextChange(2, 2, "ve", ""); // seven => sen
        PlainTextChange merged = former.mergeWith(latter).orElseThrow();
        checkContent(merged, 0, "reig", "se"); // reign => sen
        checkCaret(merged, 0, 2);
        assertTrue(latter.mergeWith(former).isEmpty());
    }

    @Test
    public void merge_consecutive_replace_then_delete() {
        PlainTextChange former = new PlainTextChange(0, 0, "reig", "seve"); // reign => seven
        PlainTextChange latter = new PlainTextChange(4, 4, "n", ""); // seven => seve
        checkContent(former.mergeWith(latter).orElseThrow(), 0, "reign", "seve"); // reign => seve
        checkContent(latter.mergeWith(former).orElseThrow(), 0, "reign", "seve"); // reign => seve
    }

    @Test
    public void merge_consecutive_add_then_add() {
        PlainTextChange former = new PlainTextChange(2, 2, "", "si"); // red => resid
        PlainTextChange latter = new PlainTextChange(4, 4, "", "gne"); // resid => resigned
        PlainTextChange merged = former.mergeWith(latter).orElseThrow();
        checkContent(merged, 2, "", "signe"); // red => resigned
        checkCaret(merged, 2, 7);
        // Invert is empty because they are non-consecutive
        assertTrue(latter.mergeWith(former).isEmpty());
    }

    @Test
    public void merge_consecutive_add_then_delete() {
        PlainTextChange former = new PlainTextChange(2, 2, "", "si"); // red => resid
        PlainTextChange latter = new PlainTextChange(4, 4, "d", ""); // resid => resi
        checkContent(former.mergeWith(latter).orElseThrow(), 2, "d", "si"); // red => resi
        // Invert is empty because they are non-consecutive
        assertTrue(latter.mergeWith(former).isEmpty());
    }

    @Test
    public void merge_consecutive_add_then_backspace() {
        PlainTextChange former = new PlainTextChange(2, 2, "", "si"); // red => resid
        PlainTextChange latter = new PlainTextChange(3, 3, "i", ""); // resid => resd
        checkContent(former.mergeWith(latter).orElseThrow(), 2, "", "s"); // red => resd
        // Invert is empty because they are non-consecutive
        assertTrue(latter.mergeWith(former).isEmpty());
    }

    @Test
    public void merge_consecutive_delete_then_delete() {
        PlainTextChange former = new PlainTextChange(0, 0, "re", ""); // resigned => signed
        PlainTextChange latter = new PlainTextChange(0, 0, "sign", ""); // signed => ed
        checkContent(former.mergeWith(latter).orElseThrow(), 0, "resign", ""); // resigned => ed
        // Invert works too because we don't know which comes first
        checkContent(latter.mergeWith(former).orElseThrow(), 0, "signre", ""); // resigned => ed
    }

    @Test
    public void merge_consecutive_delete_then_add() {
        PlainTextChange former = new PlainTextChange(2, 2, "si", ""); // resigned => regned
        PlainTextChange latter = new PlainTextChange(2, 2, "", "i"); // regned => reigned
        checkContent(former.mergeWith(latter).orElseThrow(), 2, "si", "i"); // resigned => reigned
        // Invert is empty because they are non-consecutive
        assertTrue(latter.mergeWith(former).isEmpty());
    }

    @Test
    public void merge_add_before_remove_same_final_position() {
        // Add at position 3 for size 2 followed by remove at position 2 size 3
        PlainTextChange former = new PlainTextChange(3, 3, "", "hi"); // abcdefg => abchidefg
        PlainTextChange latter = new PlainTextChange(2, 2, "chi", ""); // abchidefg => abdefg
        checkContent(former.mergeWith(latter).orElseThrow(), 2, "c", ""); // abcdefg => abdefg
        // Invert is empty because they are non-consecutive
        assertTrue(latter.mergeWith(former).isEmpty());
    }

    @Test
    public void merge_non_consecutive_add_then_add() {
        PlainTextChange former = new PlainTextChange(2, 2, "", "a"); // test => teast
        PlainTextChange latter = new PlainTextChange(4, 4, "", "i"); // teast => teasit
        // Invert is empty because they are non-consecutive
        assertTrue(former.mergeWith(latter).isEmpty());
        assertTrue(latter.mergeWith(former).isEmpty());
    }

    @Test
    public void merge_add_at_same_position() {
        PlainTextChange former = new PlainTextChange(2, 2, "", "a"); // test => teast
        PlainTextChange latter = new PlainTextChange(2, 2, "", "b"); // teast => tebast
        // Invert does not work for any
        assertTrue(latter.mergeWith(former).isEmpty());
        assertTrue(former.mergeWith(latter).isEmpty());
    }

    @Test
    public void merge_non_consecutive_add_then_remove() {
        PlainTextChange former = new PlainTextChange(2, 2, "", "a"); // test => teast
        PlainTextChange latter1 = new PlainTextChange(1, 1, "e", ""); // teast => tast
        PlainTextChange latter2 = new PlainTextChange(4, 4, "s", ""); // teast => teat
        // Invert does not work for any
        assertTrue(latter1.mergeWith(former).isEmpty());
        assertTrue(latter2.mergeWith(former).isEmpty());
        assertTrue(former.mergeWith(latter1).isEmpty());
        assertTrue(former.mergeWith(latter2).isEmpty());
    }

    @Test
    public void merge_non_consecutive_remove_then_remove() {
        PlainTextChange former = new PlainTextChange(2, 2, "e", ""); // test => tst
        PlainTextChange latter = new PlainTextChange(3, 3, "t", ""); // tst => ts
        // Invert does not work for any
        assertTrue(former.mergeWith(latter).isEmpty());
        checkContent(latter.mergeWith(former).orElseThrow(), 2, "et", "");
    }

    @Test
    public void merge_non_consecutive_remove_then_add() {
        PlainTextChange former = new PlainTextChange(2, 2, "e", ""); // test => tst
        PlainTextChange latter = new PlainTextChange(3, 3, "", "a"); // tst => tsat
        // Invert does not work for any
        assertTrue(latter.mergeWith(former).isEmpty());
        assertTrue(former.mergeWith(latter).isEmpty());
    }
}
