package org.fxmisc.richtext.demo.customobject;

import java.util.ArrayList;
import java.util.Collection;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.LinkedImage;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;


/**
 * This demo shows how to register custom objects with the RichTextFX editor.
 * It creates a sample document with some text, a custom node with a circle, a custom node
 * with a rectangle and also adds an image to show that images are supported without 
 * explicitly implementing and registering them as custom objects.
 */
public class CustomObjectDemo extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    
    @Override
    public void start(Stage primaryStage) {
        StyleClassedTextArea textArea = new StyleClassedTextArea();
        textArea.setWrapText(true);

        // create the sample document
        textArea.replaceText(0, 0, "This example shows how to add custom nodes, for example Rectangles ");
        ReadOnlyStyledDocument<Collection<String>, Collection<String>> d1 = 
                ReadOnlyStyledDocument.from(new RectangleObject(20, 10), 
                                            new ArrayList<String>()); 
        textArea.append(d1);
        textArea.appendText(" or Circles ");

        ReadOnlyStyledDocument<Collection<String>, Collection<String>> d2 =
                ReadOnlyStyledDocument.from(new CircleObject(5), 
                                            new ArrayList<String>());
        textArea.append(d2);

        textArea.appendText("\nImages are supported by default: ");
        ReadOnlyStyledDocument<Collection<String>, Collection<String>> d3 =
                ReadOnlyStyledDocument.from(new LinkedImage<Collection<String>>("sample.png", new ArrayList<String>()), 
                                            new ArrayList<String>());
        textArea.append(d3);

        textArea.appendText("\nNow, select some text from above (including one or more of the custom objects) using CTRL-C, and paste it somewhere in the document with CTRL-V.");
        
        Scene scene = new Scene(new StackPane(new VirtualizedScrollPane<>(textArea)), 600, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Custom Object demo");
        primaryStage.show();
    }
}
