// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.precisionConverter;

import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.Location;
import java_cup.runtime.Symbol;

@javax.annotation.processing.Generated("JFlex")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"URF_UNREAD_FIELD", "DLS_DEAD_LOCAL_STORE"})
%%

%cup
%class FormulaScanner
%final
%apiprivate
%line
%column

%ctorarg ComplexSymbolFactory sf
%init{
  this.sf = sf;
%init}
%{
  private final ComplexSymbolFactory sf;
  
  private Location getStartLocation() {
    return new Location("", yyline+1,yycolumn+1-yylength());
  }

  private Location getEndLocation() {
    return new Location("", yyline+1,yycolumn+1);
  }
  
  private Symbol symbol(int sym, String name) {
    return sf.newSymbol(name, sym, getStartLocation(), getEndLocation(), name);
  }
%}

LineTerminator = \R
WhiteSpace     = {LineTerminator} | [ \t\f]

SMTLetter = [:letter:] | [~!@$%\^&*_+\-=<>.?/] 
SMTLetterDigit = {SMTLetter} | [:digit:]

Numeral = 0 | [1-9][0-9]*
Decimal = {Numeral} "."  0* {Numeral}
QuotedSymbol = "|" [^|]* "|"
Symbol = {SMTLetter} {SMTLetterDigit}* 

%%

/* keywords */
<YYINITIAL> {
  "("                    { return symbol(FormulaSymbols.LPAR, yytext()); }
  ")"                    { return symbol(FormulaSymbols.RPAR, yytext()); }
  "assert"               { return symbol(FormulaSymbols.ASSERT, yytext()); }
  "declare-fun"          { return symbol(FormulaSymbols.DECLAREFUN, yytext()); }
  "define-fun"           { return symbol(FormulaSymbols.DEFINEFUN, yytext()); }
  {QuotedSymbol}         { return symbol(FormulaSymbols.SYMBOL, yytext()); }
  {Symbol}               { return symbol(FormulaSymbols.SYMBOL, yytext()); }
  {Numeral}              { return symbol(FormulaSymbols.NUMERAL, yytext()); }
  {Decimal}              { return symbol(FormulaSymbols.DECIMAL, yytext()); }
  {WhiteSpace}           { /* ignore */ }
}

/* error fallback */
[^]                              { return symbol(FormulaSymbols.error, yytext()); }
<<EOF>>                          { return symbol(FormulaSymbols.EOF, yytext()); }
