package cpa.observeranalysis;

import java_cup.runtime.*;

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
%state Pattern

%%

/* keywords */
<YYINITIAL> "->"                { return symbol("->", ObserverSym.ARROW); }
<YYINITIAL> "{"                 { return symbol("{", ObserverSym.OPENCURLY); }
<YYINITIAL> "}"                 { return symbol("}", ObserverSym.CLOSECURLY); }
<YYINITIAL> "NAME"              { return symbol("NAME", ObserverSym.NAME); }
<YYINITIAL> "LOCAL"             { return symbol("LOCAL", ObserverSym.LOCAL); }
<YYINITIAL> "INITIAL"           { return symbol("INITIAL", ObserverSym.INITIAL); }
<YYINITIAL> "STATE"             { return symbol("STATE", ObserverSym.STATE); }
<YYINITIAL> "ASS"               { return symbol("ASS", ObserverSym.ASS); }
<YYINITIAL> "MATCH"             { return symbol("MATCH", ObserverSym.MATCH); }
<YYINITIAL> "DO"                { return symbol("DO", ObserverSym.DO); }
<YYINITIAL> "GOTO"              { return symbol("GOTO", ObserverSym.GOTO); }
<YYINITIAL> "true"              { return symbol("TRUE", ObserverSym.TRUE); }
<YYINITIAL> "false"             { return symbol("FALSE", ObserverSym.FALSE); }
<YYINITIAL> "TRUE"              { return symbol("TRUE", ObserverSym.TRUE); }
<YYINITIAL> "FALSE"              { return symbol("FALSE", ObserverSym.FALSE); }

<YYINITIAL> {
  /* identifiers */ 
  {Identifier}                   { return symbol("ID", ObserverSym.IDENTIFIER, yytext()); }
 
  /* literals */
  {DecIntegerLiteral}            { return symbol("INT", ObserverSym.INTEGER_LITERAL, yytext()); }
  \"                             { string.setLength(0); yybegin(STRING); }

  /* operators */
  "=="                           { return symbol("==", ObserverSym.EQEQ); }
  "="                            { return symbol("=", ObserverSym.EQ); }
  "+"                            { return symbol("+", ObserverSym.PLUS); }

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

/* error fallback */
.|\n                             { error("Fallback error"); throw new Error("Illegal character <"+
                                                    yytext()+">"); }
