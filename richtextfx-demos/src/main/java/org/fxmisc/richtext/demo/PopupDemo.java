package org.fxmisc.richtext.demo;

import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import org.reactfx.value.Var;

import java.util.Optional;

import static org.reactfx.EventStreams.nonNullValuesOf;

public class PopupDemo extends Application {

    private Stage stage;

    /** Helper class: Popup that can track when it should temporarily hide itself if its object is outside of the
     * viewport and provides convenience to adding content to both caret/selection popup */
    private class BoundsPopup extends Popup {

        /** Indicates whether popup should still be shown even when its item (caret/selection) is outside viewport */
        private final Var<Boolean> showWhenItemOutsideViewport = Var.newSimpleVar(true);
        public final EventStream<Boolean> outsideViewportValues() { return showWhenItemOutsideViewport.values(); }
        public final void invertViewportOption() {
            showWhenItemOutsideViewport.setValue(!showWhenItemOutsideViewport.getValue());
        }

        /** Indicates whether popup has been hidden since its item (caret/selection) is outside viewport
         * and should be shown when that item becomes visible again */
        private final Var<Boolean> hideTemporarily = Var.newSimpleVar(false);
        public final boolean isHiddenTemporarily() { return hideTemporarily.getValue(); }
        public final void setHideTemporarily(boolean value) { hideTemporarily.setValue(value); }

        public final void invertVisibility() {
            if (isShowing()) {
                hide();
            } else {
                show(stage);
            }
        }

        private final VBox vbox;
        private final Button button;

        private final Label label;
        public final void setText(String text) { label.setText(text); }

        BoundsPopup(String buttonText) {
            super();
            button = new Button(buttonText);
            label = new Label();
            vbox = new VBox(button, label);
            vbox.setBackground(new Background(new BackgroundFill(Color.ALICEBLUE, null, null)));
            vbox.setPadding(new Insets(5));
            getContent().add(vbox);
        }
    }

    private VBox createPopupOptions(BoundsPopup p, String showHideButtonText, String toggleViewportText) {
        Button showOrHidePopup = new Button(showHideButtonText);
        showOrHidePopup.setOnAction(ae -> p.invertVisibility());
        Button toggleOutOfViewportOption = new Button(toggleViewportText);
        toggleOutOfViewportOption.setOnAction(ae -> p.invertViewportOption());
        return new VBox(showOrHidePopup, toggleOutOfViewportOption);
    }

    private Subscription feedVisibilityToLabelText(EventStream<Optional<Bounds>> boundsStream, BoundsPopup popup, String item) {
        return boundsStream
                .map(o -> o.isPresent() ? " is " : " is not ")
                .subscribe(visibilityStatus -> popup.setText(item + visibilityStatus + "within the viewport"));
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;

        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < 100; i++) {
            sb.append(String.valueOf(i)).append("        END\n");
        }
        InlineCssTextArea area = new InlineCssTextArea("Hello popup!\n" + sb.toString());
        area.setWrapText(true);
        VirtualizedScrollPane<InlineCssTextArea> vsPane = new VirtualizedScrollPane<>(area);

        BoundsPopup caretPopup = new BoundsPopup("I am the caret popup button!");
        BoundsPopup selectionPopup = new BoundsPopup("I am the selection popup button!");

        VBox caretOptions = createPopupOptions(caretPopup, "Show/Hide caret-based popup", "Show/Hide popup even when caret is out of viewport");
        VBox selectionOptions = createPopupOptions(selectionPopup, "Show/Hide selection-based popup", "Show/Hide popup even when selection is out of viewport");

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(caretOptions);
        borderPane.setCenter(vsPane);
        borderPane.setBottom(selectionOptions);

        primaryStage.setScene(new Scene(borderPane, 400, 500));
        primaryStage.setTitle("Popup Demo");
        primaryStage.show();

        // ### Set up EventStreams
        // update labels depending on whether item is within viewport
        EventStream<Optional<Bounds>> caretBounds = nonNullValuesOf(area.caretBoundsProperty());
        Subscription cBoundsSub = feedVisibilityToLabelText(caretBounds, caretPopup, "Caret");
        EventStream<Optional<Bounds>> selectionBounds = nonNullValuesOf(area.selectionBoundsProperty());
        Subscription sBoundsSub = feedVisibilityToLabelText(selectionBounds, selectionPopup, "Selection");

        // set up event streams to update popups every time bounds change
        double caretXOffset = 0;
        double caretYOffset = 0;
        double selectionXOffset = 30;
        double selectionYOffset = 30;

        Subscription caretPopupSub = EventStreams.combine(caretBounds, caretPopup.outsideViewportValues())
                .subscribe(tuple3 -> {
                    Optional<Bounds> opt = tuple3._1;
                    boolean showPopupWhenCaretOutside = tuple3._2;

                    if (opt.isPresent()) {
                        Bounds b = opt.get();
                        caretPopup.setX(b.getMaxX() + caretXOffset);
                        caretPopup.setY(b.getMaxY() + caretYOffset);

                        if (caretPopup.isHiddenTemporarily()) {
                            caretPopup.show(stage);
                            caretPopup.setHideTemporarily(false);
                        }

                    } else {
                        if (!showPopupWhenCaretOutside) {
                            caretPopup.hide();
                            caretPopup.setHideTemporarily(true);
                        }
                    }
                });

        Subscription selectionPopupSub = EventStreams.combine(selectionBounds, selectionPopup.outsideViewportValues())
                .subscribe(tuple3 -> {
                    Optional<Bounds> opt = tuple3._1;
                    boolean showPopupWhenSelectionOutside = tuple3._2;

                    if (opt.isPresent()) {
                        Bounds b = opt.get();
                        selectionPopup.setX(b.getMinX() + selectionXOffset + caretPopup.getWidth());
                        selectionPopup.setY(b.getMinY() + selectionYOffset);

                        if (selectionPopup.isHiddenTemporarily()) {
                            selectionPopup.show(stage);
                            selectionPopup.setHideTemporarily(false);
                        }

                    } else {
                        if (!showPopupWhenSelectionOutside) {
                            selectionPopup.hide();
                            selectionPopup.setHideTemporarily(true);
                        }
                    }
                });

        Subscription caretSubs      = caretPopupSub.and(cBoundsSub);
        Subscription selectionSubs  = selectionPopupSub.and(sBoundsSub);

        caretPopup.show(primaryStage);
        selectionPopup.show(primaryStage);
        area.moveTo(0);
        area.requestFollowCaret();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
