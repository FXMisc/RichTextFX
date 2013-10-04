/*
 * Copyright (c) 2013, Tomas Mikula. All rights reserved.
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

package codearea.demo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import codearea.control.CodeArea;

public class JavaKeywords extends Application {

    private static final List<String> KEYWORDS = Arrays.asList(
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
    );

    private static final Pattern KEYWORD = Pattern.compile("\\b(" + String.join("|", KEYWORDS) + ")\\b");

    private static final Set<String> KW_CLASSES = new HashSet<String>(1);
    static {
        KW_CLASSES.add("keyword");
    }


    public static void main(String[] args) {
        launch(args);
    }

	private final CodeArea area = new CodeArea();

	public JavaKeywords() {
	    area.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable,
                    String oldText, String newText) {
                Matcher matcher = KEYWORD.matcher(newText);
                int lastKwEnd = 0;
                while(matcher.find()) {
                    area.setStyleClasses(lastKwEnd, matcher.start(), Collections.EMPTY_SET);
                    area.setStyleClasses(matcher.start(), matcher.end(), KW_CLASSES);
                    lastKwEnd = matcher.end();
                }
                area.setStyleClasses(lastKwEnd, newText.length(), Collections.EMPTY_SET);
            }
	    });
	}

	@Override
	public void start(Stage primaryStage) {
		StackPane root = new StackPane();
        root.getChildren().add(area);
        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add(JavaKeywords.class.getResource("java-keywords.css").toExternalForm());
		primaryStage.setScene(scene);
        area.requestFocus();
		primaryStage.show();
	}

}
