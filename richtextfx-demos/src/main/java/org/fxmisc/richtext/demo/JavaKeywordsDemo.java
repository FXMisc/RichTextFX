package org.fxmisc.richtext.demo;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.PlainTextChange;
import org.reactfx.Subscription;

import static org.fxmisc.richtext.model.TwoDimensional.Bias.Backward;

public class JavaKeywordsDemo extends Application {

    private static final String sampleCode = String.join("\n", new String[] {
        "package com.example;",
        "",
        "import java.util.*;",
        "",
        "public class Foo extends Bar implements Baz {",
        "",
        "    /*",
        "     * multi-line comment",
        "     */",
        "    public static void main(String[] args) {",
        "        // single-line comment",
        "        for(String arg: args) {",
        "            if(arg.length() != 0)",
        "                System.out.println(arg);",
        "            else",
        "                System.err.println(\"Warning: empty string as argument\");",
        "        }",
        "    }",
        "",
        "}"
    });

    private static final Pattern whiteSpace = Pattern.compile( "^\\s+" );

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        CodeArea codeArea = new CodeArea();

        // add line numbers to the left of area
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.setContextMenu(new DefaultContextMenu());

        // recompute the syntax highlighting for changed paragraphs, 500 ms after user stops editing area
        Subscription cleanupWhenNoLongerNeedIt = codeArea

                // plain changes = ignore style changes that are emitted when syntax highlighting is reapplied
                // multi plain changes = save computation by not rerunning the code multiple times
                //   when making multiple changes (e.g. renaming a method at multiple parts in file)
                .multiPlainChanges()

                // do not emit an event until 500 ms have passed since the last emission of previous stream
                .reduceSuccessions(
						// We can't just add b to a and return a because a gets changed somewhere else
						(a,b) -> Stream.concat(a.stream(), b.stream()).toList(),
		                Duration.ofMillis(500) )

                // run the following code block when previous stream emits an event
                .subscribe(changes ->
                {
                	// work out which lines have been changed, remembering that later changes may affect the line numbers of earlier changes
                    Set<Integer> linesChanged = new HashSet<>();
                    for (PlainTextChange change : changes)
                	{
                	    int lineCountChange = (int) (change.getInserted().chars().filter(ch -> ch == '\n').count() - change.getRemoved().chars().filter(ch -> ch == '\n').count());
                	    int startLine = codeArea.offsetToPosition(change.getPosition(), Backward).getMajor();
                	    shiftAllLineNumbersAboveThreshold(linesChanged, startLine, lineCountChange);
                	    int endLine = codeArea.offsetToPosition(change.getInsertionEnd(), Backward).getMajor();
                	    IntStream.rangeClosed(startLine, endLine).forEach(linesChanged::add);
                    }
                	// re-style each line that was changed
                    for (int i : linesChanged)
                	{
                	    codeArea.setStyleSpans( i, 0, new JavaStyler( codeArea.getText(i) ).style() );
                    }
                });

        // when no longer need syntax highlighting and wish to clean up memory leaks
        // run: `cleanupWhenNoLongerNeedIt.unsubscribe();`

        codeArea.addEventHandler( KeyEvent.KEY_PRESSED, KE -> onKeyPressed(codeArea, KE));
        codeArea.replaceText(0, 0, sampleCode);

        Scene scene = new Scene(new StackPane(new VirtualizedScrollPane<>(codeArea)), 600, 400);
        scene.getStylesheets().add(JavaKeywordsDemo.class.getResource("java-keywords.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Java Keywords Demo");
        primaryStage.show();
    }
	
	private void shiftAllLineNumbersAboveThreshold(Set<Integer> lineNumbers, int threshold, int shift) {
		if (shift != 0) {
			Set<Integer> updatedLineNumbers = new HashSet<>();
			lineNumbers.removeIf(lineNumber -> {
				if (lineNumber > threshold) {
					updatedLineNumbers.add(Math.max(lineNumber + shift, threshold));
					return true;
				}
				return false;
			});
			lineNumbers.addAll(updatedLineNumbers);
		}
	}

    private void onKeyPressed(CodeArea codeArea, KeyEvent KE) {
        // auto-indent: insert previous line's indents on enter
        if ( KE.getCode() == KeyCode.ENTER ) {
            int caretPosition = codeArea.getCaretPosition();
            int currentParagraph = codeArea.getCurrentParagraph();
            Matcher m0 = whiteSpace.matcher( codeArea.getParagraph( currentParagraph-1 ).getSegments().get( 0 ) );
            if ( m0.find() ) {
                Platform.runLater( () -> codeArea.insertText( caretPosition, m0.group() ) );
            }
        }
    }

    private static class DefaultContextMenu extends ContextMenu
    {
        public DefaultContextMenu()
        {
            MenuItem fold = new MenuItem("Fold selected text");
            MenuItem unfold = new MenuItem( "Unfold from cursor" );
            MenuItem print = new MenuItem( "Print" );
            fold.setOnAction(AE -> { hide(); fold(); } );
            unfold.setOnAction( AE -> { hide(); unfold(); } );
            print.setOnAction( AE -> { hide(); print(); } );
            getItems().addAll(fold, unfold, print );
        }

        /**
         * Folds multiple lines of selected text, only showing the first line and hiding the rest.
         */
        private void fold() {
            ((CodeArea) getOwnerNode()).foldSelectedParagraphs();
        }

        /**
         * Unfold the CURRENT line/paragraph if it has a fold.
         */
        private void unfold() {
            CodeArea area = (CodeArea) getOwnerNode();
            area.unfoldParagraphs( area.getCurrentParagraph() );
        }

        private void print() {
            System.out.println( ((CodeArea) getOwnerNode()).getText() );
        }
    }
}
