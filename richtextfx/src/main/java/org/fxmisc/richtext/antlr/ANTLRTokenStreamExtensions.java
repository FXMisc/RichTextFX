package org.fxmisc.richtext.antlr;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

/**
 * Created by Geoff on 4/6/2016.
 */
public class ANTLRTokenStreamExtensions{

    public static RangeMap<Integer, Token> indexByCharacterRange(TokenStream tokenStream){

        ImmutableRangeMap.Builder<Integer, Token> tokensByCharacterIndex = ImmutableRangeMap.builder();

        for(int tokenIdx = 0; tokenIdx < tokenStream.size(); tokenIdx++){
            //man, I really like antlr as a library, but I haven't written one of these in... years...
            //makes me feel kinda gross

            Token token = tokenStream.get(tokenIdx);

            if(token.getType() == Token.EOF){ continue; }

            Range<Integer> targetRange = Range.closed(token.getStartIndex(), token.getStopIndex());
            tokensByCharacterIndex.put(targetRange, token);
        }

        return tokensByCharacterIndex.build();
    }
}
