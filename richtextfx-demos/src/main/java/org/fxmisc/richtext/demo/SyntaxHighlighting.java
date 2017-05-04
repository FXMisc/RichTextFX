/*
 * Copyright (c) 2013, 2014, Tomas Mikula. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.fxmisc.richtext.demo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyleSpans;
import org.fxmisc.richtext.StyleSpansBuilder;
import org.fxmisc.richtext.demo.styles.SyntaxHighlightingStyles;

import syntaxhighlight.ParseResult;
import syntaxhighlighter.SyntaxHighlighterParser;
import syntaxhighlighter.brush.BrushJava;

public class SyntaxHighlighting extends Application {
    private static final String sampleCode = String.join("\n", new String[] {
        "package com.example;",
        "",
        "import java.util.*;",
        "",
        "public class Foo extends Bar implements Baz {",
        "",
        "   public static void main(String[] args) {",
        "       for(String arg: args) {",
        "           if(arg.length() != 0)",
        "               System.out.println(arg);",
        "           else",
        "               System.err.println(\"Warning: empty string as argument\");",
        "       }",
        "   }",
        "",
        "}",
        ""
    });


    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) {
        final CodeArea codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            codeArea.setStyleSpans(0, computeHighlighting(newText));
        });
        codeArea.replaceText(0, 0, sampleCode);

        final Scene scene = new Scene(new StackPane(codeArea), 600, 400);
		scene.getStylesheets().add(SyntaxHighlightingStyles.getStyle("shCore.css"));
		scene.getStylesheets().add(SyntaxHighlightingStyles.getStyle("shThemeEclipse.css"));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static StyleSpans<Collection<String>> computeHighlighting(final String text) {
		final SyntaxHighlighterParser parser = new SyntaxHighlighterParser(new BrushJava());
		final List<ParseResult> spans = parser.parse(".java", text);
		final StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		int lastKwEnd = 0;
		for (final ParseResult span : spans) {
			spansBuilder.add(Collections.emptyList(), span.getOffset() - lastKwEnd);
			spansBuilder.add(span.getStyleKeys(), span.getLength());
			lastKwEnd = span.getOffset() + span.getLength();
        }
        return spansBuilder.create();
    }
}
