# Change Log

## [v0.11.1](https://github.com/FXMisc/RichTextFX/tree/v0.11.1) (2023-08-18)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.11.0...v0.11.1)

**Implemented enhancements:**

- Bump Junit to 4.13.2
- Bumped Flowless to 0.7.1
- Added isFolded API [\#1156](https://github.com/FXMisc/RichTextFX/pull/1156)

**Fixed bugs:**

- Prevent pom from containing JavaFX dependencies [\#1155](https://github.com/FXMisc/RichTextFX/pull/1155)
- Fixed InputMethodRequest recursive call [\#1165](https://github.com/FXMisc/RichTextFX/pull/1165)
- Fixed prefHeight calc and layout for no wrap [\#1169](https://github.com/FXMisc/RichTextFX/pull/1169)

**Notes from Flowless 0.7.1:**

- Minor update with small change to SizeTracker, catching IOOB & NoSuchElement exceptions.

## [v0.11.0](https://github.com/FXMisc/RichTextFX/tree/v0.11.0) (2022-11-14)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.10.9...v0.11.0)

**Implemented enhancements:**

- Bumped Flowless to 0.7.0
- Compiled with Java 11 (Thank you [Andreas](https://github.com/afester))
- Remove Java 8 & 9 multi-jar compatibility [\#1148](https://github.com/FXMisc/RichTextFX/pull/1148)
- Support wavy underlines [\#1131](https://github.com/FXMisc/RichTextFX/pull/1131) (Thank you [shoaniki](https://github.com/shoaniki))

**Fixed bugs:**

- Fix for SceneBuilder compatibility [\#1112](https://github.com/FXMisc/RichTextFX/pull/1112)
- Fixed paragraph graphic node width calculation [\#1125](https://github.com/FXMisc/RichTextFX/pull/1125) (Thank you [RationalityFrontline](https://github.com/RationalityFrontline))
- Fixed multi line text calculation [\#1135](https://github.com/FXMisc/RichTextFX/pull/1135)
- Fixed code area unicode word selection [\#1139](https://github.com/FXMisc/RichTextFX/pull/1139)
- Fixed TextFlow children concurrency access [\#1142](https://github.com/FXMisc/RichTextFX/pull/1142)

**Notes from Flowless 0.7.0:**

- Fix first cell not showing sometimes [\#110](https://github.com/FXMisc/Flowless/pull/110)
- Take padding into account when scrolling [\#111](https://github.com/FXMisc/Flowless/pull/111)
- Fix for scrolling [\#112](https://github.com/FXMisc/Flowless/pull/112)
- Fix wrapped text scrollbar flicker [\#113](https://github.com/FXMisc/Flowless/pull/113)
- Removed scroll noise and improved bidirectional binding behavior [\#113](https://github.com/FXMisc/Flowless/pull/113)

## [v0.10.9](https://github.com/FXMisc/RichTextFX/tree/v0.10.9) (2022-03-01)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.10.8...v0.10.9)

**Implemented enhancements:**

- Bumped Flowless to 0.6.9
- Compiled with Java 9

**Fixed bugs:**

- Both of the following were fixed in Flowless:
- Bug: Fixed thin horizontal lines appear between lines of text when rapidly scrolling vertically [\#105](https://github.com/FXMisc/Flowless/pull/105)
- Bug: Fixed horizontal scrolling of an area can result in large empty blocks in the upper right [\#106](https://github.com/FXMisc/Flowless/pull/106)

## [v0.10.8](https://github.com/FXMisc/RichTextFX/tree/v0.10.8) (2022-02-28)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.10.7...v0.10.8)

**Implemented enhancements:**

- Added overwrite mode [\#1051](https://github.com/FXMisc/RichTextFX/pull/1051)
- Added showParagraphAtCenter [\#1054](https://github.com/FXMisc/RichTextFX/pull/1054)
- Add hasChanges to MultiChangeBuilder [\#1084](https://github.com/FXMisc/RichTextFX/pull/1084)
- Added nextLine & prevLine, behaves like nextPage & prevPage [\#1086](https://github.com/FXMisc/RichTextFX/pull/1086)
- Changed code area navigation to behave more like a code editor [\#1090](https://github.com/FXMisc/RichTextFX/pull/1090)

**Fixed bugs:**

- Bug: Fixed getCaretBounds exception [\#1049](https://github.com/FXMisc/RichTextFX/pull/1049)
- Bug: Fixed follow caret with selection update [\#1059](https://github.com/FXMisc/RichTextFX/pull/1059)
- Bug: Fixed line selection off not updating [\#1066](https://github.com/FXMisc/RichTextFX/pull/1066)
- Bug: Fixed selection shape [\#1067](https://github.com/FXMisc/RichTextFX/pull/1067)
- Bug: Fixed InputMethodRequest getTextLocation fix [\#1075](https://github.com/FXMisc/RichTextFX/pull/1075)
- Bug: Fixed getCharacterBoundsOnScreen when from == to [\#1076](https://github.com/FXMisc/RichTextFX/pull/1076)
- Bug: Fixed ParagraphBox not respecting the graphic node's managed property [\#1079](https://github.com/FXMisc/RichTextFX/pull/1079)
- Bug: Fixed wrapped lines get skipped at high DPI settings when navigating with up/down arrow keys. [\#1074](https://github.com/FXMisc/RichTextFX/pull/1074)
- Bug: Fixed linehighlighter off on selection [\#1085](https://github.com/FXMisc/RichTextFX/pull/1085)
- Bug: Fixed wordBreaksForward not using locale [\#1089](https://github.com/FXMisc/RichTextFX/pull/1089)
- Bug: Reverted Flowless back to 0.6.4 for Java 8 compatibility

## [v0.10.7](https://github.com/FXMisc/RichTextFX/tree/v0.10.7) (2021-10-26)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.10.6...v0.10.7)

**Implemented enhancements:**

- Added setPlaceholder( Node, Pos ) [\#1035](https://github.com/FXMisc/RichTextFX/pull/1035)
- Bumped Flowless to 0.6.7
- Bumped undofx to 2.1.1

**Fixed bugs:**

- Bug: Fixed caret disappear with large font [\#1017](https://github.com/FXMisc/RichTextFX/issues/1017)
- Bug: Fixed CodeArea w/ LineNumberFactory throws when replacing multi-line text with .replaceText() [\#1021](https://github.com/FXMisc/RichTextFX/issues/1021)
- Bug: Fixed CaretNode not being transparent to mouse picking [\#1032](https://github.com/FXMisc/RichTextFX/pull/1032) (Thank you [chrisf-london](https://github.com/chrisf-london))
- Bug: Fixed async demo syntax highlighting after file load [\#1045](https://github.com/FXMisc/RichTextFX/pull/1045) (Thank you [BorisSkegin](https://github.com/sirop))
- Bug: Fixed visibleParToAllParIndex parameter check [\#1022](https://github.com/FXMisc/RichTextFX/pull/1022)
- Bug: Fixed StyledTextField initial height [\#1037](https://github.com/FXMisc/RichTextFX/pull/1037)

## [v0.10.6](https://github.com/FXMisc/RichTextFX/tree/v0.10.6) (2021-03-15)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.10.5...v0.10.6)

**Implemented enhancements:**

- Accommodate selection when following caret [\#933](https://github.com/FXMisc/RichTextFX/pull/933)
- Added auto height/grow property and behavior [\#944](https://github.com/FXMisc/RichTextFX/pull/944)
- Added foreign language input composition [\#985](https://github.com/FXMisc/RichTextFX/pull/985) (Thank you [xulihang](https://github.com/xulihang))
- Added paragraph folding [\#965](https://github.com/FXMisc/RichTextFX/pull/965), [\#986](https://github.com/FXMisc/RichTextFX/pull/986), [\#1000](https://github.com/FXMisc/RichTextFX/pull/1000), and [\#1007](https://github.com/FXMisc/RichTextFX/pull/1007)
- Improved page up to go to first line when visible, and page down to go to last when visible [\#983](https://github.com/FXMisc/RichTextFX/pull/983)
- Improved syntax highlighting demo efficiency [\#956](https://github.com/FXMisc/RichTextFX/pull/956), [\#962](https://github.com/FXMisc/RichTextFX/pull/962), and [\#972](https://github.com/FXMisc/RichTextFX/pull/972)
- Improved hyperlinks demo with editable hyperlinks [\#995](https://github.com/FXMisc/RichTextFX/pull/995)
- Added bracket highlighter demo [\#959](https://github.com/FXMisc/RichTextFX/pull/959) (Thank you [Pratanu Mandal](https://github.com/prat-man))
- Updated Appveyor to use JDK9
- Bumped to Flowless 0.6.3
- Bumped testfx to 4.0.16

**Fixed bugs:**

- Bug: Fixed exception when trying to get paragraph bounds when caret moves [\#945](https://github.com/FXMisc/RichTextFX/pull/945)
- Bug: Fixed follow caret not showing complete line at bottom of viewport with large font [\#947](https://github.com/FXMisc/RichTextFX/pull/947)
- Bug: Fixed full line not being highlighted when text wrap is off [\#948](https://github.com/FXMisc/RichTextFX/pull/948), [\#963](https://github.com/FXMisc/RichTextFX/pull/963)
- Bug: Fixed incorrect first visible paragraph being returned when navigating backwards [\#1002](https://github.com/FXMisc/RichTextFX/pull/1002)
- Bug: Fixed line not highlighting after either the last character or the line was deleted [\#950](https://github.com/FXMisc/RichTextFX/issues/950)
- Bug: Fixed multi-paragraph insert creation [\#951](https://github.com/FXMisc/RichTextFX/pull/951) and [\#953](https://github.com/FXMisc/RichTextFX/pull/953)
- Bug: Fixed selecting text beyond the end leads to exception on Java 9 or later [\#992](https://github.com/FXMisc/RichTextFX/pull/992)
- Bug: Fixed demo ParStyle equals & indent [\#943](https://github.com/FXMisc/RichTextFX/pull/943)
- Regression: Fixed entering curly braces and brackets on OSX not working [\#968](https://github.com/FXMisc/RichTextFX/pull/968)

**Other:**

- Removed [travis.yml](https://github.com/FXMisc/RichTextFX/commit/358df2dcc5e404e1be22b2920277e5f6acd43328)

## [v0.10.5](https://github.com/FXMisc/RichTextFX/tree/v0.10.5) (2020-04-19)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.10.4...v0.10.5)

**Implemented enhancements:**

- Upgraded Gradle Maven plugin [\#910](https://github.com/FXMisc/RichTextFX/pull/910)
- Provide suspendable undo manager and test [\#914](https://github.com/FXMisc/RichTextFX/pull/914)
- Added getLocale and setLocale for BreakIterator use [\#920](https://github.com/FXMisc/RichTextFX/pull/920)
- Allow ALT and ALT + CONTROL (or ALTGR on Windows) accelerators [\#922](https://github.com/FXMisc/RichTextFX/pull/922)

**Fixed bugs:**

- Bug: Fixed SelectionImpl not honouring constructor range [\#907](https://github.com/FXMisc/RichTextFX/pull/907)
- Bug: Fixed RichTextChange reported by ReadOnlyStyledDocument replace [\#908](https://github.com/FXMisc/RichTextFX/pull/908)
- Bug: Fixed post undo/redo caret position [\#915](https://github.com/FXMisc/RichTextFX/pull/915)
- Bug: Fixed tests not on FX thread [\#917](https://github.com/FXMisc/RichTextFX/pull/917)
- Bug: Fixed scrollbar jump [\#918](https://github.com/FXMisc/RichTextFX/pull/918), also alternative fix for visibleParToAllParIndex crashing [\#777](https://github.com/FXMisc/RichTextFX/issues/777)
- Bug: Fixed focus lost on right click [\#921](https://github.com/FXMisc/RichTextFX/pull/921) (Thank you [gaeqs](https://github.com/gaeqs))

## [v0.10.4](https://github.com/FXMisc/RichTextFX/tree/v0.10.4) (2020-02-19)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.10.3...v0.10.4)

**Implemented enhancements:**

- Added convenience method to UndoUtils to create a NO OP undo manager [\#881](https://github.com/FXMisc/RichTextFX/issues/881)
- Added styled text fields [\#894](https://github.com/FXMisc/RichTextFX/pull/894), [\#895](https://github.com/FXMisc/RichTextFX/pull/895), [\#896](https://github.com/FXMisc/RichTextFX/pull/896) & [\#897](https://github.com/FXMisc/RichTextFX/pull/897)
- Added place holder to GenericStyledArea [\#899](https://github.com/FXMisc/RichTextFX/pull/899) [\#900](https://github.com/FXMisc/RichTextFX/pull/900)
- Added prompt text to StyledTextField [\#899](https://github.com/FXMisc/RichTextFX/pull/899)
- Added Automatic-Module-Name
- Bumped to Flowless 0.6.1

**Fixed bugs:**

- Bug: Fixed paragraph graphic creation if index is -1 [\#882](https://github.com/FXMisc/RichTextFX/pull/882)
- Bug: Fixed Java 9 code [\#887](https://github.com/FXMisc/RichTextFX/pull/887/files) and [\#888](https://github.com/FXMisc/RichTextFX/pull/888)
- Bug: Fixed memory leak in ParagraphText [\#893](https://github.com/FXMisc/RichTextFX/pull/893)

## [v0.10.3](https://github.com/FXMisc/RichTextFX/tree/v0.10.3) (2019-11-27)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.10.2...v0.10.3)

**Implemented enhancements:**

- Added shift backspace to delete backward [\#855](https://github.com/FXMisc/RichTextFX/issues/855)
- Added methods to recreate or get a paragraph graphic [\#854](https://github.com/FXMisc/RichTextFX/issues/854)
- Added methods to preset the style of inserted text and paragraphs [\#864](https://github.com/FXMisc/RichTextFX/issues/864)
- Added convenience API to EditActions and StyleClassedTextArea [\#868](https://github.com/FXMisc/RichTextFX/issues/868)
- Added Bullet Lists to RichTextDemo [\#826](https://github.com/FXMisc/RichTextFX/issues/826) 

**Fixed bugs:**

- Bug: Fixed line highlighter not adjusting to area's width changes [\#845](https://github.com/FXMisc/RichTextFX/pull/845#issuecomment-540535109)
- Bug: Fixed not highlighting from beginning of line after adding text when empty [\#845](https://github.com/FXMisc/RichTextFX/pull/845#issuecomment-552252829)
- Bug: Fixed line spacing not being applied between paragraphs [\#862](https://github.com/FXMisc/RichTextFX/issues/862)
- Bug: Fixed anchor properties not updating correctly with listeners [\#874](https://github.com/FXMisc/RichTextFX/issues/874) 

**Other:**

- Balanced linespacing above and below text for better highlighting look [\#872](https://github.com/FXMisc/RichTextFX/issues/872)
- Changed styled-text-area CSS [\#738](https://github.com/FXMisc/RichTextFX/issues/738#issuecomment-555592054) 

## [v0.10.2](https://github.com/FXMisc/RichTextFX/tree/v0.10.2) (2019-08-29)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.10.1...v0.10.2)

**Implemented enhancements:**

- Added line highlighter enhancement [\#845](https://github.com/FXMisc/RichTextFX/issues/845)
- Enhanced CodeArea selection to include underscore [\#837](https://github.com/FXMisc/RichTextFX/issues/837)
- Added auto-indent on enter to JavaKeywordsDemo  [\#846](https://github.com/FXMisc/RichTextFX/issues/846)

**Fixed bugs:**

- Bug: Fixed caret position off by one on wrapped multiline [\#834](https://github.com/FXMisc/RichTextFX/issues/834)
- Bug: Fix highlight fill property not working since 0.9.0 [\#844](https://github.com/FXMisc/RichTextFX/issues/844)

**Merged pull requests:**

- Added Chorus to 'Who uses RichTextFX?' [\#825](https://github.com/FXMisc/RichTextFX/pull/825) ([iAmGio](https://github.com/iAmGio))
- Added EpubFx 'Who uses RichTextFX?' [\#828](https://github.com/FXMisc/RichTextFX/pull/828) ([finanzer](https://github.com/finanzer))

## [v0.10.1](https://github.com/FXMisc/RichTextFX/tree/v0.10.1) (2019-05-20)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.10.0...v0.10.1)

**Implemented enhancements:**

- Changed page up and down behaviour [\#688](https://github.com/FXMisc/RichTextFX/issues/688)
- Remove deprecated methods [\#818](https://github.com/FXMisc/RichTextFX/issues/818)

**Fixed bugs:**

- Bug: CodeArea caretBounds graphic bug [\#812](https://github.com/FXMisc/RichTextFX/issues/812)
- Bug: Undo after deleting styled text causes exception [\#815](https://github.com/FXMisc/RichTextFX/issues/815)

**Merged pull requests:**

- Enhancement: Changed page up and down behaviour [\#821](https://github.com/FXMisc/RichTextFX/pull/821) ([Jurgen Doll](https://github.com/Jugen))
- Fix bug: Fix Paragraph returning incorrectly styled subsequence [\#817](https://github.com/FXMisc/RichTextFX/pull/817) ([Jugen](https://github.com/Jugen))
- Fix bug: Fix for #812 caretBounds graphic bug [\#819 and \#822](https://github.com/FXMisc/RichTextFX/pull/819) ([Jugen](https://github.com/Jugen))

## [v0.10.0](https://github.com/FXMisc/RichTextFX/tree/v0.10.0) (2019-04-18)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.9.3...v0.10.0)

**Implemented enhancements:**

- RichTextFX with Java 11 [\#776](https://github.com/FXMisc/RichTextFX/issues/776)

**Fixed bugs:**

- Bug: Lines sometimes overlap when one wraps by a small amount [\#809](https://github.com/FXMisc/RichTextFX/issues/809)

**Merged pull requests:**

- Enhancement: Mrjar config and source [\#804](https://github.com/FXMisc/RichTextFX/pull/804) ([Jurgen Doll](https://github.com/Jugen))
- Fix bug: Fix height paragraph box height calculation [\#810](https://github.com/FXMisc/RichTextFX/pull/810) ([Chris Smith](https://github.com/csmith))

## [v0.9.3](https://github.com/FXMisc/RichTextFX/tree/v0.9.3) (2019-03-01)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.9.2...v0.9.3)

**Fixed bugs:**

- Bug: Alternate keyboard layouts (eg: Dvorak, or German) not supported [\#799](https://github.com/FXMisc/RichTextFX/issues/799)
- Bug: Background color is applied in the wrong place when rendered segment type is not a TextExt [\#638](https://github.com/FXMisc/RichTextFX/issues/638)
- Bug: Random crashes in GenericStyleArea.visibleParToAllParIndex [\#777](https://github.com/FXMisc/RichTextFX/issues/777)
- Bug: Replace selected text with pasted text causes exception [\#788](https://github.com/FXMisc/RichTextFX/issues/788)

**Merged pull requests:**

- Fixed issue: Immediately remove listeners in ParagraphText from selections and carets on disposal [\#791](https://github.com/FXMisc/RichTextFX/pull/791) ([JFormDesigner](https://github.com/JFormDesigner))
- Fix bug: Position of background, border and underline shapes in case that the line contains custom objects [\#793](https://github.com/FXMisc/RichTextFX/pull/793) ([JFormDesigner](https://github.com/JFormDesigner))
- Fix bug: Paragraph list trim using object comparison [\#795](https://github.com/FXMisc/RichTextFX/pull/795) ([JFormDesigner](https://github.com/JFormDesigner))
- Fix bug: Alternate keyboard layouts not supported [\#801](https://github.com/FXMisc/RichTextFX/pull/801) ([Jugen](https://github.com/Jugen))
- Added Astro IDE Project to 'Who uses RichTextFX?' [\#802](https://github.com/FXMisc/RichTextFX/pull/802) ([AmrDeveloper](https://github.com/AmrDeveloper))

## [v0.9.2](https://github.com/FXMisc/RichTextFX/tree/v0.9.2) (2018-11-23)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.9.1...v0.9.2)

**Fixed bugs:**

- Issue: Extreme memory usage with large blocks of text [\#627](https://github.com/FXMisc/RichTextFX/issues/627)
- Bug: Replace selected text with pasted text causes exception [\#774](https://github.com/FXMisc/RichTextFX/issues/774)
- Bug: After undo (ctrl-Z), text insertion point jumps to the start [\#780](https://github.com/FXMisc/RichTextFX/issues/780)

**Merged pull requests:**

- Fixed issue: Extreme memory usage with large blocks of text [\#779](https://github.com/FXMisc/RichTextFX/pull/779) ([JonathanMarchand](https://github.com/JonathanMarchand))
- Fix bug: Replace selected text with pasted text causes exception [\#775](https://github.com/FXMisc/RichTextFX/pull/775) ([MrChebik](https://github.com/MrChebik))
- Fix bug: After undo (ctrl-Z), text insertion point jumps to the start [\#785](https://github.com/FXMisc/RichTextFX/pull/785) ([Jugen](https://github.com/Jugen))
- Added George to 'Who uses RichTextFX?' [\#778](https://github.com/FXMisc/RichTextFX/pull/778) ([terjedahl](https://github.com/terjedahl))
- Added Nearde IDE to 'Who uses RichTextFX?' [\#784](https://github.com/FXMisc/RichTextFX/pull/784) ([MWGuy](https://github.com/MWGuy))

## [v0.9.1](https://github.com/FXMisc/RichTextFX/tree/v0.9.1) (2018-07-16)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.9.0...v0.9.1)

**Implemented enhancements:**

- Add a setFont method to CodeArea [\#754](https://github.com/FXMisc/RichTextFX/issues/754)
- Feature: Set padding between lines in multi-line paragraph [\#749](https://github.com/FXMisc/RichTextFX/issues/749)

**Fixed bugs:**

- Position of Caret and Selection are not updated correctly when change occurs at their current position [\#761](https://github.com/FXMisc/RichTextFX/issues/761)
- Bug: `allParToVisibleParIndex` throws Exception when allParIndex is \>= size of visible paragraphs but \<= area's last paragraph index [\#758](https://github.com/FXMisc/RichTextFX/issues/758)
- Triple click doesn't select paragraph; CaretSelectionBindImpl.moveToParEnd wrongly calculates position [\#742](https://github.com/FXMisc/RichTextFX/issues/742)

**Closed issues:**

- Question: CTRL-Z Behavior with highlighted text [\#765](https://github.com/FXMisc/RichTextFX/issues/765)
- Question: disabling new line functionality \(pressing return\) [\#763](https://github.com/FXMisc/RichTextFX/issues/763)
- StyleClassedTextArea: clear\(\), then insertText\(\), doesn't work consistently [\#762](https://github.com/FXMisc/RichTextFX/issues/762)
- Question: Change StyleClassedTextArea Text Color [\#760](https://github.com/FXMisc/RichTextFX/issues/760)
- Who uses RichTextFX \( XR3Player\)  [\#756](https://github.com/FXMisc/RichTextFX/issues/756)
- Ineffective input method [\#753](https://github.com/FXMisc/RichTextFX/issues/753)
- Question: Is it possible to use RichTextFX as a backlog view in a chat application? [\#752](https://github.com/FXMisc/RichTextFX/issues/752)
- Question: how to make a text subscript and superscript [\#751](https://github.com/FXMisc/RichTextFX/issues/751)
- Question: using version 0.9.0, ParagraphGraphicFactory generates -1 values, is this by design? [\#750](https://github.com/FXMisc/RichTextFX/issues/750)
- Putting the caret behind the character and pressing the enter causes the creation of an empty space at first [\#748](https://github.com/FXMisc/RichTextFX/issues/748)
- Error: When I set style to the second line, I get IndexOutOfBoundsException [\#741](https://github.com/FXMisc/RichTextFX/issues/741)
- CSS selection color from 0.7 to 0.9 [\#736](https://github.com/FXMisc/RichTextFX/issues/736)
- Links to the demo source code and demo page are broken [\#733](https://github.com/FXMisc/RichTextFX/issues/733)
- CodeArea not support Chinese [\#732](https://github.com/FXMisc/RichTextFX/issues/732)
- Request to re open \#724 [\#728](https://github.com/FXMisc/RichTextFX/issues/728)
- the demo JavaKeywordsAsync.java have a bug [\#725](https://github.com/FXMisc/RichTextFX/issues/725)
- \[Problem\] richChanges is called by setStyle [\#719](https://github.com/FXMisc/RichTextFX/issues/719)
- Trouble building project using NetBeans IDE due to ReactFX dependency [\#583](https://github.com/FXMisc/RichTextFX/issues/583)

**Merged pull requests:**

- Update UndoFX \(2.1.0\); stop maintaining project [\#769](https://github.com/FXMisc/RichTextFX/pull/769) ([JordanMartinez](https://github.com/JordanMartinez))
- Add JDialogue to Readme [\#767](https://github.com/FXMisc/RichTextFX/pull/767) ([SkyAphid](https://github.com/SkyAphid))
- Fix bug: Caret/Selection positions should be updated correctly when change occurs at their position [\#766](https://github.com/FXMisc/RichTextFX/pull/766) ([JordanMartinez](https://github.com/JordanMartinez))
- Fix all par index bug [\#759](https://github.com/FXMisc/RichTextFX/pull/759) ([JordanMartinez](https://github.com/JordanMartinez))
- XR3Player uses RichTextFX [\#757](https://github.com/FXMisc/RichTextFX/pull/757) ([goxr3plus](https://github.com/goxr3plus))
- Added Everest to 'Who uses RichTextFX?' [\#755](https://github.com/FXMisc/RichTextFX/pull/755) ([RohitAwate](https://github.com/RohitAwate))
- Fix bug: wrongly calculated value used for `moveToParEnd` [\#746](https://github.com/FXMisc/RichTextFX/pull/746) ([JordanMartinez](https://github.com/JordanMartinez))
- Updates README.md to add Greenfoot as a project that uses RichTextFX [\#744](https://github.com/FXMisc/RichTextFX/pull/744) ([amjdhsn](https://github.com/amjdhsn))
- Fixed the links pointing to the demo source pages [\#734](https://github.com/FXMisc/RichTextFX/pull/734) ([creativeArtie](https://github.com/creativeArtie))
- Make it easier to run and maintain demos [\#729](https://github.com/FXMisc/RichTextFX/pull/729) ([JordanMartinez](https://github.com/JordanMartinez))

## [v0.9.0](https://github.com/FXMisc/RichTextFX/tree/v0.9.0) (2018-04-12)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.8.2...v0.9.0)

**Implemented enhancements:**

- Make mouse behavior override properties FXML-friendly [\#655](https://github.com/FXMisc/RichTextFX/issues/655)
- Feature: Drag-and-drop text should only be one change, allowing a single `undo` to undo it [\#574](https://github.com/FXMisc/RichTextFX/issues/574)
- Add support for multiple carets and selection ranges [\#222](https://github.com/FXMisc/RichTextFX/issues/222)

**Fixed bugs:**

- MultiPlainChanges emits an empty list [\#720](https://github.com/FXMisc/RichTextFX/issues/720)
- rtfx-background-color spans if there is only one character between two styles [\#717](https://github.com/FXMisc/RichTextFX/issues/717)
- Incorrect result of GenericStyledArea.getStyleRangeAtPosition\(int position\) in 0.8.\* [\#711](https://github.com/FXMisc/RichTextFX/issues/711)
- Consecutive border/underline styles that are the same are rendered with multiple shapes, not one [\#709](https://github.com/FXMisc/RichTextFX/issues/709)
- Overriding default KeyEvent behavior runs default and then overriding handler [\#707](https://github.com/FXMisc/RichTextFX/issues/707)
- ArrayIndexOutOfBoundsException in ParagraphText::getRangeShapeSafely [\#689](https://github.com/FXMisc/RichTextFX/issues/689)
- Constructor `InlineCssTextArea\(String text\)` initializes area with 1 paragraph with newline characters rather than multiple paragraphs [\#676](https://github.com/FXMisc/RichTextFX/issues/676)
- RichText demo can not load sample document anymore [\#629](https://github.com/FXMisc/RichTextFX/issues/629)
- \[0.7-M3\] OutOfMemoryError Loading Documents in the RichText Demo [\#452](https://github.com/FXMisc/RichTextFX/issues/452)
- IndexOutOfBoundsException in RichText demo [\#449](https://github.com/FXMisc/RichTextFX/issues/449)
- Trackpad scrolling with momentum doesn't work on OS X. [\#265](https://github.com/FXMisc/RichTextFX/issues/265)

**Closed issues:**

- Using in commercial project [\#723](https://github.com/FXMisc/RichTextFX/issues/723)
- PsychSQL [\#708](https://github.com/FXMisc/RichTextFX/issues/708)
- IllegalArgumentException: Cannot construct a Paragraph with an empty list of segments [\#705](https://github.com/FXMisc/RichTextFX/issues/705)
- Undoing a multi change that modifies a later portion of the document before an earlier one fails [\#701](https://github.com/FXMisc/RichTextFX/issues/701)
- Exception: Cannot construct a Paragraph with StyleSpans object that contains no StyleSpan objects [\#696](https://github.com/FXMisc/RichTextFX/issues/696)
- Question: replaceText/insertText without calling richChanges [\#684](https://github.com/FXMisc/RichTextFX/issues/684)
- Incorrect documentation for some mouse hooks [\#680](https://github.com/FXMisc/RichTextFX/issues/680)
- Mac test fails: pressing mouse over text and dragging and releasing mouse triggers `onNewSelectionFinished` [\#679](https://github.com/FXMisc/RichTextFX/issues/679)

**Merged pull requests:**

- Cleanup codebase [\#727](https://github.com/FXMisc/RichTextFX/pull/727) ([JordanMartinez](https://github.com/JordanMartinez))
- Update WellBehavedFX to 0.3.3 [\#726](https://github.com/FXMisc/RichTextFX/pull/726) ([JordanMartinez](https://github.com/JordanMartinez))
- Prevent `multiPlainChanges\(\)` from emitting an empty list [\#721](https://github.com/FXMisc/RichTextFX/pull/721) ([JordanMartinez](https://github.com/JordanMartinez))
- Fix unconsecutive style issue [\#718](https://github.com/FXMisc/RichTextFX/pull/718) ([JordanMartinez](https://github.com/JordanMartinez))
- Fix `getStyleRangeAtPosition\(\)` bug [\#716](https://github.com/FXMisc/RichTextFX/pull/716) ([JordanMartinez](https://github.com/JordanMartinez))
- Fix typo in exception message. [\#714](https://github.com/FXMisc/RichTextFX/pull/714) ([Gerardwx](https://github.com/Gerardwx))
- Add PsychSQL to list of projects that use RichTextFX [\#713](https://github.com/FXMisc/RichTextFX/pull/713) ([tmptmpuser](https://github.com/tmptmpuser))
- Use correct class in `instanceOf` check; provide faster equals method [\#710](https://github.com/FXMisc/RichTextFX/pull/710) ([JordanMartinez](https://github.com/JordanMartinez))
- Use absolute replacements for undo and redo [\#702](https://github.com/FXMisc/RichTextFX/pull/702) ([JordanMartinez](https://github.com/JordanMartinez))
- Move selected text should be one change [\#700](https://github.com/FXMisc/RichTextFX/pull/700) ([JordanMartinez](https://github.com/JordanMartinez))
- Fix for \#696 [\#697](https://github.com/FXMisc/RichTextFX/pull/697) ([Jugen](https://github.com/Jugen))
- Allow multiple portions of an area's document to be updated in one call [\#695](https://github.com/FXMisc/RichTextFX/pull/695) ([JordanMartinez](https://github.com/JordanMartinez))
- Fix replaceText IOOBE [\#690](https://github.com/FXMisc/RichTextFX/pull/690) ([Jugen](https://github.com/Jugen))
- Allow area to display multiple carets and selections [\#687](https://github.com/FXMisc/RichTextFX/pull/687) ([JordanMartinez](https://github.com/JordanMartinez))
- Only save/load ".rtfx" files; warn about file format changes [\#686](https://github.com/FXMisc/RichTextFX/pull/686) ([JordanMartinez](https://github.com/JordanMartinez))
- Updated Paragraph to apply styles to empty paragraphs. [\#685](https://github.com/FXMisc/RichTextFX/pull/685) ([Jugen](https://github.com/Jugen))
- Skip failing Mac test; correct Javadoc for some mouse hooks [\#681](https://github.com/FXMisc/RichTextFX/pull/681) ([JordanMartinez](https://github.com/JordanMartinez))
- Use correct replace method when SEG is String [\#678](https://github.com/FXMisc/RichTextFX/pull/678) ([JordanMartinez](https://github.com/JordanMartinez))
- Update XMLEditor.java [\#673](https://github.com/FXMisc/RichTextFX/pull/673) ([svkreml](https://github.com/svkreml))
- RFE implementation for \#653 / \#655 [\#656](https://github.com/FXMisc/RichTextFX/pull/656) ([Jugen](https://github.com/Jugen))

## [v0.8.2](https://github.com/FXMisc/RichTextFX/tree/v0.8.2) (2018-01-20)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/0.8.1...v0.8.2)

**Implemented enhancements:**

- Make more of ROSD's modification methods public [\#648](https://github.com/FXMisc/RichTextFX/issues/648)
- Allow easier construction of a ReadOnlyStyledDocument with content [\#646](https://github.com/FXMisc/RichTextFX/issues/646)

**Fixed bugs:**

- Read only pseudo class is misspelled in GenericStyledArea [\#650](https://github.com/FXMisc/RichTextFX/issues/650)
- ExceptionInInitializerError: EventType "MOUSE\_OVER\_TEXT\_ANY"with parent "EVENT" already exists [\#634](https://github.com/FXMisc/RichTextFX/issues/634)

**Closed issues:**

- Selection is no longer highlighted of inlineCssTextArea [\#670](https://github.com/FXMisc/RichTextFX/issues/670)
- RuKey [\#662](https://github.com/FXMisc/RichTextFX/issues/662)
- Empty CodeArea [\#660](https://github.com/FXMisc/RichTextFX/issues/660)
- Regression of properties that could be set in FXML, fail in 0.8.1 [\#653](https://github.com/FXMisc/RichTextFX/issues/653)
- moveTo\(\) not scrolling the StyleClassedTextArea [\#652](https://github.com/FXMisc/RichTextFX/issues/652)
- Allow ReadOnlyStyledDocument to be created from list of paragraphs [\#644](https://github.com/FXMisc/RichTextFX/issues/644)
- Using a CodeArea and `setStyle\(int, int, inlineCssStyleString\)` doesn't style that range of text with the inline CSS styling [\#640](https://github.com/FXMisc/RichTextFX/issues/640)
- IndexOutOfBoundsException when editor is not shown but updated [\#637](https://github.com/FXMisc/RichTextFX/issues/637)
- First test run on Mac fails with TimeoutException [\#608](https://github.com/FXMisc/RichTextFX/issues/608)
- AZERTY keyboard on Windows OS: SHORTCUT+SLASH key event does not run its consumer [\#479](https://github.com/FXMisc/RichTextFX/issues/479)

**Merged pull requests:**

- Generate new sample rtfx binary file [\#672](https://github.com/FXMisc/RichTextFX/pull/672) ([JordanMartinez](https://github.com/JordanMartinez))
- Update dependencies: WellBehavedFX and UndoFX [\#669](https://github.com/FXMisc/RichTextFX/pull/669) ([JordanMartinez](https://github.com/JordanMartinez))
- Upgrade Gradle to 4.4.1-bin \(Binary-only\) and change hash code. And small changes in RichTextFx.java. [\#664](https://github.com/FXMisc/RichTextFX/pull/664) ([scientificware](https://github.com/scientificware))
- Add support for FXML \(again\) [\#654](https://github.com/FXMisc/RichTextFX/pull/654) ([Jugen](https://github.com/Jugen))
- Corrected misspelled read-only pseudo class [\#651](https://github.com/FXMisc/RichTextFX/pull/651) ([Jugen](https://github.com/Jugen))
- Make more of ROSD's replace API public, not package-private [\#649](https://github.com/FXMisc/RichTextFX/pull/649) ([JordanMartinez](https://github.com/JordanMartinez))
- Allow initializing an \[Editabled/ReadOnly\]StyledDocument with content [\#645](https://github.com/FXMisc/RichTextFX/pull/645) ([JordanMartinez](https://github.com/JordanMartinez))
- Increase Mac build's TestFX setup timeout to 5 seconds [\#641](https://github.com/FXMisc/RichTextFX/pull/641) ([JordanMartinez](https://github.com/JordanMartinez))
- Add JabRef to the projects using RichTextFX [\#639](https://github.com/FXMisc/RichTextFX/pull/639) ([koppor](https://github.com/koppor))
- Remove duplicate class due to copy-and-paste error [\#636](https://github.com/FXMisc/RichTextFX/pull/636) ([JordanMartinez](https://github.com/JordanMartinez))

## [0.8.1](https://github.com/FXMisc/RichTextFX/tree/0.8.1) (2017-10-27)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.8.0...0.8.1)

**Implemented enhancements:**

- Feature: support tables [\#628](https://github.com/FXMisc/RichTextFX/issues/628)

**Closed issues:**

- Write high-quality javadoc [\#626](https://github.com/FXMisc/RichTextFX/issues/626)
- Remove deprecated Travis Precise build [\#623](https://github.com/FXMisc/RichTextFX/issues/623)
- Drop "Milestone" from release version and continue with 0.8, 0.9, 0.10, ... [\#622](https://github.com/FXMisc/RichTextFX/issues/622)
- Update Flowless to stable release \(once it's released\) [\#516](https://github.com/FXMisc/RichTextFX/issues/516)

**Merged pull requests:**

- Get viewport height; show paragraph region; clean up source code \(boilerplate & organization\) [\#632](https://github.com/FXMisc/RichTextFX/pull/632) ([JordanMartinez](https://github.com/JordanMartinez))
- Insure all public methods are overridden in interface; move interfaces to view package [\#631](https://github.com/FXMisc/RichTextFX/pull/631) ([JordanMartinez](https://github.com/JordanMartinez))
- Document all classes [\#630](https://github.com/FXMisc/RichTextFX/pull/630) ([JordanMartinez](https://github.com/JordanMartinez))

## [v0.8.0](https://github.com/FXMisc/RichTextFX/tree/v0.8.0) (2017-10-20)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.7-M5n...v0.8.0)

**Implemented enhancements:**

- Feature: Provide mapping between all-paragraph and visible-paragraph index systems [\#599](https://github.com/FXMisc/RichTextFX/issues/599)
- Feature: Add API for Paragraph's Bounds [\#537](https://github.com/FXMisc/RichTextFX/issues/537)
- Add API for getting which visible paragraph index the caret is on [\#522](https://github.com/FXMisc/RichTextFX/issues/522)
- Feature: Allow paragraphs to fill from bottom to top [\#502](https://github.com/FXMisc/RichTextFX/issues/502)
- Optimize code by caching the CSS meta data [\#485](https://github.com/FXMisc/RichTextFX/issues/485)
- Optimize ParagraphText's background and underline shapes [\#484](https://github.com/FXMisc/RichTextFX/issues/484)
- Add support for a border around some text [\#346](https://github.com/FXMisc/RichTextFX/issues/346)
- Make RichTextFX work on Java 9 [\#270](https://github.com/FXMisc/RichTextFX/issues/270)

**Fixed bugs:**

- Background and underline shapes not properly calculated [\#594](https://github.com/FXMisc/RichTextFX/issues/594)
- Regression: Image custom object is not properly handled in RichText demo [\#584](https://github.com/FXMisc/RichTextFX/issues/584)
- Regression introduced in \#559: css doesn't work anymore [\#561](https://github.com/FXMisc/RichTextFX/issues/561)
- Undoing on a Right of Either\<SegmentOps, SegmentOps\> throws an "Unexpected change received" exception [\#554](https://github.com/FXMisc/RichTextFX/issues/554)
- paragraph-box padding works incorrectly [\#508](https://github.com/FXMisc/RichTextFX/issues/508)
- Padding for whole text area or line number area [\#507](https://github.com/FXMisc/RichTextFX/issues/507)
- MANIFEST.MF is empty [\#476](https://github.com/FXMisc/RichTextFX/issues/476)
- Wrong character insertion index on multi-line paragraphs when caret positioned at end of line-wrap [\#423](https://github.com/FXMisc/RichTextFX/issues/423)
- Text horizontally out of the viewport does not become visible when vieport size is increased [\#412](https://github.com/FXMisc/RichTextFX/issues/412)
- Restyling large portions of the document causes area to scroll when it shouldn't. [\#390](https://github.com/FXMisc/RichTextFX/issues/390)
- Undo merges with subsequent changes despite a reasonable period of user inactivity when it shouldn't. [\#362](https://github.com/FXMisc/RichTextFX/issues/362)
- assertion failure when Paragraph constructed with empty segment list [\#345](https://github.com/FXMisc/RichTextFX/issues/345)

**Closed issues:**

- Clarify contributing guidelines [\#615](https://github.com/FXMisc/RichTextFX/issues/615)
- How best to construct a ParagraphGraphicFactory that allows for dynamic positioning of Nodes [\#606](https://github.com/FXMisc/RichTextFX/issues/606)
- git pull + gradle build errors on windows [\#605](https://github.com/FXMisc/RichTextFX/issues/605)
- Regression: copying and pasting a selection ending in a new line throws an exception [\#602](https://github.com/FXMisc/RichTextFX/issues/602)
- Cannot construct a Paragraph with an empty list of segments [\#592](https://github.com/FXMisc/RichTextFX/issues/592)
- Reassignment of the key button [\#591](https://github.com/FXMisc/RichTextFX/issues/591)
- Processing of CSS of changes [\#586](https://github.com/FXMisc/RichTextFX/issues/586)
- Show caret inmediately after change [\#580](https://github.com/FXMisc/RichTextFX/issues/580)
- IndexOutOfBoundsException when deleting whole text if last line is empty [\#579](https://github.com/FXMisc/RichTextFX/issues/579)
- NPE in clipboard copy [\#573](https://github.com/FXMisc/RichTextFX/issues/573)
- Is it possible to add OSGI support? [\#570](https://github.com/FXMisc/RichTextFX/issues/570)
- Question: CodeArea content modified can be checked? [\#569](https://github.com/FXMisc/RichTextFX/issues/569)
- Question: How to translate the index of a visible paragraph into the index system of all paragraphs? [\#568](https://github.com/FXMisc/RichTextFX/issues/568)
- Refactor: Decouple style from segment [\#567](https://github.com/FXMisc/RichTextFX/issues/567)
- Calculate highlighting of brackets [\#566](https://github.com/FXMisc/RichTextFX/issues/566)
- Improve selection display [\#549](https://github.com/FXMisc/RichTextFX/issues/549)
- Unable to center paragraph. [\#548](https://github.com/FXMisc/RichTextFX/issues/548)
- CodeArea default -fx-font-family/-fx-fill [\#547](https://github.com/FXMisc/RichTextFX/issues/547)
- Looking for updated demo on creating hyperlink [\#546](https://github.com/FXMisc/RichTextFX/issues/546)
- Error importing cloned project into Eclipse [\#545](https://github.com/FXMisc/RichTextFX/issues/545)
- How to reposition the caret? [\#544](https://github.com/FXMisc/RichTextFX/issues/544)
- Add instructions to README on how to use Jitpack [\#531](https://github.com/FXMisc/RichTextFX/issues/531)
- Unable to clear the codeArea [\#527](https://github.com/FXMisc/RichTextFX/issues/527)
- Using "setStyle" two times in a row causes inexplicable scrolling [\#525](https://github.com/FXMisc/RichTextFX/issues/525)
- Support for Editor Configuration [\#521](https://github.com/FXMisc/RichTextFX/issues/521)
- Build fails on Linux with: "error: unmappable character for encoding ASCII" [\#513](https://github.com/FXMisc/RichTextFX/issues/513)
- Undoing delete has different behaviour than undoing backspace [\#493](https://github.com/FXMisc/RichTextFX/issues/493)
- Refactor: ChangeType in TextChange [\#486](https://github.com/FXMisc/RichTextFX/issues/486)
- Refactor: create `Caret` class [\#429](https://github.com/FXMisc/RichTextFX/issues/429)
- Memory Leak may happen when load a large text. [\#409](https://github.com/FXMisc/RichTextFX/issues/409)
- Write a Developer Guide [\#403](https://github.com/FXMisc/RichTextFX/issues/403)
- How To Add Padding Between StyledTextArea And Text Contents? [\#394](https://github.com/FXMisc/RichTextFX/issues/394)
- Are there modelToView/viewToModel equivalents for StyledTextArea [\#385](https://github.com/FXMisc/RichTextFX/issues/385)
- ALT + Alphanumeric as KeyCombination prints the typed symbol [\#366](https://github.com/FXMisc/RichTextFX/issues/366)
- Move UndoManager creation code outside StyledTextArea [\#333](https://github.com/FXMisc/RichTextFX/issues/333)

**Merged pull requests:**

- Additional commits in preparation for v0.8.0 release [\#625](https://github.com/FXMisc/RichTextFX/pull/625) ([JordanMartinez](https://github.com/JordanMartinez))
- moved ParagraphText specific code from createStyledTextNode\(\) methods to class ParagraphText [\#617](https://github.com/FXMisc/RichTextFX/pull/617) ([JFormDesigner](https://github.com/JFormDesigner))
- Clarify contributing guidelines [\#616](https://github.com/FXMisc/RichTextFX/pull/616) ([JordanMartinez](https://github.com/JordanMartinez))
- Java 9 compatibility \(using reflection\) [\#614](https://github.com/FXMisc/RichTextFX/pull/614) ([JFormDesigner](https://github.com/JFormDesigner))
- Fix Mac build error [\#609](https://github.com/FXMisc/RichTextFX/pull/609) ([JordanMartinez](https://github.com/JordanMartinez))
- Expose viewport dirty as public API [\#607](https://github.com/FXMisc/RichTextFX/pull/607) ([JordanMartinez](https://github.com/JordanMartinez))
- Fix regression: use only valid indexes for setting a merged segment [\#604](https://github.com/FXMisc/RichTextFX/pull/604) ([JordanMartinez](https://github.com/JordanMartinez))
- Cleanup integration test [\#601](https://github.com/FXMisc/RichTextFX/pull/601) ([JordanMartinez](https://github.com/JordanMartinez))
- Feature: map visible paragraph index to all paragraph index & vice versa [\#600](https://github.com/FXMisc/RichTextFX/pull/600) ([JordanMartinez](https://github.com/JordanMartinez))
- Paragraph - clean up and performance improvements [\#598](https://github.com/FXMisc/RichTextFX/pull/598) ([JordanMartinez](https://github.com/JordanMartinez))
- Add Windows build to CI [\#596](https://github.com/FXMisc/RichTextFX/pull/596) ([JordanMartinez](https://github.com/JordanMartinez))
- Issue \#594: Properly calculate the background and underline shapes fo… [\#595](https://github.com/FXMisc/RichTextFX/pull/595) ([afester](https://github.com/afester))
- Decouple style from segment object [\#590](https://github.com/FXMisc/RichTextFX/pull/590) ([JordanMartinez](https://github.com/JordanMartinez))
- Issue588: set the property testfx.robot to glass when running the Tes… [\#589](https://github.com/FXMisc/RichTextFX/pull/589) ([afester](https://github.com/afester))
- Add API to get paragraph bounds if visible [\#587](https://github.com/FXMisc/RichTextFX/pull/587) ([JordanMartinez](https://github.com/JordanMartinez))
- Update OSX Image and reduce log output to info [\#582](https://github.com/FXMisc/RichTextFX/pull/582) ([JordanMartinez](https://github.com/JordanMartinez))
- Use old approach to update selection's 2D positions [\#581](https://github.com/FXMisc/RichTextFX/pull/581) ([JordanMartinez](https://github.com/JordanMartinez))
- fix issue \#573: NPE in clipboard copy [\#578](https://github.com/FXMisc/RichTextFX/pull/578) ([JFormDesigner](https://github.com/JFormDesigner))
- adjust wrapped lines selection to the rest of the line [\#577](https://github.com/FXMisc/RichTextFX/pull/577) ([JFormDesigner](https://github.com/JFormDesigner))
- select moved text after drag-and-drop [\#576](https://github.com/FXMisc/RichTextFX/pull/576) ([JFormDesigner](https://github.com/JFormDesigner))
- fix selection painting of empty lines if text area displays line numbers [\#575](https://github.com/FXMisc/RichTextFX/pull/575) ([JFormDesigner](https://github.com/JFormDesigner))
- fixed caret positioning in wrapped lines \(issue \#423\) [\#572](https://github.com/FXMisc/RichTextFX/pull/572) ([JFormDesigner](https://github.com/JFormDesigner))
- Add support for OSGI [\#571](https://github.com/FXMisc/RichTextFX/pull/571) ([JordanMartinez](https://github.com/JordanMartinez))
- Fix either ops bug [\#565](https://github.com/FXMisc/RichTextFX/pull/565) ([JordanMartinez](https://github.com/JordanMartinez))
- Enhancement: Selecting a newline char selects the rest of the line [\#564](https://github.com/FXMisc/RichTextFX/pull/564) ([JordanMartinez](https://github.com/JordanMartinez))
- Fix regression due to typo [\#562](https://github.com/FXMisc/RichTextFX/pull/562) ([JordanMartinez](https://github.com/JordanMartinez))
- Support adding border strokes around a section of text [\#560](https://github.com/FXMisc/RichTextFX/pull/560) ([JordanMartinez](https://github.com/JordanMartinez))
- Reduce boilerplate for custom CssMetaData [\#559](https://github.com/FXMisc/RichTextFX/pull/559) ([JordanMartinez](https://github.com/JordanMartinez))
- Optimize background color and underline shapes [\#558](https://github.com/FXMisc/RichTextFX/pull/558) ([JordanMartinez](https://github.com/JordanMartinez))
- Only construct background/underline shapes when needed [\#557](https://github.com/FXMisc/RichTextFX/pull/557) ([JordanMartinez](https://github.com/JordanMartinez))
- Remove unused class [\#556](https://github.com/FXMisc/RichTextFX/pull/556) ([JordanMartinez](https://github.com/JordanMartinez))
- Cleanup seg ops [\#555](https://github.com/FXMisc/RichTextFX/pull/555) ([JordanMartinez](https://github.com/JordanMartinez))
- Easier custom object support [\#553](https://github.com/FXMisc/RichTextFX/pull/553) ([JordanMartinez](https://github.com/JordanMartinez))
- Cleanup css properties [\#552](https://github.com/FXMisc/RichTextFX/pull/552) ([JordanMartinez](https://github.com/JordanMartinez))
- Make TextExt constructor public and parameter optional [\#551](https://github.com/FXMisc/RichTextFX/pull/551) ([JordanMartinez](https://github.com/JordanMartinez))
- Add Hyperlink demo - minimum needed for custom object integration [\#550](https://github.com/FXMisc/RichTextFX/pull/550) ([JordanMartinez](https://github.com/JordanMartinez))
- Fix typo: return name, not constant [\#543](https://github.com/FXMisc/RichTextFX/pull/543) ([JordanMartinez](https://github.com/JordanMartinez))
- Rename STABehavior; simplify EditableStyledDocument\#plainTextChanges default method [\#542](https://github.com/FXMisc/RichTextFX/pull/542) ([JordanMartinez](https://github.com/JordanMartinez))
- Cleanup caret selection [\#541](https://github.com/FXMisc/RichTextFX/pull/541) ([JordanMartinez](https://github.com/JordanMartinez))
- Inline navigation into caret selection [\#540](https://github.com/FXMisc/RichTextFX/pull/540) ([JordanMartinez](https://github.com/JordanMartinez))
- Remove unused method/import; use caret's target caret offset, not area's [\#539](https://github.com/FXMisc/RichTextFX/pull/539) ([JordanMartinez](https://github.com/JordanMartinez))
- Remove deprecated Popup API [\#538](https://github.com/FXMisc/RichTextFX/pull/538) ([JordanMartinez](https://github.com/JordanMartinez))
- Added BlueJ to the list of projects using RichTextFX [\#536](https://github.com/FXMisc/RichTextFX/pull/536) ([twistedsquare](https://github.com/twistedsquare))
- Fix ReadMe's list formatting; link to Jitpack's documentation [\#534](https://github.com/FXMisc/RichTextFX/pull/534) ([JordanMartinez](https://github.com/JordanMartinez))
- Jitpack to readme [\#533](https://github.com/FXMisc/RichTextFX/pull/533) ([JordanMartinez](https://github.com/JordanMartinez))
- Refactor API tests to separate classes and package; point to UndoUtils in area's javadoc [\#532](https://github.com/FXMisc/RichTextFX/pull/532) ([JordanMartinez](https://github.com/JordanMartinez))
- Stop next change from merging with previous one after inactive period. [\#530](https://github.com/FXMisc/RichTextFX/pull/530) ([JordanMartinez](https://github.com/JordanMartinez))
- Reposition code and refactor / add javadoc to \#526 & \#528 [\#529](https://github.com/FXMisc/RichTextFX/pull/529) ([JordanMartinez](https://github.com/JordanMartinez))
- Allow UndoManager to use developer-defined changes \(text, view, etc.\) [\#528](https://github.com/FXMisc/RichTextFX/pull/528) ([JordanMartinez](https://github.com/JordanMartinez))
- Extract caret and selection to separate classes [\#526](https://github.com/FXMisc/RichTextFX/pull/526) ([JordanMartinez](https://github.com/JordanMartinez))
- Make custom integrationTest test suite visible in Gradle 4 or higher [\#524](https://github.com/FXMisc/RichTextFX/pull/524) ([JordanMartinez](https://github.com/JordanMartinez))
- Cleanup some aspects of the code [\#523](https://github.com/FXMisc/RichTextFX/pull/523) ([JordanMartinez](https://github.com/JordanMartinez))
- Account for padding in ParagraphBox's layout call [\#519](https://github.com/FXMisc/RichTextFX/pull/519) ([JordanMartinez](https://github.com/JordanMartinez))
- Add debug flag to Travis build [\#518](https://github.com/FXMisc/RichTextFX/pull/518) ([JordanMartinez](https://github.com/JordanMartinez))
- Include implementation and specification version info in MANIFEST.MF file in outputted JARs \(\#476\) [\#517](https://github.com/FXMisc/RichTextFX/pull/517) ([veita](https://github.com/veita))
- Upgrade Flowless to 0.6-SNAPSHOT [\#515](https://github.com/FXMisc/RichTextFX/pull/515) ([JordanMartinez](https://github.com/JordanMartinez))
- Optimize css meta data list [\#512](https://github.com/FXMisc/RichTextFX/pull/512) ([JordanMartinez](https://github.com/JordanMartinez))
- Account for insets in `layoutChildren\(\)` [\#510](https://github.com/FXMisc/RichTextFX/pull/510) ([JordanMartinez](https://github.com/JordanMartinez))
- Fix style spans issue [\#509](https://github.com/FXMisc/RichTextFX/pull/509) ([JordanMartinez](https://github.com/JordanMartinez))
- Write more TestFX tests [\#504](https://github.com/FXMisc/RichTextFX/pull/504) ([JordanMartinez](https://github.com/JordanMartinez))
- Expose visible paragraphs [\#501](https://github.com/FXMisc/RichTextFX/pull/501) ([JordanMartinez](https://github.com/JordanMartinez))

## [v0.7-M5n](https://github.com/FXMisc/RichTextFX/tree/v0.7-M5n) (2017-05-12)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.7-M5...v0.7-M5n)

**Implemented enhancements:**

- RichTextFX controls from FXML? [\#443](https://github.com/FXMisc/RichTextFX/issues/443)
- Get BoundingBox of letters in StyledTextArea [\#150](https://github.com/FXMisc/RichTextFX/issues/150)

**Fixed bugs:**

- WrapText + LineNumber, wrong line numbers position [\#488](https://github.com/FXMisc/RichTextFX/issues/488)
- \[0.7-M5\] Calling `setParagraphStyle` on an empty Paragraph \[p.length\(\) == 0\] throws Exception [\#481](https://github.com/FXMisc/RichTextFX/issues/481)

**Closed issues:**

- Calling getCharacterBoundsOnScreen on a paragraph with selection stops the selection being displayed [\#499](https://github.com/FXMisc/RichTextFX/issues/499)
- \(Question\) How to work with patterns. [\#498](https://github.com/FXMisc/RichTextFX/issues/498)
- Extend ParagraphGraphicFactory to entire window [\#491](https://github.com/FXMisc/RichTextFX/issues/491)
- Empty text if I undo continuously [\#490](https://github.com/FXMisc/RichTextFX/issues/490)
- Can I change the color of the text cursor ? [\#480](https://github.com/FXMisc/RichTextFX/issues/480)
- Eclipse can't import org.fxmisc.richtext.model [\#478](https://github.com/FXMisc/RichTextFX/issues/478)
- Performance Profile [\#411](https://github.com/FXMisc/RichTextFX/issues/411)

**Merged pull requests:**

- Speed up test fx tests [\#503](https://github.com/FXMisc/RichTextFX/pull/503) ([JordanMartinez](https://github.com/JordanMartinez))
- Fixes issue \#499: calling getCharacterBoundsOnScreen on a paragraph w… [\#500](https://github.com/FXMisc/RichTextFX/pull/500) ([twistedsquare](https://github.com/twistedsquare))
- Revert to previous merge method [\#496](https://github.com/FXMisc/RichTextFX/pull/496) ([JordanMartinez](https://github.com/JordanMartinez))
- Update undo fx [\#495](https://github.com/FXMisc/RichTextFX/pull/495) ([JordanMartinez](https://github.com/JordanMartinez))
- Added Juliar Programming Language \(JuliarFuture\) Project [\#494](https://github.com/FXMisc/RichTextFX/pull/494) ([TheAndreiM](https://github.com/TheAndreiM))
- Fix line numbers alignment and change default padding to spaces. \#488 [\#489](https://github.com/FXMisc/RichTextFX/pull/489) ([AlexP11223](https://github.com/AlexP11223))
- Limit TextChange's type's possibilities by only using it for merging [\#487](https://github.com/FXMisc/RichTextFX/pull/487) ([JordanMartinez](https://github.com/JordanMartinez))
- Fix regression: changing empty paragraph's style is a valid change [\#483](https://github.com/FXMisc/RichTextFX/pull/483) ([JordanMartinez](https://github.com/JordanMartinez))
- Added some documentation to some small classes [\#482](https://github.com/FXMisc/RichTextFX/pull/482) ([twistedsquare](https://github.com/twistedsquare))

## [v0.7-M5](https://github.com/FXMisc/RichTextFX/tree/v0.7-M5) (2017-04-02)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.7-M4...v0.7-M5)

**Fixed bugs:**

- Paragraph's Line-Spacing CSS doesn't work [\#382](https://github.com/FXMisc/RichTextFX/issues/382)

**Closed issues:**

- Wrong text insert in CodeArea [\#475](https://github.com/FXMisc/RichTextFX/issues/475)
- Selecting text using keyboard can lock movement [\#474](https://github.com/FXMisc/RichTextFX/issues/474)
- \[0.7-M4\] Inserting text via user keyboard input inserts text in reverse order because caret position doesn't advance forward after input [\#472](https://github.com/FXMisc/RichTextFX/issues/472)

**Merged pull requests:**

- Fix regression: Update caret position after replace call [\#473](https://github.com/FXMisc/RichTextFX/pull/473) ([JordanMartinez](https://github.com/JordanMartinez))

## [v0.7-M4](https://github.com/FXMisc/RichTextFX/tree/v0.7-M4) (2017-03-27)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/0.7-M3...v0.7-M4)

**Implemented enhancements:**

- Some MouseEvent behaviors cannot be safely overridden because hooks for them do not yet exist [\#357](https://github.com/FXMisc/RichTextFX/issues/357)
- Add support for nonlinear undo/redo \(clones don't currently handle undo/redo correctly\) [\#233](https://github.com/FXMisc/RichTextFX/issues/233)
- Inserting graphic in the text [\#87](https://github.com/FXMisc/RichTextFX/issues/87)
- Suggestion for snippets [\#63](https://github.com/FXMisc/RichTextFX/issues/63)

**Fixed bugs:**

- `MoveSelectedText` throw IllegalArgumentException due to type in TextChange [\#469](https://github.com/FXMisc/RichTextFX/issues/469)
- Deleting text that was just inserted merges those changes into a change that does nothing, causing an Exception [\#322](https://github.com/FXMisc/RichTextFX/issues/322)
- CodeArea horizontal scrollbar not working [\#210](https://github.com/FXMisc/RichTextFX/issues/210)
- Exception \(not sure why actually\) [\#170](https://github.com/FXMisc/RichTextFX/issues/170)

**Closed issues:**

- I cannot run the demos [\#459](https://github.com/FXMisc/RichTextFX/issues/459)
- StyledTextAreaBehaviorTest fail [\#454](https://github.com/FXMisc/RichTextFX/issues/454)
- I'm migrating from the JavaFX TextArea to CodeArea... [\#453](https://github.com/FXMisc/RichTextFX/issues/453)
- Is there code folding option in Rich text editor ? [\#451](https://github.com/FXMisc/RichTextFX/issues/451)
- It's possible to add a context menu into CodeArea? [\#448](https://github.com/FXMisc/RichTextFX/issues/448)
- Style RichTextFX in TornadoFX with Type Safe CSS [\#447](https://github.com/FXMisc/RichTextFX/issues/447)
- Alternative to setText\(\) method within RichTextFX [\#444](https://github.com/FXMisc/RichTextFX/issues/444)
- Text box like Telegram. Is it possible? [\#440](https://github.com/FXMisc/RichTextFX/issues/440)
- Caret Position = InlineCssTextArea .getText\(\).length\(\) but it doesn't scroll vertically to the bottom [\#433](https://github.com/FXMisc/RichTextFX/issues/433)
- java.lang.reflect.InvocationTargetException  [\#432](https://github.com/FXMisc/RichTextFX/issues/432)
- Emoji support [\#427](https://github.com/FXMisc/RichTextFX/issues/427)
- blink-caret-rate CSS Property doesn't work [\#426](https://github.com/FXMisc/RichTextFX/issues/426)
- moveTo\(\) does not change the scroll position anymore [\#414](https://github.com/FXMisc/RichTextFX/issues/414)
- Add Support for TokenMarker [\#413](https://github.com/FXMisc/RichTextFX/issues/413)
- Inline images [\#404](https://github.com/FXMisc/RichTextFX/issues/404)
- Rich-Text editor demo not working. [\#399](https://github.com/FXMisc/RichTextFX/issues/399)
- Styling the caret \(not just colour\) [\#397](https://github.com/FXMisc/RichTextFX/issues/397)
- Scroll position/moveTo\(\) issue [\#395](https://github.com/FXMisc/RichTextFX/issues/395)
- Link to "Arduino Harp" \(http://www.avrharp.org/\) in README is dead. [\#392](https://github.com/FXMisc/RichTextFX/issues/392)
- Demo with Jygments [\#387](https://github.com/FXMisc/RichTextFX/issues/387)
- Feature request: Caret line position vs. paragraph position [\#386](https://github.com/FXMisc/RichTextFX/issues/386)
- WYSIWYG Document Writer like Office [\#383](https://github.com/FXMisc/RichTextFX/issues/383)
- Replace Popup with property containing caret/selection bounds [\#377](https://github.com/FXMisc/RichTextFX/issues/377)
- Set text background/text highlight color [\#369](https://github.com/FXMisc/RichTextFX/issues/369)
- \[0.7\] Contextmenu not supported by : org.fxmisc.richtext.CodeArea [\#363](https://github.com/FXMisc/RichTextFX/issues/363)
- Image Info next to the lines [\#360](https://github.com/FXMisc/RichTextFX/issues/360)
- Support embedding arbitary nodes like Hyperlinks and ImageView in text layout [\#355](https://github.com/FXMisc/RichTextFX/issues/355)
- Implement search function with regex option [\#354](https://github.com/FXMisc/RichTextFX/issues/354)
- Caused by: java.lang.ClassNotFoundException: org.fxmisc.richtext.StyleClassedTextArea [\#352](https://github.com/FXMisc/RichTextFX/issues/352)
- add Markdown rendering component [\#348](https://github.com/FXMisc/RichTextFX/issues/348)
- Highlight matching words in code \(background color\) [\#338](https://github.com/FXMisc/RichTextFX/issues/338)
- Matching Bracket/Brace/Paren Highlight [\#337](https://github.com/FXMisc/RichTextFX/issues/337)
- Update gradle build file to resolve links to Flowless [\#310](https://github.com/FXMisc/RichTextFX/issues/310)
- What should be in the 0.7 release and ReadMe Updates [\#259](https://github.com/FXMisc/RichTextFX/issues/259)
- Improvement: Make insertion style a convenience generic method [\#209](https://github.com/FXMisc/RichTextFX/issues/209)
- Use as a WYSIWYG Editor with html text in background and styled design in front [\#181](https://github.com/FXMisc/RichTextFX/issues/181)
- RichTextFX support multiple selection? [\#138](https://github.com/FXMisc/RichTextFX/issues/138)

**Merged pull requests:**

- Cleanup javadoc and imports [\#471](https://github.com/FXMisc/RichTextFX/pull/471) ([JordanMartinez](https://github.com/JordanMartinez))
- Fix move selected text bug [\#470](https://github.com/FXMisc/RichTextFX/pull/470) ([JordanMartinez](https://github.com/JordanMartinez))
- Split up mouse handling so that overriding default mouse behavior does not affect other default behavior [\#468](https://github.com/FXMisc/RichTextFX/pull/468) ([JordanMartinez](https://github.com/JordanMartinez))
- Make areas FXML ready via "@NamedArg\(parameterName\)" [\#466](https://github.com/FXMisc/RichTextFX/pull/466) ([JordanMartinez](https://github.com/JordanMartinez))
- Cleanup the base area's javadoc [\#465](https://github.com/FXMisc/RichTextFX/pull/465) ([JordanMartinez](https://github.com/JordanMartinez))
- Remove middleman: ESD is the model now, not STAModel [\#463](https://github.com/FXMisc/RichTextFX/pull/463) ([JordanMartinez](https://github.com/JordanMartinez))
- Turn assert statement into IllegalArgumentException [\#462](https://github.com/FXMisc/RichTextFX/pull/462) ([JordanMartinez](https://github.com/JordanMartinez))
- Extract style and view-related API into their own interfaces for clarity [\#461](https://github.com/FXMisc/RichTextFX/pull/461) ([JordanMartinez](https://github.com/JordanMartinez))
- Ignore test due to TestFX issue \(should re-enable when fixed\) [\#460](https://github.com/FXMisc/RichTextFX/pull/460) ([JordanMartinez](https://github.com/JordanMartinez))
- Fixes Illegal Argument Exception due to merging an insertion change with a deletion change [\#458](https://github.com/FXMisc/RichTextFX/pull/458) ([JordanMartinez](https://github.com/JordanMartinez))
- Update ReadMe [\#457](https://github.com/FXMisc/RichTextFX/pull/457) ([JordanMartinez](https://github.com/JordanMartinez))
- Add feature: get character bounds on screen [\#455](https://github.com/FXMisc/RichTextFX/pull/455) ([JordanMartinez](https://github.com/JordanMartinez))
- No longer exclude ReactFX transitive dependencies [\#446](https://github.com/FXMisc/RichTextFX/pull/446) ([JordanMartinez](https://github.com/JordanMartinez))
- Update flowless; Exclude transitive ReactFX dependency explicitly [\#445](https://github.com/FXMisc/RichTextFX/pull/445) ([JordanMartinez](https://github.com/JordanMartinez))
- Add context menu [\#439](https://github.com/FXMisc/RichTextFX/pull/439) ([JordanMartinez](https://github.com/JordanMartinez))
- Update custom object example to use interface [\#438](https://github.com/FXMisc/RichTextFX/pull/438) ([JordanMartinez](https://github.com/JordanMartinez))
- Resolve javadoc links to Flowless \(0.5.0\) [\#437](https://github.com/FXMisc/RichTextFX/pull/437) ([JordanMartinez](https://github.com/JordanMartinez))
- Fix dead link to manual download of 0.7-M3 release [\#436](https://github.com/FXMisc/RichTextFX/pull/436) ([JordanMartinez](https://github.com/JordanMartinez))
- Remove deprecated class: InlineStyleTextArea [\#435](https://github.com/FXMisc/RichTextFX/pull/435) ([JordanMartinez](https://github.com/JordanMartinez))
- Line to Paragraph [\#434](https://github.com/FXMisc/RichTextFX/pull/434) ([DaveJarvis](https://github.com/DaveJarvis))
- \(chore\) Resolve links to ReactFX [\#425](https://github.com/FXMisc/RichTextFX/pull/425) ([JordanMartinez](https://github.com/JordanMartinez))
- Add dependency for TestFX tests [\#424](https://github.com/FXMisc/RichTextFX/pull/424) ([JordanMartinez](https://github.com/JordanMartinez))
- Update type to be the new base area type [\#422](https://github.com/FXMisc/RichTextFX/pull/422) ([JordanMartinez](https://github.com/JordanMartinez))
- Update method & behavior: lineStart/End, not paragraphStart/End [\#419](https://github.com/FXMisc/RichTextFX/pull/419) ([JordanMartinez](https://github.com/JordanMartinez))
- Add show methods [\#418](https://github.com/FXMisc/RichTextFX/pull/418) ([JordanMartinez](https://github.com/JordanMartinez))
- Add API: get \# of lines a wrapped paragraph spans [\#416](https://github.com/FXMisc/RichTextFX/pull/416) ([JordanMartinez](https://github.com/JordanMartinez))
- Make `positionCaret` package-private [\#415](https://github.com/FXMisc/RichTextFX/pull/415) ([JordanMartinez](https://github.com/JordanMartinez))
- Deprecate Popup API; add caret/selection bounds properties [\#410](https://github.com/FXMisc/RichTextFX/pull/410) ([JordanMartinez](https://github.com/JordanMartinez))
- fixed -fx-highlight-fill and -fx-caret-blink-rate \(issue \#303\) [\#398](https://github.com/FXMisc/RichTextFX/pull/398) ([JFormDesigner](https://github.com/JFormDesigner))
- Update Arduino Harp dead link [\#393](https://github.com/FXMisc/RichTextFX/pull/393) ([LostArchives](https://github.com/LostArchives))
- Expose API for "scroll\[x/y\]By\(delta\)" [\#391](https://github.com/FXMisc/RichTextFX/pull/391) ([JordanMartinez](https://github.com/JordanMartinez))

## [0.7-M3](https://github.com/FXMisc/RichTextFX/tree/0.7-M3) (2016-12-26)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.7-M3...0.7-M3)

**Closed issues:**

- Inserting caret offset by css padding [\#285](https://github.com/FXMisc/RichTextFX/issues/285)

## [v0.7-M3](https://github.com/FXMisc/RichTextFX/tree/v0.7-M3) (2016-12-26)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/0.7-M2...v0.7-M3)

**Implemented enhancements:**

- Option to not follow the caret on edit [\#102](https://github.com/FXMisc/RichTextFX/issues/102)

**Fixed bugs:**

- Two problems positioning popup relative to caret [\#128](https://github.com/FXMisc/RichTextFX/issues/128)
- StyledTextArea scrolls to the caret every time span style is changed [\#101](https://github.com/FXMisc/RichTextFX/issues/101)

**Closed issues:**

- Underline is hidden by background shape [\#407](https://github.com/FXMisc/RichTextFX/issues/407)
- How to set different styles to words in same paragraph or line ? [\#406](https://github.com/FXMisc/RichTextFX/issues/406)
- Word break navigation needs option for space skipping [\#401](https://github.com/FXMisc/RichTextFX/issues/401)
- Java Keyword Demo [\#400](https://github.com/FXMisc/RichTextFX/issues/400)
- Can RichTextFX be used to select an arbitrary text range for copy/paste operations? [\#388](https://github.com/FXMisc/RichTextFX/issues/388)
- Get the line where the carret is [\#380](https://github.com/FXMisc/RichTextFX/issues/380)
- Can I get a possible caret position, using scene coordinates? [\#368](https://github.com/FXMisc/RichTextFX/issues/368)
- RichTextChange cannot be cast to org.fxmisc.richtext.model.RichTextChange in clear\(\) [\#367](https://github.com/FXMisc/RichTextFX/issues/367)
- 0.7 Problem with the undo/redo method [\#361](https://github.com/FXMisc/RichTextFX/issues/361)
- Meta: Add Kanban-style \(Trello\) boards? [\#359](https://github.com/FXMisc/RichTextFX/issues/359)
- Spamming Ctrl+Z makes the JavaFXApplication crash  [\#358](https://github.com/FXMisc/RichTextFX/issues/358)
- Problem with caret position after text replacement\(FIXED\) [\#353](https://github.com/FXMisc/RichTextFX/issues/353)
- Dual-license: check comply  [\#351](https://github.com/FXMisc/RichTextFX/issues/351)
- CodeArea styling issue [\#347](https://github.com/FXMisc/RichTextFX/issues/347)
- Change to -fx-font-size for CodeArea make scroll bigger [\#344](https://github.com/FXMisc/RichTextFX/issues/344)
- Scrolling Issue [\#343](https://github.com/FXMisc/RichTextFX/issues/343)
- StyledTextArea no longer extends Control [\#342](https://github.com/FXMisc/RichTextFX/issues/342)
- 0.7-M1 has duplicate versions of reactfx [\#341](https://github.com/FXMisc/RichTextFX/issues/341)
- Styles are not applyed to Text [\#340](https://github.com/FXMisc/RichTextFX/issues/340)
- Not usable within SWT \(eclipse\) [\#336](https://github.com/FXMisc/RichTextFX/issues/336)
- Unexpected text drag behavior on abort. [\#321](https://github.com/FXMisc/RichTextFX/issues/321)
- Dotted underline  [\#316](https://github.com/FXMisc/RichTextFX/issues/316)
- VirtualizedScrollPane estimatedScrollYProperty is unstable [\#307](https://github.com/FXMisc/RichTextFX/issues/307)
- \(Shift\)+Shortcut+Left/Right skips one too many word boundaries. [\#200](https://github.com/FXMisc/RichTextFX/issues/200)
- CodeArea in Dialog wrong caret position calculation [\#196](https://github.com/FXMisc/RichTextFX/issues/196)
- Expose API for programmatic PageUp/PageDown [\#195](https://github.com/FXMisc/RichTextFX/issues/195)
- Caret visible on non-editable code area [\#144](https://github.com/FXMisc/RichTextFX/issues/144)

**Merged pull requests:**

- Added background color to spell checker demo. Fixed order of shapes s… [\#408](https://github.com/FXMisc/RichTextFX/pull/408) ([afester](https://github.com/afester))
- Fix computing hit character if padding is set on the paragraph-text [\#396](https://github.com/FXMisc/RichTextFX/pull/396) ([JFormDesigner](https://github.com/JFormDesigner))
- Make `requestFollowCaret` public and wrote its javadoc [\#379](https://github.com/FXMisc/RichTextFX/pull/379) ([JordanMartinez](https://github.com/JordanMartinez))
- Request follow caret [\#378](https://github.com/FXMisc/RichTextFX/pull/378) ([JordanMartinez](https://github.com/JordanMartinez))
- Expose API for page up/down [\#376](https://github.com/FXMisc/RichTextFX/pull/376) ([JordanMartinez](https://github.com/JordanMartinez))
- Relayout popup when any of its settings are invalidated [\#375](https://github.com/FXMisc/RichTextFX/pull/375) ([JordanMartinez](https://github.com/JordanMartinez))
- Handle drag events only if mouse release occurred inside of view [\#371](https://github.com/FXMisc/RichTextFX/pull/371) ([JordanMartinez](https://github.com/JordanMartinez))
- \(Shift\) + Shortcut + Left/Right skips only 1 word boundary, not 2 [\#370](https://github.com/FXMisc/RichTextFX/pull/370) ([JordanMartinez](https://github.com/JordanMartinez))
- Demonstrate via demo the usage of StyledTextArea\#paragraphGraphicFactory [\#364](https://github.com/FXMisc/RichTextFX/pull/364) ([JordanMartinez](https://github.com/JordanMartinez))
- Custom object support [\#356](https://github.com/FXMisc/RichTextFX/pull/356) ([afester](https://github.com/afester))

## [0.7-M2](https://github.com/FXMisc/RichTextFX/tree/0.7-M2) (2016-07-18)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.7-M1...0.7-M2)

**Implemented enhancements:**

- Getting scroll position of CodeArea [\#98](https://github.com/FXMisc/RichTextFX/issues/98)

**Closed issues:**

-  EventHandlerHelper.install\( no longer available [\#339](https://github.com/FXMisc/RichTextFX/issues/339)
- Character index inside Drag&Drop event handler [\#335](https://github.com/FXMisc/RichTextFX/issues/335)
- Add convenience method for getting a character's absolute position; add variant for other related methods [\#330](https://github.com/FXMisc/RichTextFX/issues/330)
- Programmatically select text [\#329](https://github.com/FXMisc/RichTextFX/issues/329)
- Change text color of StyledClassedTextArea with css [\#328](https://github.com/FXMisc/RichTextFX/issues/328)
- DropShadow on TextExt [\#326](https://github.com/FXMisc/RichTextFX/issues/326)
- Update RTFX-specific CSS to use project-specific prefix [\#323](https://github.com/FXMisc/RichTextFX/issues/323)
- NullPointerException after aborted text drag ? [\#320](https://github.com/FXMisc/RichTextFX/issues/320)
- Execute some code after ENTER Key pressed [\#319](https://github.com/FXMisc/RichTextFX/issues/319)
- Flowless issue: Scrollbars' height/length need to be longer [\#312](https://github.com/FXMisc/RichTextFX/issues/312)
- No vertical scrollbar since 0.7-M1 [\#311](https://github.com/FXMisc/RichTextFX/issues/311)
- Expose a "constructor" for EditableStyledDocumentImpl in its interface [\#308](https://github.com/FXMisc/RichTextFX/issues/308)
- Update RichTextFX to use InputMap API from WellBehavedFX's experimental package [\#288](https://github.com/FXMisc/RichTextFX/issues/288)
- decouple RichTextFX from string, use CharSequence [\#282](https://github.com/FXMisc/RichTextFX/issues/282)
- Is there anyway to have an inline css setstyle\(\) for a StyleClassedTextArea or CodeArea please? [\#272](https://github.com/FXMisc/RichTextFX/issues/272)
- VirtualizedScrollPane Doesn't appear in the jar [\#236](https://github.com/FXMisc/RichTextFX/issues/236)
- CodeArea calculates wrong length on initial text insertion [\#211](https://github.com/FXMisc/RichTextFX/issues/211)
- Double-clicking a word selects the word plus the leading or trailing space [\#197](https://github.com/FXMisc/RichTextFX/issues/197)
- RichText-Demo is not working [\#194](https://github.com/FXMisc/RichTextFX/issues/194)
- undo/redo/paste keyboard shortcuts [\#184](https://github.com/FXMisc/RichTextFX/issues/184)
- Set 'step' location for debugger [\#165](https://github.com/FXMisc/RichTextFX/issues/165)

**Merged pull requests:**

- Add variants of editing/navagiting methods that take relative index argument \(paragraph index, column index\) [\#331](https://github.com/FXMisc/RichTextFX/pull/331) ([JordanMartinez](https://github.com/JordanMartinez))
- Fixed text background property in richtext demo [\#327](https://github.com/FXMisc/RichTextFX/pull/327) ([afester](https://github.com/afester))
- Update another RichTextFX-specific CSS property to use the -rtfx pref… [\#325](https://github.com/FXMisc/RichTextFX/pull/325) ([JordanMartinez](https://github.com/JordanMartinez))
- Use the -rtfx prefix for RichTextFX specific underline properties, us… [\#324](https://github.com/FXMisc/RichTextFX/pull/324) ([afester](https://github.com/afester))
- Allow custom caret visibility dependencies and CSS for caret blink rate [\#318](https://github.com/FXMisc/RichTextFX/pull/318) ([JordanMartinez](https://github.com/JordanMartinez))
- Dotted underline [\#317](https://github.com/FXMisc/RichTextFX/pull/317) ([afester](https://github.com/afester))
- Moved model classes in model package [\#313](https://github.com/FXMisc/RichTextFX/pull/313) ([afester](https://github.com/afester))
- Simple esd [\#309](https://github.com/FXMisc/RichTextFX/pull/309) ([JordanMartinez](https://github.com/JordanMartinez))
- Allow customisation of caret visibility criteria and blink rate. [\#279](https://github.com/FXMisc/RichTextFX/pull/279) ([shoaniki](https://github.com/shoaniki))

## [v0.7-M1](https://github.com/FXMisc/RichTextFX/tree/v0.7-M1) (2016-05-13)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.6.10...v0.7-M1)

**Implemented enhancements:**

- Suggestion: Background color controls [\#96](https://github.com/FXMisc/RichTextFX/issues/96)
- Paragraph-level styles [\#6](https://github.com/FXMisc/RichTextFX/issues/6)

**Fixed bugs:**

- IllegalArgumentException when undo-ing then redo-ing [\#216](https://github.com/FXMisc/RichTextFX/issues/216)
- Can't set Font of StyleClassedTextArea [\#125](https://github.com/FXMisc/RichTextFX/issues/125)

**Closed issues:**

- 1.0.0-SNAPSHOT doesn't work? [\#302](https://github.com/FXMisc/RichTextFX/issues/302)
- Use of StyleSpans.subView, and replacing StyleSpans at certain positions [\#301](https://github.com/FXMisc/RichTextFX/issues/301)
- Line number at top of the line and not in the middle [\#300](https://github.com/FXMisc/RichTextFX/issues/300)
- Backspacing a newline character hides the previous line [\#298](https://github.com/FXMisc/RichTextFX/issues/298)
- Background on a text [\#295](https://github.com/FXMisc/RichTextFX/issues/295)
- The positionCaret does not work [\#293](https://github.com/FXMisc/RichTextFX/issues/293)
- Modify font type in StyleClassedTextArea [\#290](https://github.com/FXMisc/RichTextFX/issues/290)
- InlineCssTextArea paragraph style [\#289](https://github.com/FXMisc/RichTextFX/issues/289)
- Move caret position in StyleTextArea by line number [\#286](https://github.com/FXMisc/RichTextFX/issues/286)
- Highlighting a hyperlink [\#283](https://github.com/FXMisc/RichTextFX/issues/283)
- Adding components to Node created by GraphicFactory [\#274](https://github.com/FXMisc/RichTextFX/issues/274)
- Adding components to Node created by GraphicFactory [\#273](https://github.com/FXMisc/RichTextFX/issues/273)
- How to change color of caret? [\#268](https://github.com/FXMisc/RichTextFX/issues/268)
- current line highlighting feature [\#266](https://github.com/FXMisc/RichTextFX/issues/266)
- Bind to text empty [\#264](https://github.com/FXMisc/RichTextFX/issues/264)
- How to remove line highlight in StyleClassedTextArea? [\#262](https://github.com/FXMisc/RichTextFX/issues/262)
- NoSuchMethodError: org.reactfx.Suspendable.suspendWhen\(Ljavafx/beans/value/ObservableValue;\)Lorg/reactfx/Subscription [\#261](https://github.com/FXMisc/RichTextFX/issues/261)
- \[SNAPSHOT\] Regression: mouse cursor is no longer styled as TEXT [\#258](https://github.com/FXMisc/RichTextFX/issues/258)
- \[SNAPSHOT\] Regression: setting key pressed event handler results in silently discarding all key events with non-printable codes [\#257](https://github.com/FXMisc/RichTextFX/issues/257)
- How to set padding for text from borders of editable area? [\#255](https://github.com/FXMisc/RichTextFX/issues/255)
- It's possible to fill paragraph's background? [\#254](https://github.com/FXMisc/RichTextFX/issues/254)
- Cloning issues arise with subclasses [\#252](https://github.com/FXMisc/RichTextFX/issues/252)
- Text that is inserted into an empty StyledTextArea should not inherit deleted style [\#247](https://github.com/FXMisc/RichTextFX/issues/247)
- Make paragraph style arguments always precede text style arguments. [\#245](https://github.com/FXMisc/RichTextFX/issues/245)
- Allow custom implementation of EditableStyledDocument [\#244](https://github.com/FXMisc/RichTextFX/issues/244)
- Better separation of Model from View. [\#241](https://github.com/FXMisc/RichTextFX/issues/241)
- MouseOverTextEvent over specific word of text [\#237](https://github.com/FXMisc/RichTextFX/issues/237)
- focus-traversable broken [\#234](https://github.com/FXMisc/RichTextFX/issues/234)
- Include text background color in demos [\#230](https://github.com/FXMisc/RichTextFX/issues/230)
- Only blink caret when not moving [\#227](https://github.com/FXMisc/RichTextFX/issues/227)
- Overriding Drag Events [\#223](https://github.com/FXMisc/RichTextFX/issues/223)
- PopupDemo regression [\#220](https://github.com/FXMisc/RichTextFX/issues/220)
- Subclass denied acces to CharacterHit [\#218](https://github.com/FXMisc/RichTextFX/issues/218)
- Mouse-Related events aren't overrideable like KeyEvents are [\#217](https://github.com/FXMisc/RichTextFX/issues/217)
- CodeArea Jump to a Specific Line [\#208](https://github.com/FXMisc/RichTextFX/issues/208)
- Remove the Skin architecture [\#206](https://github.com/FXMisc/RichTextFX/issues/206)
- Scaling area scales VirtualFlow's content AND scrollBars [\#205](https://github.com/FXMisc/RichTextFX/issues/205)
- Hide caret on focus loss [\#204](https://github.com/FXMisc/RichTextFX/issues/204)
- CodeArea's caret blink rate is too quick [\#203](https://github.com/FXMisc/RichTextFX/issues/203)
- CodeArea lacks colored focus indicator [\#202](https://github.com/FXMisc/RichTextFX/issues/202)
- -fx-highlight-fill does not work [\#192](https://github.com/FXMisc/RichTextFX/issues/192)
- CodeArea.getStyleSpans\(from, to\).append\(...\) does not append [\#191](https://github.com/FXMisc/RichTextFX/issues/191)
- blinking caret initially not visible [\#189](https://github.com/FXMisc/RichTextFX/issues/189)
- Lines without characters [\#188](https://github.com/FXMisc/RichTextFX/issues/188)
- CodeArea in Tab [\#186](https://github.com/FXMisc/RichTextFX/issues/186)
- Controls not recognized in Scene Builder 2.0 [\#177](https://github.com/FXMisc/RichTextFX/issues/177)
- Typing text at the end of a styled area [\#162](https://github.com/FXMisc/RichTextFX/issues/162)
- Option to show a split view [\#152](https://github.com/FXMisc/RichTextFX/issues/152)
- StyleClassedTextArea stuck when appending 100,000 lines one by one. [\#121](https://github.com/FXMisc/RichTextFX/issues/121)

**Merged pull requests:**

- Removed AreaFactory [\#305](https://github.com/FXMisc/RichTextFX/pull/305) ([JordanMartinez](https://github.com/JordanMartinez))
- Remove unneeded disposal method from behavior [\#299](https://github.com/FXMisc/RichTextFX/pull/299) ([JordanMartinez](https://github.com/JordanMartinez))
- Remove javadoc for x, y params to get rid of warnings [\#297](https://github.com/FXMisc/RichTextFX/pull/297) ([JordanMartinez](https://github.com/JordanMartinez))
- Finish migration to stable release \(WellBehavedFX 0.3\) of new InputMa… [\#296](https://github.com/FXMisc/RichTextFX/pull/296) ([JordanMartinez](https://github.com/JordanMartinez))
- Deprecate StyledTextArea\#positionCaret and point to proper method for… [\#294](https://github.com/FXMisc/RichTextFX/pull/294) ([JordanMartinez](https://github.com/JordanMartinez))
- Add a demo that shows how to override the default behavior and some… [\#292](https://github.com/FXMisc/RichTextFX/pull/292) ([JordanMartinez](https://github.com/JordanMartinez))
- Migrate to WellBehavedFX's InputMapTemplate & InputMap approach [\#291](https://github.com/FXMisc/RichTextFX/pull/291) ([JordanMartinez](https://github.com/JordanMartinez))
- Custom EditableStyledDocument [\#277](https://github.com/FXMisc/RichTextFX/pull/277) ([JordanMartinez](https://github.com/JordanMartinez))
- Make StyledTextAreaModel package-private [\#267](https://github.com/FXMisc/RichTextFX/pull/267) ([JordanMartinez](https://github.com/JordanMartinez))
- Minor code refactoring to get rid of some compiler suggestions: [\#250](https://github.com/FXMisc/RichTextFX/pull/250) ([JordanMartinez](https://github.com/JordanMartinez))
- Better distinguish initial text style from initial paragraph style [\#248](https://github.com/FXMisc/RichTextFX/pull/248) ([JordanMartinez](https://github.com/JordanMartinez))
- Switched generic style objects order: paragraph comes before text \(\<S, PS\> ==\> \<PS, S\>\) [\#246](https://github.com/FXMisc/RichTextFX/pull/246) ([JordanMartinez](https://github.com/JordanMartinez))
- Extracted Model content from StyledTextArea \(view\) into its own model class \(StyledTextAreaModel\) [\#243](https://github.com/FXMisc/RichTextFX/pull/243) ([JordanMartinez](https://github.com/JordanMartinez))
- Fixed tab traversal: can now tab into area [\#239](https://github.com/FXMisc/RichTextFX/pull/239) ([JordanMartinez](https://github.com/JordanMartinez))
- Remove typo: a dependency is listed twice but the first one \(with its… [\#235](https://github.com/FXMisc/RichTextFX/pull/235) ([JordanMartinez](https://github.com/JordanMartinez))
- 2+ Views \(areas\) share the same Model \(document\) [\#232](https://github.com/FXMisc/RichTextFX/pull/232) ([JordanMartinez](https://github.com/JordanMartinez))
- Remove TODO reminder and last remnants of VirtualizedScrollPane [\#229](https://github.com/FXMisc/RichTextFX/pull/229) ([JordanMartinez](https://github.com/JordanMartinez))
- Added `onSelectionDrop ` which allows client to override the handling… [\#228](https://github.com/FXMisc/RichTextFX/pull/228) ([JordanMartinez](https://github.com/JordanMartinez))
- Open up access to CharacterHit for subclasses of StyledTextArea [\#221](https://github.com/FXMisc/RichTextFX/pull/221) ([JordanMartinez](https://github.com/JordanMartinez))
- Makes MouseEvent handling overridable using EventHandlerTemplates [\#219](https://github.com/FXMisc/RichTextFX/pull/219) ([JordanMartinez](https://github.com/JordanMartinez))
- Removed unused fontProperty. [\#215](https://github.com/FXMisc/RichTextFX/pull/215) ([JordanMartinez](https://github.com/JordanMartinez))
- Fix Type typo in AreaFactory [\#214](https://github.com/FXMisc/RichTextFX/pull/214) ([JordanMartinez](https://github.com/JordanMartinez))
- Skin removal [\#213](https://github.com/FXMisc/RichTextFX/pull/213) ([JordanMartinez](https://github.com/JordanMartinez))
- Selection Fix [\#199](https://github.com/FXMisc/RichTextFX/pull/199) ([JordanMartinez](https://github.com/JordanMartinez))
- Exposed Scrolling API for StyledTextArea [\#198](https://github.com/FXMisc/RichTextFX/pull/198) ([JordanMartinez](https://github.com/JordanMartinez))
- Merged proposed paragraph style feature branch of MewesK and the current master branch [\#190](https://github.com/FXMisc/RichTextFX/pull/190) ([jobernolte](https://github.com/jobernolte))

## [v0.6.10](https://github.com/FXMisc/RichTextFX/tree/v0.6.10) (2015-10-12)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.6.9...v0.6.10)

**Closed issues:**

- Change CodeArea default textcolor [\#187](https://github.com/FXMisc/RichTextFX/issues/187)
- Menu bar accelerators do not work when RichTextFX control has focus [\#185](https://github.com/FXMisc/RichTextFX/issues/185)
- Line terminator problems on Windows [\#183](https://github.com/FXMisc/RichTextFX/issues/183)
- Exception when backspacing a single newline character [\#180](https://github.com/FXMisc/RichTextFX/issues/180)

## [v0.6.9](https://github.com/FXMisc/RichTextFX/tree/v0.6.9) (2015-09-18)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.6.8...v0.6.9)

**Closed issues:**

- IOOB Exception when hitting backspace [\#179](https://github.com/FXMisc/RichTextFX/issues/179)
- StackOverflowError in JavaKeywords demo [\#178](https://github.com/FXMisc/RichTextFX/issues/178)
- creating multiple layers of StyleSpans for the StyledTextArea [\#151](https://github.com/FXMisc/RichTextFX/issues/151)
- MouseEvents on InLineCssTextArea [\#11](https://github.com/FXMisc/RichTextFX/issues/11)

## [v0.6.8](https://github.com/FXMisc/RichTextFX/tree/v0.6.8) (2015-09-10)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.6.7...v0.6.8)

**Implemented enhancements:**

- Copy Text With Style. [\#17](https://github.com/FXMisc/RichTextFX/issues/17)

**Closed issues:**

- Lines appearing after coping and refilling text [\#141](https://github.com/FXMisc/RichTextFX/issues/141)
- Syntax highlighting disappears if you copy some code and immediately paste it back [\#135](https://github.com/FXMisc/RichTextFX/issues/135)

## [v0.6.7](https://github.com/FXMisc/RichTextFX/tree/v0.6.7) (2015-09-05)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.6.6...v0.6.7)

**Closed issues:**

- Possibly Memory Leak [\#176](https://github.com/FXMisc/RichTextFX/issues/176)
- Change Default Text Color [\#172](https://github.com/FXMisc/RichTextFX/issues/172)
- Select--drag text moves characters, but not their style [\#169](https://github.com/FXMisc/RichTextFX/issues/169)
- Stack overflow with certain XML files in XMLEditor demo [\#167](https://github.com/FXMisc/RichTextFX/issues/167)
- Possible memory leak [\#159](https://github.com/FXMisc/RichTextFX/issues/159)

**Merged pull requests:**

- Implemented the proposed paragraph style feature \(\#6\) [\#168](https://github.com/FXMisc/RichTextFX/pull/168) ([MewesK](https://github.com/MewesK))

## [v0.6.6](https://github.com/FXMisc/RichTextFX/tree/v0.6.6) (2015-08-18)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.6.5...v0.6.6)

**Implemented enhancements:**

- Support background color for a range of text [\#22](https://github.com/FXMisc/RichTextFX/issues/22)

**Merged pull requests:**

- Implemented background colors for text [\#166](https://github.com/FXMisc/RichTextFX/pull/166) ([MewesK](https://github.com/MewesK))

## [v0.6.5](https://github.com/FXMisc/RichTextFX/tree/v0.6.5) (2015-08-09)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.6.4...v0.6.5)

**Closed issues:**

- Can't drag scroll left [\#161](https://github.com/FXMisc/RichTextFX/issues/161)
- FontSize selection exception in RichText demo \#24 [\#160](https://github.com/FXMisc/RichTextFX/issues/160)
- Moving over whitespace causes exceptions [\#157](https://github.com/FXMisc/RichTextFX/issues/157)
- editable with dragdrop [\#156](https://github.com/FXMisc/RichTextFX/issues/156)
- NoClassDefFoundError in 8u60 [\#155](https://github.com/FXMisc/RichTextFX/issues/155)
- Weird cursor position behavior [\#154](https://github.com/FXMisc/RichTextFX/issues/154)
- Click and highlight URL's in the CodeArea? [\#153](https://github.com/FXMisc/RichTextFX/issues/153)
- How to update font size of an empty line? [\#149](https://github.com/FXMisc/RichTextFX/issues/149)
- Line numbers' font size update with respective line's greatest font size [\#148](https://github.com/FXMisc/RichTextFX/issues/148)
- Text artifacts question [\#145](https://github.com/FXMisc/RichTextFX/issues/145)

**Merged pull requests:**

- Xml editor demo [\#164](https://github.com/FXMisc/RichTextFX/pull/164) ([cemartins](https://github.com/cemartins))

## [v0.6.4](https://github.com/FXMisc/RichTextFX/tree/v0.6.4) (2015-04-21)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.6.3...v0.6.4)

**Closed issues:**

- Question: Set the initial text of the rich text area [\#140](https://github.com/FXMisc/RichTextFX/issues/140)
- No \[ \] { } writable on OS X 10.10.3 in CodeArea [\#139](https://github.com/FXMisc/RichTextFX/issues/139)
- Suggestion: controlling the behavior of caret position and Inserted \(or removed\) characters.  [\#137](https://github.com/FXMisc/RichTextFX/issues/137)
- Spacebar scrolls the scrollpane [\#134](https://github.com/FXMisc/RichTextFX/issues/134)
- Calculating height of a paragraph \(for the purpose of computing page size for printing\) [\#132](https://github.com/FXMisc/RichTextFX/issues/132)
- Vertical movement of the caret doesn't work when we use Collections.emptyList\(\) as separator between words [\#131](https://github.com/FXMisc/RichTextFX/issues/131)
- Setting the vertical \(horizontal as well?\) scrollbar positions [\#130](https://github.com/FXMisc/RichTextFX/issues/130)
- mqtt-spy link [\#129](https://github.com/FXMisc/RichTextFX/issues/129)
- More access to the line numbers [\#127](https://github.com/FXMisc/RichTextFX/issues/127)
- Noticeable Input Latency -- can something be done ?  [\#126](https://github.com/FXMisc/RichTextFX/issues/126)
- Loading \(by replacing\) a document with a ReadOnlyStyledDocument doesn't display text [\#123](https://github.com/FXMisc/RichTextFX/issues/123)
- Scroll direction \(Up/Down\) [\#122](https://github.com/FXMisc/RichTextFX/issues/122)

## [v0.6.3](https://github.com/FXMisc/RichTextFX/tree/v0.6.3) (2015-02-28)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.6.2.1...v0.6.3)

**Implemented enhancements:**

- Support selection by dragging mouse outside view port [\#116](https://github.com/FXMisc/RichTextFX/issues/116)

**Fixed bugs:**

- Wrap text doesn't work well in CodeAreas [\#114](https://github.com/FXMisc/RichTextFX/issues/114)

**Closed issues:**

- Text rendering artifacts when window size is changed [\#120](https://github.com/FXMisc/RichTextFX/issues/120)
- StyleClassedTextArea consumes too much memory [\#119](https://github.com/FXMisc/RichTextFX/issues/119)
- Breaking changes in 0.6.2 [\#118](https://github.com/FXMisc/RichTextFX/issues/118)

## [v0.6.2.1](https://github.com/FXMisc/RichTextFX/tree/v0.6.2.1) (2015-02-24)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.6.2...v0.6.2.1)

**Closed issues:**

- setStyle\(\) to define selection background color broken. [\#117](https://github.com/FXMisc/RichTextFX/issues/117)

## [v0.6.2](https://github.com/FXMisc/RichTextFX/tree/v0.6.2) (2015-02-24)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.6.1...v0.6.2)

**Fixed bugs:**

- Flickering  in 0.6.1 [\#115](https://github.com/FXMisc/RichTextFX/issues/115)
- selectRange\(\) is very slow [\#99](https://github.com/FXMisc/RichTextFX/issues/99)

**Closed issues:**

- With tabs, only one tab shows text [\#113](https://github.com/FXMisc/RichTextFX/issues/113)
- View port scroll issues [\#110](https://github.com/FXMisc/RichTextFX/issues/110)

## [v0.6.1](https://github.com/FXMisc/RichTextFX/tree/v0.6.1) (2015-02-23)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.6...v0.6.1)

**Fixed bugs:**

- Surprisingly slow when adding text one character at a time. [\#112](https://github.com/FXMisc/RichTextFX/issues/112)
- Line space is not adjusted after changing font size using CodeArea.setStyle\(\). [\#100](https://github.com/FXMisc/RichTextFX/issues/100)
- Text overlapping when single line is divided into several display lines [\#95](https://github.com/FXMisc/RichTextFX/issues/95)
- Text-Rendering Issue in v5 [\#88](https://github.com/FXMisc/RichTextFX/issues/88)

**Closed issues:**

- Copy/Paste Exception [\#111](https://github.com/FXMisc/RichTextFX/issues/111)
- Missing. import org.reactfx.InterceptableEventStream; [\#109](https://github.com/FXMisc/RichTextFX/issues/109)
- Dependensies in version 0.5.1 are broken [\#108](https://github.com/FXMisc/RichTextFX/issues/108)

## [v0.6](https://github.com/FXMisc/RichTextFX/tree/v0.6) (2015-02-09)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.5.1...v0.6)

**Closed issues:**

- Bug: Selection Composite [\#105](https://github.com/FXMisc/RichTextFX/issues/105)
- Some Feedback and Suggestions [\#104](https://github.com/FXMisc/RichTextFX/issues/104)
- How to set the font and font-size of a CodeArea [\#103](https://github.com/FXMisc/RichTextFX/issues/103)
- Running demos within NetBeans \(8.0.1\) [\#94](https://github.com/FXMisc/RichTextFX/issues/94)
- Changing color of Characters like "{","}","\(","\)",";" etc. [\#93](https://github.com/FXMisc/RichTextFX/issues/93)
- Changing caret color in Code area. [\#92](https://github.com/FXMisc/RichTextFX/issues/92)
- Code Formatting Support [\#91](https://github.com/FXMisc/RichTextFX/issues/91)
- Cut, Copy, Paste [\#55](https://github.com/FXMisc/RichTextFX/issues/55)

## [v0.5.1](https://github.com/FXMisc/RichTextFX/tree/v0.5.1) (2014-10-21)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.5...v0.5.1)

**Closed issues:**

- AltGr doesn't work [\#89](https://github.com/FXMisc/RichTextFX/issues/89)
- Is there way to control the size of a tab? [\#86](https://github.com/FXMisc/RichTextFX/issues/86)
- Work in lexer-parser feature. [\#85](https://github.com/FXMisc/RichTextFX/issues/85)

## [v0.5](https://github.com/FXMisc/RichTextFX/tree/v0.5) (2014-09-29)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.4.4...v0.5)

**Merged pull requests:**

- Change format management for line numbers. [\#84](https://github.com/FXMisc/RichTextFX/pull/84) ([UzielSilva](https://github.com/UzielSilva))
- Change stylesheet management for line numbers. [\#83](https://github.com/FXMisc/RichTextFX/pull/83) ([UzielSilva](https://github.com/UzielSilva))
- Adjust .gitignore for Intellij IDEA Project. [\#82](https://github.com/FXMisc/RichTextFX/pull/82) ([UzielSilva](https://github.com/UzielSilva))

## [v0.4.4](https://github.com/FXMisc/RichTextFX/tree/v0.4.4) (2014-08-27)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.4.3...v0.4.4)

## [v0.4.3](https://github.com/FXMisc/RichTextFX/tree/v0.4.3) (2014-08-26)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.4.2...v0.4.3)

**Closed issues:**

- Overriding the default EventHandler [\#80](https://github.com/FXMisc/RichTextFX/issues/80)
- Email [\#79](https://github.com/FXMisc/RichTextFX/issues/79)
- Large paragraphs don't make the control grow vertically [\#78](https://github.com/FXMisc/RichTextFX/issues/78)
- LineNumberFactory causes ArrayIndexOutOfBoundsException in JavaFX Application Thread.  [\#76](https://github.com/FXMisc/RichTextFX/issues/76)

## [v0.4.2](https://github.com/FXMisc/RichTextFX/tree/v0.4.2) (2014-08-13)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.4.1...v0.4.2)

**Closed issues:**

- Weird Drawing Artifacts [\#75](https://github.com/FXMisc/RichTextFX/issues/75)
- text alignment  [\#74](https://github.com/FXMisc/RichTextFX/issues/74)
- paste rtf text [\#73](https://github.com/FXMisc/RichTextFX/issues/73)

## [v0.4.1](https://github.com/FXMisc/RichTextFX/tree/v0.4.1) (2014-07-11)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.4...v0.4.1)

**Closed issues:**

- Styling broken with R.04. \(Running RichText Demo\) [\#71](https://github.com/FXMisc/RichTextFX/issues/71)
- text render anomalies surfaced with r.04 [\#70](https://github.com/FXMisc/RichTextFX/issues/70)
- previousWord and NonDigits includes the precending whitespace [\#69](https://github.com/FXMisc/RichTextFX/issues/69)
- getIndexRange\(\) on StyledText in reference to text area. Enhancement suggestion [\#66](https://github.com/FXMisc/RichTextFX/issues/66)

## [v0.4](https://github.com/FXMisc/RichTextFX/tree/v0.4) (2014-07-07)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.3...v0.4)

**Closed issues:**

- Hitting ESC while text is selected, erases it. \(tested with richtextfx demo\) [\#68](https://github.com/FXMisc/RichTextFX/issues/68)
- Scene update error when closing a stage that has a popup. [\#67](https://github.com/FXMisc/RichTextFX/issues/67)
- paragraph.getSegments\(\) duplicates. [\#65](https://github.com/FXMisc/RichTextFX/issues/65)
- Text moves when paragraphsize exceedes the editorwindow. [\#25](https://github.com/FXMisc/RichTextFX/issues/25)

## [v0.3](https://github.com/FXMisc/RichTextFX/tree/v0.3) (2014-06-19)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.2...v0.3)

**Closed issues:**

- CodeField [\#64](https://github.com/FXMisc/RichTextFX/issues/64)
- Question: Multiple styles in code area [\#62](https://github.com/FXMisc/RichTextFX/issues/62)

## [v0.2](https://github.com/FXMisc/RichTextFX/tree/v0.2) (2014-06-09)
[Full Changelog](https://github.com/FXMisc/RichTextFX/compare/v0.1...v0.2)

**Closed issues:**

- End greater than length error [\#61](https://github.com/FXMisc/RichTextFX/issues/61)
- Missing dependencies   [\#59](https://github.com/FXMisc/RichTextFX/issues/59)
- Scrolling Issue. [\#58](https://github.com/FXMisc/RichTextFX/issues/58)
- Can't compile JavaKeywords Demo [\#57](https://github.com/FXMisc/RichTextFX/issues/57)
-  Testing a matching algorithm [\#56](https://github.com/FXMisc/RichTextFX/issues/56)
- Tool tip support [\#54](https://github.com/FXMisc/RichTextFX/issues/54)
- Out of order error [\#53](https://github.com/FXMisc/RichTextFX/issues/53)
- Enhancement: Popup positioning. [\#36](https://github.com/FXMisc/RichTextFX/issues/36)
- CSS color not working on InLineCssTextArea [\#10](https://github.com/FXMisc/RichTextFX/issues/10)

## [v0.1](https://github.com/FXMisc/RichTextFX/tree/v0.1) (2014-05-12)
**Implemented enhancements:**

- Position a popup window next to the caret \(was: Get Caret position In Terms of Screen X and Y\) [\#30](https://github.com/FXMisc/RichTextFX/issues/30)
- Method to get style range at the current position. [\#26](https://github.com/FXMisc/RichTextFX/issues/26)
- Style-aware undo/redo [\#7](https://github.com/FXMisc/RichTextFX/issues/7)
- API to set/get multiple style ranges at once [\#5](https://github.com/FXMisc/RichTextFX/issues/5)
- Feature request: set text range background  [\#4](https://github.com/FXMisc/RichTextFX/issues/4)
- Property to turn on and off line wrapping [\#3](https://github.com/FXMisc/RichTextFX/issues/3)

**Closed issues:**

- Coping style spans [\#52](https://github.com/FXMisc/RichTextFX/issues/52)
- Undo Issues.  [\#51](https://github.com/FXMisc/RichTextFX/issues/51)
- IndexRange of style span [\#50](https://github.com/FXMisc/RichTextFX/issues/50)
- Enhancement: Style Match [\#49](https://github.com/FXMisc/RichTextFX/issues/49)
- Q: How to apply multiple styles at the same time ? [\#48](https://github.com/FXMisc/RichTextFX/issues/48)
- Dependency issue with reactfx [\#47](https://github.com/FXMisc/RichTextFX/issues/47)
- ENTER key + typing.. [\#45](https://github.com/FXMisc/RichTextFX/issues/45)
- ENTER key behavior [\#44](https://github.com/FXMisc/RichTextFX/issues/44)
- Out of bounds error on applying style to a selection [\#43](https://github.com/FXMisc/RichTextFX/issues/43)
- Enhancement: provide IndexRange for Paragraph [\#42](https://github.com/FXMisc/RichTextFX/issues/42)
- "OutOfBoundsException" When hitting Enter [\#39](https://github.com/FXMisc/RichTextFX/issues/39)
- Enhancement: Split StyledDocument by A Regex [\#38](https://github.com/FXMisc/RichTextFX/issues/38)
- Override PopUp Location/ stop it from updating based on a boolean condition. [\#37](https://github.com/FXMisc/RichTextFX/issues/37)
- Undo has strange behaviour [\#34](https://github.com/FXMisc/RichTextFX/issues/34)
- request: add index range property to styled text [\#33](https://github.com/FXMisc/RichTextFX/issues/33)
- Change styles at the caretposition [\#32](https://github.com/FXMisc/RichTextFX/issues/32)
- Can't change background properties of an InlineCssTextArea [\#29](https://github.com/FXMisc/RichTextFX/issues/29)
- EasyBind once more [\#28](https://github.com/FXMisc/RichTextFX/issues/28)
- EasyBind not valid [\#27](https://github.com/FXMisc/RichTextFX/issues/27)
- Error when hitting ENTER within styled text [\#24](https://github.com/FXMisc/RichTextFX/issues/24)
- Saving Content to file. [\#23](https://github.com/FXMisc/RichTextFX/issues/23)
- TextWrap Not Working. [\#20](https://github.com/FXMisc/RichTextFX/issues/20)
- NPE at javafx.scene.text.Text.getSpanBounds\(Text.java:292\) [\#15](https://github.com/FXMisc/RichTextFX/issues/15)
- Create Maven Snapshots [\#14](https://github.com/FXMisc/RichTextFX/issues/14)
- IndexOutOfBoundsException when using positionCaret [\#13](https://github.com/FXMisc/RichTextFX/issues/13)
- Type error in MyListView [\#8](https://github.com/FXMisc/RichTextFX/issues/8)
- Add an inline style to a text segment [\#2](https://github.com/FXMisc/RichTextFX/issues/2)
- Caret Position [\#1](https://github.com/FXMisc/RichTextFX/issues/1)

**Merged pull requests:**

- Documented Java 8 development workarounds [\#9](https://github.com/FXMisc/RichTextFX/pull/9) ([jeffreyguenther](https://github.com/jeffreyguenther))



\* *This Change Log was automatically generated by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*