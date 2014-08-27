/*
 * Copyright (c) 2013, Tomas Mikula. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.fxmisc.richtext.demo;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.IndexRange;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.fxmisc.richtext.StyleClassedTextArea;

public class ManualHighlighting extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private final StyleClassedTextArea area = new StyleClassedTextArea();
    {
        area.setWrapText(true);
    }

    @Override
    public void start(Stage primaryStage) {
        HBox panel = new HBox();
        Button red = createColorButton(Color.RED, "red");
        Button green = createColorButton(Color.GREEN, "green");
        Button blue = createColorButton(Color.BLUE, "blue");
        Button bold = createBoldButton("bold");
        panel.getChildren().addAll(red, green, blue, bold);

        VBox vbox = new VBox();
        VBox.setVgrow(area, Priority.ALWAYS);
        vbox.getChildren().addAll(panel, area);

        Scene scene = new Scene(vbox, 600, 400);
        scene.getStylesheets().add(ManualHighlighting.class.getResource("manual-highlighting.css").toExternalForm());
        primaryStage.setScene(scene);
        area.requestFocus();
        primaryStage.setTitle("Manual Highlighting Demo");
        primaryStage.show();
    }

    private Button createBoldButton(String styleClass) {
        Button button = new Button("B");
        button.styleProperty().set("-fx-font-weight: bold;");
        setPushHandler(button, styleClass);
        return button;
    }

    private Button createColorButton(Color color, String styleClass) {
        Rectangle rect = new Rectangle(20, 20, color);
        Button button = new Button(null, rect);
        setPushHandler(button, styleClass);
        return button;
    }

    private void setPushHandler(Button button, String styleClass) {
        button.onActionProperty().set(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent evt) {
                IndexRange range = area.getSelection();
                area.setStyleClass(range.getStart(), range.getEnd(), styleClass);
                area.requestFocus();
            }
        });
    }

}
