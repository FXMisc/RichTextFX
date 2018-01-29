## Read these guidelines first, write your issue, and only then submit it.

Click the "Preview" tab to read this issue template in a clearer way.

Determine what kind of issue this is (bug report, feature request, or a question) and follow the corresponding guidelines below

### Guidelines For Bug Reports

- Check whether this issue has already been reported
- Use the first set of headers to structure what you write (`expected behavior`, `actual behavior`, `reproducible demo`, and `environment info`) and delete the other parts of this issue template
- Use a clear title that summarizes the problem

### Guidelines For Feature Requests

- Use a clear title that starts with words like "Add", "Support", etc.
- Use the second set of headers to structure what you write (`Desired Feature`, `Current Workaroudns`, and `Implementation Costs`) and delete the other parts of this issue template (e.g. question & feature request guidelines)

### Guidelines For Questions

- Explain whether this is a question about the API, best practices, feedback on some approach to solving a problem, etc.
- Use a clear title that either starts with "Question" or ends with a "?" to signify that this is a question.
  - Example: "Question: syntax highlighting best practices"
  - Example: "What is the best way to do syntax highlighting?"
- If asking for feedback on some approach to solving a problem, explain your goal and your constraints in reaching that goal before explaining what you are trying to do to solve that problem
- Once the above has been done, delete the rest of this issue template and feel free to structure your issue in whatever way works best for you.

<hr>

## Expected Behavior

Describe what should be occurring when you use some method or the end-user does some behavior

## Actual Behavior

Describe what actually occurs when you use some method or the end-user does some behavior

## Reproducible Demo

Provide a demo that maintainers of this project can copy, paste, and run to reproduce it immediately.

Use the following template to get started.
````java
public class Bug extends Application {

 public void start(Stage primaryStage) {

  primaryStage.show();
 }

}
````

## Environment info:

- RichTextFX Version: \<version\>
- Operating System: \<my OS\>
- Java version: \<version\>

<hr>

## Desired Feature

Explain what you want RichTextFX or some component within it to be able to do and why

## Current Workarounds

Explain whether the feature can be done right now or not. If it can, how much one would need to hack at the code to produce the desired result? Is such a hack inconvenient but possible, difficult and troublesome, or impossible in the current version?

## Implementation Costs

To the best of your knowledge...

- What would need to be changed to support the feature (e.g. accessibility changes, type refactoring, splitting up a method into multiple methods, etc.)?
- What problems or issues would need to be resolved first (if any)?
- What problems might this feature create (e.g. compatibility issues, inconvenience upon some developers, etc.)