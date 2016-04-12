// Generated from D:\Users\Geoff\Code\RichTextFX\richtextfx-demos\src\main\antlr\JavishMath.g4 by ANTLR 4.5

    package org.fxmisc.richtext.parser;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class JavishMathLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, INTEGER=3, FLOAT=4, DIGIT=5, VARIABLE=6, VAR=7, LAMBDA=8, 
		LT=9, LTEQ=10, GT=11, GTEQ=12, EQ=13, MULT=14, DIV=15, MOD=16, PLUS=17, 
		MINUS=18, EXPONENT=19, OPEN_PAREN=20, CLOSE_PAREN=21, OPEN_BRACKET=22, 
		CLOSE_BRACKET=23, COMMA=24, COMMENT=25, LINEBREAKS=26;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "INTEGER", "FLOAT", "DIGIT", "VARIABLE", "VAR", "LAMBDA", 
		"LT", "LTEQ", "GT", "GTEQ", "EQ", "MULT", "DIV", "MOD", "PLUS", "MINUS", 
		"EXPONENT", "OPEN_PAREN", "CLOSE_PAREN", "OPEN_BRACKET", "CLOSE_BRACKET", 
		"COMMA", "VARIABLE_START", "VARIABLE_PART", "COMMENT", "LINEBREAKS"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'='", "';'", null, null, null, null, "'var'", "'->'", "'<'", "'<='", 
		"'>'", "'>='", "'=='", "'*'", "'/'", "'%'", "'+'", "'-'", "'^'", "'('", 
		"')'", "'['", "']'", "','"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, "INTEGER", "FLOAT", "DIGIT", "VARIABLE", "VAR", "LAMBDA", 
		"LT", "LTEQ", "GT", "GTEQ", "EQ", "MULT", "DIV", "MOD", "PLUS", "MINUS", 
		"EXPONENT", "OPEN_PAREN", "CLOSE_PAREN", "OPEN_BRACKET", "CLOSE_BRACKET", 
		"COMMA", "COMMENT", "LINEBREAKS"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public JavishMathLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "JavishMath.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\34\u00a5\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\3\2\3\2\3\3\3\3\3\4\6\4"+
		"A\n\4\r\4\16\4B\3\5\7\5F\n\5\f\5\16\5I\13\5\3\5\3\5\6\5M\n\5\r\5\16\5"+
		"N\3\5\3\5\5\5S\n\5\3\5\6\5V\n\5\r\5\16\5W\5\5Z\n\5\3\6\3\6\3\7\3\7\7\7"+
		"`\n\7\f\7\16\7c\13\7\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\n\3\n\3\13\3\13\3\13"+
		"\3\f\3\f\3\r\3\r\3\r\3\16\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22"+
		"\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30\3\31"+
		"\3\31\3\32\3\32\3\32\3\32\5\32\u0093\n\32\3\33\3\33\5\33\u0097\n\33\3"+
		"\34\3\34\3\34\3\34\7\34\u009d\n\34\f\34\16\34\u00a0\13\34\3\35\3\35\3"+
		"\35\3\35\2\2\36\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31"+
		"\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\2\65\2"+
		"\67\339\34\3\2\n\4\2GGgg\5\2C\\aac|\4\2\2\u0101\ud802\udc01\3\2\ud802"+
		"\udc01\3\2\udc02\ue001\3\2\62;\4\2\f\f\17\17\5\2\f\f\17\17\"\"\u00ad\2"+
		"\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2"+
		"\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2"+
		"\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2"+
		"\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2"+
		"\2\61\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\3;\3\2\2\2\5=\3\2\2\2\7@\3\2\2\2"+
		"\tG\3\2\2\2\13[\3\2\2\2\r]\3\2\2\2\17d\3\2\2\2\21h\3\2\2\2\23k\3\2\2\2"+
		"\25m\3\2\2\2\27p\3\2\2\2\31r\3\2\2\2\33u\3\2\2\2\35x\3\2\2\2\37z\3\2\2"+
		"\2!|\3\2\2\2#~\3\2\2\2%\u0080\3\2\2\2\'\u0082\3\2\2\2)\u0084\3\2\2\2+"+
		"\u0086\3\2\2\2-\u0088\3\2\2\2/\u008a\3\2\2\2\61\u008c\3\2\2\2\63\u0092"+
		"\3\2\2\2\65\u0096\3\2\2\2\67\u0098\3\2\2\29\u00a1\3\2\2\2;<\7?\2\2<\4"+
		"\3\2\2\2=>\7=\2\2>\6\3\2\2\2?A\5\13\6\2@?\3\2\2\2AB\3\2\2\2B@\3\2\2\2"+
		"BC\3\2\2\2C\b\3\2\2\2DF\5\13\6\2ED\3\2\2\2FI\3\2\2\2GE\3\2\2\2GH\3\2\2"+
		"\2HJ\3\2\2\2IG\3\2\2\2JL\7\60\2\2KM\5\13\6\2LK\3\2\2\2MN\3\2\2\2NL\3\2"+
		"\2\2NO\3\2\2\2OY\3\2\2\2PR\t\2\2\2QS\7/\2\2RQ\3\2\2\2RS\3\2\2\2SU\3\2"+
		"\2\2TV\5\13\6\2UT\3\2\2\2VW\3\2\2\2WU\3\2\2\2WX\3\2\2\2XZ\3\2\2\2YP\3"+
		"\2\2\2YZ\3\2\2\2Z\n\3\2\2\2[\\\4\62;\2\\\f\3\2\2\2]a\5\63\32\2^`\5\65"+
		"\33\2_^\3\2\2\2`c\3\2\2\2a_\3\2\2\2ab\3\2\2\2b\16\3\2\2\2ca\3\2\2\2de"+
		"\7x\2\2ef\7c\2\2fg\7t\2\2g\20\3\2\2\2hi\7/\2\2ij\7@\2\2j\22\3\2\2\2kl"+
		"\7>\2\2l\24\3\2\2\2mn\7>\2\2no\7?\2\2o\26\3\2\2\2pq\7@\2\2q\30\3\2\2\2"+
		"rs\7@\2\2st\7?\2\2t\32\3\2\2\2uv\7?\2\2vw\7?\2\2w\34\3\2\2\2xy\7,\2\2"+
		"y\36\3\2\2\2z{\7\61\2\2{ \3\2\2\2|}\7\'\2\2}\"\3\2\2\2~\177\7-\2\2\177"+
		"$\3\2\2\2\u0080\u0081\7/\2\2\u0081&\3\2\2\2\u0082\u0083\7`\2\2\u0083("+
		"\3\2\2\2\u0084\u0085\7*\2\2\u0085*\3\2\2\2\u0086\u0087\7+\2\2\u0087,\3"+
		"\2\2\2\u0088\u0089\7]\2\2\u0089.\3\2\2\2\u008a\u008b\7_\2\2\u008b\60\3"+
		"\2\2\2\u008c\u008d\7.\2\2\u008d\62\3\2\2\2\u008e\u0093\t\3\2\2\u008f\u0093"+
		"\n\4\2\2\u0090\u0091\t\5\2\2\u0091\u0093\t\6\2\2\u0092\u008e\3\2\2\2\u0092"+
		"\u008f\3\2\2\2\u0092\u0090\3\2\2\2\u0093\64\3\2\2\2\u0094\u0097\t\7\2"+
		"\2\u0095\u0097\5\63\32\2\u0096\u0094\3\2\2\2\u0096\u0095\3\2\2\2\u0097"+
		"\66\3\2\2\2\u0098\u0099\7\61\2\2\u0099\u009a\7\61\2\2\u009a\u009e\3\2"+
		"\2\2\u009b\u009d\n\b\2\2\u009c\u009b\3\2\2\2\u009d\u00a0\3\2\2\2\u009e"+
		"\u009c\3\2\2\2\u009e\u009f\3\2\2\2\u009f8\3\2\2\2\u00a0\u009e\3\2\2\2"+
		"\u00a1\u00a2\t\t\2\2\u00a2\u00a3\3\2\2\2\u00a3\u00a4\b\35\2\2\u00a4:\3"+
		"\2\2\2\r\2BGNRWYa\u0092\u0096\u009e\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}