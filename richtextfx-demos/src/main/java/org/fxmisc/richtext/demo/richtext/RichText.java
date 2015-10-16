/*
 * Created 2014 by Tomas Mikula.
 *
 * The author dedicates this file to the public domain.
 */

package org.fxmisc.richtext.demo.richtext;

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

import org.fxmisc.richtext.InlineStyleTextArea;
import org.fxmisc.richtext.Paragraph;
import org.fxmisc.richtext.StyleSpans;
import org.reactfx.SuspendableNo;

public class RichText extends Application {

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
