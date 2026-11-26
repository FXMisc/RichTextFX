package org.fxmisc.richtext;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;

import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.StyledDocument;
import org.fxmisc.richtext.model.TextOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the encoding and decoding of styled documents for the clipboard, covering every shape
 * {@code Clipboard.getContent(DataFormat)} can return across JavaFX and RichTextFX versions.
 * These tests use no JavaFX toolkit: they feed the helper exactly what JavaFX would return.
 */
class ClipboardHelperTest {

    private static final TextOps<String, String> SEG_OPS = SegmentOps.styledTextOps();

    /** The codec InlineCssTextArea uses. */
    private static final Codec<StyledDocument<String, String, String>> CODEC = ReadOnlyStyledDocument.codec(
            Codec.STRING_CODEC, Codec.styledTextCodec(Codec.STRING_CODEC), SEG_OPS);

    private static final StyledDocument<String, String, String> DOC =
            ReadOnlyStyledDocument.fromString("Hello ", "-fx-par: 1;", "-fx-fill: red;", SEG_OPS)
                    .concat(ReadOnlyStyledDocument.fromString("world\nsecond line", "-fx-par: 2;",
                            "-fx-font-weight: bold;", SEG_OPS));

    // ------------------------------------------------------------------ helpers

    /** The codec payload, i.e. what RichTextFX 0.11.7 and earlier put on the clipboard as a byte[]. */
    private static byte[] payload() throws IOException {
        ByteBuffer buffer = ClipboardHelper.encode(CODEC, DOC);
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    /** What JavaFX does with Serializable clipboard content: ObjectOutputStream, wrapped in a ByteBuffer. */
    private static ByteBuffer javaFxSerialized(Object o) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(o);
        }
        return ByteBuffer.wrap(bos.toByteArray());
    }

    // ------------------------------------------------------------------ the shapes paste must accept

    @Test
    @DisplayName("encode produces a non-Serializable ByteBuffer, so JavaFX transfers it without serialization")
    void encodeProducesByteBuffer() throws IOException {
        ByteBuffer encoded = ClipboardHelper.encode(CODEC, DOC);
        // reflective check: ByteBuffer is sealed on newer JDKs, so javac rejects a plain instanceof here
        assertFalse(Serializable.class.isInstance(encoded));
    }

    @Test
    @DisplayName("ByteBuffer from encode (this version, any JavaFX) decodes to the original document")
    void roundTripThroughByteBuffer() throws IOException {
        assertEquals(DOC, ClipboardHelper.decode(CODEC, ClipboardHelper.encode(CODEC, DOC)));
    }

    @Test
    @DisplayName("byte[] (RichTextFX <= 0.11.7 on JavaFX that still deserializes) decodes")
    void legacyByteArray() throws IOException {
        assertEquals(DOC, ClipboardHelper.decode(CODEC, payload()));
    }

    @Test
    @DisplayName("serialized byte[] in a ByteBuffer (RichTextFX <= 0.11.7 on JavaFX 17.0.21/21.0.13/25.0.5/27.0.1+) decodes")
    void legacySerializedByteArray() throws IOException {
        assertEquals(DOC, ClipboardHelper.decode(CODEC, javaFxSerialized(payload())));
    }

    @Test
    @DisplayName("only the remaining bytes of a ByteBuffer are used, and the buffer's position is left alone")
    void respectsBufferPositionAndLimit() throws IOException {
        byte[] bytes = payload();
        byte[] padded = new byte[bytes.length + 7];
        System.arraycopy(bytes, 0, padded, 3, bytes.length);
        Arrays.fill(padded, 0, 3, (byte) 0x7F);
        Arrays.fill(padded, 3 + bytes.length, padded.length, (byte) 0x7F);
        ByteBuffer view = ByteBuffer.wrap(padded, 3, bytes.length);

        assertEquals(DOC, ClipboardHelper.decode(CODEC, view));
        assertEquals(3, view.position());
        assertEquals(DOC, ClipboardHelper.decode(CODEC, view)); // decodable again
    }

    // ------------------------------------------------------------------ fall back to plain text instead of throwing

    @Test
    @DisplayName("unrecognized content types are rejected")
    void unrecognizedContentType() {
        assertNull(ClipboardHelper.extractBytes(null));
        assertNull(ClipboardHelper.extractBytes("some text"));
        assertNull(ClipboardHelper.decode(CODEC, "some text"));
    }

    @Test
    @DisplayName("corrupt payload returns null instead of throwing (e.g. negative paragraph count)")
    void corruptPayload() {
        byte[] garbage = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 1, 2, 3 };
        assertNull(ClipboardHelper.decode(CODEC, ByteBuffer.wrap(garbage)));
        assertNull(ClipboardHelper.decode(CODEC, garbage));
    }

    @Test
    @DisplayName("truncated payload returns null instead of throwing")
    void truncatedPayload() throws IOException {
        byte[] bytes = payload();
        assertNull(ClipboardHelper.decode(CODEC, ByteBuffer.wrap(Arrays.copyOf(bytes, bytes.length / 2))));
    }

    @Test
    @DisplayName("truncated serialized byte[] is rejected")
    void truncatedSerializedByteArray() throws IOException {
        ByteBuffer serialized = javaFxSerialized(payload());
        byte[] truncated = Arrays.copyOf(serialized.array(), serialized.remaining() / 2);
        assertNull(ClipboardHelper.extractBytes(ByteBuffer.wrap(truncated)));
    }

    // ------------------------------------------------------------------ the deserialization filter

    @Test
    @DisplayName("serialized objects other than a byte[] are rejected")
    void otherSerializedObjectsAreRejected() throws IOException {
        assertNull(ClipboardHelper.extractBytes(javaFxSerialized("a string")));
        assertNull(ClipboardHelper.extractBytes(javaFxSerialized(new ArrayList<>(Arrays.asList(1, 2, 3)))));
        assertNull(ClipboardHelper.extractBytes(javaFxSerialized(new byte[][] { { 1, 2 }, { 3 } })));
        assertNull(ClipboardHelper.extractBytes(javaFxSerialized(new int[] { 1, 2, 3 })));
    }

    @Test
    @DisplayName("the filter rejects other classes before they are instantiated")
    void otherClassesAreNeverInstantiated() throws IOException {
        Canary.deserialized = false;
        assertNull(ClipboardHelper.extractBytes(javaFxSerialized(new Canary())));
        assertFalse(Canary.deserialized, "Canary.readObject must not run");
    }

    @Test
    @DisplayName("a forged huge array length is rejected without allocating it")
    void forgedArrayLengthIsRejected() throws IOException {
        byte[] stream = javaFxSerialized(new byte[4]).array();
        // a serialized byte[4] ends with its length (4-byte int) followed by its 4 elements
        int lengthIndex = stream.length - 4 - 4;
        assertEquals(4, ByteBuffer.wrap(stream, lengthIndex, 4).getInt());
        ByteBuffer.wrap(stream).putInt(lengthIndex, Integer.MAX_VALUE - 8);

        assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> assertNull(ClipboardHelper.extractBytes(ByteBuffer.wrap(stream))));
    }

    @Test
    @DisplayName("an unwrapped serialized byte[] is returned exactly")
    void serializedByteArrayIsUnwrappedExactly() throws IOException {
        byte[] all = new byte[256];
        for (int i = 0; i < all.length; i++) {
            all[i] = (byte) i;
        }
        assertArrayEquals(all, ClipboardHelper.extractBytes(javaFxSerialized(all)));
    }

    /** Records whether deserialization ever reached it. */
    static class Canary implements Serializable {
        private static final long serialVersionUID = 1L;
        static volatile boolean deserialized;

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            deserialized = true;
            in.defaultReadObject();
        }
    }
}
