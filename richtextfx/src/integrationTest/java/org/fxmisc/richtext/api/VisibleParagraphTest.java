package org.fxmisc.richtext.api;

import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.model.Paragraph;
import org.junit.Test;
import org.junit.Assert;
import org.reactfx.collection.LiveList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class VisibleParagraphTest extends InlineCssTextAreaAppTest {

    @Test
    public void get_first_visible_paragraph_index_with_non_blank_lines() {
        String[] lines = {
                "abc",
                "def",
                "ghi"
        };
        interact(() -> {
            area.setWrapText(true);
            stage.setWidth(120);
            area.replaceText(String.join("\n", lines));
        });
        assertEquals(0, area.firstVisibleParToAllParIndex());
        assertEquals(2, area.lastVisibleParToAllParIndex());
        assertEquals(1, area.visibleParToAllParIndex(1));
    }
    @Test
    public void get_last_visible_paragraph_index_with_non_blank_lines() {
        String[] lines = {
                "abc",
                "def",
                "ghi"
        };
        interact(() -> {
            area.setWrapText(true);
            stage.setWidth(120);
            area.replaceText(String.join("\n", lines));
        });
        assertEquals(2, area.lastVisibleParToAllParIndex());
    }
    @Test
    public void get_specific_visible_paragraph_index_with_non_blank_lines() {
        String[] lines = {
                "abc",
                "def",
                "ghi"
        };
        interact(() -> {
            area.setWrapText(true);
            stage.setWidth(120);
            area.replaceText(String.join("\n", lines));
        });
        assertEquals(2, area.visibleParToAllParIndex(2));
    }
    @Test
    public void get_first_visible_paragraph_index_with_all_blank_lines() {
        String[] lines = {
                "",
                "",
                ""
        };
        interact(() -> {
            area.setWrapText(true);
            stage.setWidth(120);
            area.replaceText(String.join("\n", lines));
        });
        assertEquals(0, area.firstVisibleParToAllParIndex());
    }
    @Test
    public void get_last_visible_paragraph_index_with_all_blank_lines() {
        String[] lines = {
                "",
                "",
                ""
        };
        interact(() -> {
            area.setWrapText(true);
            stage.setWidth(120);
            area.replaceText(String.join("\n", lines));
        });
        assertEquals(2, area.lastVisibleParToAllParIndex());
    }
    @Test
    public void get_specific_visible_paragraph_index_with_all_blank_lines() {
        String[] lines = {
                "",
                "",
                ""
        };
        interact(() -> {
            area.setWrapText(true);
            stage.setWidth(120);
            area.replaceText(String.join("\n", lines));
        });
        assertEquals(2, area.visibleParToAllParIndex(2));
    }

    @Test
    public void get_first_visible_paragraph_index_with_some_blank_lines() {
        String[] lines = {
                "abc",
                "",
                "",
                "",
                "def",
                ""
        };
        interact(() -> {
            area.setWrapText(true);
            stage.setWidth(120);
            area.replaceText(String.join("\n", lines));
        });
        assertEquals(0, area.firstVisibleParToAllParIndex());
    }
    @Test
    public void get_last_visible_paragraph_index_with_some_blank_lines() {
        String[] lines = {
                "abc",
                "",
                "",
                "",
                "def",
                ""
        };
        interact(() -> {
            area.setWrapText(true);
            stage.setWidth(120);
            area.replaceText(String.join("\n", lines));
        });
        assertEquals(5, area.lastVisibleParToAllParIndex());
    }
    @Test
    public void get_specific_visible_paragraph_index_with_some_blank_lines() {
        String[] lines = {
                "abc",
                "",
                "",
                "",
                "def",
                ""
        };
        interact(() -> {
            area.setWrapText(true);
            stage.setWidth(120);
            area.replaceText(String.join("\n", lines));
        });
        assertEquals(3, area.visibleParToAllParIndex(3));
    }
}