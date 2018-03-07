package org.fxmisc.richtext.api;

import javafx.stage.Stage;
import org.fxmisc.richtext.InlineCssTextAreaAppTest;
import org.fxmisc.richtext.MultiChangeBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MultiChangeTest extends InlineCssTextAreaAppTest {

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);

        // initialize area with some text
        area.replaceText("(text)");
    }

    @Test
    public void committing_single_change_works() {
        interact(() -> {
            String text = area.getText();
            area.createMultiChange(1)
                    .deleteText(0, 1)
                    .commit();

            assertEquals(text.substring(1), area.getText());
        });
    }

    @Test
    public void committing_relative_change_works() {
        interact(() -> {
            String text = area.getText();
            String hello = "hello";
            String world = "world";
            area.createMultiChange(2)
                    .insertText(0, hello)
                    .insertText(0, world)
                    .commit();

            assertEquals(hello + world + text, area.getText());
        });
    }

    @Test
    public void committing_relative_change_backToFront_works() {
        interact(() -> {
            String text = area.getText();
            String hello = "hello";
            String world = "world";
            area.createMultiChange(2)
                    .insertText(6, world)
                    .insertText(0, hello)
                    .commit();

            assertEquals(hello + text + world, area.getText());

            area.undo();
            assertEquals(text, area.getText());
        });
    }

    @Test
    public void committing_absolute_change_works() {
        interact(() -> {
            String text = area.getText();
            String hello = "hello";
            String world = "world";
            area.createMultiChange(2)
                    .insertText(0, hello)
                    .insertTextAbsolutely(0, world)
                    .commit();

            assertEquals(world + hello + text, area.getText());
        });
    }

    @Test
    public void changing_same_content_multiple_times_works() {
        interact(() -> {
            String text = area.getText();

            area.createMultiChange(4)
                    .replaceTextAbsolutely(0, 1, "a")
                    .replaceTextAbsolutely(0, 1, "b")
                    .replaceTextAbsolutely(0, 1, "c")
                    .replaceTextAbsolutely(0, 1, "d")
                    .commit();

            assertEquals("d" + text.substring(1), area.getText());
        });
    }

    @Test
    public void attempting_to_reuse_builder_throws_exception() {
        interact(() -> {
            MultiChangeBuilder<String, String, String> builder = area.createMultiChange(1)
                    .insertText(0, "hey");
            builder.commit();
            try {
                builder.commit();
                fail();
            } catch (IllegalStateException e) {
                // cannot reuse builder once commit changes
            }
        });
    }

    @Test
    public void attempting_to_commit_without_any_stored_changes_throws_exception() {
        interact(() -> {
            MultiChangeBuilder<String, String, String> builder = area.createMultiChange(1);
            try {
                builder.commit();
                fail();
            } catch (IllegalStateException e) {
                // no changes were stored in the builder
            }
        });
    }
}
