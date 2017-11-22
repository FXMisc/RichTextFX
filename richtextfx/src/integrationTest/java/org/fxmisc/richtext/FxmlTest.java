package org.fxmisc.richtext;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public class FxmlTest extends Application {

	@Override public void start(Stage primaryStage)
	{
		Object obj;
		try	{ obj = FXMLLoader.load( getClass().getResource( "FxmlTest.fxml" ) ); }
		catch ( Exception EX ) { EX.printStackTrace(); }
		org.junit.Assert.assertNotNull( obj );
		Platform.exit();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
