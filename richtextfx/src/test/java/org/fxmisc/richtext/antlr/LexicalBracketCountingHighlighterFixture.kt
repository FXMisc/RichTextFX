
package org.fxmisc.richtext.antlr

//import org.assertj.guava.api.Assertions.assertThat
//import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Created by Geoff on 4/11/2016.
 */
class wLexicalBracketCountingHighlighterFixture {

    @Test fun when_highlighting_a_selected_bracket_should_properly_highlight_corresponding_bracket(){

        //setup
        val listener = LexicalBracketCountingHighlighter("(", ")", "bracket")
        val textArea = StructuredTextArea(
                "org.fxmisc.richtext.parser.TestLangParser",
                "org.fxmisc.richtext.parser.TestLangLexer",
                "body"
        )
        //indexes                                     |18  |23
        textArea.replaceText(0, 0, "BracketHell: (()()(()()))")

        //act
        val styles = listener.generateNewStyles(textArea, textArea.tokensByCharIndex)

        //assert
//        assertThat(styles.get(18)).isEqualTo("bracket");
//        assertThat(styles.get(23)).isEqualTo("bracket");
    }
}