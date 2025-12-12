package org.fxmisc.richtext.demo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
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

import static java.util.stream.Collectors.toSet;
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
		        // in the meantime, store text changes in a list
                .reduceSuccessions(this::concatChangeLists, // returns a new list because the lists passed in get modified elsewhere
		                Duration.ofMillis(500) )

                // run this lambda to update syntax highlighting when previous stream emits an event
                .subscribe(textChanges -> restyleChangedLines(codeArea, textChanges));

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
	
	private List<PlainTextChange> concatChangeLists(List<PlainTextChange> firstChanges, List<PlainTextChange> secondChanges) {
		List<PlainTextChange> combinedChangeList = new ArrayList<>(firstChanges);
		combinedChangeList.addAll(secondChanges);
		return combinedChangeList;
	}
	
	private void restyleChangedLines(CodeArea codeArea, List<PlainTextChange> textChanges) {
		Set<IndexRange> positionsChanged = getPositionsChanged(textChanges);
		Set<Integer> lineNumbersChanged = getLineNumbersChanged(codeArea, positionsChanged);
		restyleLines(codeArea, lineNumbersChanged);
	}
	
	// Converting positions to line numbers could be slow for large texts, so use IndexRanges instead of enumerating all positions
	private Set<IndexRange> getPositionsChanged(List<PlainTextChange> textChanges) {
		// Only the last change has up-to-date position data (i.e. up-to-date dirty range that needs restyling)
		// Earlier changes need to have their positions updated by shifting them based on later changes
		Set<IndexRange> dirtyRangesSoFar = new HashSet<>(); // Reflects changes that have happened so far as we go through them in order
		for (PlainTextChange textChange : textChanges) {
			dirtyRangesSoFar = updateDirtyRanges(dirtyRangesSoFar, textChange);
		}
		return dirtyRangesSoFar;
	}
	
	private Set<IndexRange> updateDirtyRanges(Set<IndexRange> dirtyRangesSoFar, PlainTextChange nextChange) {
		dirtyRangesSoFar = shiftIndicesAtOrAbovePosition(dirtyRangesSoFar, nextChange.getPosition(), nextChange.getNetLength());
		dirtyRangesSoFar.add(new IndexRange(nextChange.getPosition(), nextChange.getInsertionEnd()));
		return dirtyRangesSoFar;
	}
	
	private Set<IndexRange> shiftIndicesAtOrAbovePosition(Set<IndexRange> indexRanges, int changePosition, int shift) {
		if (shift == 0) {
			return indexRanges;
		} else {
			return indexRanges.stream()
					.map(range -> shiftRangeAtOrAbovePosition(range, changePosition, shift))
					.collect(toSet());
		}
	}
	
	private IndexRange shiftRangeAtOrAbovePosition(IndexRange range, int changePosition, int shift) {
		if (range.getEnd() >= changePosition) {
			int newStart = shiftIndexAtOrAbovePosition(range.getStart(), changePosition, shift);
			int newEnd = shiftIndexAtOrAbovePosition(range.getEnd(), changePosition, shift);
			return new IndexRange(newStart, newEnd);
		}
		return range;
	}
	
	private int shiftIndexAtOrAbovePosition(int index, int changePosition, int shift) {
		if (index < changePosition) {
			return index;
		}
		index += shift;
		// index could now be lower than changePosition if the change deleted this character
		// But deleted characters don't need restyling, so in that case just return changePosition, which already needs restyling
		return Math.max(index, changePosition);
	}
	
	private Set<Integer> getLineNumbersChanged(CodeArea codeArea, Set<IndexRange> positionsChanged) {
		return positionsChanged.stream()
				.flatMap(indexRange -> getLineNumbers(codeArea, indexRange))
				.collect(toSet());
	}
	
	private Stream<Integer> getLineNumbers(CodeArea codeArea, IndexRange indexRange) {
		int startLineNumber = getLineNumber(codeArea, indexRange.getStart());
		// indexRange has an exclusive end that needs 1 subtracting, but we don't want the end position to be before the start
		int endPosition = Math.max(indexRange.getEnd() - 1, indexRange.getStart());
		// getLineNumber() could be expensive for a huge text, so avoid calling if possible
		int endLineNumber = endPosition == indexRange.getStart() ? startLineNumber : getLineNumber(codeArea, endPosition);
		return IntStream.rangeClosed(startLineNumber, endLineNumber).boxed();
	}
	
	private int getLineNumber(CodeArea codeArea, int position) {
		return codeArea.offsetToPosition(position, Backward).getMajor();
	}
	
	private void restyleLines(CodeArea codeArea, Set<Integer> lineNumbers) {
		for (int lineNumber : lineNumbers)
		{
			String line = codeArea.getText(lineNumber);
			codeArea.setStyleSpans( lineNumber, 0, new JavaStyler(line).style() );
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
