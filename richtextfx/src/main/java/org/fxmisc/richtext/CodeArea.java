package org.fxmisc.richtext;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.NamedArg;
import org.fxmisc.richtext.model.EditableStyledDocument;

/**
 * A convenience subclass of {@link StyleClassedTextArea} with fixed-width font and an undo manager that observes
 * only plain text changes (not styled changes). It's style class is {@code code-area}.
 */
public class CodeArea extends StyleClassedTextArea {

    {
        getStyleClass().add("code-area");

        // load the default style that defines a fixed-width font
        getStylesheets().add(CodeArea.class.getResource("code-area.css").toExternalForm());

        // don't apply preceding style to typed text
        setUseInitialStyleForInsertion(true);
    }
    
    /**
     * Creates an area that can render and edit the same {@link EditableStyledDocument} as another {@link CodeArea}.
     */
    public CodeArea(@NamedArg("document") EditableStyledDocument<Collection<String>, String, Collection<String>> document) {
        super(document, false);
    }

    /**
     * Creates an area with no text.
     */
    public CodeArea() {
        super(false);
    }

    /**
     * Creates a text area with initial text content.
     * Initial caret position is set at the beginning of text content.
     *
     * @param text Initial text content.
     */
    public CodeArea(@NamedArg("text") String text) {
        this();

        appendText(text);
        getUndoManager().forgetHistory();
        getUndoManager().mark();

        // position the caret at the beginning
        selectRange(0, 0);
    }

    protected Pattern WORD_PATTERN = Pattern.compile( "\\w+", Pattern.UNICODE_CHARACTER_CLASS );
    protected Pattern WORD_OR_SYMBOL = Pattern.compile(
            "([\\W&&[^\\h]]{2}"    // Any two non-word characters (excluding white spaces), matches like:
                                   // !=  <=  >=  ==  +=  -=  *=  --  ++  ()  []  <>  &&  ||  //  /*  */
            +"|\\w*)"              // Zero or more word characters [a-zA-Z_0-9]
            +"\\h*"                // Both cases above include any trailing white space
            , Pattern.UNICODE_CHARACTER_CLASS
        );

    /**
     * Skips ONLY 1 number of word boundaries backwards.
     * @param n is ignored !
     */
    @Override
    public void wordBreaksBackwards(int n, SelectionPolicy selectionPolicy)
    {
        if ( getLength() == 0 ) return;

        CaretSelectionBind<?,?,?> csb = getCaretSelectionBind();
        int paragraph = csb.getParagraphIndex();
        int position = csb.getColumnPosition(); 
        int prevWord = 0;

        if ( position == 0 ) {
            prevWord = getParagraph( --paragraph ).length();
            moveTo( paragraph, prevWord, selectionPolicy );
            return;
        }
        
        Matcher m = WORD_OR_SYMBOL.matcher( getText( paragraph ) );
        
        while ( m.find() )
        {
            if ( m.start() == position ) {
                moveTo( paragraph, prevWord, selectionPolicy );
                break;
            }
            if ( (prevWord = m.end()) >= position ) {
                moveTo( paragraph, m.start(), selectionPolicy );
                break;
            }
        }
    }
    
    /**
     * Skips ONLY 1 number of word boundaries forward.
     * @param n is ignored !
     */
    @Override
    public void wordBreaksForwards(int n, SelectionPolicy selectionPolicy)
    {
        if ( getLength() == 0 ) return;

        CaretSelectionBind<?,?,?> csb = getCaretSelectionBind();
        int paragraph = csb.getParagraphIndex();
        int position = csb.getColumnPosition(); 
        
        Matcher m = WORD_OR_SYMBOL.matcher( getText( paragraph ) );
        
        while ( m.find() )
        {
            if ( m.start() > position ) {
                moveTo( paragraph, m.start(), selectionPolicy );
                break;
            }
            if ( m.hitEnd() ) {
                moveTo( paragraph+1, 0, selectionPolicy );
            }
        }
    }
    
    @Override
    public void selectWord()
    {
        if ( getLength() == 0 ) return;

        CaretSelectionBind<?,?,?> csb = getCaretSelectionBind();
        int paragraph = csb.getParagraphIndex();
        int position = csb.getColumnPosition(); 
        
        Matcher m = WORD_PATTERN.matcher( getText( paragraph ) );

        while ( m.find() )
        {
            if ( m.end() > position ) {
                csb.selectRange( paragraph, m.start(), paragraph, m.end() );
                return;
            }
        }
    }
}
