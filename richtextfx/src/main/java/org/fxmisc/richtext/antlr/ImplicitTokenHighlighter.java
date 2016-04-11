package org.fxmisc.richtext.antlr;

import com.google.common.base.CaseFormat;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by Geoff on 4/7/2016.
 */
public class ImplicitTokenHighlighter implements StructuredHighlighters.TokenHighlighter {


    //TODO proper terminal highlighter
    // consider sample.g4

    // parser rule
    // something : expr MULTI expr ';' ;
    //
    // lexer rules:
    // MULTI :  '*' | '/';
    // TIMES :  '*';
    // DIVIDE:  '/';

    // a token highlighter generates two style classes, "times" and "divide",
    // whereas a terminal highlighter would generate just one: "multi"

    // unfortunately that's a boatload more recursion, unless I'm missing something, :sigh:

    Cache<StructuredTextArea, Vocabulary> vocabByParent = CacheBuilder.newBuilder().maximumSize(1).build();

    @Override
    public RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, RangeMap<Integer, Token> newTokenStream) {

        Vocabulary vocab;
        try { vocab = vocabByParent.get(parent, () -> reflectivelyFindVocabulary(parent));}
        catch (ExecutionException e) { throw new RuntimeException(e); }

        ImmutableRangeMap.Builder<Integer, String> tokenStyles = ImmutableRangeMap.builder();

        for(Map.Entry<Range<Integer>, Token> kvp : newTokenStream.asMapOfRanges().entrySet()){

            Range<Integer> targetRange = kvp.getKey();
            Token symbol = kvp.getValue();

            int typeIndex = symbol.getType();

            String terminalName = vocab.getSymbolicName(typeIndex);

            if(terminalName == null){ continue; }

            String style = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, terminalName);
            //TODO scrub 'illegal' (unconventional?) css characters,
            // http://stackoverflow.com/questions/448981/which-characters-are-valid-in-css-class-names-selectors
            // and assert the result a legal css identifier?

            tokenStyles.put(targetRange, style);
        }

        return tokenStyles.build();
    }

    //TODO pull this up onto the StructuredTextArea?
    // I feel like that class should hide these little nasty reflection-ee details,
    private Vocabulary reflectivelyFindVocabulary(StructuredTextArea parent) {
        Field symNamesField;
        try { symNamesField = parent.getParserClass().getField("VOCABULARY"); }
        catch (NoSuchFieldException e) { throw new RuntimeException(e); }

        assert Modifier.isStatic(symNamesField.getModifiers()) : "field not static? '" + symNamesField + "'";
        symNamesField.setAccessible(true);

        Vocabulary symNames;
        try { symNames = (Vocabulary) symNamesField.get(null); }
        catch (IllegalAccessException e) { throw new RuntimeException(e); }

        return symNames;
    }
}


