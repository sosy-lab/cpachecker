package org.sosy_lab.cpachecker.cpa.automatonanalysis;

import java.io.FileReader;
import java.io.File;
import java_cup.runtime.*;
import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
@SuppressWarnings(value = { "all" })
%%

%cup
%class AutomatonScanner
%implements AutomatonSym
%line
%column

%{
  private StringBuilder string = new StringBuilder();
  private SymbolFactory sf;
  private Configuration config;
  private LogManager logger;
  private List<String> scannedFiles = new ArrayList<String>();

   public AutomatonScanner(java.io.InputStream r, Configuration config, LogManager logger, SymbolFactory sf){
	this(r);
	this.sf = sf;
	this.config = config;
	this.logger = logger;
  }
  public int getLine() {
     return this.yyline;
   }
   public int getColumn() {
     return this.yycolumn;
   }
   
   private File getFile(String pYytext) throws FileNotFoundException {
  	assert pYytext.startsWith("#include ");
  	String fileName = pYytext.replaceFirst("#include ", "").trim();
  	if (scannedFiles.contains(fileName)) {
  	  logger.log(Level.WARNING, "File \"" + fileName + "\" was referenced multiple times. Redundant or cyclic references were ignored.");
  	  return null;
  	}
  	String rootPrefix = "";
  	if (config != null && config.getRootDirectory() != null) {
  	  rootPrefix = config.getRootDirectory();
  	}
  	File file = new File(rootPrefix + fileName);
  	Files.checkReadableFile(file);
  	scannedFiles.add(fileName);
  	return file;
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
    return symbol("EOF", AutomatonSym.EOF);
%eofval}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n] | .
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
<YYINITIAL>
	"#include" {InputCharacter}+ 
	{ File file = getFile(yytext()); 
	  if (file != null) yypushStream(new FileReader(file)); }
<YYINITIAL> ";"                 { return symbol(";", AutomatonSym.SEMICOLON); }
<YYINITIAL> ":"                 { return symbol(":", AutomatonSym.COLON); }
<YYINITIAL> "("                 { return symbol("(", AutomatonSym.OPEN_BRACKETS); }
<YYINITIAL> ")"                 { return symbol(")", AutomatonSym.CLOSE_BRACKETS); }
<YYINITIAL> "->"                { return symbol("->", AutomatonSym.ARROW); }
<YYINITIAL> "AUTOMATON"         { return symbol("AUTOMATON", AutomatonSym.AUTOMATON); }
<YYINITIAL> "END"               { return symbol("LABEL", AutomatonSym.END); }
<YYINITIAL> "OBSERVER"          { return symbol("OBSERVER", AutomatonSym.OBSERVER); }
<YYINITIAL> "CONTROL"           { return symbol("CONTROL", AutomatonSym.CONTROL); }
<YYINITIAL> "LOCAL"             { return symbol("LOCAL", AutomatonSym.LOCAL); }
<YYINITIAL> "INITIAL"           { return symbol("INITIAL", AutomatonSym.INITIAL); }
<YYINITIAL> "STATE"             { return symbol("STATE", AutomatonSym.STATE); }
<YYINITIAL> "ERROR"             { return symbol("ERROR", AutomatonSym.ERROR); }
<YYINITIAL> "STOP"              { return symbol("STOP", AutomatonSym.STOP); }
<YYINITIAL> "ASSERT"            { return symbol("ASSERT", AutomatonSym.ASS); }
<YYINITIAL> "MATCH"             { return symbol("MATCH", AutomatonSym.MATCH); }
<YYINITIAL> "LABEL"             { return symbol("LABEL", AutomatonSym.LABEL); }
<YYINITIAL> "EVAL"              { return symbol("EVAL", AutomatonSym.EVAL); }
<YYINITIAL> "CHECK"             { return symbol("EVAL", AutomatonSym.CHECK); }
<YYINITIAL> "MODIFY"            { return symbol("MODIFY", AutomatonSym.MODIFY); }
<YYINITIAL> "DO"                { return symbol("DO", AutomatonSym.DO); }
<YYINITIAL> "GOTO"              { return symbol("GOTO", AutomatonSym.GOTO); }
<YYINITIAL> "true"              { return symbol("TRUE", AutomatonSym.TRUE); }
<YYINITIAL> "false"             { return symbol("FALSE", AutomatonSym.FALSE); }
<YYINITIAL> "TRUE"              { return symbol("TRUE", AutomatonSym.TRUE); }
<YYINITIAL> "FALSE"             { return symbol("FALSE", AutomatonSym.FALSE); }
<YYINITIAL> "PRINT"             { return symbol("PRINT", AutomatonSym.PRINT); }
<YYINITIAL> "NONDET"			{ return symbol("NONDET", AutomatonSym.NONDET); }

<YYINITIAL> {
  /* identifiers */ 
  {Identifier}                   { return symbol("ID", AutomatonSym.IDENTIFIER, yytext()); }
 
  /* literals */
  {DecIntegerLiteral}            { return symbol("INT", AutomatonSym.INTEGER_LITERAL, yytext()); }
  \"                             { string.setLength(0); yybegin(STRING); }
  \{                             { string.setLength(0); yybegin(CURLYEXPR); }
  \[                             { string.setLength(0); yybegin(SQUAREEXPR); }

  /* operators */
  "!"                           { return symbol("!", AutomatonSym.EXCLAMATION); }
  "=="                           { return symbol("==", AutomatonSym.EQEQ); }
  "&&"                           { return symbol("&&", AutomatonSym.AND); }
  "||"                           { return symbol("||", AutomatonSym.OR); }
  "!="                           { return symbol("!=", AutomatonSym.NEQ); }
  "="                            { return symbol("=", AutomatonSym.EQ); }
  "+"                            { return symbol("+", AutomatonSym.PLUS); }
  "-"                            { return symbol("-", AutomatonSym.MINUS); }

  /* comments */
  {Comment}                      { /* ignore */ }
 
  /* whitespace */
  {WhiteSpace}                   { /* ignore */ }
}

<STRING> {
  \"                             { yybegin(YYINITIAL); 
                                   return symbol("STRING", AutomatonSym.STRING_LITERAL, 
                                   string.toString()); }
  [^\n\r\"\\]+                   { string.append( yytext() ); }
  \\t                            { string.append('\t'); }
  \\n                            { string.append('\n'); }

  \\r                            { string.append('\r'); }
  \\\"                           { string.append('"'); }
  \\                             { string.append('\\'); }
}
<CURLYEXPR> {
  \}                             { yybegin(YYINITIAL); 
                                   return symbol("CURLYEXPR", AutomatonSym.CURLYEXPR, 
                                   string.toString()); }
  [^\n\r\}\\]+                   { string.append( yytext() ); }
  \\t                            { string.append('\t'); }
  \\n                            { string.append('\n'); }

  \\r                            { string.append('\r'); }
  \\\}                           { string.append('}'); }
  \\                             { string.append('\\'); }
}
<SQUAREEXPR> {
  \]                             { yybegin(YYINITIAL); 
                                   return symbol("CURLYEXPR", AutomatonSym.SQUAREEXPR, 
                                   string.toString()); }
  [^\n\r\]\\]+                   { string.append( yytext() ); }
  \\t                            { string.append('\t'); }
  \\n                            { string.append('\n'); }

  \\r                            { string.append('\r'); }
  \\\]                           { string.append(']'); }
  \\                             { string.append('\\'); }
}
<<EOF>> {if (yymoreStreams()) yypopStream(); else return symbol("EOF", AutomatonSym.EOF); }
/* error fallback */
.|\n                             { error("Fallback error"); throw new Error("Illegal character <"+
                                                    yytext()+">"); }
