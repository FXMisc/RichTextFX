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

Links
-----

[API Documentation](http://tomasmikula.github.io/CodeAreaFX/)  
[Downloads](https://googledrive.com/host/0B4a5AnNnZhkbZ3dRam5ONHJGOHM/downloads/)
