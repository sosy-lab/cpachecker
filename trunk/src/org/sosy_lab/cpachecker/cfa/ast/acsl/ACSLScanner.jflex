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
import java.util.logging.Level;
import java_cup.runtime.Symbol;
import org.sosy_lab.common.log.LogManager;

@javax.annotation.processing.Generated("JFlex")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"URF_UNREAD_FIELD", "DLS_DEAD_LOCAL_STORE"})
%%

%class ACSLScanner
%ctorarg LogManager pLogger
%init{
  logger = pLogger;
%init}

%unicode
%cup
%line
%column

%{
    StringBuilder builder = new StringBuilder();
    int currentAnnotation = -1;
    Queue<Symbol> symbols = new ArrayDeque<>();
    boolean pred_start = false;
    LogManager logger;

    private Symbol symbol(int type) {
      return new Symbol(type, yyline, yycolumn);
    }

    private Symbol symbol(int type, Object value) {
      return new Symbol(type, yyline, yycolumn, value);
    }

    public Symbol getNext() throws IOException {
      if (symbols.isEmpty()) {
        bufferNextAnnotation();
        currentAnnotation++;
      }
      if (!symbols.isEmpty()) {
        return symbols.poll();
      } else {
        return symbol(ACSLSymbols.EOF);
      }
    }

    private void bufferNextAnnotation() throws IOException {
      Symbol token = next_token();
      if (token.sym == ACSLSymbols.EOF) {
        return;
      }
      symbols.add(symbol(ACSLSymbols.NEXTCONTRACT));
      boolean invalidAnnotation = false;

      while (token.sym != ACSLSymbols.EOF && token.sym != ACSLSymbols.ANNOTATION_END) {
        if (token.sym == ACSLSymbols.SYNTAXERROR) {
          logger.logf(
              Level.INFO, "Unsupported character: <%s>. Ignoring current annotation", yytext());
          invalidAnnotation = true;
        }
        symbols.add(token);
        if (pred_start) {
          symbols.add(symbol(ACSLSymbols.PRED_START));
          pred_start = false;
        }
        token = next_token();
      }
      symbols.add(symbol(ACSLSymbols.ANNOTATION_END));
      assert !pred_start;

      if (invalidAnnotation) {
        symbols.clear();
        symbols.add(symbol(ACSLSymbols.SYNTAXERROR));
      }
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
   "//@"                {yybegin(SINGLE_LINE_ANNOTATION);}
   "/*@"                {yybegin(MULTI_LINE_ANNOTATION);}
   {LineBreak}          {/* do nothing */}
   .                    {/* do nothing */}
}

<SINGLE_LINE_ANNOTATION> {
    {LineBreak}         {yybegin(YYINITIAL); return symbol(ACSLSymbols.ANNOTATION_END);}
}

<MULTI_LINE_ANNOTATION> {
    "*/"                {yybegin(YYINITIAL); return symbol(ACSLSymbols.ANNOTATION_END);}
    {LineBreak}         {/* do nothing */}
}

<SINGLE_LINE_ANNOTATION, MULTI_LINE_ANNOTATION> {
    "\\true"            {return symbol(ACSLSymbols.TRUE);}
    "\\false"           {return symbol(ACSLSymbols.FALSE);}
    "["                 {return symbol(ACSLSymbols.LBRACKET);}
    "]"                 {return symbol(ACSLSymbols.RBRACKET);}
    "?"                 {pred_start = true; return symbol(ACSLSymbols.QUESTION);}
    "!"                 {pred_start = true; return symbol(ACSLSymbols.NEG);}
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
    "&&"                {pred_start = true; return symbol(ACSLSymbols.AND);}
    "||"                {pred_start = true; return symbol(ACSLSymbols.OR);}
    "==>"               {pred_start = true; return symbol(ACSLSymbols.IMP);}
    "<==>"              {pred_start = true; return symbol(ACSLSymbols.EQV);}
    "^^"                {pred_start = true; return symbol(ACSLSymbols.XOR);}
    "=="                {return symbol(ACSLSymbols.EQ);}
    "!="                {return symbol(ACSLSymbols.NEQ);}
    "<="                {return symbol(ACSLSymbols.LEQ);}
    ">="                {return symbol(ACSLSymbols.GEQ);}
    ">"                 {return symbol(ACSLSymbols.GT);}
    "<"                 {return symbol(ACSLSymbols.LT);}
    "("                 {pred_start = true; return symbol(ACSLSymbols.LPAREN);}
    ")"                 {return symbol(ACSLSymbols.RPAREN);}
    "for"               {return symbol(ACSLSymbols.FOR);}
    "sizeof"            {return symbol(ACSLSymbols.SIZEOF);}
    "behavior"          {return symbol(ACSLSymbols.BEHAVIOR);}
    "complete behaviors"    {return symbol(ACSLSymbols.COMPLETE_BEHAVIORS);}
    "disjoint behaviors"    {return symbol(ACSLSymbols.DISJOINT_BEHAVIORS);}
    "ensures"           {pred_start = true; return symbol(ACSLSymbols.ENS);}
    "requires"          {pred_start = true; return symbol(ACSLSymbols.REQ);}
    "loop invariant"    {pred_start = true; return symbol(ACSLSymbols.LINVARIANT);}
    "assert"            {pred_start = true; return symbol(ACSLSymbols.ASSERT);}
    "check"             {pred_start = true; return symbol(ACSLSymbols.CHECK);}
    "assumes"           {pred_start = true; return symbol(ACSLSymbols.ASS);}
    ":"                 {pred_start = true; return symbol(ACSLSymbols.COLON);}
    ","                 {return symbol(ACSLSymbols.COMMA);}
    ";"                 {pred_start = true; return symbol(ACSLSymbols.SEMI);}
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

    [^]                 {return symbol(ACSLSymbols.SYNTAXERROR);}
    <<EOF>>             {return symbol(ACSLSymbols.EOF, yytext());}
