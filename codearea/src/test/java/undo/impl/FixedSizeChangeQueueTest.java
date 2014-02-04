package undo.impl;

import static org.junit.Assert.*;

import org.junit.Test;

public class FixedSizeChangeQueueTest {

    @Test
    public void testOverflow() {
        ChangeQueue<Integer> queue = new FixedSizeChangeQueue<>(5);
        queue.push(1, 2, 3);
        queue.push(4, 5, 6, 7, 8, 9);

        assertFalse(queue.hasNext());
        assertTrue(queue.hasPrev());
        assertEquals(Integer.valueOf(9), queue.prev());
        assertTrue(queue.hasNext());
        assertEquals(Integer.valueOf(8), queue.prev());
        assertEquals(Integer.valueOf(7), queue.prev());
        assertEquals(Integer.valueOf(6), queue.prev());
        assertEquals(Integer.valueOf(5), queue.prev());
        assertFalse(queue.hasPrev());
        assertTrue(queue.hasNext());
    }

}
