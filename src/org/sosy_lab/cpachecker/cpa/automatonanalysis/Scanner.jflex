package org.sosy_lab.cpachecker.cpa.observeranalysis;

import java_cup.runtime.*;
@SuppressWarnings(value = { "all" })
%%

%cup
%class ObserverScanner
%implements ObserverSym
%line
%column


%{
  private StringBuilder string = new StringBuilder();
  private SymbolFactory sf;

   public ObserverScanner(java.io.InputStream r, SymbolFactory sf){
	this(r);
	this.sf = sf;
  }
  public int getLine() {
     return this.yyline;
   }
   public int getColumn() {
     return this.yycolumn;
   }
  
  private Symbol symbol(String name, int sym) {
    return  sf.newSymbol(name, sym);
  }
  private Symbol symbol(String name, int sym, Object val) {
    return  sf.newSymbol(name, sym, val);
  }
  
  private void error(String message) {
    System.out.println("Error at line "+(yyline+1)+", column "+(yycolumn+1)+" : "+message);
  }
%}
%eofval{
    return symbol("EOF", ObserverSym.EOF);
%eofval}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]

/* comments */
Comment = {TraditionalComment} | {EndOfLineComment}

TraditionalComment   = "/*" [^*] ~"*/" | "/*" "*"+ "/"
EndOfLineComment     = "//" {InputCharacter}* {LineTerminator}

Identifier = [:jletter:] [:jletterdigit:]*

DecIntegerLiteral = 0 | [1-9][0-9]*

%state STRING
%state CURLYEXPR
%state SQUAREEXPR


%%

/* keywords */
<YYINITIAL> ";"                 { return symbol(";", ObserverSym.SEMICOLON); }
<YYINITIAL> ":"                 { return symbol(":", ObserverSym.COLON); }
<YYINITIAL> "("                 { return symbol("(", ObserverSym.OPEN_BRACKETS); }
<YYINITIAL> ")"                 { return symbol(")", ObserverSym.CLOSE_BRACKETS); }
<YYINITIAL> "->"                { return symbol("->", ObserverSym.ARROW); }
<YYINITIAL> "AUTOMATON"         { return symbol("AUTOMATON", ObserverSym.AUTOMATON); }
<YYINITIAL> "LOCAL"             { return symbol("LOCAL", ObserverSym.LOCAL); }
<YYINITIAL> "INITIAL"           { return symbol("INITIAL", ObserverSym.INITIAL); }
<YYINITIAL> "STATE"             { return symbol("STATE", ObserverSym.STATE); }
<YYINITIAL> "ERROR"             { return symbol("ERROR", ObserverSym.ERROR); }
<YYINITIAL> "BOTTOM"            { return symbol("BOTTOM", ObserverSym.BOTTOM); }
<YYINITIAL> "ASSERT"            { return symbol("ASSERT", ObserverSym.ASS); }
<YYINITIAL> "MATCH"             { return symbol("MATCH", ObserverSym.MATCH); }
<YYINITIAL> "LABEL"             { return symbol("LABEL", ObserverSym.LABEL); }
<YYINITIAL> "CHECK"             { return symbol("CHECK", ObserverSym.CHECK); }
<YYINITIAL> "MODIFY"             { return symbol("MODIFY", ObserverSym.MODIFY); }
<YYINITIAL> "DO"                { return symbol("DO", ObserverSym.DO); }
<YYINITIAL> "GOTO"              { return symbol("GOTO", ObserverSym.GOTO); }
<YYINITIAL> "true"              { return symbol("TRUE", ObserverSym.TRUE); }
<YYINITIAL> "false"             { return symbol("FALSE", ObserverSym.FALSE); }
<YYINITIAL> "TRUE"              { return symbol("TRUE", ObserverSym.TRUE); }
<YYINITIAL> "FALSE"             { return symbol("FALSE", ObserverSym.FALSE); }
<YYINITIAL> "PRINT"             { return symbol("PRINT", ObserverSym.PRINT); }
<YYINITIAL> "NONDET"			{ return symbol("NONDET", ObserverSym.NONDET); }

<YYINITIAL> {
  /* identifiers */ 
  {Identifier}                   { return symbol("ID", ObserverSym.IDENTIFIER, yytext()); }
 
  /* literals */
  {DecIntegerLiteral}            { return symbol("INT", ObserverSym.INTEGER_LITERAL, yytext()); }
  \"                             { string.setLength(0); yybegin(STRING); }
  \{                             { string.setLength(0); yybegin(CURLYEXPR); }
  \[                             { string.setLength(0); yybegin(SQUAREEXPR); }

  /* operators */
  "!"                           { return symbol("!", ObserverSym.EXCLAMATION); }
  "=="                           { return symbol("==", ObserverSym.EQEQ); }
  "&&"                           { return symbol("&&", ObserverSym.AND); }
  "||"                           { return symbol("||", ObserverSym.OR); }
  "!="                           { return symbol("!=", ObserverSym.NEQ); }
  "="                            { return symbol("=", ObserverSym.EQ); }
  "+"                            { return symbol("+", ObserverSym.PLUS); }
  "-"                            { return symbol("-", ObserverSym.MINUS); }

  /* comments */
  {Comment}                      { /* ignore */ }
 
  /* whitespace */
  {WhiteSpace}                   { /* ignore */ }
}

<STRING> {
  \"                             { yybegin(YYINITIAL); 
                                   return symbol("STRING", ObserverSym.STRING_LITERAL, 
                                   string.toString()); }
  [^\n\r\"\\]+                   { string.append( yytext() ); }
  \\t                            { string.append('\t'); }
  \\n                            { string.append('\n'); }

  \\r                            { string.append('\r'); }
  \\\"                           { string.append('\"'); }
  \\                             { string.append('\\'); }
}
<CURLYEXPR> {
  \}                             { yybegin(YYINITIAL); 
                                   return symbol("CURLYEXPR", ObserverSym.CURLYEXPR, 
                                   string.toString()); }
  [^\n\r\}\\]+                   { string.append( yytext() ); }
  \\t                            { string.append('\t'); }
  \\n                            { string.append('\n'); }

  \\r                            { string.append('\r'); }
  \\\"                           { string.append('\"'); }
  \\                             { string.append('\\'); }
}
<SQUAREEXPR> {
  \]                             { yybegin(YYINITIAL); 
                                   return symbol("CURLYEXPR", ObserverSym.SQUAREEXPR, 
                                   string.toString()); }
  [^\n\r\]\\]+                   { string.append( yytext() ); }
  \\t                            { string.append('\t'); }
  \\n                            { string.append('\n'); }

  \\r                            { string.append('\r'); }
  \\\"                           { string.append('\"'); }
  \\                             { string.append('\\'); }
}

/* error fallback */
.|\n                             { error("Fallback error"); throw new Error("Illegal character <"+
                                                    yytext()+">"); }
