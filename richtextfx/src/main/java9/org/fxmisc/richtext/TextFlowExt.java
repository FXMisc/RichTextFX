package org.fxmisc.richtext;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.*;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.model.TwoLevelNavigator;

import javafx.geometry.Point2D;
import javafx.scene.shape.PathElement;
import javafx.scene.text.TextFlow;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.text.HitInfo;


/**
 * Adds additional API to {@link TextFlow}.
 */
class TextFlowExt extends TextFlow {

    private static Method mGetTextLayout;
    private static Method mGetRange;

    static {
        try {
            mGetTextLayout = TextFlow.class.getDeclaredMethod("getTextLayout");
            mGetRange = TextFlow.class.getDeclaredMethod("getRange", int.class, int.class, int.class);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
        mGetTextLayout.setAccessible(true);
        mGetRange.setAccessible(true);
    }

    private static Object invoke(Method m, Object obj, Object... args) {
        try {
            return m.invoke(obj, args);
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    int getLineCount() {
        return getLines().length;
    }

    int getLineStartPosition(int charIdx) {
        TextLine[] lines = getLines();
        TwoLevelNavigator navigator = new TwoLevelNavigator(
                () -> lines.length,
                i -> lines[i].getLength());
        int currentLineIndex = navigator.offsetToPosition(charIdx, Forward).getMajor();
        return navigator.position(currentLineIndex, 0).toOffset();
    }

    int getLineEndPosition(int charIdx) {
        TextLine[] lines = getLines();
        TwoLevelNavigator navigator = new TwoLevelNavigator(
                () -> lines.length,
                i -> lines[i].getLength());
        int currentLineIndex = navigator.offsetToPosition(charIdx, Forward).getMajor();
        int minor = currentLineIndex == lines.length - 1 ? 0 : -1;
        return navigator.position(currentLineIndex + 1, minor).toOffset();
    }

    int getLineOfCharacter(int charIdx) {
        TextLine[] lines = getLines();
        TwoLevelNavigator navigator = new TwoLevelNavigator(
                () -> lines.length,
                i -> lines[i].getLength());
        return navigator.offsetToPosition(charIdx, Forward).getMajor();
    }

    PathElement[] getCaretShape(int charIdx, boolean isLeading) {
    	return caretShape(charIdx, isLeading);
        // return textLayout().getCaretShape(charIdx, isLeading, 0.0f, 0.0f);
    }

    PathElement[] getRangeShape(IndexRange range) {
        return getRangeShape(range.getStart(), range.getEnd());
    }

    PathElement[] getRangeShape(int from, int to) {
    	return rangeShape(from, to);
        // return textLayout().getRange(from, to, TextLayout.TYPE_TEXT, 0, 0);
    }

    PathElement[] getUnderlineShape(IndexRange range) {
        return getUnderlineShape(range.getStart(), range.getEnd());
    }

    /**
     * @param from The index of the first character.
     * @param to The index of the last character.
     * @return An array with the PathElement objects which define an
     *         underline from the first to the last character.
     */
    PathElement[] getUnderlineShape(int from, int to) {
        // get a Path for the text underline
    	PathElement[] shape;
    	         try {
    	             shape = (PathElement[]) mGetRange.invoke(this, from, to, 1 << 1);
    	         }
    	         catch (IllegalAccessException | InvocationTargetException e) {
    	             throw new RuntimeException();
    	         }
        // PathElement[] shape = textLayout().getRange(from, to, TextLayout.TYPE_UNDERLINE, 0, 0);

        // The shape is returned as a closed Path (a thin rectangle).
        // If we use the Path as it is, this causes rendering issues.
        // Hence we only use the MoveTo and the succeeding LineTo elements for the result
        // so that simple line segments instead of rectangles are returned.
        List<PathElement> result = new ArrayList<>();

        boolean collect = false;
        for (PathElement elem : shape) {
            if (elem instanceof MoveTo) {   // There seems to be no API to get the type of the PathElement
                result.add(elem);
                collect = true;
            } else if (elem instanceof LineTo) {
                if (collect) {
                    result.add(elem);
                    collect = false;
                }
            }
        }

       return result.toArray(new PathElement[0]);
    }

    CharacterHit hitLine(double x, int lineIndex) {
        return hit(x, getLineCenter(lineIndex));
    }

    CharacterHit hit(double x, double y) {
        HitInfo hit = hitTest(new Point2D(x, y));
        int charIdx = hit.getCharIndex();
        boolean leading = hit.isLeading();

        int lineIdx = getLineIndex((float) y);
        if(lineIdx >= getLineCount()) {
            return CharacterHit.insertionAt(getCharCount());
        }

        TextLine[] lines = getLines();
        TextLine line = lines[lineIdx];
        RectBounds lineBounds = line.getBounds();

        // If this is a wrapped paragraph and hit character is at end of hit line,
        // make sure that the "character hit" stays at the end of the hit line
        // (and not at the beginning of the next line).
        if(lines.length > 1 &&
            lineIdx < lines.length - 1 &&
            charIdx + 1 >= line.getStart() + line.getLength() &&
            !leading)
        {
            leading = true;
        }

        if(x < lineBounds.getMinX() || x > lineBounds.getMaxX()) {
            if(leading) {
                return CharacterHit.insertionAt(charIdx);
            } else {
                return CharacterHit.insertionAt(charIdx + 1);
            }
        } else {
            if(leading) {
                return CharacterHit.leadingHalfOf(charIdx);
            } else {
                return CharacterHit.trailingHalfOf(charIdx);
            }
        }
    }

    private float getLineY(int index) {
        TextLine[] lines = getLines();
        float spacing = (float) getLineSpacing();
        float lineY = 0;
        for(int i = 0; i < index; ++i) {
            lineY += lines[i].getBounds().getHeight() + spacing;
        }
        return lineY;
    }

    private float getLineCenter(int index) {
        return getLineY(index) + getLines()[index].getBounds().getHeight() / 2;
    }

    private TextLine[] getLines() {
    	return textLayout().getLines();
    }

    private int getLineIndex(float y) {
    	return textLayout().getLineIndex(y);
    }

    private int getCharCount() {
    	 return textLayout().getCharCount();
    }

    private TextLayout textLayout() {
    	return GenericIceBreaker.proxy(TextLayout.class, invoke(mGetTextLayout, this));
    }

    
    
    
    
    static class GenericIceBreaker implements InvocationHandler {
    private final Object delegate;

   public GenericIceBreaker(Object delegate) {
         this.delegate = delegate;
     }
 
     @Override
     public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         Method delegateMethod = delegate.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
         if (!delegateMethod.canAccess(delegate)) {
             delegateMethod.setAccessible(true);
        }
 
        Object delegateMethodReturn = null;
        try {
            delegateMethodReturn = delegateMethod.invoke(delegate, args);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException("problems invoking " + method.getName());
        }
        if (delegateMethodReturn == null) {
            return null;
        }

        if (method.getReturnType().isArray()) {
            if (method.getReturnType().getComponentType().isInterface()
                    && !method.getReturnType().getComponentType().equals(delegateMethod.getReturnType().getComponentType())) {

                int arrayLength = Array.getLength(delegateMethodReturn);
                Object retArray = Array.newInstance(method.getReturnType().getComponentType(), arrayLength);
                for (int i = 0; i < arrayLength; i++) {
                    Array.set(retArray,
                            i,
                            proxy(
                                    method.getReturnType().getComponentType(),
                                    Array.get(delegateMethodReturn, i)));
                }

                return retArray;
            }
        }

        if (method.getReturnType().isInterface()
                && !method.getReturnType().equals(delegateMethod.getReturnType())) {
            return proxy(method.getReturnType(), delegateMethodReturn);
        }

        return delegateMethodReturn;
    }

    public static <T> T proxy(Class<T> iface, Object delegate) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class[]{iface},
                new GenericIceBreaker(delegate));
    }
}    
    
    
    
    
}
