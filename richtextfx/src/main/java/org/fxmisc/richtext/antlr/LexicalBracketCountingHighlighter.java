package org.fxmisc.richtext.antlr;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import javafx.beans.NamedArg;
import org.antlr.v4.runtime.Token;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

//TODO docs
// note that this does not use parser rules,
// meaning if you have special contexts you want brackets excluded from,
// you'll have to do it with semantics or bundle the bracket in a lexcal rule.
// but that seems exceedingly rare.
/**
 * Created by Geoff on 4/7/2016.
 */
public class LexicalBracketCountingHighlighter implements StructuredHighlighter.TokenHighlighter {

    private final String openingBracket;
    private final String closingBracket;
    private final String styleClass;

    public LexicalBracketCountingHighlighter(@NamedArg(value="openingBracket", defaultValue="(") String openingBracket,
                                             @NamedArg(value="closingBracket", defaultValue=")") String closingBracket,
                                             @NamedArg(value="styleClass", defaultValue="bracket") String styleClass){

        this.openingBracket = openingBracket;
        this.closingBracket = closingBracket;
        this.styleClass = styleClass;
    }

    @Override
    public RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, RangeMap<Integer, Token> tokensByCharIndex) {

        // I dont usually use final locals but I've been burned twice by reassigning stack vars,
        // so I'm strictly SSA for this method

        final int currentCharIndex = parent.getCaretPosition();

        final Optional<Token> openingBracketTokenCandidate = Arrays.asList(
                tokensByCharIndex.get(currentCharIndex),
                tokensByCharIndex.get(currentCharIndex - 1)
        )
                .stream()
                .filter(t -> t != null)
                .filter(this::isBracket)
                .findFirst();

        if ( ! openingBracketTokenCandidate.isPresent()){ return StructuredHighlighter.NO_NEW_HIGHLIGHTS; }
        final Token openingBracketToken = openingBracketTokenCandidate.get();

        final List<Token> tokens = ImmutableList.copyOf(tokensByCharIndex.asMapOfRanges().values());
        final ImmutableRangeMap.Builder<Integer, String> bracketsSoFar = ImmutableRangeMap.builder();

        final Range<Integer> openingRange = Range.closed(
                openingBracketToken.getStartIndex(),
                openingBracketToken.getStopIndex()
        );
        bracketsSoFar.put(openingRange, styleClass);

        final Optional<Integer> counterpartsCandidateIndex = findIndexOfCounterpart(tokens, openingBracketToken);
        if ( ! counterpartsCandidateIndex.isPresent()) { return bracketsSoFar.build(); }
        int counterpartsIndex = counterpartsCandidateIndex.get();

        final Token closingBracketToken = tokens.get(counterpartsIndex);

        final Range<Integer> closingRange = Range.closed(
                closingBracketToken.getStartIndex(),
                closingBracketToken.getStopIndex()
        );
        bracketsSoFar.put(closingRange, styleClass);

        return bracketsSoFar.build();
    }

    private Optional<Integer> findIndexOfCounterpart(List<Token> tokens, Token currentToken) {

        int tokenIndex = currentToken.getTokenIndex();
        String openingBracketText = currentToken.getText();

        UnaryOperator<Integer> moveNext =
                current -> openingBracketText.equals(openingBracket) ? current + 1 : current - 1;

        int openCount = 0;
        while(tokenIndex < tokens.size() && tokenIndex >= 0){

            currentToken = tokens.get(tokenIndex);

            if (currentToken.getText().equals(openingBracket)) {
                openCount += 1;
            }
            if (currentToken.getText().equals(closingBracket)) {
                openCount -= 1;
            }

            if (openCount == 0) {
                break;
            }

            tokenIndex = moveNext.apply(tokenIndex);
        }

        if (tokenIndex < 0 || tokenIndex >= tokens.size()) {
            return Optional.empty();
        }

        return Optional.of(tokenIndex);
    }

    private boolean isBracket(Token token) {
        String text = token.getText();
        return text.equals(openingBracket) || text.equals(closingBracket);
    }
}
