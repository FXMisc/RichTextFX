// Generated from C:\Users\Geoff\Code\RichTextFX\richtextfx-demos\src\main\antlr\JavishMath.g4 by ANTLR 4.5

    package org.fxmisc.richtext.parser;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link JavishMathParser}.
 */
public interface JavishMathListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link JavishMathParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(JavishMathParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavishMathParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(JavishMathParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavishMathParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(JavishMathParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavishMathParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(JavishMathParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavishMathParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(JavishMathParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavishMathParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(JavishMathParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavishMathParser#comment}.
	 * @param ctx the parse tree
	 */
	void enterComment(JavishMathParser.CommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavishMathParser#comment}.
	 * @param ctx the parse tree
	 */
	void exitComment(JavishMathParser.CommentContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavishMathParser#variable}.
	 * @param ctx the parse tree
	 */
	void enterVariable(JavishMathParser.VariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavishMathParser#variable}.
	 * @param ctx the parse tree
	 */
	void exitVariable(JavishMathParser.VariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link JavishMathParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(JavishMathParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavishMathParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(JavishMathParser.LiteralContext ctx);
}