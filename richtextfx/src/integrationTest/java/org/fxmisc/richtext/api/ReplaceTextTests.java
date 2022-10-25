package org.fxmisc.richtext.api;

import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.junit.Test;

public class ReplaceTextTests extends InlineCssTextAreaAppTest {

    @Test
    public void deselect_before_replaceText_does_not_cause_index_out_of_bounds_exception()
    {
        interact( () ->
        {
            area.replaceText( "1234567890\n\nabcdefghij\n\n1234567890\n\nabcdefghij" );

            // Select last line of text
            area.requestFollowCaret();
            area.selectLine();

            // Calling deselect, primes an IndexOutOfBoundsException to be thrown after replaceText
            area.deselect();

            // An internal IndexOutOfBoundsException may occur in ParagraphText.getRangeShapeSafely
            area.replaceText( "1234567890\n\nabcdefghijklmno\n\n1234567890\n\nabcde" );

            // This would fail if an exception occurred during ParagraphText.layoutChildren:updateSelectionShape()
            area.selectLine();
        });

    }

    @Test
    public void previous_selection_before_replaceText_does_not_cause_index_out_of_bounds_exception()
    {
        interact( () ->
        {
        	// For this test to work the area MUST be at the end of the document
        	area.requestFollowCaret();

            // First text supplied by bug reporter: has 9 paragraphs, 344 characters
            area.replaceText( getTextA() );

            // Any text can be selected anywhere in the document, this primed the exception
            area.selectWord();

            // Second text supplied by bug reporter: has 9 paragraphs, 344 characters, and contains two � characters
            area.replaceText( getTextB() );

            // An internal IndexOutOfBoundsException may occur in ParagraphText.getRangeShapeSafely
            area.replaceText( getTextA() );

            // This would fail if an exception occurred during ParagraphText.layoutChildren:updateSelectionShape()
            area.selectLine();
        });
    }


    // Reduced text supplied by bug reporter: has 9 paragraphs, 344 characters
    private String getTextA()
    {
        return "<!DOCTYPE HTML>\n" +
                "\n" +
                "    Parempaa kuvaa ja &auml;&auml;nentoistoa jo vuodesta 1981 - HifiStudio</title>\n" +
                "\n" +
                "<meta property=\"og:title\" content=\"HifiStudio - Parempaa kuvaa ja &auml;&auml;nentoistoa jo vuodesta 1981\" />\n" +
                "\n" +
                "<meta property=\"og:url\" content=\"https://www.hifistudio.fi/fi/\" />\n" +
                "\n" +
                "                            <li><a href=\"/fi/tuotteet/muut-hifil";
    }


    // Reduced text supplied by bug reporter: has 9 paragraphs, 344 characters, and contains two � characters
    private String getTextB()
    {
        return "<!DOCTYPE HTML>\n" +
                "\n" +
                "    SEIN�TELINEET - HifiStudio</title>\n" +
                "\n" +
                "<meta property=\"og:title\" content=\"HifiStudio - SEIN�TELINEET\" />\n" +
                "\n" +
                "<meta property=\"og:url\" content=\"https://www.hifistudio.fi/fi/tuotteet/laitetelineet/seinatelineet/91052\" />\n" +
                "\n" +
                "                            <li><a href=\"/fi/tuotteet/muut-hifilaitteet/cd-soittimet/15035\" class=\"top-product";
    }
}