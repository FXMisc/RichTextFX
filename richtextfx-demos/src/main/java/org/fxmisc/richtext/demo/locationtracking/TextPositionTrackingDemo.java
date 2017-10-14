package org.fxmisc.richtext.demo.locationtracking;

import java.util.*;
import java.util.function.*;

import javafx.application.*;

import javafx.beans.property.*;

import javafx.geometry.*;

import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;

import javafx.stage.*;

import org.fxmisc.flowless.*;
import org.fxmisc.richtext.*;
import org.fxmisc.richtext.model.*;
import org.fxmisc.richtext.model.TwoDimensional.*;

import org.reactfx.util.*;

/**
 * Tracks positions within a GenericStyledArea and displays a visual indicator of the tracked positions in
 * a margin positioned to the left of the area.  The indicators are modelled using the class (below) indicator
 * which is a basic Text object.
 *
 * The class TextPosition is used to track changes within the area.  A subscription to the richChanges stream of the
 * area triggers updates to the tracked positions and then an update of the visual location of the margin and its Indicators.
 *
 * The area is wrapped in a VirtualizedScrollPane, changes to properties: estimatedScrollYProperty and boundsInLocalProperty
 * will trigger a layout request to the margin.  This is done on a future pulse via Platform.runLater to ensure that the
 * proerpties have the correct/ up-to-date value when the layout of the margin occurs (otherwise a lag can occur between the
 * viewport updating and the indicators moving to the correct positions).
 *
 * Add a new position to track by clicking on the area.  Right click to remove all the currently tracked positions.
 *
 * Method modelToView is named after its Swing TextComponent counterparts and performs a similar function, although
 * it returns screen coordinates.
 *
 * This demo requires GenericStyledArea.getRangeBoundsOnScreen to be made public so it can be used in the modelToView method.
 */
public class TextPositionTrackingDemo extends Application {
    private final TextOps<String, TextStyle>               styledTextOps = SegmentOps.styledTextOps();
    private GenericStyledArea<ParStyle, String, TextStyle> area          = null;
    private Margin                                         margin        = null;

    private Node createNode(StyledSegment<String, TextStyle> seg, BiConsumer<? super TextExt, TextStyle> applyStyle) {
        return StyledTextArea.createStyledTextNode(seg.getSegment(), seg.getStyle(), applyStyle);
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Get the bounds of a position in the area.  Is a wrapper call to modelToView(from, from + 1).
     *
     * @param from The from position.
     * @return An optional bounds, optional if the location(s) don't map to a valid location within the text.
     */
    public Optional<Bounds> modelToView(int from) {
        return this.modelToView(from, from + 1);
    }

    /**
     * Get the bounds of a position in the area.  The from/to may not line up with an actual character
     * in the text.  For example the area may have no text but it is still valuable to find the bounds of the
     * first character.
     *
     * New lines are treated as having a location, technically speaking they don't have a location but from a user's perspective
     * they do, they can click at the end of a line of text and expect a location to be there.
     *
     * This method is named after JTextComponent.modelToView where an offset into the text is mapped to a rectangle recpresenting the
     * bounds of the character in the view.
     *
     * This method is based GenericStyledArea.getCharacterBoundsOnScreen(int, int) but differs in its handling of new lines and "invalid"
     * positions such as 0, 0 when the text is empty.
     *
     * @param from The start location.
     * @param to The end location.
     * @return An optional bounds, optional if the location(s) don't map to a valid location within the text.
     */
    public Optional<Bounds> modelToView(int from, int to) {
        if (from < 0) {
            throw new IllegalArgumentException("From is negative: " + from);
        }

        if (from > to) {
            throw new IllegalArgumentException(String.format("From is greater than to. from=%s to=%s", from, to));
        }

        if (area.getLength() == 0) {
            if (from == 0) {

                // Special case, get the location of an assumed first character.
                return area.getRangeBoundsOnScreen(0, 0, 0);
            }
        } else {
            if (from >= area.getLength()) {

                // Special case, get the location of the last character, or very end of the text.
                Position pos = area.offsetToPosition(from, Bias.Forward);

                return area.getRangeBoundsOnScreen(pos.getMajor(), pos.getMinor(), pos.getMinor());
            }
        }

        if (area.getText(from, to).equals("\n")) {
            Position pos = area.offsetToPosition(from, Bias.Forward);

            return area.getRangeBoundsOnScreen(pos.getMajor(), pos.getMinor(), pos.getMinor());
        }

        // if 'from' is the newline character at the end of a multi-line paragraph, it returns a Bounds that whose
        // minX & minY are the minX and minY of the paragraph itself, not the newline character. So, ignore it.
        int      realFrom      = area.getText(from, from + 1).equals("\n")
                                 ? from + 1
                                 : from;
        Position startPosition = area.offsetToPosition(realFrom, Bias.Forward);
        int      startRow      = startPosition.getMajor();
        Position endPosition   = startPosition.offsetBy(to - realFrom, Bias.Forward);
        int      endRow        = endPosition.getMajor();

        if (startRow == endRow) {
            return area.getRangeBoundsOnScreen(startRow, startPosition.getMinor(), endPosition.getMinor());
        } else {
            Optional<Bounds> rangeBounds = area.getRangeBoundsOnScreen(startRow,
                                                                       startPosition.getMinor(),
                                                                       area.getParagraph(startRow).length());

            for (int i = startRow + 1; i <= endRow; i++) {
                Optional<Bounds> nextLineBounds = area.getRangeBoundsOnScreen(i,
                                                                              0,
                                                                              (i == endRow)
                                                                              ? endPosition.getMinor()
                                                                              : area.getParagraph(i).length());

                if (nextLineBounds.isPresent()) {
                    if (rangeBounds.isPresent()) {
                        Bounds lineBounds = nextLineBounds.get();

                        rangeBounds = rangeBounds.map(
                            b -> {
                                double minX = Math.min(b.getMinX(), lineBounds.getMinX());
                                double minY = Math.min(b.getMinY(), lineBounds.getMinY());
                                double maxX = Math.max(b.getMaxX(), lineBounds.getMaxX());
                                double maxY = Math.max(b.getMaxY(), lineBounds.getMaxY());

                                return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
                            } );
                    } else {
                        rangeBounds = nextLineBounds;
                    }
                }
            }

            return rangeBounds;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        this.area = new GenericStyledArea<>(ParStyle.EMPTY,    // default paragraph style
                                            (paragraph, style) -> paragraph.setStyle(style.toCss()),    // paragraph style setter
                                            TextStyle.EMPTY.updateFontSize(12).updateFontFamily(
                                                "Serif").updateTextColor(Color.BLACK),    // default segment style
                                            styledTextOps,    // segment operations
                                            (seg) -> createNode(seg, (text, style) -> text.setStyle(style.toCss())));    // Node creator and segment style setter
        area.setWrapText(true);
        area.setOnMouseReleased(
            ev -> {
                if (ev.isPopupTrigger()) {
                    this.margin.removeAllItems();

                    return;
                }

                int h = this.area.hit(ev.getX(), ev.getY()).getInsertionIndex();

                if (area.getLength() == 0) {
                    h = 0;
                } else {
                    if (h >= area.getLength()) {
                        h = area.getLength();
                    }
                }

                this.margin.addItem(new Indicator(h));
            } );
        this.margin = new Margin(area, this);
        this.margin.setStyle(
            "-fx-pref-width: 50; -fx-background-color: yellow; -fx-border-width: 1; -fx-border-color: green;");

        final VirtualizedScrollPane<GenericStyledArea<ParStyle, String, TextStyle>> vsPane =
            new VirtualizedScrollPane<>(area);

        vsPane.estimatedScrollYProperty().addListener((ev, oldv, newv) -> this.margin.updateLayout());
        vsPane.boundsInLocalProperty().addListener((ev, oldv, newv) -> this.margin.updateLayout());

        HBox main = new HBox(0);

        main.getChildren().addAll(this.margin, vsPane);
        HBox.setHgrow(vsPane, Priority.ALWAYS);

        Scene scene = new Scene(main, 600, 400);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Text Location Tracking");
        primaryStage.show();
    }
}
