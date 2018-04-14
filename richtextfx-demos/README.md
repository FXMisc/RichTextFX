Demos
-----

### Table of Contents

* [Instructions for Running Demos](#instructions-for-running-demos)
* [Structure of the Demos package](#structure-of-the-demos-package)
* All Demos
  * [Rich-text editor](#rich-text-editor)
  * [Highlighting of Java keywords](#automatic-highlighting-of-java-keywords)
  * [XML Editor](#xml-editor)
  * [Custom tooltips](#custom-tooltips)

### Instructions for running demos

1. Clone the repository: `git clone https://www.github.com/FXMisc/RichTextFX.git`
2. Checkout the latest release version: `git checkout v0.9.0`
3. See the list of demos using a gradle task `./gradlew demos`
4. Run a demo using a gradle task: `./gradlew [Demo Name]`

(For Windows users, replace `./gradlew` with `gradlew.bat` in the above commands)

### Structure of the Demos package

- Each runnable demo class that extends `Application` ends in the suffix: "Demo"
- If a demo requires additional classes that should not be nested, the demo and its helper classes appear in their own package named after the demo (e.g. `RichTextDemo` is found in the `richtext` package)

### Rich-text editor

![Screenshot of the RichText demo](https://cloud.githubusercontent.com/assets/8413037/24158984/22d36a10-0e1b-11e7-95e0-f4546cb528c3.png)

#### Source code

[RichText.java](https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/richtext/RichText.java)

### Automatic highlighting of Java keywords

![Screenshot of the JavaKeywords demo](https://cloud.githubusercontent.com/assets/8413037/24158979/1ef7af14-0e1b-11e7-8c06-69cb9e5a2dd7.png)

#### Source code

[JavaKeywords.java](https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/JavaKeywords.java)

[JavaKeywordsAsync.java](https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/JavaKeywordsAsync.java)

The former computes highlighting on the JavaFX application thread, while the latter computes highlighting on a background thread.

### XML Editor

Similar to the [Java Keywords](#automatic-highlighting-of-java-keywords) demo above, this demo highlights XML syntax. Courtesy of @cemartins.

#### Source code

[XMLEditor.java](https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/XMLEditor.java)

### Custom tooltips

When the mouse pauses over the text area, you can get index of the character under the mouse. This allows you to implement, for example, custom tooltips whose content depends on the text under the mouse.

![Screenshot of the RichText demo](https://cloud.githubusercontent.com/assets/8413037/24158992/2741225e-0e1b-11e7-9d6b-6040dc30cee1.png)

#### Source code

[TooltipDemo.java](https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/TooltipDemo.java)
