/*
 * Created 2014 by Tomas Mikula.
 *
 * The author dedicates this file to the public domain.
 */

package org.fxmisc.richtext.demo;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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

import org.fxmisc.richtext.InlineStyleTextArea;
import org.fxmisc.richtext.StyleSpans;
import org.reactfx.SuspendableNo;

public class RichText extends Application {

    private static class StyleInfo {
        public static final StyleInfo EMPTY = new StyleInfo();
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
            bold = Optional.empty();
            italic = Optional.empty();
            underline = Optional.empty();
            strikethrough = Optional.empty();
            fontSize = Optional.empty();
            fontFamily = Optional.empty();
            textColor = Optional.empty();
            backgroundColor = Optional.empty();
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

    private final InlineStyleTextArea<StyleInfo> area =
            new InlineStyleTextArea<StyleInfo>(
                    StyleInfo.EMPTY.updateFontSize(12).updateFontFamily("Serif").updateTextColor(Color.BLACK),
                    style -> style.toCss());
    {
        area.setWrapText(true);
    }

    private final SuspendableNo updatingToolbar = new SuspendableNo();

    @Override
    public void start(Stage primaryStage) {
        CheckBox wrapToggle = new CheckBox("Wrap");
        wrapToggle.setSelected(true);
        area.wrapTextProperty().bind(wrapToggle.selectedProperty());
        Button undoBtn = createButton("undo", () -> area.undo());
        Button redoBtn = createButton("redo", () -> area.redo());
        Button cutBtn = createButton("cut", () -> area.cut());
        Button copyBtn = createButton("copy", () -> area.copy());
        Button pasteBtn = createButton("paste", () -> area.paste());
        Button boldBtn = createButton("bold", () -> toggleBold());
        Button italicBtn = createButton("italic", () -> toggleItalic());
        Button underlineBtn = createButton("underline", () -> toggleUnderline());
        Button strikeBtn = createButton("strikethrough", () -> toggleStrikethrough());
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
                    String[] families = styles.styleStream().map(s -> s.fontFamily.orElse(null)).distinct().toArray(i -> new String[i]);
                    fontFamily = families.length == 1 ? families[0] : null;
                    Color[] colors = styles.styleStream().map(s -> s.textColor.orElse(null)).distinct().toArray(i -> new Color[i]);
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
        updateStyleInSelection(spans -> StyleInfo.EMPTY.updateBold(!spans.styleStream().allMatch(style -> style.bold.orElse(false))));
    }

    private void toggleItalic() {
        updateStyleInSelection(spans -> StyleInfo.EMPTY.updateItalic(!spans.styleStream().allMatch(style -> style.italic.orElse(false))));
    }

    private void toggleUnderline() {
        updateStyleInSelection(spans -> StyleInfo.EMPTY.updateUnderline(!spans.styleStream().allMatch(style -> style.underline.orElse(false))));
    }

    private void toggleStrikethrough() {
        updateStyleInSelection(spans -> StyleInfo.EMPTY.updateStrikethrough(!spans.styleStream().allMatch(style -> style.strikethrough.orElse(false))));
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
        if(selection.getLength() != 0) {
            StyleSpans<StyleInfo> styles = area.getStyleSpans(selection);
            StyleSpans<StyleInfo> newStyles = styles.mapStyles(style -> style.updateWith(mixin));
            area.setStyleSpans(selection.getStart(), newStyles);
        }
    }

    private void updateFontSize(Integer size) {
        if(!updatingToolbar.get()) {
            updateStyleInSelection(StyleInfo.fontSize(size));
        }
    }

    private void updateFontFamily(String family) {
        if(!updatingToolbar.get()) {
            updateStyleInSelection(StyleInfo.fontFamily(family));
        }
    }

    private void updateTextColor(Color color) {
        if(!updatingToolbar.get()) {
            updateStyleInSelection(StyleInfo.textColor(color));
        }
    }

    private void updateBackgroundColor(Color color) {
        if(!updatingToolbar.get()) {
            updateStyleInSelection(StyleInfo.backgroundColor(color));
        }
    }
}
