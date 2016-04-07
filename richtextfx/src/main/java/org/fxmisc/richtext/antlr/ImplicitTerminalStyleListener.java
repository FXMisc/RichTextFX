package org.fxmisc.richtext.antlr;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.lang.reflect.Field;

/**
 * Created by Geoff on 4/7/2016.
 */
public class ImplicitTerminalStyleListener implements StructuredTextAreaListener.SemanticAnalysisListener {

    @Override
    public RangeMap<Integer, String> generateNewStyles(StructuredTextArea parent, TerminalNode terminalOnNewTree) {
        Field symNamesField;
        try {
            symNamesField = parent.getParserClass().getField("VOCABULARY");
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        symNamesField.setAccessible(true);

        Vocabulary symNames;
        try {
            symNames = (Vocabulary) symNamesField.get(null);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        Token symbol = terminalOnNewTree.getSymbol();
        int typeIndex = symbol.getType();

        String terminalName = symNames.getDisplayName(typeIndex);
        String style = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, terminalName);

        Range<Integer> targetRange = Range.closed(symbol.getStartIndex(), symbol.getStopIndex());

        throw new UnsupportedOperationException("uhh, more to do but, nifty. Specifically: dont want to lookup by non-capitolized name");
//        return ImmutableRangeMap.of(targetRange, style);
    }

}
