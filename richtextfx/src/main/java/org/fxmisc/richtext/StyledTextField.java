package org.fxmisc.richtext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.fxmisc.richtext.model.EditableStyledDocument;

import javafx.application.Application;
import javafx.beans.NamedArg;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.css.CssMetaData;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.AccessibleRole;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

/**
 * A text field whose segment generic has been specified to be a {@link String}. How the text
 * will be styled is not yet specified in this class, but use {@link StyleClassedTextField} for a style class
 * approach to styling the text and {@link InlineCssTextField} for an inline css approach to styling the text.
 *
 * <p>Use CSS Style Class ".styled-text-field" for styling the control.</p>
 * 
 * @param <PS> type of paragraph style
 * @param <S> type of style that can be applied to text.
 * 
 * @author Jurgen
 */
public abstract class StyledTextField<PS, S> extends StyledTextArea<PS, S>
{
    private final static List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA_LIST;

    private final static CssMetaData<StyledTextField,TextAlignment> TEXT_ALIGNMENT = new CustomCssMetaData<>(
        "-fx-alignment", (StyleConverter<?,TextAlignment>) StyleConverter.getEnumConverter(TextAlignment.class),
        TextAlignment.LEFT, s -> (StyleableObjectProperty) s.alignmentProperty()
    );

    private final static CssMetaData<StyledTextField,Paint> PROMPT_TEXT_FILL = new CustomCssMetaData<>(
        "-fx-prompt-text-fill", (StyleConverter<?,Paint>) StyleConverter.getPaintConverter(),
        Color.GRAY, s -> (StyleableObjectProperty) s.promptTextFillProperty()
    );

    private final static Pattern VERTICAL_WHITESPACE = Pattern.compile( "\\v+" );
    private final static String STYLE_SHEET;
    private final static double HEIGHT;
    static {
        List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(GenericStyledArea.getClassCssMetaData());
        styleables.add( PROMPT_TEXT_FILL );  styleables.add( TEXT_ALIGNMENT );
        CSS_META_DATA_LIST = Collections.unmodifiableList(styleables);

        String globalCSS = System.getProperty( "javafx.userAgentStylesheetUrl" ); // JavaFX preference!
        if ( globalCSS == null ) globalCSS = Application.getUserAgentStylesheet();
        if ( globalCSS == null ) globalCSS = Application.STYLESHEET_MODENA;
        globalCSS = "styled-text-field-"+ globalCSS.toLowerCase() +".css";
        STYLE_SHEET = StyledTextField.class.getResource( globalCSS ).toExternalForm();

        // Ugly hack to get a TextFields default height :(
        // as it differs between Caspian, Modena, etc.
        TextField tf = new TextField( "GetHeight" );
        new Scene(tf); tf.applyCss(); tf.layout();
        HEIGHT = tf.getHeight();
    }

    private boolean selectAll = true;
    private StyleableObjectProperty<TextAlignment> textAlignment;
    private StyleableObjectProperty<Paint> promptFillProp;


    public StyledTextField(@NamedArg("initialParagraphStyle") PS initialParagraphStyle,
            @NamedArg("applyParagraphStyle")   BiConsumer<TextFlow, PS> applyParagraphStyle,
            @NamedArg("initialTextStyle")      S initialTextStyle,
            @NamedArg("applyStyle")            BiConsumer<? super TextExt, S> applyStyle,
            @NamedArg("document")              EditableStyledDocument<PS, String, S> document)
    {
        super( initialParagraphStyle, applyParagraphStyle, initialTextStyle, applyStyle, document, true );

        getStylesheets().add( STYLE_SHEET );
        getStyleClass().setAll( "styled-text-field" );

        setAccessibleRole( AccessibleRole.TEXT_FIELD );
        setPrefSize( 135, HEIGHT );

        addEventFilter( KeyEvent.KEY_PRESSED, KE -> {
            if ( KE.getCode() == KeyCode.ENTER ) {
                fireEvent( new ActionEvent( this, null ) );
                KE.consume();
            }
            else if ( KE.getCode() == KeyCode.TAB ) {
                traverse( this.getParent(), this, KE.isShiftDown() ? -1 : +1 );
                KE.consume();
            }
        });

        addEventFilter( MouseEvent.MOUSE_PRESSED, ME -> selectAll = isFocused() );

        focusedProperty().addListener( (ob,was,focused) -> {
            if ( ! was && focused && selectAll ) {
                selectRange( getLength(), 0 );
            }
            else if ( ! focused && was ) {
                moveTo( 0 ); requestFollowCaret();
            }
            selectAll = true;
        });

        super.setWrapText( false );
        wrapTextProperty().addListener( (ob,ov,wrap) -> {
            if ( wrap ) { // veto any changes
                wrapTextProperty().unbind();
                super.setWrapText(false);
            }
        });
    }

    /*
     * There's no public API to move the focus forward or backward
     * without explicitly knowing the node. So here's a basic local
     * implementation to accomplish that.
     */
    private Node traverse( Parent p, Node from, int dir )
    {
        if ( p == null ) return null;

        List<Node> nodeList = p.getChildrenUnmodifiable();
        int len = nodeList.size();
        int neighbor = -1;
        
        if ( from != null ) while ( ++neighbor < len && nodeList.get(neighbor) != from );
        else if ( dir == 1 ) neighbor = -1;
        else neighbor = len;
        
        for ( neighbor += dir; neighbor > -1 && neighbor < len; neighbor += dir ) {

            Node target = nodeList.get( neighbor );

            if ( target instanceof Pane || target instanceof Group ) {
                target = traverse( (Parent) target, null, dir ); // down
                if ( target != null ) return target;
            }
            else if ( target.isVisible() && ! target.isDisabled() && target.isFocusTraversable() ) {
                target.requestFocus();
                return target;
            }
        }

        return traverse( p.getParent(), p, dir ); // up
    }

    /**
     * Specifies how the text should be aligned when there is empty space within the TextField.
     * To configure via CSS use {@code -fx-alignment:} and values from {@link javafx.scene.text.TextAlignment}.
     */
    public final ObjectProperty<TextAlignment> alignmentProperty() {
        if (textAlignment == null) {
            textAlignment = new CustomStyleableProperty<>( TextAlignment.LEFT, "textAlignment", this, TEXT_ALIGNMENT );
            textAlignment.addListener( (ob,ov,alignment) -> changeAlignment( alignment ) );
        }
        return textAlignment;
    }
    public final TextAlignment getAlignment() { return textAlignment == null ? TextAlignment.LEFT : textAlignment.getValue(); }
    public final void setAlignment( TextAlignment value ) { alignmentProperty().setValue( value ); }
    protected abstract void changeAlignment( TextAlignment txtAlign );

    /**
     * The action handler associated with this text field, or {@code null} if no action handler is assigned.
     * The action handler is normally called when the user types the ENTER key.
     */
    private ObjectProperty<EventHandler<ActionEvent>> onAction = new ObjectPropertyBase<EventHandler<ActionEvent>>() {
        @Override
        protected void invalidated() {
            setEventHandler(ActionEvent.ACTION, get());
        }

        @Override
        public Object getBean() {
            return StyledTextField.this;
        }

        @Override
        public String getName() {
            return "onAction";
        }
    };
    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() { return onAction; }
    public final EventHandler<ActionEvent> getOnAction() { return onActionProperty().get(); }
    public final void setOnAction(EventHandler<ActionEvent> value) { onActionProperty().set(value); }


    /**
     * The prompt text to display or <tt>null</tt> if no prompt text is to be displayed.
     * <p>The Text will be aligned according to the text fields alignment setting and have a default
     * text fill of GRAY unless you have changed it by any means, e.g. with CSS "-fx-prompt-text-fill" 
     */
    public final ObjectProperty<? super Text> promptTextProperty() { return placeholderProperty(); }
    public final Text getPromptText() { return getPlaceholder() instanceof Text ? (Text) getPlaceholder() : null; }
    public final void setPromptText( Text value ) { setPlaceholder( value ); }
    @Override protected void configurePlaceholder( Node placeholder )
    {
        placeholder.layoutYProperty().bind( Bindings.createDoubleBinding( () ->
            (getHeight() - placeholder.getLayoutBounds().getHeight()) / 2 + Math.abs( placeholder.getLayoutBounds().getMinY() ),
            heightProperty(), placeholder.layoutBoundsProperty() )
        );
        
        placeholder.layoutXProperty().bind( Bindings.createDoubleBinding( () -> calcHorizontalPos(),
            widthProperty(), placeholder.layoutBoundsProperty(), paddingProperty(), alignmentProperty() )
        );
        
        if ( placeholder instanceof Text && ((Text) placeholder).getFill() == Color.BLACK ) {
            ((Text) placeholder).fillProperty().bind( promptTextFillProperty() );
        }
    }

    private final ObjectProperty<Paint> promptTextFillProperty() {
        if ( promptFillProp == null ) {
            promptFillProp = new CustomStyleableProperty<>( Color.GRAY, "promptFill", this, PROMPT_TEXT_FILL );
        }
        return promptFillProp;
    }

    private double calcHorizontalPos()
    {
        double leftPad = getPadding().getLeft();
        double rightPad = getPadding().getRight();
        double promptWidth = getPlaceholder().getLayoutBounds().getWidth();
        TextAlignment alignment = getAlignment();
        double alignmentPadding = leftPad;
        
        if ( alignment == TextAlignment.RIGHT ) alignmentPadding = rightPad;
        else if ( alignment == TextAlignment.CENTER ) alignmentPadding = 0;
            
        if ( promptWidth < (getWidth() - alignmentPadding) ) setClip( null );
        else setClip( new Rectangle( getWidth(), getHeight() ) );

        switch ( alignment )
        {
            case CENTER : return (getWidth() - promptWidth) / 2;
            case RIGHT  : return getWidth() - rightPad - promptWidth;
            default     : return leftPad;
        }
    }

    @Override
    public void replaceText( int start, int end, String text )
    {
        super.replaceText( start, end, VERTICAL_WHITESPACE.matcher( text ).replaceAll( " " ) );
    }

    public void setText( String text )
    {
        replaceText( text );
    }

    /** This is a <b>no op</b> for text fields and therefore marked as <i>deprecated</i>. */
    @Override @Deprecated public void setWrapText( boolean value ) {}
    /** This <u>always</u> returns <b>false</b> for styled text fields. */
    @Override public boolean isWrapText() { return false; }


    @Override public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CSS_META_DATA_LIST;
    }
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS_META_DATA_LIST;
    }
}
