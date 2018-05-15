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
grammar LtlGrammar;

options {
  language = Java;
}

/*
 * Parser Rules
 */

property : CHECK LPAREN initFunction COMMA ltlProperty RPAREN EOF ;

initFunction : INIT LPAREN FUNCTIONNAME RPAREN ;

ltlProperty : LTL LPAREN root=expression RPAREN ;

formula : root=expression EOF ;

expression : orExpression ;

orExpression : andExpression (OR andExpression)* ;

andExpression : binaryExpression (AND binaryExpression)* ;

binaryExpression : left=unaryExpression op=binaryOp right=binaryExpression # binaryOperation
                 | unaryExpression # binaryUnary
                 ;

unaryExpression : op=unaryOp inner=binaryExpression # unaryOperation
                | atomExpression # unaryAtom
                ;

atomExpression : constant=bool # boolean
               | QUOTATIONMARK var=VARIABLE EQUALS val=VALUE QUOTATIONMARK # quotedVariable
               | var=VARIABLE # variable
               | LPAREN nested=expression RPAREN # nested
               ;

unaryOp : NOT
        | FINALLY
        | GLOBALLY
        | NEXT
        ;

binaryOp : EQUIV
         | IMP
         | XOR
         | UNTIL
         | WUNTIL
         | RELEASE
         | SRELEASE
         ;

bool : TRUE
     | FALSE
     ;


/* 
 * Lexer Rules
 */

// LOGIC
TRUE            : 'TRUE' | 'True' | 'true' | '1' ;
FALSE           : 'FALSE' | 'False' | 'false' | '0' ;

// Logical Unary
NOT             : '!' | 'NOT' ;

// Logical Binary
IMP             : '->' | '-->' | '=>' | '==>' | 'IMP' ;
EQUIV           : '<->' | '<=>' | 'EQUIV' ;
XOR             : '^' | 'XOR' ;

// Logical n-ary
AND             : '&&' | '&' | 'AND' ;
OR              : '||' | '|' | 'OR' ;

// Modal Unary
FINALLY         : 'F' | '<>' ;
GLOBALLY        : 'G' | '[]' ;
NEXT            : 'X' ;

// Modal Binary
UNTIL           : 'U' ;
WUNTIL          : 'W' | 'WU' ;
RELEASE         : 'R' | 'V' ;
SRELEASE        : 'S' ; 

// Parantheses
LPAREN          : '(' ;
RPAREN          : ')' ;

// Keywords for parsing a ltl-property file (that is, a file ending with "*.prp")
CHECK : 'CHECK' ;
INIT : 'init' ;
LTL : 'LTL' ;
COMMA : ',' ;
QUOTATIONMARK : '"' ;
EQUALS : '==' ;
FUNCTIONNAME : [a-zA-Z]+ '()' ;

// Variables
VARIABLE        : [a-z][a-zA-Z0-9]* ;      // match lower-case identifier followed by any letter or number

// Values
VALUE           : [0-9]+ ;      // match any non-empty number

// Whitespace
WS              : [ \t\r\n\f] -> skip ;   // skip spaces, tabs, newlines

