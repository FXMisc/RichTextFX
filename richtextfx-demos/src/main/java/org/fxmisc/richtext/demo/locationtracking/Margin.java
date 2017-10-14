package org.fxmisc.richtext.demo.locationtracking;

import java.util.*;

import javafx.application.*;

import javafx.geometry.*;

import javafx.scene.layout.*;

import org.fxmisc.richtext.GenericStyledArea;

/**
 * A region that will hold all the text position indicators.
 * When the size of the viewport changes or the scroll y value changes then a layout is triggered to move the
 * child indicators as necessary.
 */
public class Margin extends Region {
    private List<Indicator>                                inds   = new ArrayList<>();
    private GenericStyledArea<ParStyle, String, TextStyle> area   = null;
    private TextPositionTrackingDemo                       parent = null;

    public Margin(GenericStyledArea<ParStyle, String, TextStyle> area, TextPositionTrackingDemo parent) {
        this.area   = area;
        this.parent = parent;

        // Listen for rich text changes, i.e. position movements.
        // This isn't the proper place for this, in an ideal world you would have this in
        // the TextPosition class or better still hold the positions in the StyledDocument itself.
        // This method prevents a plethora of listeners being created and causing numerous layout requests.
        this.area.richChanges().subscribe(event -> {
                if (this.inds.size() == 0) {
                    return;
                }

                // Update all relevant positions.
                this.inds.stream()
                         .filter(
                             i -> {
                                 if ((event.getPosition() >= i.getPosition().get()) && (event.getNetLength() < 0)) {
                                     return false;
                                 }

                                 return true;
                             } )
                         .forEach(
                             i -> {
                                 if (event.getPosition() <= i.getPosition().get()) {
                                     i.getPosition().update(event.getNetLength());
                                 }
                             } );

                // Force a layout of our children on a future pulse.  This ensures that the positions will be in sync.
                // We do this once to prevent a flood of requests overwhelming the app thread.
                this.updateLayout();
            } );
    }

    /**
     * Add a new item.
     *
     * @param it The indicator to add.
     */
    public void addItem(Indicator it) {
        this.getChildren().add(it);
        this.inds.add(it);
    }

    @Override
    protected void layoutChildren() {

        // Determine the bounds of the margin.
        Bounds selfb  = this.getLayoutBounds();
        Bounds sselfb = this.localToScreen(selfb);

        // A filter is not used here for performance reasons, a modelToView call can be expensive so only performing it
        // once and manually filtering is better than having to do it twice.
        this.inds.stream().forEach(i -> {
                i.setVisible(false);

                Optional<Bounds> charb = this.parent.modelToView(i.getPosition().get());

                if (!charb.isPresent()) {
                    return;
                }

                double cy     = charb.get().getMinY();
                double sy     = sselfb.getMinY();
                Insets insets = this.getInsets();

                sy -= insets.getTop();

                Bounds nb = i.getLayoutBounds();

                // See if the character y-bound is within the margin.
                if (((cy + nb.getHeight()) > sy) && (cy <= (sy + sselfb.getHeight()))) {

                    // Any indicator not within our bounds is hidden.
                    i.setVisible(true);

                    // Move our indicator to the correct y offset, the x is any suitable value within the
                    // margin width bounds.
                    i.relocate(0, cy - sy);
                }
            } );
    }

    /**
     * Remove all the indicators.
     */
    public void removeAllItems() {
        this.getChildren().clear();
        this.inds = new ArrayList<>();
    }

    /**
     * Requests a layout for this Node, only does so if we have one or more Indicators.
     */
    public void updateLayout() {
        if (this.inds.size() == 0) {
            return;
        }

        // Run on a future pulse to ensure that things are synced.
        Platform.runLater(() -> this.requestLayout());
    }
}
