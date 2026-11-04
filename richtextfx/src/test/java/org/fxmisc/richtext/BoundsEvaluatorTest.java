package org.fxmisc.richtext;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BoundsEvaluatorTest {
    private void checkBounds(Bounds bounds, double xMin, double yMin, double xMax, double yMax) {
        assertEquals(xMin, bounds.getMinX(), "Invalid min X value");
        assertEquals(yMin, bounds.getMinY(), "Invalid min Y value");
        assertEquals(xMax, bounds.getMaxX(), "Invalid max X value");
        assertEquals(yMax, bounds.getMaxY(), "Invalid max Y value");
    }

    @Test
    @DisplayName("Evaluate paragraph bounds on screen")
    void evaluateParagraphBoundsOnScreen() {
        ScreenBounds boundsEvaluator = new ScreenBounds(new BoundingBox(20, 20, 100, 100));
        // Start < Screen ; End < Screen
        checkBounds(boundsEvaluator.getParagraphBoundsFrom(new BoundingBox(10, 10, 50, 50)),
                20, 20, 120, 60);
        // Start < Screen ; End > Screen
        checkBounds(boundsEvaluator.getParagraphBoundsFrom(new BoundingBox(10, 10, 111, 111)),
                20, 20, 120, 120);
        // Start > Screen ; End > Screen
        checkBounds(boundsEvaluator.getParagraphBoundsFrom(new BoundingBox(21, 21, 100, 100)),
                21, 21, 121, 120);
        // Start > Screen ; End < Screen
        checkBounds(boundsEvaluator.getParagraphBoundsFrom(new BoundingBox(21, 21, 98, 98)),
                21, 21, 121, 119);
        // Start = Screen ; End = Screen
        checkBounds(boundsEvaluator.getParagraphBoundsFrom(new BoundingBox(20, 20, 100, 100)),
                20, 20, 120, 120);
        // Miscellaneous
        checkBounds(boundsEvaluator.getParagraphBoundsFrom(new BoundingBox(0, 0, 80, 80)),
                20, 20, 120, 80);
        checkBounds(boundsEvaluator.getParagraphBoundsFrom(new BoundingBox(30, 30, 120, 120)),
                30, 30, 130, 120);
        checkBounds(boundsEvaluator.getParagraphBoundsFrom(new BoundingBox(110, 110, 130, 130)),
                110, 110, 210, 120);
    }
}
