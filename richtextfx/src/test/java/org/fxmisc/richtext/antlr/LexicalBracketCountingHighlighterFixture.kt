package org.fxmisc.richtext.antlr

import org.junit.Test

/**
 * Created by Geoff on 4/11/2016.
 */
class LexicalBracketCountingHighlighterFixture {

    @Test fun when_stuff(){
        var listener = LexicalBracketCountingHighlighter("", "", "")

        listener.generateNewStyles()
    }
}