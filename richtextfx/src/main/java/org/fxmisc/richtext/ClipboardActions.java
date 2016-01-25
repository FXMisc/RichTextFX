package org.fxmisc.richtext;

import static org.fxmisc.richtext.ClipboardHelper.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;

import javafx.scene.control.IndexRange;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;

import org.reactfx.util.Tuple2;

/**
 * Clipboard actions for {@link TextEditingArea}.
 */
public interface ClipboardActions<PS, S> extends EditActions<PS, S> {

    Optional<Tuple2<Codec<PS>, Codec<S>>> getStyleCodecs();

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
                Codec<StyledDocument<PS, S>> codec = ReadOnlyStyledDocument.codec(codecs._1, codecs._2);
                DataFormat format = dataFormat(codec.getName());
                StyledDocument<PS, S> doc = subDocument(selection.getStart(), selection.getEnd());
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
            Tuple2<Codec<PS>, Codec<S>> codecs = getStyleCodecs().get();
            Codec<StyledDocument<PS, S>> codec = ReadOnlyStyledDocument.codec(codecs._1, codecs._2);
            DataFormat format = dataFormat(codec.getName());
            if(clipboard.hasContent(format)) {
                byte[] bytes = (byte[]) clipboard.getContent(format);
                ByteArrayInputStream is = new ByteArrayInputStream(bytes);
                DataInputStream dis = new DataInputStream(is);
                StyledDocument<PS, S> doc = null;
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

        if (clipboard.hasString()) {
            String text = clipboard.getString();
            if (text != null) {
                replaceSelection(text);
            }
        }
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