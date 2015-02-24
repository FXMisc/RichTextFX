RichTextFX
==========

RichTextFX provides a text area for JavaFX with API to style ranges of text. It is intended as a base for rich-text editors and code editors with syntax highlighting.

Scroll down to [Demos](#demos) to see it in action.

There is plenty of room for enhancements, e.g. support for paragraph-level styles (#6), placing arbitrary nodes in the text (#87), copy/paste including style information (#17), ... To give me or someone else more incentive to implement these or other features, you can [create a bounty](https://www.bountysource.com/).


Who uses RichTextFX?
--------------------

[Kappa IDE](https://bitbucket.org/TomasMikula/kappaide/)  
[Squirrel SQL client](http://www.squirrelsql.org/) (its JavaFX version)  
[mqtt-spy](https://code.google.com/p/mqtt-spy/)  
[Alt.Text](http://alttexting.com/)  
[Xanthic](https://github.com/jrguenther/Xanthic)  
[Arduino Harp](http://www.avrharp.org/)  

Please, let me know if you use RichTextFX in your project and would like it to be listed here.


Features
--------

* Assign arbitrary styles to arbitrary ranges of text.
* Display line numbers or, more generally, any graphic in front of each paragraph. Can be used to show breakpoint toggles on each line of code.
* Positioning a popup window relative to the caret or selection. Useful e.g. to position an autocompletion box.
* Getting the character index under the mouse when the mouse stays still over the text for a specified period of time. Useful for displaying tooltips depending on the word under the mouse.


Flavors
-------

### StyleClassedTextArea

`StyleClassedTextArea` lets you assign style classes to ranges of text. You can define the style classes in your stylesheet.

Example.java:

```java
area.setStyleClass(from, to, "red");
```

example.css:

```css
.red { -fx-fill: red; }
```

This renders the text in the range `[from, to)` in red.

Note that the style classes are assigned to instances of [Text](http://download.java.net/jdk8/jfxdocs/javafx/scene/text/Text.html), so you can specify any [CSS properties applicable to a Text node](http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html#text).

#### CodeArea

`CodeArea` is a variant of `StyleClassedTextArea` that uses a fixed width font by default, making it a convenient base for source code editors. `CodeArea` is used in the [Java Keywords demo](#1-automatic-highlighting-of-java-keywords) below.


### InlineCssTextArea

`InlineCssTextArea` lets you specify inline CSS for a range of text.

```java
area.setStyle(from, to, "-fx-font-weight: bold;");
```

Again, you can use any CSS properties applicable to a `Text` node.


### InlineStyleTextArea

`InlineStyleTextArea<S>` is a more general version of `InlineCssTextArea`. In the end, there is still inline CSS assigned to `Text` nodes, but instead of using the CSS string directly, you use an instance of your custom style representation `S` and provide a way (function) to convert `S` to CSS string in `InlineStyleTextArea` constructor.

```java
class MyStyleInfo {
    boolean bold;
    boolean italic;
    
    String toCss() {
        return "-fx-font-weight: " + (bold ? "bold" : "normal") + ";"
               + "-fx-font-style: " + (italic ? "italic" : "normal") + ";";
    }
}

InlineStyleTextArea<MyStyleInfo> area =
        new InlineStyleTextArea<>(new MyStyleInfo(), styleInfo -> styleInfo.toCss());
```

The first constructor argument is the default style to use for ranges of text where you don't set the style explicitly. The second constructor argument is the function to convert the custom style representation to CSS.

You then assign an instance of your custom style representation to a range of text.

```java
MyStyleInfo styleInfo = ...;

area.setStyle(from, to, styleInfo);
```

You appreciate the benefits of this approach over `InlineCssTextArea` when you need to query the style used at a given position in text - you get back an instance of your style representation instead of a CSS string.

```java
MyStyleInfo styleInfo = area.getStyleAt(charIndex);
```

`InlineStyleTextArea` is used in the [Rich-text demo](#2-rich-text-editor) below.


Requirements
------------

[JDK8](https://jdk8.java.net/download.html) is required, because [TextFlow](http://download.java.net/jdk8/jfxdocs/javafx/scene/text/TextFlow.html), introduced in JavaFX 8.0, is used to render each line. Also, there's a heavy use of lambdas, defender methods and the stream API in the code base.


Demos
-----

### Automatic highlighting of Java keywords

![Screenshot of the JavaKeywords demo](https://googledrive.com/host/0B4a5AnNnZhkbYlVlbVprYnhPdVk/java-keywords.png)

#### Run using the pre-built JAR

[Download](https://github.com/TomasMikula/RichTextFX/releases/download/v0.6.2/richtextfx-demos-fat-0.6.2.jar) the pre-built "fat" JAR file and run

    java -cp richtextfx-demos-fat-0.6.2.jar org.fxmisc.richtext.demo.JavaKeywords

or

    java -cp richtextfx-demos-fat-0.6.2.jar org.fxmisc.richtext.demo.JavaKeywordsAsync

#### Run from the source repo

    gradle JavaKeywords

or

    gradle JavaKeywordsAsync

#### Source code

[JavaKeywords.java](https://github.com/TomasMikula/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/JavaKeywords.java)

[JavaKeywordsAsync.java](https://github.com/TomasMikula/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/JavaKeywordsAsync.java)

The former computes highlighting on the JavaFX application thread, while the latter computes highlighting on a background thread.


### Rich-text editor

![Screenshot of the RichText demo](https://googledrive.com/host/0B4a5AnNnZhkbYlVlbVprYnhPdVk/rich-text.png)

#### Run using the pre-built JAR
[Download](https://github.com/TomasMikula/RichTextFX/releases/download/v0.6.2/richtextfx-demos-fat-0.6.2.jar) the pre-built "fat" JAR file and run

    java -cp richtextfx-demos-fat-0.6.2.jar org.fxmisc.richtext.demo.RichText

#### Run from the source repo

    gradle RichText

#### Source code

[RichText.java](https://github.com/TomasMikula/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/RichText.java)


### Custom tooltips

When the mouse pauses over the text area, you can get index of the character under the mouse. This allows you to implement, for example, custom tooltips whose content depends on the text under the mouse.

![Screenshot of the RichText demo](https://googledrive.com/host/0B4a5AnNnZhkbYlVlbVprYnhPdVk/tooltip-demo.png)

#### Run using the pre-built JAR
[Download](https://github.com/TomasMikula/RichTextFX/releases/download/v0.6.2/richtextfx-demos-fat-0.6.2.jar) the pre-built "fat" JAR file and run

    java -cp richtextfx-demos-fat-0.6.2.jar org.fxmisc.richtext.demo.TooltipDemo

#### Run from the source repo

    gradle TooltipDemo

#### Source code

[TooltipDemo.java](https://github.com/TomasMikula/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/TooltipDemo.java)


Use RichTextFX in your project
------------------------------

### Stable release

Current stable release is 0.6.2.

#### Maven coordinates

| Group ID            | Artifact ID | Version |
| :-----------------: | :---------: | :-----: |
| org.fxmisc.richtext | richtextfx  | 0.6.2   |

#### Gradle example

```groovy
dependencies {
    compile group: 'org.fxmisc.richtext', name: 'richtextfx', version: '0.6.2'
}
```

#### Sbt example

```scala
libraryDependencies += "org.fxmisc.richtext" % "richtextfx" % "0.6.2"
```

#### Manual download

Download [the JAR file](https://github.com/TomasMikula/RichTextFX/releases/download/v0.6.2/richtextfx-0.6.2.jar) or [the fat JAR file (including dependencies)](https://github.com/TomasMikula/RichTextFX/releases/download/v0.6.2/richtextfx-fat-0.6.2.jar) and place it on your classpath.


### Snapshot releases

Snapshot releases are deployed to Sonatype snapshot repository.

#### Maven coordinates

| Group ID            | Artifact ID | Version        |
| :-----------------: | :---------: | :------------: |
| org.fxmisc.richtext | richtextfx  | 1.0.0-SNAPSHOT |

#### Gradle example

```groovy
repositories {
    maven {
        url 'https://oss.sonatype.org/content/repositories/snapshots/' 
    }
}

dependencies {
    compile group: 'org.fxmisc.richtext', name: 'richtextfx', version: '1.0.0-SNAPSHOT'
}
```

#### Sbt example

```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "org.fxmisc.richtext" % "richtextfx" % "1.0.0-SNAPSHOT"
```


License
-------

Dual-licensed under [BSD 2-Clause License](http://opensource.org/licenses/BSD-2-Clause) and [GPLv2 with the Classpath Exception](http://openjdk.java.net/legal/gplv2+ce.html).


Links
-----

[API Documentation (Javadoc)](http://www.fxmisc.org/richtext/javadoc/org/fxmisc/richtext/package-summary.html)  
[Dropped Features](https://github.com/TomasMikula/RichTextFX/wiki/Dropped-Features)  
[Known Issues](https://github.com/TomasMikula/RichTextFX/wiki/Known-Issues)  
