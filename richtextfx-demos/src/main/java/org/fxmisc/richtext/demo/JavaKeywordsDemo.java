package org.fxmisc.richtext.demo;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.collection.ListModification;

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
/*
        // recompute the syntax highlighting for all text, 500 ms after user stops editing area
        // Note that this shows how it can be done but is not recommended for production with
        // large files as it does a full scan of ALL the text every time there is a change !
        Subscription cleanupWhenNoLongerNeedIt = codeArea

                // plain changes = ignore style changes that are emitted when syntax highlighting is reapplied
                // multi plain changes = save computation by not rerunning the code multiple times
                //   when making multiple changes (e.g. renaming a method at multiple parts in file)
                .multiPlainChanges()

                // do not emit an event until 500 ms have passed since the last emission of previous stream
                .successionEnds(Duration.ofMillis(500))

                // run the following code block when previous stream emits an event
                .subscribe(ignore -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));

        // when no longer need syntax highlighting and wish to clean up memory leaks
        // run: `cleanupWhenNoLongerNeedIt.unsubscribe();`
*/
        // recompute syntax highlighting only for visible paragraph changes
        // Note that this shows how it can be done but is not recommended for production where multi-line
        // syntax requirements are needed, like comment blocks without a leading * on each line.
        codeArea.getVisibleParagraphs().addModificationObserver(new ParagraphModificationHandler<>( codeArea, text -> new JavaStyler(text).style()));

        codeArea.addEventHandler( KeyEvent.KEY_PRESSED, KE -> onKeyPressed(codeArea, KE));
        codeArea.replaceText(0, 0, sampleCode);

        Scene scene = new Scene(new StackPane(new VirtualizedScrollPane<>(codeArea)), 600, 400);
        scene.getStylesheets().add(JavaKeywordsAsyncDemo.class.getResource("java-keywords.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Java Keywords Demo");
        primaryStage.show();
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

    /**
     * One time-use class to evaluate the style of the text provided.
     */
    private static class JavaStyler {
        private static final String[] KEYWORDS = new String[] {
                "abstract", "assert", "boolean", "break", "byte",
                "case", "catch", "char", "class", "const",
                "continue", "default", "do", "double", "else",
                "enum", "extends", "final", "finally", "float",
                "for", "goto", "if", "implements", "import",
                "instanceof", "int", "interface", "long", "native",
                "new", "package", "private", "protected", "public",
                "return", "short", "static", "strictfp", "super",
                "switch", "synchronized", "this", "throw", "throws",
                "transient", "try", "void", "volatile", "while"
        };

        private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
        private static final String PAREN_PATTERN = "\\(|\\)";
        private static final String BRACE_PATTERN = "\\{|\\}";
        private static final String BRACKET_PATTERN = "\\[|\\]";
        private static final String SEMICOLON_PATTERN = "\\;";
        private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
        private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"   // for whole text processing (text blocks)
                + "|" + "/\\*[^\\v]*" + "|" + "^\\h*\\*([^\\v]*|/)";  // for visible paragraph processing (line by line)

        private static final String GROUP_KEYWORD = "KEYWORD";
        private static final String GROUP_PAREN = "PAREN";
        private static final String GROUP_BRACE = "BRACE";
        private static final String GROUP_BRACKET = "BRACKET";
        private static final String GROUP_SEMICOLON = "SEMICOLON";
        private static final String GROUP_STRING = "STRING";
        private static final String GROUP_COMMENT = "COMMENT";

        private static final Pattern PATTERN = Pattern.compile(
                "(?<" + GROUP_KEYWORD + ">" + KEYWORD_PATTERN + ")" +
                "|(?<" + GROUP_PAREN + ">" + PAREN_PATTERN + ")" +
                "|(?<" + GROUP_BRACE + ">" + BRACE_PATTERN + ")" +
                "|(?<" + GROUP_BRACKET + ">" + BRACKET_PATTERN + ")" +
                "|(?<" + GROUP_SEMICOLON + ">" + SEMICOLON_PATTERN + ")" +
                "|(?<" + GROUP_STRING + ">" + STRING_PATTERN + ")" +
                "|(?<" + GROUP_COMMENT + ">" + COMMENT_PATTERN + ")"
        );

        private final String text;
        private final Matcher matcher;
        private final StyleSpansBuilder<Collection<String>> spansBuilder;
        private int lastSpanEnd;

        public JavaStyler(String text) {
            this.text = text;
            this.matcher = PATTERN.matcher(text);
            this.spansBuilder = new StyleSpansBuilder<>();
            this.lastSpanEnd = 0;
        }

        public StyleSpans<Collection<String>> style() {
            while(matcher.find()) {
                String styleClass = evaluateNextStyle(matcher);
                evaluateMatch(matcher.start(), matcher.end(), styleClass);
            }
            endStyle();
            return spansBuilder.create();
        }

        private void endStyle() {
            spansBuilder.add(Collections.emptyList(), text.length() - lastSpanEnd);
        }

        private void evaluateMatch(int start, int end, String styleClass) {
            assert styleClass != null;
            // From the last style found to the new one we apply an empty list (no style)
            spansBuilder.add(Collections.emptyList(), start - lastSpanEnd);
            // Then we apply the one found by the matcher
            spansBuilder.add(Collections.singleton(styleClass), end - start);
            // Save the end
            lastSpanEnd = end;
        }

        private String evaluateNextStyle(Matcher matcher) {
            return matcher.group(GROUP_KEYWORD) != null ? "keyword" :
                   matcher.group(GROUP_PAREN) != null ? "paren" :
                   matcher.group(GROUP_BRACE) != null ? "brace" :
                   matcher.group(GROUP_BRACKET) != null ? "bracket" :
                   matcher.group(GROUP_SEMICOLON) != null ? "semicolon" :
                   matcher.group(GROUP_STRING) != null ? "string" :
                   matcher.group(GROUP_COMMENT) != null ? "comment" :
                   null; /* never happens */
        }
    }

    private static class ParagraphModificationHandler<PS, SEG, S> implements Consumer<ListModification<? extends Paragraph<PS, SEG, S>>>
    {
        private final GenericStyledArea<PS, SEG, S> area;
        private final Function<String,StyleSpans<S>> computeStyles;
        private int prevParagraph, prevTextLength;

        public ParagraphModificationHandler(GenericStyledArea<PS, SEG, S> area,
                                            Function<String,StyleSpans<S>> computeStyles) {
            this.computeStyles = computeStyles;
            this.area = area;
        }

        @Override
        public void accept(ListModification<? extends Paragraph<PS, SEG, S>> modifications)
        {
            if (modifications.getAddedSize() > 0) {
                Platform.runLater(() -> {
                    int paragraph = Math.min(area.firstVisibleParToAllParIndex() + modifications.getFrom(), area.getParagraphs().size() - 1);
                    String text = area.getText(paragraph, 0, paragraph, area.getParagraphLength(paragraph));

                    if (paragraph != prevParagraph || text.length() != prevTextLength) {
                        if (paragraph < area.getParagraphs().size() - 1) {
                            int startPos = area.getAbsolutePosition(paragraph, 0);
                            area.setStyleSpans(startPos, computeStyles.apply(text));
                        }
                        prevTextLength = text.length();
                        prevParagraph = paragraph;
                    }
                });
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
