/*
 * Created 2014 by Tomas Mikula.
 *
 * The author dedicates this file to the public domain.
 */

package org.fxmisc.richtext.demo.richtext;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import javafx.application.Application;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.TextExt;
import org.fxmisc.richtext.model.Codec;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.SegmentOps;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyledDocument;
import org.fxmisc.richtext.model.StyledSegment;
import org.fxmisc.richtext.model.TextOps;
import org.reactfx.SuspendableNo;
import org.reactfx.util.Either;
import org.reactfx.util.Tuple2;

public class RichTextDemo extends Application {

    // the saved/loaded files and their format are arbitrary and may change across versions
    private static final String RTFX_FILE_EXTENSION = ".rtfx";

    public static void main(String[] args) {
        // The following properties are required on Linux for improved text rendering
        //System.setProperty("prism.lcdtext", "false");
        //System.setProperty("prism.text", "t2k");
        launch(args);
    }

    private final FoldableStyledArea area = new FoldableStyledArea();

    static class FoldableStyledArea extends GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle>
    {
        private final static TextOps<String, TextStyle> styledTextOps = SegmentOps.styledTextOps();
        private final static LinkedImageOps<TextStyle> linkedImageOps = new LinkedImageOps<>();

        public FoldableStyledArea()
        {
            super(
                ParStyle.EMPTY,                                                 // default paragraph style
                (paragraph, style) -> paragraph.setStyle(style.toCss()),        // paragraph style setter
                TextStyle.EMPTY.updateFontSize(12).updateFontFamily("Serif").updateTextColor(Color.BLACK),  // default segment style
                styledTextOps._or(linkedImageOps, (s1, s2) -> Optional.empty()),                            // segment operations
                seg -> createNode(seg, (text, style) -> text.setStyle(style.toCss())));                     // Node creator and segment style setter
        }

        private static Node createNode( StyledSegment<Either<String, LinkedImage>, TextStyle> seg,
                                        BiConsumer<? super TextExt, TextStyle> applyStyle ) {
            return seg.getSegment().unify(
                    text -> StyledTextArea.createStyledTextNode(text, seg.getStyle(), applyStyle),
                    LinkedImage::createNode
            );
        }

        public void foldParagraphs( int startPar, int endPar ) {
            foldParagraphs( startPar, endPar, getAddFoldStyle() );
        }

        public void foldSelectedParagraphs() {
            foldSelectedParagraphs( getAddFoldStyle() );
        }

        public void foldText( int start, int end ) {
            fold( start, end, getAddFoldStyle() );
        }

        public void unfoldParagraphs( int startingFromPar ) {
            unfoldParagraphs( startingFromPar, getFoldStyleCheck(), getRemoveFoldStyle() );
        }

        public void unfoldText( int startingFromPos ) {
            startingFromPos = offsetToPosition( startingFromPos, Bias.Backward ).getMajor();
            unfoldParagraphs( startingFromPos, getFoldStyleCheck(), getRemoveFoldStyle() );
        }

        protected UnaryOperator<ParStyle> getAddFoldStyle() {
            return pstyle -> pstyle.updateFold( true );
        }

        protected UnaryOperator<ParStyle> getRemoveFoldStyle() {
            return pstyle -> pstyle.updateFold( false );
        }

        protected Predicate<ParStyle> getFoldStyleCheck() {
            return pstyle -> pstyle.isFolded();
        }
    }

    private class DefaultContextMenu extends ContextMenu
    {
        private MenuItem fold, unfold;

        public DefaultContextMenu()
        {
            fold = new MenuItem( "Fold selected text" );
            fold.setOnAction( AE -> { hide(); fold(); } );

            unfold = new MenuItem( "Unfold from cursor" );
            unfold.setOnAction( AE -> { hide(); unfold(); } );

            getItems().addAll( fold, unfold );
        }

        /**
         * Folds multiple lines of selected text, only showing the first line and hiding the rest.
         */
        private void fold()
        {
            ((FoldableStyledArea) getOwnerNode()).foldSelectedParagraphs();
        }

        /**
         * Unfold the CURRENT line/paragraph if it has a fold.
         */
        private void unfold()
        {
            FoldableStyledArea area = (FoldableStyledArea) getOwnerNode();
            area.unfoldParagraphs( area.getCurrentParagraph() );
        }
    }

    {
        area.setWrapText(true);
        area.setStyleCodecs(
                ParStyle.CODEC,
                Codec.styledSegmentCodec(Codec.eitherCodec(Codec.STRING_CODEC, LinkedImage.codec()), TextStyle.CODEC));
        area.setParagraphGraphicFactory( new BulletFactory( area ) );  // and folded paragraph indicator
        area.setContextMenu( new DefaultContextMenu() );
    }

    private Stage mainStage;

    private final SuspendableNo updatingToolbar = new SuspendableNo();

    @Override
    public void start(Stage primaryStage) {
        mainStage = primaryStage;

        Button loadBtn = createButton("loadfile", this::loadDocument,
                "Load document.\n\n" +
                        "Note: the demo will load only previously-saved \"" + RTFX_FILE_EXTENSION + "\" files. " +
                        "This file format is abitrary and may change across versions.");
        Button saveBtn = createButton("savefile", this::saveDocument,
                "Save document.\n\n" +
                        "Note: the demo will save the area's content to a \"" + RTFX_FILE_EXTENSION + "\" file. " +
                        "This file format is abitrary and may change across versions.");
        CheckBox wrapToggle = new CheckBox("Wrap");
        wrapToggle.setSelected(true);
        area.wrapTextProperty().bind(wrapToggle.selectedProperty());
        Button undoBtn = createButton("undo", area::undo, "Undo");
        Button redoBtn = createButton("redo", area::redo, "Redo");
        Button cutBtn = createButton("cut", area::cut, "Cut");
        Button copyBtn = createButton("copy", area::copy, "Copy");
        Button pasteBtn = createButton("paste", area::paste, "Paste");
        Button boldBtn = createButton("bold", this::toggleBold, "Bold");
        Button italicBtn = createButton("italic", this::toggleItalic, "Italic");
        Button underlineBtn = createButton("underline", this::toggleUnderline, "Underline");
        Button strikeBtn = createButton("strikethrough", this::toggleStrikethrough, "Strike Trough");
        Button insertImageBtn = createButton("insertimage", this::insertImage, "Insert Image");
        Button increaseIndentBtn = createButton("increaseIndent", this::increaseIndent, "Increase indent");
        Button decreaseIndentBtn = createButton("decreaseIndent", this::decreaseIndent, "Decrease indent");
        ToggleGroup alignmentGrp = new ToggleGroup();
        ToggleButton alignLeftBtn = createToggleButton(alignmentGrp, "align-left", this::alignLeft, "Align left");
        ToggleButton alignCenterBtn = createToggleButton(alignmentGrp, "align-center", this::alignCenter, "Align center");
        ToggleButton alignRightBtn = createToggleButton(alignmentGrp, "align-right", this::alignRight, "Align right");
        ToggleButton alignJustifyBtn = createToggleButton(alignmentGrp, "align-justify", this::alignJustify, "Justify");
        ColorPicker paragraphBackgroundPicker = new ColorPicker();
        ComboBox<Integer> sizeCombo = new ComboBox<>(FXCollections.observableArrayList(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 28, 32, 36, 40, 48, 56, 64, 72));
        sizeCombo.getSelectionModel().select(Integer.valueOf(12));
        sizeCombo.setTooltip(new Tooltip("Font size"));
        ComboBox<String> familyCombo = new ComboBox<>(FXCollections.observableList(Font.getFamilies()));
        familyCombo.getSelectionModel().select("Serif");
        familyCombo.setTooltip(new Tooltip("Font family"));
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

        undoBtn.disableProperty().bind(area.undoAvailableProperty().map(x -> !x));
        redoBtn.disableProperty().bind(area.redoAvailableProperty().map(x -> !x));

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
                List<Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle>> pars = area.getParagraphs().subList(startPar, endPar + 1);

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

        ToolBar toolBar1 = new ToolBar(
                loadBtn, saveBtn,  new Separator(Orientation.VERTICAL),
                wrapToggle, new Separator(Orientation.VERTICAL),
                undoBtn, redoBtn, new Separator(Orientation.VERTICAL),
                cutBtn, copyBtn, pasteBtn, new Separator(Orientation.VERTICAL),
                boldBtn, italicBtn, underlineBtn, strikeBtn, new Separator(Orientation.VERTICAL),
                alignLeftBtn, alignCenterBtn, alignRightBtn, alignJustifyBtn, new Separator(Orientation.VERTICAL),
                increaseIndentBtn, decreaseIndentBtn, new Separator(Orientation.VERTICAL),
                insertImageBtn, new Separator(Orientation.VERTICAL),
                paragraphBackgroundPicker);
        
        ToolBar toolBar2 = new ToolBar(sizeCombo, familyCombo, textColorPicker, backgroundColorPicker);

        VirtualizedScrollPane<GenericStyledArea<ParStyle, Either<String, LinkedImage>, TextStyle>> vsPane = new VirtualizedScrollPane<>(area);
        VBox vbox = new VBox();
        VBox.setVgrow(vsPane, Priority.ALWAYS);
        vbox.getChildren().addAll(toolBar1, toolBar2, vsPane);

        Scene scene = new Scene(vbox, 600, 400);
        scene.getStylesheets().add(RichTextDemo.class.getResource("rich-text.css").toExternalForm());
        primaryStage.setScene(scene);
        area.requestFocus();
        primaryStage.setTitle("Rich Text Demo");
        primaryStage.show();
    }


    private Button createButton(String styleClass, Runnable action, String toolTip) {
        Button button = new Button();
        button.getStyleClass().add(styleClass);
        button.setOnAction(evt -> {
            action.run();
            area.requestFocus();
        });
        button.setPrefWidth(25);
        button.setPrefHeight(25);
        if (toolTip != null) {
            button.setTooltip(new Tooltip(toolTip));
        }
        return button;
    }

    private ToggleButton createToggleButton(ToggleGroup grp, String styleClass, Runnable action, String toolTip) {
        ToggleButton button = new ToggleButton();
        button.setToggleGroup(grp);
        button.getStyleClass().add(styleClass);
        button.setOnAction(evt -> {
            action.run();
            area.requestFocus();
        });
        button.setPrefWidth(25);
        button.setPrefHeight(25);
        if (toolTip != null) {
            button.setTooltip(new Tooltip(toolTip));
        }
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
        fileChooser.setSelectedExtensionFilter(
                new FileChooser.ExtensionFilter("Arbitrary RTFX file", "*" + RTFX_FILE_EXTENSION));
        File selectedFile = fileChooser.showOpenDialog(mainStage);
        if (selectedFile != null) {
            area.clear();
            load(selectedFile);
        }
    }

    private void load(File file) {
        if(area.getStyleCodecs().isPresent()) {
            Tuple2<Codec<ParStyle>, Codec<StyledSegment<Either<String, LinkedImage>, TextStyle>>> codecs = area.getStyleCodecs().get();
            Codec<StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle>>
                codec = ReadOnlyStyledDocument.codec(codecs._1, codecs._2, area.getSegOps());

            try {
                FileInputStream fis = new FileInputStream(file);
                DataInputStream dis = new DataInputStream(fis);
                StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle> doc = codec.decode(dis);
                fis.close();

                if(doc != null) {
                    area.replaceSelection(doc);
                    return;
                }
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
        fileChooser.setInitialFileName("example rtfx file" + RTFX_FILE_EXTENSION);
        File selectedFile = fileChooser.showSaveDialog(mainStage);
        if (selectedFile != null) {
            save(selectedFile);
        }
    }


    private void save(File file) {
        StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle> doc = area.getDocument();

        // Use the Codec to save the document in a binary format
        area.getStyleCodecs().ifPresent(codecs -> {
            Codec<StyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle>> codec =
                    ReadOnlyStyledDocument.codec(codecs._1, codecs._2, area.getSegOps());
            try {
                FileOutputStream fos = new FileOutputStream(file);
                DataOutputStream dos = new DataOutputStream(fos);
                codec.encode(dos, doc);
                fos.close();
            } catch (IOException fnfe) {
                fnfe.printStackTrace();
            }
        });
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
            String imagePath = selectedFile.getAbsolutePath();
            imagePath = imagePath.replace('\\',  '/');
            ReadOnlyStyledDocument<ParStyle, Either<String, LinkedImage>, TextStyle> ros =
                    ReadOnlyStyledDocument.fromSegment(Either.right(new RealLinkedImage(imagePath)),
                                                       ParStyle.EMPTY, TextStyle.EMPTY, area.getSegOps());
            area.replaceSelection(ros);
        }
    }

    private void increaseIndent() {
        updateParagraphStyleInSelection( ps -> ps.increaseIndent() );
    }

    private void decreaseIndent() {
        updateParagraphStyleInSelection( ps -> ps.decreaseIndent() );
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
            Paragraph<ParStyle, Either<String, LinkedImage>, TextStyle> paragraph = area.getParagraph(i);
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
