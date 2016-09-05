/*
 * Created 2014 by Tomas Mikula.
 *
 * The author dedicates this file to the public domain.
 */

package org.fxmisc.richtext.demo.richtext;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.model.CustomObject;
import org.fxmisc.richtext.model.ObjectData;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.SegmentType;
import org.fxmisc.richtext.model.DefaultSegmentTypes;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyledDocument;
import org.reactfx.SuspendableNo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class RichText extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private final StyledTextArea<ParStyle, TextStyle> area = new StyledTextArea<>(
                    ParStyle.EMPTY, ( paragraph, style) -> paragraph.setStyle(style.toCss()),
                    TextStyle.EMPTY.updateFontSize(12).updateFontFamily("Serif").updateTextColor(Color.BLACK),
                    ( text, style) -> text.setStyle(style.toCss()));
    {
        area.setWrapText(true);
        area.setStyleCodecs(ParStyle.CODEC, TextStyle.CODEC);
    }

    private Stage mainStage;

    private final SuspendableNo updatingToolbar = new SuspendableNo();

    @Override
    public void start(Stage primaryStage) {
        mainStage = primaryStage;

        Button loadBtn = createButton("loadfile", this::loadDocument);
        Button saveBtn = createButton("savefile", this::saveDocument);
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
        Button insertImageBtn = createButton("insertimage", this::insertImage, "Insert Image");
        Button insertTableBtn = createButton("inserttable", this::insertTable, "Insert Table");
        ToggleGroup alignmentGrp = new ToggleGroup();
        ToggleButton alignLeftBtn = createToggleButton(alignmentGrp, "align-left", this::alignLeft);
        ToggleButton alignCenterBtn = createToggleButton(alignmentGrp, "align-center", this::alignCenter);
        ToggleButton alignRightBtn = createToggleButton(alignmentGrp, "align-right", this::alignRight);
        ToggleButton alignJustifyBtn = createToggleButton(alignmentGrp, "align-justify", this::alignJustify);
        ColorPicker paragraphBackgroundPicker = new ColorPicker();
        ComboBox<Integer> sizeCombo = new ComboBox<>(FXCollections.observableArrayList(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 28, 32, 36, 40, 48, 56, 64, 72));
        sizeCombo.getSelectionModel().select(Integer.valueOf(12));
        ComboBox<String> familyCombo = new ComboBox<>(FXCollections.observableList(Font.getFamilies()));
        familyCombo.getSelectionModel().select("Serif");
        ColorPicker textColorPicker = new ColorPicker(Color.BLACK);
        ColorPicker backgroundColorPicker = new ColorPicker();

        paragraphBackgroundPicker.setTooltip(new Tooltip("Paragraph background"));
        textColorPicker.setTooltip(new Tooltip("Text color"));
        backgroundColorPicker.setTooltip(new Tooltip("Text background"));

        paragraphBackgroundPicker.valueProperty().addListener((o, old, color) -> updateParagraphBackground(color));
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
                    StyleSpans<TextStyle> styles = area.getStyleSpans(selection);
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
                    TextStyle style = area.getStyleAtPosition(p, col);
                    bold = style.bold.orElse(false);
                    italic = style.italic.orElse(false);
                    underline = style.underline.orElse(false);
                    strike = style.strikethrough.orElse(false);
                    fontSize = style.fontSize.orElse(-1);
                    fontFamily = style.fontFamily.orElse(null);
                    textColor = style.textColor.orElse(null);
                    backgroundColor = style.backgroundColor.orElse(null);
                }

                int startPar = area.offsetToPosition(selection.getStart(), Forward).getMajor();
                int endPar = area.offsetToPosition(selection.getEnd(), Backward).getMajor();
                List<Paragraph<ParStyle, TextStyle>> pars = area.getParagraphs().subList(startPar, endPar + 1);

                @SuppressWarnings("unchecked")
                Optional<TextAlignment>[] alignments = pars.stream().map(p -> p.getParagraphStyle().alignment).distinct().toArray(Optional[]::new);
                Optional<TextAlignment> alignment = alignments.length == 1 ? alignments[0] : Optional.empty();

                @SuppressWarnings("unchecked")
                Optional<Color>[] paragraphBackgrounds = pars.stream().map(p -> p.getParagraphStyle().backgroundColor).distinct().toArray(Optional[]::new);
                Optional<Color> paragraphBackground = paragraphBackgrounds.length == 1 ? paragraphBackgrounds[0] : Optional.empty();

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

                    if(alignment.isPresent()) {
                        TextAlignment al = alignment.get();
                        switch(al) {
                            case LEFT: alignmentGrp.selectToggle(alignLeftBtn); break;
                            case CENTER: alignmentGrp.selectToggle(alignCenterBtn); break;
                            case RIGHT: alignmentGrp.selectToggle(alignRightBtn); break;
                            case JUSTIFY: alignmentGrp.selectToggle(alignJustifyBtn); break;
                        }
                    } else {
                        alignmentGrp.selectToggle(null);
                    }

                    paragraphBackgroundPicker.setValue(paragraphBackground.orElse(null));

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
        panel1.getChildren().addAll(
                loadBtn, saveBtn,
                wrapToggle, undoBtn, redoBtn, cutBtn, copyBtn, pasteBtn,
                boldBtn, italicBtn, underlineBtn, strikeBtn,
                alignLeftBtn, alignCenterBtn, alignRightBtn, alignJustifyBtn, insertImageBtn, insertTableBtn,
                paragraphBackgroundPicker);
        panel2.getChildren().addAll(sizeCombo, familyCombo, textColorPicker, backgroundColorPicker);

        VirtualizedScrollPane<StyledTextArea<ParStyle, TextStyle>> vsPane = new VirtualizedScrollPane<>(area);
        VBox vbox = new VBox();
        VBox.setVgrow(vsPane, Priority.ALWAYS);
        vbox.getChildren().addAll(panel1, panel2, vsPane);

        Scene scene = new Scene(vbox, 600, 400);
        scene.getStylesheets().add(RichText.class.getResource("rich-text.css").toExternalForm());
        primaryStage.setScene(scene);
        area.requestFocus();
        primaryStage.setTitle("Rich Text Demo");
        primaryStage.show();
    }

    @Deprecated
    private Button createButton(String styleClass, Runnable action) {
        return createButton(styleClass, action, styleClass);
    }

    private Button createButton(String styleClass, Runnable action, String toolTip) {
        Button button = new Button();
        button.getStyleClass().add(styleClass);
        button.setOnAction(evt -> {
            action.run();
            area.requestFocus();
        });
        button.setPrefWidth(20);
        button.setPrefHeight(20);
        button.setTooltip(new Tooltip(toolTip));
        return button;
    }

    private ToggleButton createToggleButton(ToggleGroup grp, String styleClass, Runnable action) {
        ToggleButton button = new ToggleButton();
        button.setToggleGroup(grp);
        button.getStyleClass().add(styleClass);
        button.setOnAction(evt -> {
            action.run();
            area.requestFocus();
        });
        button.setPrefWidth(20);
        button.setPrefHeight(20);
        return button;
    }

    private void toggleBold() {
        updateStyleInSelection(spans -> TextStyle.bold(!spans.styleStream().allMatch(style -> style.bold.orElse(false))));
    }

    private void toggleItalic() {
        updateStyleInSelection(spans -> TextStyle.italic(!spans.styleStream().allMatch(style -> style.italic.orElse(false))));
    }

    private void toggleUnderline() {
        updateStyleInSelection(spans -> TextStyle.underline(!spans.styleStream().allMatch(style -> style.underline.orElse(false))));
    }

    private void toggleStrikethrough() {
        updateStyleInSelection(spans -> TextStyle.strikethrough(!spans.styleStream().allMatch(style -> style.strikethrough.orElse(false))));
    }

    private void alignLeft() {
        updateParagraphStyleInSelection(ParStyle.alignLeft());
    }

    private void alignCenter() {
        updateParagraphStyleInSelection(ParStyle.alignCenter());
    }

    private void alignRight() {
        updateParagraphStyleInSelection(ParStyle.alignRight());
    }

    private void alignJustify() {
        updateParagraphStyleInSelection(ParStyle.alignJustify());
    }

    private void loadDocument() {
        String initialDir = System.getProperty("user.dir");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load document");
        fileChooser.setInitialDirectory(new File(initialDir));
        File selectedFile = fileChooser.showOpenDialog(mainStage);
        if (selectedFile != null) {
            area.clear();
            
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(selectedFile);

                doc.getDocumentElement().normalize();
                System.out.print("Root element: ");
                System.out.println(doc.getDocumentElement().getNodeName());

                NodeList paragraphs = doc.getElementsByTagName("Paragraph");
                for (int idx = 0;  idx < paragraphs.getLength();  idx++) {
                    Element paragraph = (Element) paragraphs.item(idx);
                    String parStyle = paragraph.getAttribute("style");
                    ParStyle paraStyle = ParStyle.fromCss(parStyle);
                    System.err.println("Paragraph: [" + paraStyle + "]");

                    NodeList segments = paragraph.getChildNodes();
                    for (int segIdx = 0;  segIdx < segments.getLength();  segIdx++) {
                        Node node = segments.item(segIdx);
                        switch(node.getNodeName()) {
                            case "StyledText" : 
                                Element styledText = (Element) node;
                                String textStyle = styledText.getAttribute("style");
                                String text = styledText.getTextContent();

                                TextStyle style = TextStyle.fromCss(textStyle);
                                // System.err.println("   StyledText: [" + style + "], \"" + text + "\"");
                                ReadOnlyStyledDocument<ParStyle, TextStyle> ros = ReadOnlyStyledDocument.fromString(text, paraStyle, style);
                                System.err.println("   StyledDocument: " + ros);
                                area.append(ros);
                                break;

                            case "InlineImage" : 
                                Element customObject = (Element) node;
                                String fileName = customObject.getAttribute("fileName");
                                ObjectData data = new ObjectData(DefaultSegmentTypes.INLINE_IMAGE, fileName);
                                ReadOnlyStyledDocument<ParStyle, TextStyle> cos = ReadOnlyStyledDocument.createObject(data, paraStyle, new TextStyle());
                                System.err.println("   CustomObject: " + cos);
                                area.append(cos);
                                break;

                            default: 
                                break;
                        }
                    }

                    ReadOnlyStyledDocument<ParStyle, TextStyle> ros = ReadOnlyStyledDocument.fromString("\n", paraStyle, new TextStyle());
                    area.append(ros);
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void saveDocument() {
        String initialDir = System.getProperty("user.dir");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save document");
        fileChooser.setInitialDirectory(new File(initialDir));
        File selectedFile = fileChooser.showSaveDialog(mainStage);
        if (selectedFile != null) {
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder;
                dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.newDocument();
                Element rootElement = doc.createElement("StyledDocument");
                doc.appendChild(rootElement);

                StyledDocument<ParStyle, TextStyle> richDocument = area.getDocument();
                richDocument.getParagraphs().forEach(par -> {
                    Element parElement = doc.createElement("Paragraph");
                    parElement.setAttribute("style", par.getParagraphStyle().toCss());
                    rootElement.appendChild(parElement);

                    par.getSegments().forEach(seg -> {
                        Element segElement;
                        SegmentType segType = seg.getTypeId();
                        segElement = doc.createElement(segType.getName());
                        if (segType == DefaultSegmentTypes.STYLED_TEXT) {
                            segElement.setAttribute("style", seg.getStyle().toCss());
                            Node textNode = doc.createTextNode(seg.getText());
                            segElement.appendChild(textNode);
                        } else {
                            // TODO: Generic data - not just a file name!
                            ObjectData data = ((CustomObject<TextStyle>) seg).getObjectData();
                            String fileName = data.getData();
                            segElement.setAttribute("fileName", fileName);
                        }
                        parElement.appendChild(segElement);
                    });
                });

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);

                StreamResult consoleResult = new StreamResult(selectedFile);
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transformer.transform(source, consoleResult);
            } catch (ParserConfigurationException | TransformerException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Action listener which inserts a new image at the current caret position.
     */
    private void insertImage() {
        String initialDir = System.getProperty("user.dir");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Insert image");
        fileChooser.setInitialDirectory(new File(initialDir));
        File selectedFile = fileChooser.showOpenDialog(mainStage);
        if (selectedFile != null) {

            ReadOnlyStyledDocument<ParStyle, TextStyle> ros = null;
            try {
                String imageUrl = selectedFile.toURI().toURL().toExternalForm();
                ros = ReadOnlyStyledDocument.createObject(new ObjectData(DefaultSegmentTypes.INLINE_IMAGE, imageUrl), 
                                                          ParStyle.EMPTY, TextStyle.EMPTY); 
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            area.replaceSelection(ros);
        }
    }


    private void insertTable() {
        System.err.println("Inserting table ...");
        ReadOnlyStyledDocument<ParStyle, TextStyle> ros = 
                    ReadOnlyStyledDocument.createObject(new ObjectData(DefaultSegmentTypes.INLINE_TABLE, null), 
                    ParStyle.EMPTY, TextStyle.EMPTY);
        area.replaceSelection(ros);
    }


    private void updateStyleInSelection(Function<StyleSpans<TextStyle>, TextStyle> mixinGetter) {
        IndexRange selection = area.getSelection();
        if(selection.getLength() != 0) {
            StyleSpans<TextStyle> styles = area.getStyleSpans(selection);
            TextStyle mixin = mixinGetter.apply(styles);
            StyleSpans<TextStyle> newStyles = styles.mapStyles(style -> style.updateWith(mixin));
            area.setStyleSpans(selection.getStart(), newStyles);
        }
    }

    private void updateStyleInSelection(TextStyle mixin) {
        IndexRange selection = area.getSelection();
        if (selection.getLength() != 0) {
            StyleSpans<TextStyle> styles = area.getStyleSpans(selection);
            StyleSpans<TextStyle> newStyles = styles.mapStyles(style -> style.updateWith(mixin));
            area.setStyleSpans(selection.getStart(), newStyles);
        }
    }

    private void updateParagraphStyleInSelection(Function<ParStyle, ParStyle> updater) {
        IndexRange selection = area.getSelection();
        int startPar = area.offsetToPosition(selection.getStart(), Forward).getMajor();
        int endPar = area.offsetToPosition(selection.getEnd(), Backward).getMajor();
        for(int i = startPar; i <= endPar; ++i) {
            Paragraph<ParStyle, TextStyle> paragraph = area.getParagraph(i);
            area.setParagraphStyle(i, updater.apply(paragraph.getParagraphStyle()));
        }
    }

    private void updateParagraphStyleInSelection(ParStyle mixin) {
        updateParagraphStyleInSelection(style -> style.updateWith(mixin));
    }

    private void updateFontSize(Integer size) {
        if(!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.fontSize(size));
        }
    }

    private void updateFontFamily(String family) {
        if(!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.fontFamily(family));
        }
    }

    private void updateTextColor(Color color) {
        if(!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.textColor(color));
        }
    }

    private void updateBackgroundColor(Color color) {
        if(!updatingToolbar.get()) {
            updateStyleInSelection(TextStyle.backgroundColor(color));
        }
    }

    private void updateParagraphBackground(Color color) {
        if(!updatingToolbar.get()) {
            updateParagraphStyleInSelection(ParStyle.backgroundColor(color));
        }
    }
}
