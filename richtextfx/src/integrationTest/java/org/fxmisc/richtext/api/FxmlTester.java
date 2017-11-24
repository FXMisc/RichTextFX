package org.fxmisc.richtext.api;

import java.io.IOException;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.fxml.LoadException;
import org.fxmisc.richtext.RichTextFXTestBase;
import org.junit.Test;

public class FxmlTester extends RichTextFXTestBase {

    @Override
    public void start(Stage stage) throws Exception {
        // Nothing needed here
    }

    @Test
	public void test_fxml_construction_of_area() throws IOException, LoadException
	{
        // FxmlTest.fxml is located in resources folder:
        // src/integrationTest/resources/org/fxmisc/richtext/api/
		Object obj = FXMLLoader.load( getClass().getResource( "FxmlTest.fxml" ) );
        // FXMLLoader will throw a LoadException if any properties failed to be set,
        // so if obj is not null then all properties are guaranteed to have been set.
        org.junit.Assert.assertNotNull( obj );
    }

}
