package org.fxmisc.richtext.antlr;

import com.google.common.base.CaseFormat;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.ExecutionException;

/**
 * Created by Geoff on 4/7/2016.
 */
public class ImplicitTerminalStyleHighlighter implements StructuredTextAreaHighlighter.SemanticAnalysisListener {

    Cache<StructuredTextArea, Vocabulary> vocabByParent = CacheBuilder.newBuilder().maximumSize(1).build();

    @Override
    public RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, TerminalNode terminalOnNewTree) {

        Vocabulary vocab = null;
        try { vocab = vocabByParent.get(parent, () -> reflectivelyFindVocabulary(parent));}
        catch (ExecutionException e) { throw new RuntimeException(e); }

        Token symbol = terminalOnNewTree.getSymbol();
        int typeIndex = symbol.getType();

        String terminalName = vocab.getDisplayName(typeIndex);
        String style = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, terminalName);

        Range<Integer> targetRange = Range.closed(symbol.getStartIndex(), symbol.getStopIndex());

        return ImmutableRangeMap.of(targetRange, style);
    }

    //TODO pull this up onto the StructuredTextArea?
    // I feel like that class should hide these little nasty reflection-ee details,
    // especially since the ANTLR genreated class is
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


