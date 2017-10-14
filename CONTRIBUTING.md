
## Issue Reporting

- Provide your environment info (OS, RTFX version, Java version)
- If issue is a bug:
 - at the very least, provide copy-and-paste code as an Application that reproduces bug/issue
 - at the very best, provide copy-and-paste code as a TestFX test that reproduces the bug/issue when run
- If issue is a question, explain your use case (the why) and the desired outcome and (if applicable) a photo showing that (the goal). We'll discuss the implementation (the how) in the issue.

## PR guidelines for demo package

- Modulate the code to multiple classes (if things cannot be set up in `Application#start`, helper methods, or code would be better understood as a separate class).
- Add the suffix "Demo" to the demo class that extends `Application` (e.g. `RichTextDemo`), so that people know which class is the demo and which classes are things used eventually in the demo
- If demo is a single class, put it in topmost package (e.g. `org.fxmisc.richtext.demo`)
- If demo requires multiple classes, put it and the classes it uses into its own package (e.g. `org.fxmisc.richtext.demo.mydemo`).

## Merging pull requests

Commiters can merge pull requests. If you are a committer, please follow these (rather informal) rules when merging a pull request:

 - When a pull request is a no brainer (by your best judgment), go ahead and merge it.
 - When in doubt, seek an opinion of at least one more committer and work with the proponent of the PR until all involved committers (including you) give a :+1:.


## List of committers

 - [TomasMikula](https://github.com/TomasMikula)
 - [JordanMartinez](https://github.com/JordanMartinez)
 - [afester](https://github.com/afester)
