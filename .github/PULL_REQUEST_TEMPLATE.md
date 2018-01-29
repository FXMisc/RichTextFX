## Guidelines for Pull Requests

### General Guidelines
- You are either submitting a pull request to the RichTextFX project or submitting a new demo for its demo package. See the corresponding guidelines.
- State which issue this fixes/addresses/resolves (e.g. "Fixes #0" or "Resolves #0"). If such an issue doesn't exist yet, open a new issue first.
- Each PR should resolve only one issue. Do not fix multiple things in a single request; rather open multiple requests that each fix one issue.
- State any concerns you have with your current approach, any requests for comments or feedback on specific points, etc.

## Guidelines for contributing to RichTextFX

### Documentation Guidelines
The following expectations must be met before a PR will be merged:

- Do not document self-explanatory things unless one must be aware of some issues that may arise with using it incorrectly.
  - Do not document getters and setters. We know what they do.
  - `moveCaretToFirstLine()` explains what it does. However, if it has a side-effect of scrolling the viewport, then it should be documented
- Document private classes/methods if it would take us more than 2 minutes to understand the code 6 months after it was merged.
- Document all non-self-explanatory public classes:
  - Why does the class exist?
  - What other classes relate to it and its usage?
- Document all non-self-explanatory public methods:
  - What does it do?
  - What side effects does it have (if any)?
  - What is one example of its usage?

### Testing Guidelines
Each PR that changes the code may need to include tests (if they aren't already covered) to insure that such changes do not affect the stability of the project. The following expectations must be met before a PR will be merged:

- Things that should go in the `src/test` package
  - tests which do not require starting or running on the JavaFX Application Thread
  - unit tests (e.g. tests on model-related classes/API)
- Things that should go in the `src/integrationTest` package
  - tests which require starting and running on the JavaFX Application Thread
  - integration tests
  - behavior tests
  - API tests
- Test Structure
  - Use nested classes to reduce code duplication (see the integrationTest package's tests as an example)
  - method names should be snake case (e.g. `moving_caret_to_first_line_works()`)
  - test one feature/API per test method


## Guidelines for contributing to RichTextFX's demo package

- Modulate the code:
  - Start creating a single class that extends `Application`.
  - Name the class something related to the thing it illustrates and ends the name with "Demo" (e.g. `XMLHighlightingDemo`)
  - Put all of the demo code into `Application#start`
  - If the required code cannot fit into `Application#start` method or should be broken up, create helper methods with clear names (e.g. `computeHighlighting()`) in the demo class
  - Do not use nested classes: put support classes into their own separate files
- If the demo is a single class, put it in topmost package (e.g. `org.fxmisc.richtext.demo`)
- If the demo requires multiple classes,
  - Put the demo and its classes into their own appropriately-named package (e.g. `org.fxmisc.richtext.demo.mydemo`).
  - Write a brief class-level javadoc on each class that explains what it does



