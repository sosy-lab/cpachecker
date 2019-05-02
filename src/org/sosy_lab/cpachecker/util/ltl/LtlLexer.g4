/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */

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

