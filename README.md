
RichTextFX
==========

RichTextFX provides a memory-efficient text area for JavaFX that allows the developer to style ranges of text, display custom objects in-line (no more HTMLEditor), and override the default behavior only where necessary without overriding any other part of the behavior.

It does not follow the MVC paradigm as this prevented access to view-specific API (e.g., getting the bounds of the caret/selection/characters, scrolling by some amount, etc.).

It is intended as a base for rich-text editors and code editors with syntax highlighting. Since it is a base, a number of suggested features (specific syntax highlighters, search-and-replace, specific support for hyperlinks, etc.) will not be implemented directly in this project. Rather, developers can implement these on top of RichTextFX and submit their work as a PR to the `richtextfx-demos` package.

For a greater explanation of RichTextFX, its design principles, how it works, and how to style its areas via CSS, please [see the wiki](https://github.com/TomasMikula/RichTextFX/wiki)

Demos
-----

Stand-alone Applications that demonstrate some of the features of RichTextFX have been moved to their own folder [here](./richtextfx-demos/)

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
  * [0.10.0](http://fxmisc.github.io/richtext/javadoc/0.9.1/org/fxmisc/richtext/package-summary.html)
* [License](#license)
* [Contributing](./CONTRIBUTING.md)


Who uses RichTextFX?
--------------------

- [Kappa IDE](https://bitbucket.org/TomasMikula/kappaide/)
- [Squirrel SQL client](http://www.squirrelsql.org/) (its JavaFX version)
- [mqtt-spy](http://kamilfb.github.io/mqtt-spy/)
- [Alt.Text](http://alttexting.com/)
- [Xanthic](https://github.com/jrguenther/Xanthic)
- [Arduino Harp](https://www.youtube.com/watch?v=rv5raLcsPNs)
- [Markdown Writer FX](https://github.com/JFormDesigner/markdown-writer-fx)
- [OmniEditor](https://github.com/giancosta86/OmniEditor), which is then used by [Chronos IDE](https://github.com/giancosta86/Chronos-IDE)
- [JuliarFuture](https://juliar.org)
- [BlueJ](https://www.bluej.org/)
- [JabRef](http://www.jabref.org/)
- [PsychSQL](http://softbydoc.dx.am/?page_id=135)
- [Greenfoot](https://www.greenfoot.org/)
- [Everest REST client](https://github.com/RohitAwate/Everest)
- [XR3Player](https://github.com/goxr3plus/XR3Player)
- [JDialogue](https://github.com/SkyAphid/JDialogue)
- [George](http://www.george.andante.no)
- [Nearde IDE](https://github.com/VenityStudio/Nearde-IDE)
- [Astro IDE](https://github.com/AmrDeveloper/Astro)

If you use RichTextFX in an interesting project, I would like to know!


Features
--------

* Assign arbitrary styles to arbitrary ranges of text. A style can be an object, a CSS string, or a style class string.
* Display line numbers or, more generally, any graphic in front of each paragraph. Can be used to show breakpoint toggles on each line of code.
* Support for displaying other `Node`s in-line
* Positioning a popup window relative to the caret or selection. Useful e.g. to position an autocompletion box.
* Getting the character index under the mouse when the mouse stays still over the text for a specified period of time. Useful for displaying tooltips depending on the word under the mouse.
* Overriding the default behavior only where necessary without overriding any other part.


Flavors
-------

The following explains the different rich text area classes. The first one is the base class from which all others extend: it needs further customization before it can be used but provides all aspects of the project's features. The later ones extend this base class in various ways to provide out-of-box functionality for specific use cases. **Most will use one of these subclasses.**

### GenericStyledArea

`GenericStyledArea` allows one to inline custom objects into the area alongside of text. As such, it uses generics and functional programming to accomplish this task in a completely type-safe way.

It has three parameter types:
 - `PS`, the paragraph style. This can be used for text alignment or setting the background color for the entire paragraph. A paragraph is either one line when text wrap is off or a long text displayed over multiple lines in a narrow viewport when text wrap is on,
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

`StyleClassedTextArea` uses the `Node#setStyleClass(String styleClass) method to style `Text` objects. You can define the style classes in your stylesheet.

example.css:

```css
.red { -fx-fill: red; }
```

Example.java:

```java
area.setStyleClass(from, to, "red");
```

This renders the text in the range `[from, to)` in red.

#### CodeArea

`CodeArea` is a variant of `StyleClassedTextArea` that uses a fixed width font by default, making it a convenient base for source code editors. `CodeArea` is used in the [Java Keywords demo](richtextfx-demos/README.md#automatic-highlighting-of-java-keywords).

Requirements
------------

[JDK8](https://jdk8.java.net/download.html) is required, because [TextFlow](http://download.java.net/jdk8/jfxdocs/javafx/scene/text/TextFlow.html), introduced in JavaFX 8.0, is used to render each line. Also, there's a heavy use of lambdas, defender methods and the stream API in the code base.

Download
--------

### Stable release

Current stable release is 0.10.0 which is a multi-release JAR that is compatible with Java 9 and UP without the need for `add-exports` or `add-opens` JVM arguments. 

#### Maven coordinates

| Group ID            | Artifact ID | Version |
| :-----------------: | :---------: | :-----: |
| org.fxmisc.richtext | richtextfx  | 0.10.0  |

#### Gradle example

```groovy
dependencies {
    compile group: 'org.fxmisc.richtext', name: 'richtextfx', version: '0.10.0'
}
```

#### Sbt example

```scala
libraryDependencies += "org.fxmisc.richtext" % "richtextfx" % "0.10.0"
```

#### Manual download

Download [the JAR file](https://github.com/TomasMikula/RichTextFX/releases/download/v0.10.0/richtextfx-0.10.0.jar) or [the fat JAR file (including dependencies)](https://github.com/TomasMikula/RichTextFX/releases/download/v0.10.0/richtextfx-fat-0.10.0.jar) and place it on your classpath.

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
