package org.sosy_lab.cpachecker.util.predicates.precisionConverter;

import java_cup.runtime.*;
import java_cup.runtime.ComplexSymbolFactory.Location;

@javax.annotation.Generated("JFlex")
@SuppressWarnings(value = { "all", "cast", "MissingOverride" })
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = {"DLS_DEAD_LOCAL_STORE"})
%%

%cup
%class FormulaScanner
%line
%column

%{
  private ComplexSymbolFactory sf;

  public FormulaScanner(java.io.Reader r, ComplexSymbolFactory sf) {
    this(r);
    this.sf = sf;
  }
  
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
%eofval{
    return symbol("EOF", Symbol.EOF);
%eofval}

LineTerminator = \R
WhiteSpace     = {LineTerminator} | [ \t\f]

SMTLetter = [:letter:] | [~!@$%\^&*_+\-=<>.?/] 
SMTLetterDigit = {SMTLetter} | [:digit:]

Numeral = 0 | [1-9][0-9]*
Decimal = {Numeral} "."  0* {Numeral}
QuotedSymbol = "|" [^|]* "|"
Symbol = {SMTLetter} {SMTLetterDigit}* 

%state STRING


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
