package org.fxmisc.richtext;

import org.fxmisc.richtext.JavaFXCompatibility;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class MultiReleaseJarTest
{
    @Test
    public void tests_correct_classes_are_used() {
        
    	if ( System.getProperty( "javafx.version" ).split("\\.")[0].equals("8") ) {
    		assertTrue( JavaFXCompatibility.isJavaEight() );
    	}
    	else {
    		assertFalse( JavaFXCompatibility.isJavaEight() );
    	}
    }

}
