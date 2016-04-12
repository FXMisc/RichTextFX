grammar TestLang;

@header {
    package org.fxmisc.richtext.parser;
}

body
    : DIRECT_TO_TOKEN ';'
    | indirect_to_token ';'
    | '{' sub_expr '}' ';'
    | bracket_hell
    | nonterminal_token
    ;

sub_expr
    : '(' sub_expr ')'
    | sub_expr ('+' | '-' | '*' | '/') sub_expr
    | LITERAL
    ;

indirect_to_token   : 'DIRECT_TO_TOKEN' ;
bracket_hell        : 'BracketHell:' ( OPEN_PAREN CLOSE_PAREN? )* ';' ;
nonterminal_token   : 'NonterminalToken:' NONTERMINAL_TOKEN ';' ;

DIRECT_TO_TOKEN     : 'A' ;
NONTERMINAL_TOKEN   : LOWER_A | LOWER_B | LOWER_C ;
BRACKET_LABEL       : 'BracketHell:' ;

LITERAL         : [0-9]+ ;

SEMI_COLON      : ';' ;

OPEN_PAREN      : '(' ;
CLOSE_PAREN     : ')' ;

OPEN_CURLEY     : '{' ;
CLOSE_CURLEY    : '}' ;

LOWER_A : 'a' ;
LOWER_B : 'b' ;
LOWER_C : 'c' ;

COMMENT         :   '//' ~[\r\n]*;
LINEBREAKS      : ('\r' | '\n' | ' ') -> skip ;
