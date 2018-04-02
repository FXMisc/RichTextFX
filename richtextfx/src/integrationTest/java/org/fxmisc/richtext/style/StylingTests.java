package org.fxmisc.richtext.style;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.fxmisc.richtext.SceneGraphTests;
import org.fxmisc.richtext.model.StyleSpansBuilder;
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

    @Test
    public void consecutive_border_styles_that_are_the_same_are_rendered_with_one_shape() {
        final String boxed = "-rtfx-border-stroke-width: .75pt;"
                           + "-rtfx-border-stroke-type: outside;"
                           + "-rtfx-border-stroke-color: darkgoldenrod;"
                           + "-rtfx-background-color: antiquewhite;";

        final String other = "-fx-font-weight: bolder;"
                           + "-fx-fill: blue;";

        String text = "Lorem ipsum dolor sit amet consectetuer adipiscing elit";
        interact(() -> area.replaceText(text));

        // split the text up for easier style adding using String#length
        String first = text.substring(0, "Lorem ".length());
        int offset = first.length();
        String second = text.substring(offset, offset + "ipsum dolo".length());
        offset += second.length();
        String third = text.substring(offset, offset + "r sit amet".length());
        offset += third.length();
        String fourth = text.substring(offset);

        // create the styleSpans object with overlayed styles at the second span
        StyleSpansBuilder<String> builder = new StyleSpansBuilder<>();
        builder.add(boxed, first.length());
        builder.add(boxed + other, second.length());
        builder.add(boxed, third.length());
        builder.add("", fourth.length());
        interact(() -> area.setStyleSpans(0, builder.create()));

        // end result: 1 path should be used for Boxed, not three
        // Text:  "Lorem ipsum dolor sit amet consectetuer adipiscing elit
        // Boxed: |**************************|
        // Other:       |**********|

        assertEquals(1, getBorderPaths(0).size());
    }

    @Test
    public void consecutive_underline_styles_that_are_the_same_are_rendered_with_one_shape() {
        final String underline = "-rtfx-underline-width: .75pt;" +
                                 "-rtfx-underline-color: red;" +
                                 "-rtfx-underline-dash-array: 2 2;";

        final String other = "-fx-font-weight: bolder;" +
                             "-fx-fill: blue;";

        String text = "Lorem ipsum dolor sit amet consectetuer adipiscing elit";
        interact(() -> area.replaceText(text));

        // split the text up for easier style adding using String#length
        String first = text.substring(0, "Lorem ".length());
        int offset = first.length();
        String second = text.substring(offset, offset + "ipsum dolo".length());
        offset += second.length();
        String third = text.substring(offset, offset + "r sit amet".length());
        offset += third.length();
        String fourth = text.substring(offset);

        // create the styleSpans object with overlayed styles at the second span
        StyleSpansBuilder<String> builder = new StyleSpansBuilder<>();
        builder.add(underline, first.length());
        builder.add(underline + other, second.length());
        builder.add(underline, third.length());
        builder.add("", fourth.length());
        interact(() -> area.setStyleSpans(0, builder.create()));

        // end result: 1 path should be used for Underline, not three
        // Text:      "Lorem ipsum dolor sit amet consectetuer adipiscing elit
        // Underline: |**************************|
        // Other:           |**********|

        assertEquals(1, getUnderlinePaths(0).size());
    }

    @Test
    public void unconsecutive_background_styles_should_each_be_rendered_with_their_own_shape() {
        String style = "-rtfx-background-color: #ccc;";
        interact(() -> {
            // Text:  |aba abba|
            // Style: |x x x  x|

            area.replaceText("aba abba");
            area.setStyle(0, 1, style);
            area.setStyle(2, 3, style);
            area.setStyle(4, 5, style);
            area.setStyle(7, 8, style);
        });
        List<Path> paths = getBackgroundPaths(0);
        assertEquals(4, paths.size());
    }
}
