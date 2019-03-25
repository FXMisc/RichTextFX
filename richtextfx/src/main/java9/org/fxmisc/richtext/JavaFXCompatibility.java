package org.fxmisc.richtext;

import javafx.beans.property.ObjectProperty;
import javafx.css.converter.SizeConverter;
import javafx.css.StyleConverter;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

/* ************************************************* *
 * 													 *
 *  Also look for and remove deprecated methods !!!  *
 *  												 *
 * ************************************************* */
/**
 * Used to use reflection to make this project's code work on Java 8 and Java 9 in a single jar
 */
@Deprecated
public class JavaFXCompatibility {

    static public boolean isJavaEight() {
    	return false;
    }

    static ObjectProperty<Paint> Text_selectionFillProperty(Text text) {
    	return text.selectionFillProperty();
    }

    static StyleConverter<?, Number[]> SizeConverter_SequenceConverter_getInstance() {
    	return SizeConverter.SequenceConverter.getInstance();
    }
}
