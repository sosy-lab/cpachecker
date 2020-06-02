// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/* 
 * Lexer Rules
 */
lexer grammar LtlLexer;


/*
 * Rules for ltl syntax (= DEFAULT_MODE)
 */

// LOGIC
TRUE                : 'TRUE' | 'True' | 'true' | '1' ;
FALSE               : 'FALSE' | 'False' | 'false' | '0' ;

// Logical Unary
NOT                 : '!' | 'NOT' ;

// Logical Binary
IMP                 : '->' | '-->' | '=>' | '==>' | 'IMP' ;
EQUIV               : '<->' | '<=>' | 'EQUIV' ;
XOR                 : '^' | 'XOR' ;

// Logical n-ary
AND                 : '&&' | '&' | 'AND' ;
OR                  : '||' | '|' | 'OR' ;

// Modal Unary
FINALLY             : 'F' | '<>' ;
GLOBALLY            : 'G' | '[]' ;
NEXT                : 'X' ;

// Modal Binary
UNTIL               : 'U' ;
WUNTIL              : 'W' | 'WU' ;
RELEASE             : 'R' | 'V' ;
SRELEASE            : 'S' ;

// Parantheses
LPAREN              : '(' ;
RPAREN              : ')' ;

// Necessary keywords for parsing a ltl-property file (that is, a file ending with "*.prp")
CHECK               : 'CHECK' ;
INIT                : 'init' ;
LTL                 : 'LTL' ;
COMMA               : ',' ;
QUOTATIONMARK_START : '"' -> mode(C_EXPRESSION) ;
FUNCTIONNAME        : LETTER+ '()' ;

// Variables (a lower-case identifier followed by any letter or number)
VARIABLE            : LOWERCASE (LETTER | NUMBER)* ;

// Whitespace
WS                  : [ \t\r\n\f] -> skip ;   // skip spaces, tabs, newlines


/*
 * Lexer rules for content within quotationmarks ( == CExpressions )
 */

mode C_EXPRESSION;

QUOTATIONMARK_END   : '"' -> mode(DEFAULT_MODE) ;
PARAM               : LETTER+ ;
COMPARATOR          : EQ | INEQ | GEQ | LEQ | GT | LT ;
MATHOP              : PLUS | MINUS | TIMES | DIV | POW ;
VALUE               : MINUS? NUMBER+ ;    // match any non-empty number

EQ                  : '==' ;
INEQ                : '!=' ;
GEQ                 : '>=' ;
LEQ                 : '<=' ;
GT                  : '>' ;
LT                  : '<' ;

fragment SIGN       : (PLUS | MINUS) ;
PLUS                : '+' ;
MINUS               : '-' ;
TIMES               : '*' ;
DIV                 : '/' ;
POW                 : '^' ;

// Whitespace
SPACES              : [ ] -> skip ;   // skip spaces


/*
 * Inline functions for a more readable grammar and a better maintainability. Valid in all modes.
 */
fragment LETTER     : (LOWERCASE | UPPERCASE) ;
fragment LOWERCASE  : [a-z] ;
fragment UPPERCASE  : [A-Z] ;
fragment NUMBER     : [0-9] ;

