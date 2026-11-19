package org.fxmisc.richtext;

import static org.fxmisc.richtext.ClipboardHelper.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

import javafx.scene.control.IndexRange;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;

import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.StyledDocument;
import org.fxmisc.richtext.model.StyledSegment;
import org.reactfx.util.Tuple2;

/**
 * Clipboard actions for {@link TextEditingArea}.
 */
public interface ClipboardActions<PS, SEG, S> extends EditActions<PS, SEG, S> {

    /**
     * Gets codecs to encode/decode style information to/from binary format.
     * Providing codecs enables clipboard actions to retain the style information.
     */
    Optional<Tuple2<Codec<PS>, Codec<StyledSegment<SEG, S>>>> getStyleCodecs();
    void setStyleCodecs(Codec<PS> paragraphStyleCodec, Codec<StyledSegment<SEG, S>> textStyleCodec);

    SegmentOps<SEG, S> getSegOps();

    /**
     * Transfers the currently selected text to the clipboard,
     * removing the current selection.
     */
    default void cut() {
        copy();
        IndexRange selection = getSelection();
        deleteText(selection.getStart(), selection.getEnd());
    }

    /**
     * Transfers the currently selected text to the clipboard,
     * leaving the current selection.
     */
    default void copy() {
        IndexRange selection = getSelection();
        if(selection.getLength() > 0) {
            ClipboardContent content = new ClipboardContent();

            content.putString(getSelectedText());

            getStyleCodecs().ifPresent(codecs -> {
                Codec<StyledDocument<PS, SEG, S>> codec = ReadOnlyStyledDocument.codec(codecs._1, codecs._2, getSegOps());
                DataFormat format = dataFormat(codec.getName());
                StyledDocument<PS, SEG, S> doc = subDocument(selection.getStart(), selection.getEnd());
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(os);
                try {
                    codec.encode(dos, doc);
                    content.put(format, os.toByteArray());
                } catch (IOException e) {
                    System.err.println("Codec error: Exception in encoding '" + codec.getName() + "':");
                    e.printStackTrace();
                }
            });

            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    /**
     * Inserts the content from the clipboard into this text-editing area,
     * replacing the current selection. If there is no selection, the content
     * from the clipboard is inserted at the current caret position.
     */
    default void paste() {
        Clipboard clipboard = Clipboard.getSystemClipboard();

        if(getStyleCodecs().isPresent()) {
            Tuple2<Codec<PS>, Codec<StyledSegment<SEG, S>>> codecs = getStyleCodecs().get();
            Codec<StyledDocument<PS, SEG, S>> codec = ReadOnlyStyledDocument.codec(codecs._1, codecs._2, getSegOps());
            DataFormat format = dataFormat(codec.getName());
            if(clipboard.hasContent(format)) {
                byte[] bytes = extractBytes(clipboard.getContent(format));
                if (bytes != null) {
                    ByteArrayInputStream is = new ByteArrayInputStream(bytes);
                    DataInputStream dis = new DataInputStream(is);
                    StyledDocument<PS, SEG, S> doc = null;
                    try {
                        doc = codec.decode(dis);
                    } catch (IOException e) {
                        System.err.println("Codec error: Failed to decode '" + codec.getName() + "':");
                        e.printStackTrace();
                    }
                    if(doc != null) {
                        replaceSelection(doc);
                        return;
                    }
                }
            }
        }

        if (clipboard.hasString()) {
            String text = clipboard.getString();
            if (text != null) {
				text = removeEndingNullChars(text);
                replaceSelection(text);
            }
        }
    }
	
	/**
	 * Removes null characters that were appended to clipboard text on Windows, e.g. when copying from Microsoft Word.
	 */
	private String removeEndingNullChars(String text) {
		if (text.endsWith("\u0000")) {
			return text.replaceAll("\u0000+\\Z", "");
		}
		return text;
	}

    /**
     * Extracts the raw byte[] for a custom {@link DataFormat} from clipboard
     * content, which is not guaranteed to be a byte[].
     *
     * <p>On Linux (GTK toolkit), {@code Clipboard.getContent(DataFormat)} for a
     * custom format returns a {@link ByteBuffer} (even for content freshly put
     * by the same application instance) whose bytes are the
     * {@link java.io.ObjectOutputStream}-serialized form of the original
     * byte[], rather than the payload itself.</p>
     *
     * @return the byte[] payload, or null if the content type is unrecognized
     */
    private static byte[] extractBytes(Object content) {
        byte[] raw = null;
        if (content instanceof byte[] bytes) {
            raw = bytes;
        } else if (content instanceof ByteBuffer buffer) {
            ByteBuffer duplicate = buffer.duplicate();
            raw = new byte[duplicate.remaining()];
            duplicate.get(raw);
        }
        if (raw == null) {
            return null;
        }
        // Unwrap a java-serialized byte[] if the toolkit serialized the payload
        if (raw.length >= 4 && (raw[0] & 0xFF) == 0xAC && (raw[1] & 0xFF) == 0xED) {
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(raw))) {
                Object obj = ois.readObject();
                if (obj instanceof byte[] bytes) {
                    return bytes;
                }
            } catch (IOException | ClassNotFoundException | RuntimeException e) {
                // Not a serialized object after all: use the raw bytes as-is
            }
        }
        return raw;
    }
}

class ClipboardHelper {
    static DataFormat dataFormat(String name) {
        DataFormat format = DataFormat.lookupMimeType(name);
        if(format != null) {
            return format;
        } else {
            return new DataFormat(name);
        }
    }
}