package org.fxmisc.richtext.antlr;

/**
 * Created by Geoff on 4/7/2016.
 */
public class StructuredTextAreaTest {

    // So, how can I structure these tests (for the structured text area)?
    // I think i can supply a couple of grammars at in the tests and assert that they're loaded properly
    // maybe split those into use cases?

    //so, unique things about this extension:
    // 1 - order of calls:
    //      first 51653156156156156156156156156156156
    //      first lexical
    //      then semantic, in order of outer-most production to inner-most production
    //      then error

    // assert that auto-label correctly finds names, include oddly-named parser rules.

    // assert that auto-error properly labels an error

    // I need to assert that the ranges are built properly, maybe i can push that to StreamExtensions test

    // snap ranges: assert that when I use Range.open or range.closedOpen, etc, that the styles are built correctly

    // also, write these in kotlin (scala even?!)
    // Is there a good reason to write tests in java?

    //each of the highlighters themselves can be tested individually, perhalps aside from the ones above
    // accessible via boolean flags.
}
