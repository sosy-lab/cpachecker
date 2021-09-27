// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Queue;
import java_cup.runtime.Symbol;

@javax.annotation.processing.Generated("JFlex")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"URF_UNREAD_FIELD", "DLS_DEAD_LOCAL_STORE"})
%%

%class ACSLScanner
%unicode
%cup
%line
%column

%{
    StringBuilder builder = new StringBuilder();
    boolean nextAnnotation = false;
    int currentAnnotation = -1;
    Queue<java_cup.runtime.Symbol> deq = new ArrayDeque<>(1);

    private Symbol symbol(int type) {
        return new Symbol(type, yyline, yycolumn);
    }

    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline, yycolumn, value);
    }

    public java_cup.runtime.Symbol getNext() throws IOException {
        if (nextAnnotation) {
          currentAnnotation++;
          nextAnnotation = false;
        }
        if (!deq.isEmpty()) {
            return deq.remove();
        } else {
            return next_token();
        }
    }

    private void addTokenToQueue(java_cup.runtime.Symbol sym) {
        deq.offer(sym);
    }

    public int getCurrentAnnotation() {
      return currentAnnotation;
    }
%}

LineBreak   = \r|\n|\r\n
Space       = \s|@
DecInt      = (0 | [1-9][0-9]*)[uU]?[lL]?[lL]?
String      = \".*\"
CType       = _bool|float|(long[ \t\f]+)?double|(un)?signed|
              ((un)?signed[ \t\f]+)?(char|short|short[ \t\f]+int|int|long|long[ \t\f]+int|long[ \t\f]+long|long[ \t\f]+long[ \t\f]+int)
ACSLType    = boolean|integer|real
Identifier  = [_a-zA-Z][_a-zA-Z0-9]*

%state SINGLE_LINE_ANNOTATION, MULTI_LINE_ANNOTATION

%%

<YYINITIAL> {
   "//@"                {yybegin(SINGLE_LINE_ANNOTATION); nextAnnotation = true;
                        return symbol(ACSLSymbols.NEXTCONTRACT);}
   "/*@"                {yybegin(MULTI_LINE_ANNOTATION);  nextAnnotation = true;
                        return symbol(ACSLSymbols.NEXTCONTRACT);}
   {LineBreak}          {/* do nothing */}
   .                    {/* do nothing */}
}

<SINGLE_LINE_ANNOTATION> {
    {LineBreak}         {yybegin(YYINITIAL);}
}

<MULTI_LINE_ANNOTATION> {
    "*/"                {yybegin(YYINITIAL);}
    {LineBreak}         {/* do nothing */}
}

<SINGLE_LINE_ANNOTATION, MULTI_LINE_ANNOTATION> {
    "\\true"            {return symbol(ACSLSymbols.TRUE);}
    "\\false"           {return symbol(ACSLSymbols.FALSE);}
    "["                 {return symbol(ACSLSymbols.LBRACKET);}
    "]"                 {return symbol(ACSLSymbols.RBRACKET);}
    "?"                 {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.QUESTION);}
    "!"                 {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.NEG);}
    "~"                 {return symbol(ACSLSymbols.BNEG);}
    "*"                 {return symbol(ACSLSymbols.STAR);}
    "/"                 {return symbol(ACSLSymbols.DIVIDE);}
    "%"                 {return symbol(ACSLSymbols.MOD);}
    "+"                 {return symbol(ACSLSymbols.PLUS);}
    "-"                 {return symbol(ACSLSymbols.MINUS);}
    "<<"                {return symbol(ACSLSymbols.LSHIFT);}
    ">>"                {return symbol(ACSLSymbols.RSHIFT);}
    "&"                 {return symbol(ACSLSymbols.AMPERSAND);}
    "|"                 {return symbol(ACSLSymbols.BOR);}
    "^"                 {return symbol(ACSLSymbols.BXOR);}
    "-->"               {return symbol(ACSLSymbols.BIMP);}
    "<-->"              {return symbol(ACSLSymbols.BEQV);}
    "&&"                {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.AND);}
    "||"                {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.OR);}
    "==>"               {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.IMP);}
    "<==>"              {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.EQV);}
    "^^"                {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.XOR);}
    "=="                {return symbol(ACSLSymbols.EQ);}
    "!="                {return symbol(ACSLSymbols.NEQ);}
    "<="                {return symbol(ACSLSymbols.LEQ);}
    ">="                {return symbol(ACSLSymbols.GEQ);}
    ">"                 {return symbol(ACSLSymbols.GT);}
    "<"                 {return symbol(ACSLSymbols.LT);}
    "("                 {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.LPAREN);}
    ")"                 {return symbol(ACSLSymbols.RPAREN);}
    "for"               {return symbol(ACSLSymbols.FOR);}
    "sizeof"            {return symbol(ACSLSymbols.SIZEOF);}
    "behavior"          {return symbol(ACSLSymbols.BEHAVIOR);}
    "complete behaviors"    {return symbol(ACSLSymbols.COMPLETE_BEHAVIORS);}
    "disjoint behaviors"    {return symbol(ACSLSymbols.DISJOINT_BEHAVIORS);}
    "ensures"           {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.ENS);}
    "requires"          {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.REQ);}
    "loop invariant"    {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.LINVARIANT);}
    "assert"            {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.ASSERT);}
    "check"             {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.CHECK);}
    "assumes"           {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.ASS);}
    ":"                 {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.COLON);}
    ","                 {return symbol(ACSLSymbols.COMMA);}
    ";"                 {addTokenToQueue(symbol(ACSLSymbols.PRED_START)); return symbol(ACSLSymbols.SEMI);}
    "\\old"             {return symbol(ACSLSymbols.OLD);}
    "\\result"          {return symbol(ACSLSymbols.RETVAL);}
    "\\forall"          {return symbol(ACSLSymbols.FORALL);}
    "\\exists"          {return symbol(ACSLSymbols.EXISTS);}
    {DecInt}            {builder.setLength(0); String matched = yytext().toLowerCase();
                        while (matched.endsWith("u") || matched.endsWith("l")) {
                          matched = matched.substring(0, matched.length() - 1);
                        }
                        return symbol(ACSLSymbols.LITERAL, new BigInteger(builder.append(matched).toString()));}
    {CType}             {builder.setLength(0);
                        return symbol(ACSLSymbols.TYPE, new ACSLType(builder.append(yytext()).toString()));}
    {ACSLType}          {builder.setLength(0);
                        return symbol(ACSLSymbols.TYPE, new ACSLType(builder.append(yytext()).toString()));}
    {Identifier}        {builder.setLength(0);
                        return symbol(ACSLSymbols.IDENTIFIER, builder.append(yytext()).toString());}
    {String}            {builder.setLength(0);
                        return symbol(ACSLSymbols.STRING_LITERAL, builder.append(yytext()).toString());}
    {Space}             {/* do nothing */}
}

    [^]                 {throw new Error("Illegal character: <" + yytext() + ">");}
    <<EOF>>             { return symbol(ACSLSymbols.EOF, yytext()); }