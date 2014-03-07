package org.fxmisc.richtext;

import static org.fxmisc.richtext.TwoDimensional.Bias.*;
import static org.junit.Assert.*;

import org.fxmisc.richtext.TwoDimensional.Position;
import org.junit.Test;

public class TwoLevelNavigatorTest {

    // navigator with 5 elements, each of length 10
    private final TwoLevelNavigator navigator = new TwoLevelNavigator(() -> 5, i -> 10);

    @Test
    public void testPositiveOffsetWithBackwardBias() {
        Position pos = navigator.offsetToPosition(10, Backward);
        assertEquals(0, pos.getMajor());
        assertEquals(10, pos.getMinor());
    }

    @Test
    public void testPositiveOffsetWithForwardBias() {
        Position pos = navigator.offsetToPosition(10, Forward);
        assertEquals(1, pos.getMajor());
        assertEquals(0, pos.getMinor());
    }

    @Test
    public void testNegativeOffsetWithBackwardBias() {
        Position pos = navigator.position(4, 10);
        pos = pos.offsetBy(-10, Backward);
        assertEquals(3, pos.getMajor());
        assertEquals(10, pos.getMinor());
    }

    @Test
    public void testNegativeOffsetWithForwardBias() {
        Position pos = navigator.position(4, 10);
        pos = pos.offsetBy(-10, Forward);
        assertEquals(4, pos.getMajor());
        assertEquals(0, pos.getMinor());
    }

    @Test
    public void testZeroOffsetWithBackwardBias() {
        Position pos = navigator.position(3, 0);
        pos = pos.offsetBy(0, Backward);
        assertEquals(2, pos.getMajor());
        assertEquals(10, pos.getMinor());

        // additional zero backward offset should have no effect
        assertEquals(pos, pos.offsetBy(0, Backward));
    }

    @Test
    public void testZeroOffsetWithForwardBias() {
        Position pos = navigator.position(2, 10);
        pos = pos.offsetBy(0, Forward);
        assertEquals(3, pos.getMajor());
        assertEquals(0, pos.getMinor());

        // additional zero forward offset should have no effect
        assertEquals(pos, pos.offsetBy(0, Forward));
    }

    @Test
    public void testRightBoundary() {
        Position pos = navigator.offsetToPosition(100, Forward);
        assertEquals(4, pos.getMajor());
        assertEquals(60, pos.getMinor());

        pos = pos.clamp();
        assertEquals(4, pos.getMajor());
        assertEquals(9, pos.getMinor());
    }

    @Test
    public void testLeftBoundary() {
        Position pos = navigator.offsetToPosition(25, Forward).offsetBy(-50, Forward);
        assertEquals(0, pos.getMajor());
        assertEquals(0, pos.getMinor());
    }
}
