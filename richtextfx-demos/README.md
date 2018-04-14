Demos
-----

## Run using the pre-built JAR
[Download the pre-built "Fat" Demo Jar]() and run

    java -cp richtextfx-demos-fat-0.9.0.jar org.fxmisc.richtext.demo.richtext.[demo name]

## Demos

* [Rich-text editor](#rich-text-editor)
* [Highlighting of Java keywords](#automatic-highlighting-of-java-keywords)
* [XML Editor](#xml-editor)
* [Custom tooltips](#custom-tooltips)

### Rich-text editor

![Screenshot of the RichText demo](https://cloud.githubusercontent.com/assets/8413037/24158984/22d36a10-0e1b-11e7-95e0-f4546cb528c3.png)

#### Run using the pre-built JAR
[Download](https://github.com/FXMisc/RichTextFX/releases/download/v0.9.0/richtextfx-demos-fat-0.9.0.jar) the pre-built "fat" JAR file and run

    java -cp richtextfx-demos-fat-0.9.0.jar org.fxmisc.richtext.demo.richtext.RichText

#### Run from the source repo

    gradle RichText

#### Source code

[RichText.java](https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/richtext/RichText.java)

### Automatic highlighting of Java keywords

![Screenshot of the JavaKeywords demo](https://cloud.githubusercontent.com/assets/8413037/24158979/1ef7af14-0e1b-11e7-8c06-69cb9e5a2dd7.png)

#### Run using the pre-built JAR

[Download](https://github.com/FXMisc/RichTextFX/releases/download/v0.9.0/richtextfx-demos-fat-0.9.0.jar) the pre-built "fat" JAR file and run

    java -cp richtextfx-demos-fat-0.9.0.jar org.fxmisc.richtext.demo.JavaKeywords

or

    java -cp richtextfx-demos-fat-0.9.0.jar org.fxmisc.richtext.demo.JavaKeywordsAsync

#### Run from the source repo

    gradle JavaKeywords

or

    gradle JavaKeywordsAsync

#### Source code

[JavaKeywords.java](https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/JavaKeywords.java)

[JavaKeywordsAsync.java](https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/JavaKeywordsAsync.java)

The former computes highlighting on the JavaFX application thread, while the latter computes highlighting on a background thread.


### XML Editor

Similar to the [Java Keywords](#automatic-highlighting-of-java-keywords) demo above, this demo highlights XML syntax. Courtesy of @cemartins.

#### Run using the pre-built JAR

[Download](https://github.com/FXMisc/RichTextFX/releases/download/v0.9.0/richtextfx-demos-fat-0.9.0.jar) the pre-built "fat" JAR file and run

    java -cp richtextfx-demos-fat-0.9.0.jar org.fxmisc.richtext.demo.XMLEditor

#### Run from the source repo

    gradle XMLEditor

#### Source code

[XMLEditor.java](https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/XMLEditor.java)

### Custom tooltips

When the mouse pauses over the text area, you can get index of the character under the mouse. This allows you to implement, for example, custom tooltips whose content depends on the text under the mouse.

![Screenshot of the RichText demo](https://cloud.githubusercontent.com/assets/8413037/24158992/2741225e-0e1b-11e7-9d6b-6040dc30cee1.png)

#### Run using the pre-built JAR
[Download](https://github.com/FXMisc/RichTextFX/releases/download/v0.9.0/richtextfx-demos-fat-0.9.0.jar) the pre-built "fat" JAR file and run

    java -cp richtextfx-demos-fat-0.9.0.jar org.fxmisc.richtext.demo.TooltipDemo

#### Run from the source repo

    gradle TooltipDemo

#### Source code

[TooltipDemo.java](https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/TooltipDemo.java)
