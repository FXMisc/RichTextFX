package org.fxmisc.richtext.style;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.fxmisc.richtext.SceneGraphTests;
import org.junit.Test;

import javafx.scene.shape.Path;
import javafx.scene.text.Text;


public class StylingTests extends SceneGraphTests {

    private final static String HELLO = "Hello ";
    private final static String WORLD = "World";
    private final static String AND_ALSO_THE = " and also the ";
    private final static String SUN = "Sun";
    private final static String AND_MOON = " and Moon";

    @Test
    public void simpleStyling() {
        // setup
        interact(() -> {
            area.replaceText(HELLO + WORLD + AND_MOON);
        });

        // expected: one text node which contains the complete text
        List<Text> textNodes = getTextNodes(0);
        assertEquals(1, textNodes.size());

        interact(() -> {
            area.setStyle(HELLO.length(), HELLO.length() + WORLD.length(), "-fx-font-weight: bold;");
        });

        // expected: three text nodes
        textNodes = getTextNodes(0);
        assertEquals(3, textNodes.size());

        Text first = textNodes.get(0);
        assertEquals("Hello ", first.getText());
        assertEquals("Regular", first.getFont().getStyle());

        Text second = textNodes.get(1);
        assertEquals("World", second.getText());
        assertEquals("Bold", second.getFont().getStyle());

        Text third = textNodes.get(2);
        assertEquals(" and Moon", third.getText());
        assertEquals("Regular", third.getFont().getStyle());
    }


    @Test
    public void underlineStyling() {

        final String underlineStyle = "-rtfx-underline-color: red; -rtfx-underline-dash-array: 2 2; -rtfx-underline-width: 1; -rtfx-underline-cap: butt;";
        
        // setup
        interact(() -> {
            area.replaceText(HELLO + WORLD + AND_ALSO_THE + SUN + AND_MOON);
        });

        // expected: one text node which contains the complete text
        List<Text> textNodes = getTextNodes(0);
        assertEquals(1, textNodes.size());
        assertEquals(HELLO + WORLD + AND_ALSO_THE + SUN + AND_MOON, 
                     textNodes.get(0).getText());

        interact(() -> {
            final int start1 = HELLO.length();
            final int end1 = start1 + WORLD.length();
            area.setStyle(start1, end1, underlineStyle);

            final int start2 = end1 + AND_ALSO_THE.length();
            final int end2 = start2 + SUN.length();
            area.setStyle(start2, end2, underlineStyle);
        });

        // expected: five text nodes
        textNodes = getTextNodes(0);
        assertEquals(5, textNodes.size());

        Text first = textNodes.get(0);
        assertEquals(HELLO, first.getText());
        Text second = textNodes.get(1);
        assertEquals(WORLD, second.getText());
        Text third = textNodes.get(2);
        assertEquals(AND_ALSO_THE, third.getText());
        Text fourth = textNodes.get(3);
        assertEquals(SUN, fourth.getText());
        Text fifth = textNodes.get(4);
        assertEquals(AND_MOON, fifth.getText());

        // determine the underline paths - need to be two of them!
        List<Path> underlineNodes = getUnderlinePaths(0);
        assertEquals(2, underlineNodes.size());
    }
}
