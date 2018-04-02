package org.fxmisc.richtext;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.richtext.UnderlinePath;

import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Contains inspection methods to analyze the scene graph which has been rendered by RichTextFX.
 * TestFX tests should subclass this if it needs to run tests on a simple area and needs to inspect
 * whether the scene graph has been properly created.
 */
public abstract class SceneGraphTests extends InlineCssTextAreaAppTest {

    /**
     * @param index The index of the desired paragraph box
     * @return The paragraph box for the paragraph at the specified index
     */
    protected Region getParagraphBox(int index) {
        @SuppressWarnings("unchecked")
        VirtualFlow<String, Cell<String, Node>> flow = (VirtualFlow<String, Cell<String, Node>>) area.getChildrenUnmodifiable().get(0);
        Cell<String, Node> gsa = flow.getCellIfVisible(index)
                .orElseThrow(() -> new IllegalArgumentException("paragraph " + index + " is not rendered on the screen"));

        // get the ParagraphBox (protected subclass of Region) 
        return (Region) gsa.getNode();
    }
    
    
    /**
     * @param index The index of the desired paragraph box
     * @return The ParagraphText (protected subclass of TextFlow) for the paragraph at the specified index
     */
    protected TextFlow getParagraphText(int index) {
        // get the ParagraphBox (protected subclass of Region) 
        Region paragraphBox = getParagraphBox(index);

        // get the ParagraphText (protected subclass of TextFlow)
        return (TextFlow) paragraphBox.getChildrenUnmodifiable().stream()
                .filter(n -> n instanceof TextFlow)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No TextFlow node found in area at index: " + index));
    }


    /**
     * @param index The index of the desired paragraph box
     * @return A list of text nodes which render the text in the ParagraphBox 
     *         specified by the given index.
     */
    protected List<Text> getTextNodes(int index) {
        TextFlow tf = getParagraphText(index);

        List<Text> result = new ArrayList<>();
        tf.getChildrenUnmodifiable().filtered(n -> n instanceof Text).forEach(n -> result.add((Text) n));
        return result;
    }


    /**
     * @param index The index of the desired paragraph box
     * @return A list of nodes which render the underlines for the text in the ParagraphBox 
     *         specified by the given index.
     */
    protected List<Path> getUnderlinePaths(int index) {
        return getParagraphTextChildren(index, n -> n instanceof UnderlinePath, n -> (UnderlinePath) n);
    }

    protected List<Path> getBorderPaths(int index) {
        return getParagraphTextChildren(index, n -> n instanceof BorderPath, n -> (BorderPath) n);
    }

    protected List<Path> getBackgroundPaths(int index) {
        return getParagraphTextChildren(index, n -> n instanceof BackgroundPath, n -> (BackgroundPath) n);
    }

    private <T> List<T> getParagraphTextChildren(int index, Predicate<Node> instanceOfCheck, Function<Node, T> cast) {
        TextFlow tf = getParagraphText(index);

        List<T> result = new ArrayList<>();
        tf.getChildrenUnmodifiable()
                .filtered(instanceOfCheck)
                .forEach(n -> result.add(cast.apply(n)));
        return result;

    }
}