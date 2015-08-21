/*
 * Created 2014 by Tomas Mikula.
 *
 * The author dedicates this file to the public domain.
 */

package org.fxmisc.richtext.demo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.IndexRange;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import org.fxmisc.richtext.Codec;
import org.fxmisc.richtext.InlineStyleTextArea;
import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.StyleSpan;
import org.fxmisc.richtext.StyleSpans;
import org.reactfx.SuspendableNo;

public class RichText extends Application {

    private static class StyleInfo {

        public static final StyleInfo EMPTY = new StyleInfo();

        public static final Codec<StyleInfo> CODEC = new Codec<StyleInfo>() {

            private final Codec<Color> COLOR_CODEC = new Codec<Color>() {

                @Override
                public String getName() {
                    return "color";
                }

                @Override
                public void encode(DataOutputStream os, Color c)
                        throws IOException {
                    os.writeDouble(c.getRed());
                    os.writeDouble(c.getGreen());
                    os.writeDouble(c.getBlue());
                    os.writeDouble(c.getOpacity());
                }

                @Override
                public Color decode(DataInputStream is) throws IOException {
                    return Color.color(
                            is.readDouble(),
                            is.readDouble(),
                            is.readDouble(),
                            is.readDouble());
                }

            };

            @Override
            public String getName() {
                return "style-info";
            }

            @Override
            public void encode(DataOutputStream os, StyleInfo s)
                    throws IOException {
                os.writeByte(encodeBoldItalicUnderlineStrikethrough(s));
                os.writeInt(encodeOptionalUint(s.fontSize));
                encodeOptional(os, s.fontFamily, Codec.STRING_CODEC);
                encodeOptional(os, s.textColor, COLOR_CODEC);
                encodeOptional(os, s.backgroundColor, COLOR_CODEC);
            }

            @Override
            public StyleInfo decode(DataInputStream is) throws IOException {
                byte bius = is.readByte();
                Optional<Integer> fontSize = decodeOptionalUint(is.readInt());
                Optional<String> fontFamily = decodeOptional(is, Codec.STRING_CODEC);
                Optional<Color> textColor = decodeOptional(is, COLOR_CODEC);
                Optional<Color> bgrColor = decodeOptional(is, COLOR_CODEC);
                return new StyleInfo(
                        bold(bius), italic(bius), underline(bius), strikethrough(bius),
                        fontSize, fontFamily, textColor, bgrColor);
            }

            private int encodeBoldItalicUnderlineStrikethrough(StyleInfo s) {
                return encodeOptionalBoolean(s.bold) << 6 |
                       encodeOptionalBoolean(s.italic) << 4 |
                       encodeOptionalBoolean(s.underline) << 2 |
                       encodeOptionalBoolean(s.strikethrough);
            }

            private Optional<Boolean> bold(byte bius) throws IOException {
                return decodeOptionalBoolean((bius >> 6) & 3);
            }

            private Optional<Boolean> italic(byte bius) throws IOException {
                return decodeOptionalBoolean((bius >> 4) & 3);
            }

            private Optional<Boolean> underline(byte bius) throws IOException {
                return decodeOptionalBoolean((bius >> 2) & 3);
            }

            private Optional<Boolean> strikethrough(byte bius) throws IOException {
                return decodeOptionalBoolean((bius >> 0) & 3);
            }

            private int encodeOptionalBoolean(Optional<Boolean> ob) {
                return ob.map(b -> 2 + (b ? 1 : 0)).orElse(0);
            }

            private Optional<Boolean> decodeOptionalBoolean(int i) throws IOException {
                switch(i) {
                    case 0: return Optional.empty();
                    case 2: return Optional.of(false);
                    case 3: return Optional.of(true);
                }
                throw new MalformedInputException(0);
            }

            private int encodeOptionalUint(Optional<Integer> oi) {
                return oi.orElse(-1);
            }

            private Optional<Integer> decodeOptionalUint(int i) {
                return (i < 0) ? Optional.empty() : Optional.of(i);
            }

            private <T> void encodeOptional(DataOutputStream os, Optional<T> ot, Codec<T> codec) throws IOException {
                if(ot.isPresent()) {
                    os.writeBoolean(true);
                    codec.encode(os, ot.get());
                } else {
                    os.writeBoolean(false);
                }
            }

            private <T> Optional<T> decodeOptional(DataInputStream is, Codec<T> codec) throws IOException {
                return is.readBoolean()
                        ? Optional.of(codec.decode(is))
                        : Optional.empty();
            }
        };

        public static StyleInfo fontSize(int fontSize) { return EMPTY.updateFontSize(fontSize); }
        public static StyleInfo fontFamily(String family) { return EMPTY.updateFontFamily(family); }
        public static StyleInfo textColor(Color color) { return EMPTY.updateTextColor(color); }
        public static StyleInfo backgroundColor(Color color) { return EMPTY.updateBackgroundColor(color); }

        private static String cssColor(Color color) {
            int red = (int) (color.getRed() * 255);
            int green = (int) (color.getGreen() * 255);
            int blue = (int) (color.getBlue() * 255);
            return "rgb(" + red + ", " + green + ", " + blue + ")";
        }

        final Optional<Boolean> bold;
        final Optional<Boolean> italic;
        final Optional<Boolean> underline;
        final Optional<Boolean> strikethrough;
        final Optional<Integer> fontSize;
        final Optional<String> fontFamily;
        final Optional<Color> textColor;
        final Optional<Color> backgroundColor;

        public StyleInfo() {
            this(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
            );
        }

        public StyleInfo(
                Optional<Boolean> bold,
                Optional<Boolean> italic,
                Optional<Boolean> underline,
                Optional<Boolean> strikethrough,
                Optional<Integer> fontSize,
                Optional<String> fontFamily,
                Optional<Color> textColor,
                Optional<Color> backgroundColor) {
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.strikethrough = strikethrough;
            this.fontSize = fontSize;
            this.fontFamily = fontFamily;
            this.textColor = textColor;
            this.backgroundColor = backgroundColor;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    bold, italic, underline, strikethrough,
                    fontSize, fontFamily, textColor, backgroundColor);
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof StyleInfo) {
                StyleInfo that = (StyleInfo) other;
                return Objects.equals(this.bold,            that.bold) &&
                       Objects.equals(this.italic,          that.italic) &&
                       Objects.equals(this.underline,       that.underline) &&
                       Objects.equals(this.strikethrough,   that.strikethrough) &&
                       Objects.equals(this.fontSize,        that.fontSize) &&
                       Objects.equals(this.fontFamily,      that.fontFamily) &&
                       Objects.equals(this.textColor,       that.textColor) &&
                       Objects.equals(this.backgroundColor, that.backgroundColor);
            } else {
                return false;
            }
        }

        public String toCss() {
            StringBuilder sb = new StringBuilder();

            if(bold.isPresent()) {
                if(bold.get()) {
                    sb.append("-fx-font-weight: bold;");
                } else {
                    sb.append("-fx-font-weight: normal;");
                }
            }

            if(italic.isPresent()) {
                if(italic.get()) {
                    sb.append("-fx-font-style: italic;");
                } else {
                    sb.append("-fx-font-style: normal;");
                }
            }

            if(underline.isPresent()) {
                if(underline.get()) {
                    sb.append("-fx-underline: true;");
                } else {
                    sb.append("-fx-underline: false;");
                }
            }

            if(strikethrough.isPresent()) {
                if(strikethrough.get()) {
                    sb.append("-fx-strikethrough: true;");
                } else {
                    sb.append("-fx-strikethrough: false;");
                }
            }

            if(fontSize.isPresent()) {
                sb.append("-fx-font-size: " + fontSize.get() + "pt;");
            }

            if(fontFamily.isPresent()) {
                sb.append("-fx-font-family: " + fontFamily.get() + ";");
            }

            if(textColor.isPresent()) {
                Color color = textColor.get();
                sb.append("-fx-fill: " + cssColor(color) + ";");
            }

            if(backgroundColor.isPresent()) {
                Color color = backgroundColor.get();
                sb.append("-fx-background-fill: " + cssColor(color) + ";");
            }

            return sb.toString();
        }

        public StyleInfo updateWith(StyleInfo mixin) {
            return new StyleInfo(
                    mixin.bold.isPresent() ? mixin.bold : bold,
                    mixin.italic.isPresent() ? mixin.italic : italic,
                    mixin.underline.isPresent() ? mixin.underline : underline,
                    mixin.strikethrough.isPresent() ? mixin.strikethrough : strikethrough,
                    mixin.fontSize.isPresent() ? mixin.fontSize : fontSize,
                    mixin.fontFamily.isPresent() ? mixin.fontFamily : fontFamily,
                    mixin.textColor.isPresent() ? mixin.textColor : textColor,
                    mixin.backgroundColor.isPresent() ? mixin.backgroundColor : backgroundColor);
        }

        public StyleInfo updateBold(boolean bold) {
            return new StyleInfo(Optional.of(bold), italic, underline, strikethrough, fontSize, fontFamily, textColor, backgroundColor);
        }

        public StyleInfo updateItalic(boolean italic) {
            return new StyleInfo(bold, Optional.of(italic), underline, strikethrough, fontSize, fontFamily, textColor, backgroundColor);
        }

        public StyleInfo updateUnderline(boolean underline) {
            return new StyleInfo(bold, italic, Optional.of(underline), strikethrough, fontSize, fontFamily, textColor, backgroundColor);
        }

        public StyleInfo updateStrikethrough(boolean strikethrough) {
            return new StyleInfo(bold, italic, underline, Optional.of(strikethrough), fontSize, fontFamily, textColor, backgroundColor);
        }

        public StyleInfo updateFontSize(int fontSize) {
            return new StyleInfo(bold, italic, underline, strikethrough, Optional.of(fontSize), fontFamily, textColor, backgroundColor);
        }

        public StyleInfo updateFontFamily(String fontFamily) {
            return new StyleInfo(bold, italic, underline, strikethrough, fontSize, Optional.of(fontFamily), textColor, backgroundColor);
        }

        public StyleInfo updateTextColor(Color textColor) {
            return new StyleInfo(bold, italic, underline, strikethrough, fontSize, fontFamily, Optional.of(textColor), backgroundColor);
        }

        public StyleInfo updateBackgroundColor(Color backgroundColor) {
            return new StyleInfo(bold, italic, underline, strikethrough, fontSize, fontFamily, textColor, Optional.of(backgroundColor));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private final InlineStyleTextArea<StyleInfo, StyleInfo> area =
            new InlineStyleTextArea<>(
                    StyleInfo.EMPTY.updateFontSize(12).updateFontFamily("Serif").updateTextColor(Color.BLACK),
                    StyleInfo::toCss,
                    StyleInfo.EMPTY,
                    StyleInfo::toCss);
    {
        area.setWrapText(true);
        area.setStyleCodec(StyleInfo.CODEC);
    }

    private final SuspendableNo updatingToolbar = new SuspendableNo();
    private final BooleanProperty applyToParagraph = new SimpleBooleanProperty(false);

    @Override
    public void start(Stage primaryStage) {
        CheckBox wrapToggle = new CheckBox("Wrap");
        wrapToggle.setSelected(true);
        area.wrapTextProperty().bind(wrapToggle.selectedProperty());
        Button undoBtn = createButton("undo", area::undo);
        Button redoBtn = createButton("redo", area::redo);
        Button cutBtn = createButton("cut", area::cut);
        Button copyBtn = createButton("copy", area::copy);
        Button pasteBtn = createButton("paste", area::paste);
        Button boldBtn = createButton("bold", this::toggleBold);
        Button italicBtn = createButton("italic", this::toggleItalic);
        Button underlineBtn = createButton("underline", this::toggleUnderline);
        Button strikeBtn = createButton("strikethrough", this::toggleStrikethrough);
        ComboBox<Integer> sizeCombo = new ComboBox<>(FXCollections.observableArrayList(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 28, 32, 36, 40, 48, 56, 64, 72));
        sizeCombo.getSelectionModel().select(Integer.valueOf(12));
        ComboBox<String> familyCombo = new ComboBox<>(FXCollections.observableList(Font.getFamilies()));
        familyCombo.getSelectionModel().select("Serif");
        ColorPicker textColorPicker = new ColorPicker(Color.BLACK);
        ColorPicker backgroundColorPicker = new ColorPicker();

        sizeCombo.setOnAction(evt -> updateFontSize(sizeCombo.getValue()));
        familyCombo.setOnAction(evt -> updateFontFamily(familyCombo.getValue()));
        textColorPicker.valueProperty().addListener((o, old, color) -> updateTextColor(color));
        backgroundColorPicker.valueProperty().addListener((o, old, color) -> updateBackgroundColor(color));

        undoBtn.disableProperty().bind(Bindings.not(area.undoAvailableProperty()));
        redoBtn.disableProperty().bind(Bindings.not(area.redoAvailableProperty()));

        BooleanBinding selectionEmpty = new BooleanBinding() {
            { bind(area.selectionProperty()); }

            @Override
            protected boolean computeValue() {
                return area.getSelection().getLength() == 0;
            }
        };

        cutBtn.disableProperty().bind(selectionEmpty);
        copyBtn.disableProperty().bind(selectionEmpty);

        area.beingUpdatedProperty().addListener((o, old, beingUpdated) -> {
            if(!beingUpdated) {
                boolean bold, italic, underline, strike;
                Integer fontSize;
                String fontFamily;
                Color textColor;
                Color backgroundColor;

                IndexRange selection = area.getSelection();
                if(selection.getLength() != 0) {
                    StyleSpans<StyleInfo> styles = area.getStyleSpans(selection);
                    bold = styles.styleStream().anyMatch(s -> s.bold.orElse(false));
                    italic = styles.styleStream().anyMatch(s -> s.italic.orElse(false));
                    underline = styles.styleStream().anyMatch(s -> s.underline.orElse(false));
                    strike = styles.styleStream().anyMatch(s -> s.strikethrough.orElse(false));
                    int[] sizes = styles.styleStream().mapToInt(s -> s.fontSize.orElse(-1)).distinct().toArray();
                    fontSize = sizes.length == 1 ? sizes[0] : -1;
                    String[] families = styles.styleStream().map(s -> s.fontFamily.orElse(null)).distinct().toArray(String[]::new);
                    fontFamily = families.length == 1 ? families[0] : null;
                    Color[] colors = styles.styleStream().map(s -> s.textColor.orElse(null)).distinct().toArray(Color[]::new);
                    textColor = colors.length == 1 ? colors[0] : null;
                    Color[] backgrounds = styles.styleStream().map(s -> s.backgroundColor.orElse(null)).distinct().toArray(i -> new Color[i]);
                    backgroundColor = backgrounds.length == 1 ? backgrounds[0] : null;
                } else {
                    int p = area.getCurrentParagraph();
                    int col = area.getCaretColumn();
                    StyleInfo style = area.getStyleAtPosition(p, col);
                    bold = style.bold.orElse(false);
                    italic = style.italic.orElse(false);
                    underline = style.underline.orElse(false);
                    strike = style.strikethrough.orElse(false);
                    fontSize = style.fontSize.orElse(-1);
                    fontFamily = style.fontFamily.orElse(null);
                    textColor = style.textColor.orElse(null);
                    backgroundColor = style.backgroundColor.orElse(null);
                }

                updatingToolbar.suspendWhile(() -> {
                    if(bold) {
                        if(!boldBtn.getStyleClass().contains("pressed")) {
                            boldBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        boldBtn.getStyleClass().remove("pressed");
                    }

                    if(italic) {
                        if(!italicBtn.getStyleClass().contains("pressed")) {
                            italicBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        italicBtn.getStyleClass().remove("pressed");
                    }

                    if(underline) {
                        if(!underlineBtn.getStyleClass().contains("pressed")) {
                            underlineBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        underlineBtn.getStyleClass().remove("pressed");
                    }

                    if(strike) {
                        if(!strikeBtn.getStyleClass().contains("pressed")) {
                            strikeBtn.getStyleClass().add("pressed");
                        }
                    } else {
                        strikeBtn.getStyleClass().remove("pressed");
                    }

                    if(fontSize != -1) {
                        sizeCombo.getSelectionModel().select(fontSize);
                    } else {
                        sizeCombo.getSelectionModel().clearSelection();
                    }

                    if(fontFamily != null) {
                        familyCombo.getSelectionModel().select(fontFamily);
                    } else {
                        familyCombo.getSelectionModel().clearSelection();
                    }

                    if(textColor != null) {
                        textColorPicker.setValue(textColor);
                    }

                    backgroundColorPicker.setValue(backgroundColor);
                });
            }
        });

        HBox panel1 = new HBox(3.0);
        HBox panel2 = new HBox(3.0);
        panel1.getChildren().addAll(wrapToggle, undoBtn, redoBtn, cutBtn, copyBtn, pasteBtn, boldBtn, italicBtn, underlineBtn, strikeBtn);
        panel2.getChildren().addAll(sizeCombo, familyCombo, textColorPicker, backgroundColorPicker);

        VBox vbox = new VBox();
        VBox.setVgrow(area, Priority.ALWAYS);
        vbox.getChildren().addAll(panel1, panel2, area);

        Scene scene = new Scene(vbox, 600, 400);
        scene.getStylesheets().add(RichText.class.getResource("rich-text.css").toExternalForm());
        primaryStage.setScene(scene);
        area.requestFocus();
        primaryStage.setTitle("Rich Text Demo");
        primaryStage.show();
    }

    private Button createButton(String styleClass, Runnable action) {
        Button button = new Button();
        button.getStyleClass().add(styleClass);
        button.setOnAction((evt) -> {
            action.run();
            area.requestFocus();
        });
        button.setPrefWidth(20);
        button.setPrefHeight(20);
        return button;
    }

    private void toggleBold() {
        if (!applyToParagraph.get()) {
            updateStyleInSelection(spans -> StyleInfo.EMPTY.updateBold(!spans.styleStream().allMatch(style -> style.bold.orElse(false))));
        } else {
            updateParagraphStyleInSelection(styleInfo -> styleInfo.updateBold(!styleInfo.bold.orElse(false)));
        }
    }

    private void toggleItalic() {
        if (!applyToParagraph.get()) {
            updateStyleInSelection(spans -> StyleInfo.EMPTY.updateItalic(!spans.styleStream().allMatch(style -> style.italic.orElse(false))));
        } else {
            updateParagraphStyleInSelection(styleInfo -> styleInfo.updateItalic(!styleInfo.italic.orElse(false)));
        }
    }

    private void toggleUnderline() {
        if (!applyToParagraph.get()) {
            updateStyleInSelection(spans -> StyleInfo.EMPTY.updateUnderline(!spans.styleStream().allMatch(style -> style.underline.orElse(false))));
        } else {
            updateParagraphStyleInSelection(styleInfo -> styleInfo.updateUnderline(!styleInfo.underline.orElse(false)));
        }
    }

    private void toggleStrikethrough() {
        if (!applyToParagraph.get()) {
            updateStyleInSelection(spans -> StyleInfo.EMPTY.updateStrikethrough(!spans.styleStream().allMatch(style -> style.strikethrough.orElse(false))));
        } else {
            updateParagraphStyleInSelection(styleInfo -> styleInfo.updateStrikethrough(!styleInfo.strikethrough.orElse(false)));
        }
    }

    private void updateStyleInSelection(Function<StyleSpans<StyleInfo>, StyleInfo> mixinGetter) {
        IndexRange selection = area.getSelection();
        if(selection.getLength() != 0) {
            StyleSpans<StyleInfo> styles = area.getStyleSpans(selection);
            StyleInfo mixin = mixinGetter.apply(styles);
            StyleSpans<StyleInfo> newStyles = styles.mapStyles(style -> style.updateWith(mixin));
            area.setStyleSpans(selection.getStart(), newStyles);
        }
    }

    private void updateStyleInSelection(StyleInfo mixin) {
        IndexRange selection = area.getSelection();
        if (selection.getLength() != 0) {
            StyleSpans<StyleInfo> styles = area.getStyleSpans(selection);
            StyleSpans<StyleInfo> newStyles = styles.mapStyles(style -> style.updateWith(mixin));
            area.setStyleSpans(selection.getStart(), newStyles);
        }
    }

    private void updateParagraphStyleInSelection(Function<StyleInfo, StyleInfo> updater) {
        Paragraph<StyleInfo, StyleInfo> paragraph = area.getParagraph(area.getCurrentParagraph());
        paragraph.setParagraphStyle(updater.apply(paragraph.getParagraphStyle()));
    }

    private void updateFontSize(Integer size) {
        if(!updatingToolbar.get()) {
            if (!applyToParagraph.get()) {
                updateStyleInSelection(StyleInfo.fontSize(size));
            } else {
                updateParagraphStyleInSelection(styleInfo -> styleInfo.updateFontSize(size));
            }
        }
    }

    private void updateFontFamily(String family) {
        if(!updatingToolbar.get()) {
            if (!applyToParagraph.get()) {
                updateStyleInSelection(StyleInfo.fontFamily(family));
            } else {
                updateParagraphStyleInSelection(styleInfo -> styleInfo.updateFontFamily(family));
            }
        }
    }

    private void updateTextColor(Color color) {
        if(!updatingToolbar.get()) {
            if (!applyToParagraph.get()) {
                updateStyleInSelection(StyleInfo.textColor(color));
            } else {
                updateParagraphStyleInSelection(styleInfo -> styleInfo.updateTextColor(color));
            }
        }
    }

    private void updateBackgroundColor(Color color) {
        if(!updatingToolbar.get()) {
            if (!applyToParagraph.get()) {
                updateStyleInSelection(StyleInfo.backgroundColor(color));
            } else {
                updateParagraphStyleInSelection(styleInfo -> styleInfo.updateBackgroundColor(color));
            }
        }
    }
}
