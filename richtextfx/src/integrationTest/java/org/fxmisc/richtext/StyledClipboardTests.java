package org.fxmisc.richtext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.stage.Stage;

import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.StyledDocument;
import org.fxmisc.richtext.model.StyledSegment;
import org.junit.Before;
import org.junit.Test;
import org.reactfx.util.Tuple2;

/**
 * Copies and pastes styled text through the real system clipboard (issue #1325).
 * Calls {@code copy()}/{@code paste()} directly rather than pressing keys, so it runs on every OS.
 */
public class StyledClipboardTests extends InlineCssTextAreaAppTest {

    private static final String RED = "-fx-fill: red;";
    private static final String BOLD = "-fx-font-weight: bold;";

    private Codec<StyledDocument<String, String, String>> codec;
    private DataFormat format;

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);
        Tuple2<Codec<String>, Codec<StyledSegment<String, String>>> codecs = area.getStyleCodecs().get();
        codec = ReadOnlyStyledDocument.codec(codecs._1, codecs._2, area.getSegOps());
        format = ClipboardHelper.dataFormat(codec.getName());
    }

    @Before
    public void setUp() {
        interact(() -> {
            area.replaceText("red bold plain");
            area.setStyle(0, 3, RED);
            area.setStyle(4, 8, BOLD);
            Clipboard.getSystemClipboard().clear();
        });
    }

    @Test
    public void copy_does_not_put_java_serialized_content_on_the_clipboard() {
        interact(() -> {
            area.selectRange(0, 8);
            area.copy();

            Clipboard clipboard = Clipboard.getSystemClipboard();
            assertTrue(clipboard.hasContent(format));
            Object content = clipboard.getContent(format);
            assertTrue("expected the raw codec payload as a ByteBuffer, got " + content.getClass(),
                    content instanceof ByteBuffer);
            ByteBuffer buffer = ((ByteBuffer) content).duplicate();
            assertFalse("payload must not be Java-serialized",
                    buffer.remaining() >= 2 && (buffer.get() & 0xFF) == 0xAC && (buffer.get() & 0xFF) == 0xED);
            assertEquals("red bold", clipboard.getString());
        });
    }

    @Test
    public void copy_then_paste_preserves_styles() {
        interact(() -> {
            area.selectRange(0, 8);
            area.copy();
            area.moveTo(area.getLength());
            area.paste();
        });

        int start = "red bold plain".length();
        assertEquals("red bold plainred bold", area.getText());
        assertEquals(RED, area.getStyleOfChar(start));
        assertEquals(RED, area.getStyleOfChar(start + 2));
        assertEquals(BOLD, area.getStyleOfChar(start + 4));
        assertEquals(BOLD, area.getStyleOfChar(start + 7));
    }

    /**
     * RichTextFX 0.11.7 and earlier put a byte[] on the clipboard. Depending on the JavaFX version running the
     * test, it comes back as a byte[], or (JavaFX 17.0.21, 21.0.13, 25.0.5, 27.0.1 and later) as a ByteBuffer
     * holding the serialized byte[]. Either way it must paste with its styles.
     */
    @Test
    public void paste_of_content_copied_by_older_versions_preserves_styles() {
        interact(() -> {
            StyledDocument<String, String, String> doc = area.subDocument(0, 8);
            ByteBuffer encoded;
            try {
                encoded = ClipboardHelper.encode(codec, doc);
            } catch (java.io.IOException e) {
                throw new AssertionError(e);
            }
            byte[] legacy = new byte[encoded.remaining()];
            encoded.get(legacy);

            ClipboardContent content = new ClipboardContent();
            content.putString("red bold");
            content.put(format, legacy); // what RichTextFX <= 0.11.7 did
            Clipboard.getSystemClipboard().setContent(content);

            area.moveTo(area.getLength());
            area.paste();
        });

        int start = "red bold plain".length();
        assertEquals("red bold plainred bold", area.getText());
        assertEquals(RED, area.getStyleOfChar(start));
        assertEquals(BOLD, area.getStyleOfChar(start + 4));
    }

    @Test
    public void paste_falls_back_to_plain_text_when_styled_content_is_corrupt() {
        interact(() -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(" fallback");
            content.put(format, ByteBuffer.wrap(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }));
            Clipboard.getSystemClipboard().setContent(content);

            area.moveTo(area.getLength());
            area.paste();
        });

        assertEquals("red bold plain fallback", area.getText());
    }
}
