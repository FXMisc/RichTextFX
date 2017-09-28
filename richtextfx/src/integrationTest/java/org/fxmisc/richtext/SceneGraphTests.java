package org.fxmisc.richtext;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

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
        VirtualFlow<String, Cell<String, Node>> flow = (VirtualFlow<String, Cell<String, Node>>) area.getChildrenUnmodifiable().get(index);
        Cell<String, Node> gsa = flow.getCell(0);

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
        TextFlow tf = (TextFlow) paragraphBox.getChildrenUnmodifiable().stream().filter(n -> n instanceof TextFlow)
                                 .findFirst().orElse(null);
        assertNotNull("No TextFlow node found in rich text area", tf);

        return tf;
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
        TextFlow tf = getParagraphText(index);

        List<Path> result = new ArrayList<>();
        tf.getChildrenUnmodifiable().filtered(n -> n instanceof UnderlinePath).forEach(n -> result.add((Path) n));
        return result;
    }
}