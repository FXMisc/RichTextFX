package org.fxmisc.richtext.demo.styles;

public class SyntaxHighlightingStyles {
	public static String getStyle(String name) {
		return SyntaxHighlightingStyles.class.getResource(name).toExternalForm();
	}
}
