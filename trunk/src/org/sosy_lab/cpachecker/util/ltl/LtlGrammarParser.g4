// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
 
parser grammar LtlGrammarParser;

options {
  tokenVocab = LtlLexer;
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
               | QUOTATIONMARK_START var=PARAM comp=COMPARATOR val=VALUE (MATHOP VALUE)* QUOTATIONMARK_END # quotedVariable
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

