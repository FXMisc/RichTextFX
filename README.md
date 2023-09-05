
RichTextFX  
==========

RichTextFX provides a memory-efficient text area for JavaFX that allows the developer to style ranges of text, display custom objects in-line (no more `HTMLEditor`), and override specific default behaviors only where necessary.

The library does not follow the model-view-controller paradigm, which prevents access to view-specific APIs (e.g., obtaining caret/selection/character bounds, programmic scrolling, and such).

Use the library as a foundation for building rich text editors and code editors that offer syntax highlighting. Being a foundation, many features will not be implemented in this project (such as language-specific syntax highlighters, search-and-replace, hyperlink support, and similar). Rather, developers may implement these features then submit the work as a PR to the `richtextfx-demos` package.

For further details about RichTextFX, its design principles, how it works, and applying CSS styling, [see the wiki](https://github.com/FXMisc/RichTextFX/wiki).

[![JFXCentral](https://img.shields.io/badge/Find_me_on-JFXCentral-blue?logo=googlechrome&logoColor=white)](https://www.jfx-central.com/libraries/richtextfx)

Demos
-----

Standalone applications that demonstrate RichTextFX features may be found in the [RichTextFX demos](./richtextfx-demos/) directory.

Table of Contents
-----------------

* [Who uses RichTextFX?](#who-uses-richtextfx)
* [Features](#features)
* [Flavors](#flavors)
  * [GenericStyledArea (Base area class requiring customization)](#genericstyledarea)
  * [StyledTextArea (Areas ready out-of-box)](#styledtextarea)
     * [InlineCssTextArea](#inlinecsstextarea)
     * [StyleClassedTextArea](#styleclassedtextarea)
         * [CodeArea (Base for code editors)](#codearea)
* [Requirements](#requirements)
* [Download](#download)
  * [Stable](#stable-release)
  * [Snapshot](#snapshot-releases)
* API Documentation (Javadoc)
  * [0.11.1](http://fxmisc.github.io/richtext/javadoc/0.11.1/index.html?org/fxmisc/richtext/package-summary.html)
* [License](#license)
* [Contributing](./CONTRIBUTING.md)


Who uses RichTextFX?
--------------------

- [Arduino Harp](https://www.youtube.com/watch?v=rv5raLcsPNs)
- [Astro IDE](https://github.com/AmrDeveloper/Astro) 
- [BasicCAT](https://github.com/xulihang/BasicCAT/)
- [BlueJ](https://www.bluej.org/)
- [Boomega](https://github.com/Dansoftowner/Boomega)
- [Chorus](https://github.com/iAmGio/chorus)
- [Chronos IDE](https://github.com/giancosta86/Chronos-IDE)
- [George](http://www.george.andante.no)
- [Greenfoot](https://www.greenfoot.org/)
- [EpubFx](https://gitlab.com/finanzer/epubfx/)
- [Everest REST client](https://github.com/RohitAwate/Everest)
- [JabRef](http://www.jabref.org/)
- [JfxIDE](https://github.com/Zev-G/JfxIDE)
- [JFXDevTools](https://github.com/Zev-G/JFXDevTools)
- [JDialogue](https://github.com/SkyAphid/JDialogue)
- [JuliarFuture](https://juliar.org)
- [JVM Explorer](https://github.com/Naton1/jvm-explorer)
- [Kappa IDE](https://bitbucket.org/TomasMikula/kappaide/)
- [KeenWrite](https://github.com/DaveJarvis/keenwrite)
- [Markdown Writer FX](https://github.com/JFormDesigner/markdown-writer-fx)
- [mqtt-spy](http://kamilfb.github.io/mqtt-spy/)
- [Nearde IDE](https://github.com/VenityStudio/Nearde-IDE)
- [OmniEditor](https://github.com/giancosta86/OmniEditor)
- [Recaf](https://github.com/Col-E/Recaf)
- [SqlBrowserFx](https://github.com/pariskol/sqlbrowserfx/)
- [Squirrel SQL client](http://www.squirrelsql.org/)
- [Xanthic](https://github.com/jrguenther/Xanthic)
- [XR3Player](https://github.com/goxr3plus/XR3Player)

Let us know if you use RichTextFX in your project!

Features
--------

* Assign arbitrary styles to arbitrary ranges of text. A style can be an object, a CSS string, or a style class string.
* Display line numbers or, more generally, any graphic in front of each paragraph. Can be used to show breakpoint toggles on each line of code.
* Support for displaying other `Node`s in-line.
* Positioning a popup window relative to the caret or selection. Useful e.g. to position an autocompletion box.
* Getting the character index under the mouse when the mouse stays still over the text for a specified period of time. Useful for displaying tooltips depending on the word under the mouse.
* Overriding the default behavior only where necessary without overriding any other part.


Flavors
-------

The following explains the different rich text area classes. The first one is the base class from which all others extend: it needs further customization before it can be used but provides all aspects of the project's features. The later ones extend this base class in various ways to provide out-of-box functionality for specific use cases. **Most will use one of these subclasses.**

### GenericStyledArea

`GenericStyledArea` allows one to inline custom objects into the area alongside of text. As such, it uses generics and functional programming to accomplish this task in a completely type-safe way.

It has the following parameter types:

 - `PS`, the paragraph style. This can be used for text alignment or setting the background color for the entire paragraph. A paragraph is either one line when text wrap is off or a long text displayed over multiple lines in a narrow viewport when text wrap is on.
 - `SEG`, the segment object. This specifies what immutable object to store in the model part of the area: text, hyperlinks, images, emojis, or any combination thereof.
 - `S`, the segment style. This can be used for text and object styling. Usually, this will be a CSS style or CSS style class.

Functional programming via lambdas specify how to apply styles, how to create a `Node` for a given segment, and how to operate on a given segment (e.g., getting its length, combining it with another segment, etc.).

`GenericStyledArea` is used in the [Rich-text demo](richtextfx-demos/README.md#rich-text-editor).

See the wiki for a basic pattern that one must follow to implement custom objects correctly.

### StyledTextArea

`StyledTextArea<PS, S>`, or one of its subclasses below, is the area you will most likely use if you don't need to display custom objects in your area.

It extends `GenericStyledArea<PS, StyledText<S>, S>>`. `StyledText` is simply a text (`String`) and a style object (`S`). A slightly-enhanced [JavaFX `Text`](https://docs.oracle.com/javase/8/javafx/api/javafx/scene/text/Text.html) node is used to display the `StyledText<S>`, so you can style it using [its CSS properties](https://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html#text) and additional RichTextFX-specific CSS (see the wiki for more details).

It properly handles the aforementioned functional programming to properly display and operate on `StyledText<S>` objects.

The style object (`S`) can either be a CSS String (`-fx-fill: red;`), a CSS styleclass (`.red { -fx-fill: red; }`), or an object that handles this in a different way. Since most will use either the CSS String or CSS style class approach, there are two subclasses that already handle this correctly.

### InlineCssTextArea

`InlineCssTextArea` uses the `Node#setStyle(String cssStyle)` method to style `Text` objects:

```java
area.setStyle(from, to, "-fx-font-weight: bold;");
```

### StyleClassedTextArea

`StyleClassedTextArea` uses the `Node#setStyleClass(String styleClass)` method to style `Text` objects. You can define the style classes in your stylesheet.

example.css:

```css
.red { -fx-fill: red; }
```

Example.java:

```java
area.setStyleClass(from, to, "red");
```

This renders the text in the range `(from, to)` in red.

#### CodeArea

`CodeArea` is a variant of `StyleClassedTextArea` that uses a fixed width font by default, making it a convenient base for source code editors. `CodeArea` is used in the [Java Keywords demo](richtextfx-demos/README.md#automatic-highlighting-of-java-keywords).

Requirements
------------

**JDK11** or higher is required. (Can still be compiled with JDK9 if needed)

Download
--------

### Stable release

Current stable release is 0.11.1. which is ONLY compatible with Java 11 and UP without the need for `add-exports` or `add-opens` JVM arguments. 

#### Maven coordinates

| Group ID            | Artifact ID | Version |
| :-----------------: | :---------: | :-----: |
| org.fxmisc.richtext | richtextfx  | 0.11.1  |

#### Gradle example

```groovy
dependencies {
    compile group: 'org.fxmisc.richtext', name: 'richtextfx', version: '0.11.1'
}
```

#### Sbt example

```scala
libraryDependencies += "org.fxmisc.richtext" % "richtextfx" % "0.11.1"
```

#### Manual download

Download [the JAR file](https://github.com/FXMisc/RichTextFX/releases/download/v0.11.1/richtextfx-0.11.1.jar) or [the fat JAR file (including dependencies)](https://github.com/FXMisc/RichTextFX/releases/download/v0.11.1/richtextfx-fat-0.11.1.jar) and place it on your classpath.

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

