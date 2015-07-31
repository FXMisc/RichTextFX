package org.fxmisc.richtext.demo;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyleSpans;
import org.fxmisc.richtext.StyleSpansBuilder;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class XMLEditor extends Application {
	
    private static final Pattern XML_TAG = Pattern.compile("(?<ELEMENT>(</?)(\\w+)([^<>]*)(/?>))"
    		+"|(?<COMMENT><!--[^<>]+-->)");
    
    private static final Pattern ATTRIBUTES = Pattern.compile("(\\w+\\h*)(=)(\\h*\"[^\"]+\")");

    private static final String sampleCode = String.join("\n", new String[] {
    		"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>",
    		"<!-- Sample XML -->",
    		"<orders>",
    		"	<Order number=\"1\" table=\"center\">",
    		"		<items>",
    		"			<Item>",
    		"				<type>ESPRESSO</type>",
    		"				<shots>2</shots>",
    		"				<iced>false</iced>",
    		"				<orderNumber>1</orderNumber>",
    		"			</Item>",
    		"			<Item>",
    		"				<type>CAPPUCCINO</type>",
    		"				<shots>1</shots>",
    		"				<iced>false</iced>",
    		"				<orderNumber>1</orderNumber>",
    		"			</Item>",
    		"			<Item>",
    		"			<type>LATTE</type>",
    		"				<shots>2</shots>",
    		"				<iced>false</iced>",
    		"				<orderNumber>1</orderNumber>",
    		"			</Item>",
    		"			<Item>",
    		"				<type>MOCHA</type>",
    		"				<shots>3</shots>",
    		"				<iced>true</iced>",
    		"				<orderNumber>1</orderNumber>",
    		"			</Item>",
    		"		</items>",
    		"	</Order>",
    		"</orders>"
    		});


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        CodeArea codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            codeArea.setStyleSpans(0, computeHighlighting(newText));
        });
        codeArea.replaceText(0, 0, sampleCode);

        Scene scene = new Scene(new StackPane(codeArea), 600, 400);
        scene.getStylesheets().add(JavaKeywordsAsync.class.getResource("xml-highlighting.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("XML Editor Demo");
        primaryStage.show();
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
    	
        Matcher matcher = XML_TAG.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while(matcher.find()) {
        	
        	spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
        	if(matcher.group("COMMENT") != null) {
        		spansBuilder.add(Collections.singleton("comment"), matcher.end() - matcher.start());
        	}
        	else {
        		if(matcher.group("ELEMENT") != null) {
        			String openBracket = matcher.group(2);
        			String elementName = matcher.group(3);
        			String attributesText = matcher.group(4);
        			String closeBracket = matcher.group(5);
        			
        			spansBuilder.add(Collections.singleton("tagmark"), openBracket.length());
        			spansBuilder.add(Collections.singleton("anytag"), elementName.length());
        			if(!attributesText.isEmpty()) {
        				
        				int lastAttrEnd = 0;
        				
        				Matcher amatcher = ATTRIBUTES.matcher(attributesText);
        				while(amatcher.find()) {
        					spansBuilder.add(Collections.emptyList(), amatcher.start() - lastAttrEnd);
        					spansBuilder.add(Collections.singleton("attribute"), amatcher.group(1).length());
        					spansBuilder.add(Collections.singleton("tagmark"), amatcher.group(2).length());
        					spansBuilder.add(Collections.singleton("avalue"), amatcher.group(3).length());
        					lastAttrEnd = amatcher.end();
        				}
        			}
        			
        			spansBuilder.add(Collections.singleton("tagmark"), closeBracket.length());
        		}
        	}
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
