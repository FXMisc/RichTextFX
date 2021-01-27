package org.fxmisc.richtext;

import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;

/**
 * Graphic factory that produces labels containing line numbers and a "+" to indicate folded paragraphs.
 * To customize appearance, use {@code .lineno} and {@code .fold-indicator} style classes in CSS stylesheets.
 */
public class LineNumberFactory<PS> implements IntFunction<Node> {

    private static final Insets DEFAULT_INSETS = new Insets(0.0, 5.0, 0.0, 5.0);
    private static final Paint DEFAULT_TEXT_FILL = Color.web("#666");
    private static final Font DEFAULT_FONT = Font.font("monospace", FontPosture.ITALIC, 13);
    private static final Font DEFAULT_FOLD_FONT = Font.font("monospace", FontWeight.BOLD, 13);
    private static final Background DEFAULT_BACKGROUND = new Background(new BackgroundFill(Color.web("#ddd"), null, null));

    public static IntFunction<Node> get(GenericStyledArea<?, ?, ?> area) {
        return get(area, digits -> "%1$" + digits + "s");
    }

    public static <PS> IntFunction<Node> get( GenericStyledArea<PS, ?, ?> area, IntFunction<String> format )
    {
        if ( area instanceof StyleClassedTextArea ) {
            StyleClassedTextArea classArea = (StyleClassedTextArea) area;
            return get( classArea, format, classArea.getFoldStyleCheck(), classArea.getRemoveFoldStyle() );
        }
        else if ( area instanceof InlineCssTextArea ) {
            InlineCssTextArea inlineArea = (InlineCssTextArea) area;
            return get( inlineArea, format, inlineArea.getFoldStyleCheck(), inlineArea.getRemoveFoldStyle() );
        }
        return get( area, format, null, null );
    }

    /**
     * Use this if you extended GenericStyledArea for your own text area and you're using paragraph folding.
     *
     * @param <PS> The paragraph style type being used by the text area
     * @param format Given an int convert to a String for the line number.
     * @param isFolded Given a paragraph style PS check if it's folded.
     * @param removeFoldStyle Given a paragraph style PS, return a <b>new</b> PS that excludes fold styling.
     */
    public static <PS> IntFunction<Node> get(
            GenericStyledArea<PS, ?, ?> area,
            IntFunction<String> format,
            Predicate<PS> isFolded,
            UnaryOperator<PS> removeFoldStyle )
    {
        return new LineNumberFactory<>( area, format, isFolded, removeFoldStyle );
    }

    private final Val<Integer> nParagraphs;
    private final IntFunction<String> format;
    private final GenericStyledArea<PS, ?, ?> area;
    private final UnaryOperator<PS> removeFoldStyle;
    private final Predicate<PS> isFoldedCheck;

    private LineNumberFactory(
            GenericStyledArea<PS, ?, ?> area,
            IntFunction<String> format,
            Predicate<PS> isFolded,
            UnaryOperator<PS> removeFoldStyle )
    {
        nParagraphs = LiveList.sizeOf(area.getParagraphs());
        this.removeFoldStyle = removeFoldStyle;
        this.isFoldedCheck = isFolded;
        this.format = format;
        this.area = area;

        if ( isFoldedCheck != null ) {
            area.getParagraphs().sizeProperty().addListener( (ob,ov,nv) -> {
                if ( nv <= ov ) Platform.runLater( () -> deleteParagraphCheck() );
                else  Platform.runLater( () -> insertParagraphCheck() );
            });
        }
    }

    @Override
    public Node apply(int idx) {
        Val<String> formatted = nParagraphs.map(n -> format(idx+1, n));

        Label lineNo = new Label();
        lineNo.setFont(DEFAULT_FONT);
        lineNo.setBackground(DEFAULT_BACKGROUND);
        lineNo.setTextFill(DEFAULT_TEXT_FILL);
        lineNo.setPadding(DEFAULT_INSETS);
        lineNo.setAlignment(Pos.TOP_RIGHT);
        lineNo.getStyleClass().add("lineno");

        // bind label's text to a Val that stops observing area's paragraphs
        // when lineNo is removed from scene
        lineNo.textProperty().bind(formatted.conditionOnShowing(lineNo));

        if ( isFoldedCheck != null )
        {
            Label foldIndicator = new Label( " " );
            foldIndicator.setTextFill( Color.BLUE ); // Prevents CSS errors
            foldIndicator.setFont( DEFAULT_FOLD_FONT );

            lineNo.setContentDisplay( ContentDisplay.RIGHT );
            lineNo.setGraphic( foldIndicator );

            if ( area.getParagraphs().size() > idx+1 ) {
                if ( isFoldedCheck.test( area.getParagraph( idx+1 ).getParagraphStyle() )
                && ! isFoldedCheck.test( area.getParagraph( idx ).getParagraphStyle() ) ) {
                    foldIndicator.setOnMouseClicked( ME -> area.unfoldParagraphs
                    (
                        idx, isFoldedCheck, removeFoldStyle
                    ));
                    foldIndicator.getStyleClass().add( "fold-indicator" );
                    foldIndicator.setCursor( Cursor.HAND );
                    foldIndicator.setText( "+" );
                }
            }
        }

        return lineNo;
    }

    private String format(int x, int max) {
        int digits = (int) Math.floor(Math.log10(max)) + 1;
        return String.format(format.apply(digits), x);
    }

    private void deleteParagraphCheck()
    {
        int p = area.getCurrentParagraph();
        // Was the deleted paragraph in the viewport ?
        if ( p >= area.firstVisibleParToAllParIndex() && p <= area.lastVisibleParToAllParIndex() )
        {
            int col = area.getCaretColumn();
            // Delete was pressed on an empty paragraph, and so the cursor is now at the start of the next paragraph.
            if ( col == 0 ) {
                // Check if the now current paragraph is folded.
                if ( isFoldedCheck.test( area.getParagraph( p ).getParagraphStyle() ) ) {
                    p = Math.max( p-1, 0 );              // Adjust to previous paragraph.
                    area.recreateParagraphGraphic( p );  // Show fold/unfold indicator on previous paragraph.
                    area.moveTo( p, 0 );                 // Move cursor to previous paragraph.
                }
            }
            // Backspace was pressed on an empty paragraph, and so the cursor is now at the end of the previous paragraph.
            else if ( col == area.getParagraph( p ).length() ) {
                area.recreateParagraphGraphic( p ); // Shows fold/unfold indicator on current paragraph if needed.
            }
            // In all other cases the paragraph graphic is created/updated automatically.
        }
    }

    private void insertParagraphCheck()
    {
        int p = area.getCurrentParagraph();
        // Is the inserted paragraph in the viewport ?
        if ( p > area.firstVisibleParToAllParIndex() && p <= area.lastVisibleParToAllParIndex() ) {
            // Check limits, as p-1 and p+1 are accessed ...
            if ( p > 0 && p+1 < area.getParagraphs().size() ) {
                // Now check if the inserted paragraph is before a folded block ?
                if ( isFoldedCheck.test( area.getParagraph( p+1 ).getParagraphStyle() ) ) {
                    area.recreateParagraphGraphic( p-1 ); // Remove the unfold indicator.
                }
            }
        }
    }

}
