package org.sosy_lab.cpachecker.util.predicates.precisionConverter;

import java.io.Reader;
import java_cup.runtime.*;
import java_cup.runtime.ComplexSymbolFactory.Location;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@javax.annotation.Generated("JFlex")
@SuppressWarnings(value = { "all", "unchecked", "fallthrough", "SelfAssignment" })
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = {"DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING", "SA_FIELD_SELF_ASSIGNMENT"})
%%

%cup
%class FormulaScanner
%implements FormulaSymbols
%line
%column

%{
  private StringBuilder string = new StringBuilder();
  private ComplexSymbolFactory sf;
  private LogManager logger;

  public FormulaScanner(java.io.InputStream r, LogManager logger, ComplexSymbolFactory sf) {
    this(r);
    this.sf = sf;
    this.logger = logger;
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
  
  private void error(String message) {
    logger.log(Level.WARNING, message + " near " + getStartLocation() + " - " + getEndLocation());
    throw new RuntimeException("Syntax error");
  }
  
%}
%eofval{
    return symbol("EOF", Symbol.EOF);
%eofval}

LineTerminator = \r|\n|\r\n
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

<STRING> {
  \"                             { String value = string.toString();
                                   string = null;
                                   yybegin(YYINITIAL);
                                   return symbol(FormulaSymbols.STRING, value); }
  [^\"\\]+                       { string.append( yytext() ); }
  \\\"                           { string.append('\"'); }
  \\\\                           { string.append('\\'); }
  \\                             { string.append('\\'); }
}

/* error fallback */
.|\n                             { return symbol(FormulaSymbols.error, yytext()); }
<<EOF>>                          { return symbol(FormulaSymbols.EOF, yytext()); }
