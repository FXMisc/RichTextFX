package org.fxmisc.richtext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.StringTokenizer;
import javafx.beans.property.ObjectProperty;
import javafx.css.StyleConverter;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

/**
 * Uses reflection to make this project's code work on Java 8 and Java 9 in a single jar
 */
public class JavaFXCompatibility {

    private static boolean isJava9orLater;

    static {
        try {
            // Java 9 version-String Scheme: http://openjdk.java.net/jeps/223
            StringTokenizer st = new StringTokenizer(System.getProperty("java.version"), "._-+");
            int majorVersion = Integer.parseInt(st.nextToken());
            isJava9orLater = majorVersion >= 9;
        } catch (Exception e) {
            // Java 8 or older
        }
    }

    /**
     * There is a Java 9 version of this that returns false in src/main/java9/...
     * and is used to check if tests are running against a multi-release jar.
     */
    public static boolean isJavaEight() {
    	return true;
    }
    
    /**
     * Java 8:  javafx.scene.text.Text.impl_selectionFillProperty()
     * Java 9+: javafx.scene.text.Text.selectionFillProperty()
     */
    @SuppressWarnings("unchecked")
    static ObjectProperty<Paint> Text_selectionFillProperty(Text text) {
        try {
            if (mText_selectionFillProperty == null) {
                mText_selectionFillProperty = Text.class.getMethod(
                        isJava9orLater ? "selectionFillProperty" : "impl_selectionFillProperty");
            }
            return (ObjectProperty<Paint>) mText_selectionFillProperty.invoke(text);
        } catch(NoSuchMethodException | SecurityException | IllegalAccessException |
                IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    private static Method mText_selectionFillProperty;

    /**
     * Java 8:  com.sun.javafx.css.converters.SizeConverter.SequenceConverter.getInstance()
     * Java 9+: javafx.css.converter.SizeConverter.SequenceConverter.getInstance()
     */
    @SuppressWarnings("unchecked")
    static StyleConverter<?, Number[]> SizeConverter_SequenceConverter_getInstance() {
        try {
            if (mSizeConverter_SequenceConverter_getInstance == null) {
                Class<?> c = Class.forName(isJava9orLater
                        ? "javafx.css.converter.SizeConverter$SequenceConverter"
                        : "com.sun.javafx.css.converters.SizeConverter$SequenceConverter");
                mSizeConverter_SequenceConverter_getInstance = c.getMethod("getInstance");
            }
            return (StyleConverter<?, Number[]>) mSizeConverter_SequenceConverter_getInstance.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException |
                IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    private static Method mSizeConverter_SequenceConverter_getInstance;
}
