package org.fxmisc.richtext.antlr;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import org.antlr.v4.runtime.Token;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Created by Geoff on 4/7/2016.
 */
public class LexicalBracketCountingHighlighter implements StructuredTextAreaHighlighter.LexicalAnalysisListener {

    @Override
    public RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, RangeMap<Integer, Token> tokensByCharIndex) {

        int currentCharIndex = parent.getCaretPosition();

        List<Token> tokens = ImmutableList.copyOf(tokensByCharIndex.asMapOfRanges().values());

        Optional<Integer> tokenIndexCandidate = Optional.ofNullable(tokensByCharIndex.get(currentCharIndex)).map(Token::getTokenIndex);

        TreeRangeMap<Integer, String> bracketsSoFar = TreeRangeMap.create();
        if (!tokenIndexCandidate.isPresent()) {
            return bracketsSoFar;
        }
        int tokenIndex = tokenIndexCandidate.get();

        if (!isBracket(tokens, tokenIndex) && tokens.get(tokenIndex).getStartIndex() == currentCharIndex) {
            tokenIndex -= 1;
        }

        if (!isBracket(tokens, tokenIndex)) {
            return bracketsSoFar;
        }

        Token openingBracketToken = tokens.get(tokenIndex);

        Range<Integer> openingRange = Range.closed(
                openingBracketToken.getStartIndex(),
                openingBracketToken.getStopIndex()
        );
        bracketsSoFar.put(openingRange, "bracket");

        Optional<Integer> counterpartsCandidateIndex = findIndexOfCounterpart(tokens, tokenIndex);
        if (!counterpartsCandidateIndex.isPresent()) {
            return bracketsSoFar;
        }
        int counterpartsIndex = counterpartsCandidateIndex.get();

        Token closingBracketToken = tokens.get(counterpartsIndex);

        Range<Integer> closingRange = Range.closed(
                closingBracketToken.getStartIndex(),
                closingBracketToken.getStopIndex()
        );
        bracketsSoFar.put(closingRange, "bracket");

        return bracketsSoFar;
    }

    private Optional<Integer> findIndexOfCounterpart(List<Token> tokens, int tokenIndex) {

        Token currentToken = tokens.get(tokenIndex);
        String openingBracketText = currentToken.getText();

        UnaryOperator<Integer> moveNext = current -> openingBracketText.equals("(") ? current + 1 : current - 1;

        int openCount = 0;
        do {
            currentToken = tokens.get(tokenIndex);
            if (currentToken.getText().equals("(")) {
                openCount += 1;
            }
            if (currentToken.getText().equals(")")) {
                openCount -= 1;
            }

            if (openCount == 0) {
                break;
            }

            tokenIndex = moveNext.apply(tokenIndex);
        }
        while (currentToken.getType() != Token.EOF);

        if (currentToken.getType() == Token.EOF) {
            return Optional.empty();
        }

        return Optional.of(tokenIndex);
    }

    private boolean isBracket(List<Token> tokens, int tokenIndex) {
        String text = tokens.get(tokenIndex).getText();
        return text.equals("(") || text.equals(")");
    }
}
