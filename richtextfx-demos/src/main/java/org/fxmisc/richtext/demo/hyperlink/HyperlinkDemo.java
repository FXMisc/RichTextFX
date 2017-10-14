package org.fxmisc.richtext.demo.hyperlink;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

/**
 * Demonstrates the minimum needed to support custom objects (in this case, hyperlinks) alongside of text.
 *
 * Note: demo does not handle cases where the link changes its state when it has already been visited
 */
public class HyperlinkDemo extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Consumer<String> showLink = (string) -> {
            try
            {
                Desktop.getDesktop().browse(new URI(string));
            }
            catch (IOException | URISyntaxException e)
            {
                throw new RuntimeException(e);
            }
        };
        TextHyperlinkArea area = new TextHyperlinkArea(showLink);

        area.appendText("Some text in the area\n");
        area.appendWithLink("Google.com", "http://www.google.com");

        VirtualizedScrollPane<TextHyperlinkArea> vsPane = new VirtualizedScrollPane<>(area);

        Scene scene = new Scene(vsPane, 500, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
