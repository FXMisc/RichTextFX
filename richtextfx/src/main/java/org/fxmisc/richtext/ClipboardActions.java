package org.fxmisc.richtext;

import static org.fxmisc.richtext.ClipboardHelper.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
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
                try {
                    content.put(format, encode(codec, doc));
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
                StyledDocument<PS, SEG, S> doc = decode(codec, clipboard.getContent(format));
                if(doc != null) {
                    replaceSelection(doc);
                    return;
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

    /**
     * Encodes {@code value} for the clipboard.
     *
     * <p>The result is a {@link ByteBuffer} rather than a {@code byte[]} on purpose: JavaFX stores
     * {@link java.io.Serializable} clipboard content (such as a {@code byte[]}) in Java-serialized form,
     * and since JavaFX 17.0.21, 21.0.13, 25.0.5 and 27.0.1 it no longer deserializes it when reading, so the
     * reader would receive the serialized form instead of the payload. A {@code ByteBuffer} is not
     * {@code Serializable}, so JavaFX transfers its bytes as they are, on every JavaFX version and platform.</p>
     */
    static <T> ByteBuffer encode(Codec<T> codec, T value) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        codec.encode(new DataOutputStream(os), value);
        return ByteBuffer.wrap(os.toByteArray());
    }

    /**
     * Decodes clipboard content produced by {@link #encode(Codec, Object)} or by an earlier RichTextFX version.
     *
     * @return the decoded value, or {@code null} if the content could not be decoded, in which case
     *         the caller should fall back to plain text
     */
    static <T> T decode(Codec<T> codec, Object content) {
        byte[] bytes = extractBytes(content);
        if (bytes == null) {
            return null;
        }
        try {
            return codec.decode(new DataInputStream(new ByteArrayInputStream(bytes)));
        } catch (IOException | RuntimeException e) {
            // corrupt or foreign data can make a codec throw unchecked exceptions too,
            // e.g. a negative collection size
            System.err.println("Codec error: Failed to decode '" + codec.getName() + "':");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Extracts the codec payload from content returned by {@code Clipboard.getContent(DataFormat)}.
     * Depending on the JavaFX version and on which RichTextFX version copied it, this can be:
     * <ul>
     *     <li>a {@link ByteBuffer} holding the payload: copied by this version, on any JavaFX version;</li>
     *     <li>a {@code byte[]}: copied by RichTextFX 0.11.7 or earlier, read on a JavaFX version that still
     *     deserializes clipboard content;</li>
     *     <li>a {@link ByteBuffer} holding a Java-serialized {@code byte[]}: copied by RichTextFX 0.11.7 or
     *     earlier, read on JavaFX 17.0.21, 21.0.13, 25.0.5, 27.0.1 or later.</li>
     * </ul>
     *
     * @return the payload, or {@code null} if the content is not in a recognized form
     */
    static byte[] extractBytes(Object content) {
        if (content instanceof byte[]) {
            return (byte[]) content;
        }
        if (!(content instanceof ByteBuffer)) {
            return null;
        }
        ByteBuffer buffer = ((ByteBuffer) content).duplicate();
        byte[] raw = new byte[buffer.remaining()];
        buffer.get(raw);
        if (isJavaSerialized(raw)) {
            return deserializeByteArray(raw);
        }
        return raw;
    }

    /**
     * Whether {@code bytes} starts with the Java serialization stream header (0xACED 0x0005). A codec payload
     * cannot start like this: every RichTextFX document starts with its paragraph count as a 4-byte int,
     * and these 4 bytes would make it negative.
     */
    private static boolean isJavaSerialized(byte[] bytes) {
        return bytes.length >= 4
                && (bytes[0] & 0xFF) == 0xAC && (bytes[1] & 0xFF) == 0xED
                && bytes[2] == 0x00 && bytes[3] == 0x05;
    }

    /**
     * Deserializes a {@code byte[]} written by {@link java.io.ObjectOutputStream}, as JavaFX does for content
     * copied by RichTextFX 0.11.7 or earlier. Clipboard content can come from any process, so this accepts
     * exactly one {@code byte[]}, no larger than the stream itself, and rejects any other class before it is
     * loaded or instantiated. It is not the unrestricted deserialization that JavaFX removed.
     *
     * @return the array, or {@code null} if the stream holds anything else
     */
    private static byte[] deserializeByteArray(byte[] serialized) {
        final int maxLength = serialized.length;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
            ois.setObjectInputFilter(info -> {
                Class<?> c = info.serialClass();
                if ((c != null && c != byte[].class) || info.depth() > 1 || info.arrayLength() > maxLength) {
                    return ObjectInputFilter.Status.REJECTED;
                }
                return ObjectInputFilter.Status.ALLOWED;
            });
            Object o = ois.readObject();
            return o instanceof byte[] ? (byte[]) o : null;
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            return null;
        }
    }
}