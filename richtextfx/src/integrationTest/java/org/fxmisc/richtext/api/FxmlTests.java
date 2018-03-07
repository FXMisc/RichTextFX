package org.fxmisc.richtext.api;

import java.io.IOException;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.fxml.LoadException;
import org.fxmisc.richtext.RichTextFXTestBase;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class FxmlTests extends RichTextFXTestBase {

    @Override
    public void start(Stage stage) {
        // Nothing needed here
    }

    @Test
    public void test_fxml_construction_of_area() throws IOException {
        // FxmlTest-Controller.fxml is located in resources folder: src/integrationTest/resources/org/fxmisc/richtext/api/
        FXMLLoader fxml = new FXMLLoader(getClass().getResource("FxmlTest-Controller.fxml"));
        StyleClassedTextArea area = fxml.load();
        // fxml.load() will throw a LoadException if any properties failed to be set,
        // so if 'area' is not null then all properties are guaranteed to have been set.
        assertNotNull(area);
        
        FxmlController ctrl = fxml.getController();
        // Check that the controller was loaded and that it has the relevant
        // test methods which are referenced in the loaded fxml file.
        assertNotNull(ctrl);
        ctrl.testWithMouseEvent(null);
        ctrl.testWithOutMouseEvent();
    }

}
