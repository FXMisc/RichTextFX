grammar JavishMath;

//TODO: why cant i specify this from the build system?
@header {
    package org.fxmisc.richtext.parser;
}

block : statement* EOF;

statement
    : 'var' variable '=' expr ';'
    | comment
    ;

expr
    : variable '->' expr
    | (literal | variable)
    | '(' expr ')'
    | '-' expr
    | expr '^' expr
    | expr ('*' | '/' | '%') expr
    | expr ('+' | '-') expr
    | expr ('<' | '<=' | '>' | '>=' | '==') expr
    ;

comment : COMMENT;
variable : VARIABLE;
literal : INTEGER | FLOAT;

INTEGER : DIGIT+ ;
FLOAT   : DIGIT* '.' DIGIT+ ( ('e'|'E') '-'? DIGIT+)? ;
DIGIT   : '0'..'9';

VARIABLE      : VARIABLE_START VARIABLE_PART*;

LAMBDA        : '->' ;

LT            : '<' ;
LTEQ          : '<=' ;
GT            : '>' ;
GTEQ          : '>=' ;
EQ            : '==' ;

MULT          : '*' ;
DIV           : '/' ;
MOD           : '%' ;

PLUS          : '+' ;
MINUS         : '-' ;

EXPONENT      : '^' ;

OPEN_PAREN    : '(' ;
CLOSE_PAREN   : ')' ;
OPEN_BRACKET  : '[' ;
CLOSE_BRACKET : ']' ;

COMMA         : ',' ;

fragment
VARIABLE_START
    : [a-zA-Z_]
    | ~[\u0000-\u00FF\uD800-\uDBFF] //non 'surrogate' unicode
    | [\uD800-\uDBFF] [\uDC00-\uDFFF] // covers UTF-16 surrogate pair-encodings for U+10000 to U+10FFFF
    ;

fragment
VARIABLE_PART
    : [0-9]
    | VARIABLE_START
    ;

COMMENT       :   '//' ~[\r\n]*;
LINEBREAKS    : ('\r' | '\n' | ' ') -> skip ;
