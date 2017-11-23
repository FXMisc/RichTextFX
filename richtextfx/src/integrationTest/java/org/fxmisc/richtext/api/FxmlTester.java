package org.fxmisc.richtext.api;

import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import org.fxmisc.richtext.RichTextFXTestBase;
import org.junit.Test;

public class FxmlTester extends RichTextFXTestBase {

    @Override
    public void start(Stage stage) throws Exception {
    	// Nothing needed here
    }
    
	@Test
	public void test_fxml_construction_of_area()
	{
		Object obj = null;
		try	{ obj = FXMLLoader.load( getClass().getResource( "FxmlTest.fxml" ) ); }
		catch ( Exception EX ) { EX.printStackTrace(); }
		org.junit.Assert.assertNotNull( obj );
	}

}
