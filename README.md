CodeAreaFX
==========

CodeAreaFX is a text area for JavaFX that supports assigning style classes for ranges of text. It is intended as a base for code editors with syntax highlighting.


Usage
-----

Example.java:

```java
codeArea.setStyleClass(from, to, "red");
```

example.css:

```css
.red { -fx-fill: red; }
```

This renders the text in the range `[from, to)` in red.


Requirements
------------

[JDK8](https://jdk8.java.net/download.html) is required, because [TextFlow](http://download.java.net/jdk8/jfxdocs/javafx/scene/text/TextFlow.html), introduced in JavaFX 8.0, is used to render each line.


Demos
-----

### 1. Automatic highlighting of Java keywords

![Screenshot of the JavaKeywords demo](https://googledrive.com/host/0B4a5AnNnZhkbYlVlbVprYnhPdVk/java-keywords.png)

#### Run using the pre-built JAR

[Download](https://googledrive.com/host/0B4a5AnNnZhkbZ3dRam5ONHJGOHM/downloads/) the pre-built JAR file and run

    java -cp codearea.jar codearea.demo.JavaKeywords

#### Run from the source repo

    ant JavaKeywords

#### Source code

[JavaKeywords.java](https://github.com/TomasMikula/CodeAreaFX/blob/master/src/demo/codearea/demo/JavaKeywords.java)


### 2. Manual text highlighting

![Screenshot of the ManualHighlighting demo](https://googledrive.com/host/0B4a5AnNnZhkbYlVlbVprYnhPdVk/manual-highlighting.png)

#### Run using the pre-built JAR
[Download](https://googledrive.com/host/0B4a5AnNnZhkbZ3dRam5ONHJGOHM/downloads/) the pre-built JAR file and run

    java -cp codearea.jar codearea.demo.ManualHighlighting

#### Run from the source repo

    ant ManualHighlighting

#### Source code

[ManualHighlighting.java](https://github.com/TomasMikula/CodeAreaFX/blob/master/src/demo/codearea/demo/ManualHighlighting.java)


License
-------

[GPLv2 with the Classpath Exception](http://openjdk.java.net/legal/gplv2+ce.html)


Links
-----

[API Documentation](http://tomasmikula.github.io/CodeAreaFX/)  
[Downloads](https://googledrive.com/host/0B4a5AnNnZhkbZ3dRam5ONHJGOHM/downloads/)
[Dropped Features](https://github.com/TomasMikula/CodeAreaFX/wiki/Dropped-Features)
[Known Issues](https://github.com/TomasMikula/CodeAreaFX/wiki/Known-Issues)
