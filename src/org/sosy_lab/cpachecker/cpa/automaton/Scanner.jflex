package org.sosy_lab.cpachecker.cpa.automaton;

import java_cup.runtime.*;
import java_cup.runtime.ComplexSymbolFactory.Location;
import org.sosy_lab.common.io.MoreFiles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.sosy_lab.common.log.LogManager;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@javax.annotation.Generated("JFlex")
@SuppressWarnings(value = { "all", "cast", "MissingOverride" })
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = {"DLS_DEAD_LOCAL_STORE"})
%%

%cup
%class AutomatonScanner
%line
%column

%{
  private StringBuilder string = new StringBuilder();
  private ComplexSymbolFactory sf;
  private LogManager logger;
  private final List<Path> scannedFiles = new ArrayList<>();
  private final Deque<Path> filesStack = new ArrayDeque<>();

  public AutomatonScanner(java.io.Reader r, Path file, LogManager logger, ComplexSymbolFactory sf) {
    this(r);
    filesStack.push(file);
    this.sf = sf;
    this.logger = logger;
  }
   
  private Path getFile(String pYytext) throws FileNotFoundException {
    assert pYytext.startsWith("#include ");
    String fileName = pYytext.replaceFirst("#include ", "").trim();
    
    Path file = Paths.get(fileName);
    Path currentFile = filesStack.peek();
    file = currentFile.resolveSibling(file);

    if (scannedFiles.contains(file)) {
      logger.log(Level.WARNING, "File \"" + file + "\" was referenced multiple times. Redundant or cyclic references were ignored.");
      return null;
    }

    MoreFiles.checkReadableFile(file);
    scannedFiles.add(file);
    filesStack.push(file);
    return file;
  }
  
  private Location getStartLocation() {
    return new Location(filesStack.peek().toString(), yyline+1,yycolumn+1-yylength());
  }

  private Location getEndLocation() {
    return new Location(filesStack.peek().toString(), yyline+1,yycolumn+1);
  }
  
  private Symbol symbol(String name, int sym) {
    return sf.newSymbol(name, sym, getStartLocation(), getEndLocation());
  }

  private Symbol symbol(String name, int sym, String val) {
    return sf.newSymbol(name, sym, getStartLocation(), getEndLocation(), val);
  }
  
  private void error() throws IOException {
    Location start = getStartLocation();
    StringBuilder msg = new StringBuilder();
    msg.append(filesStack.getLast().toString());
    msg.append(" (Illegal character <");
    msg.append(yytext());
    msg.append("> at column ");
    msg.append(start.getColumn());
    msg.append(" in line ");
    msg.append(start.getLine());
    if (filesStack.size() != 1) {
      msg.append(" of ");
      msg.append(start.getUnit());
    }
    msg.append(")");
    throw new IOException(msg.toString());
  }
%}
%eofval{
    return symbol("EOF", AutomatonSym.EOF);
%eofval}

LineTerminator = \R
InputCharacter = .
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
        { Path file = getFile(yytext()); 
          if (file != null) {
            yypushStream(Files.newBufferedReader(file, StandardCharsets.US_ASCII));
          }
        }
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
<YYINITIAL> "BREAK"             { return symbol("BREAK", AutomatonSym.BREAK); }
<YYINITIAL> "EXIT"              { return symbol("EXIT", AutomatonSym.EXIT); }
<YYINITIAL> "ENTRY"             { return symbol("ENTRY", AutomatonSym.ENTRY); }
<YYINITIAL> "ASSUME"            { return symbol("ASSUME", AutomatonSym.ASSUME); }
<YYINITIAL> "ASSERT"            { return symbol("ASSERT", AutomatonSym.ASSERT); }
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
<YYINITIAL> "USEFIRST"          { return symbol("USEFIRST", AutomatonSym.USEFIRST); }
<YYINITIAL> "USEALL"            { return symbol("USEALL", AutomatonSym.USEALL); }
<YYINITIAL> "TARGET"            { return symbol("TARGET", AutomatonSym.TARGET); }
<YYINITIAL> "IS_TARGET_STATE"   { return symbol("IS_TARGET_STATE", AutomatonSym.IS_TARGET_STATE); }
<YYINITIAL> ","                 { return symbol("COMMA", AutomatonSym.COMMA); }

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
  \\\\                           { string.append('\\'); }
}
<CURLYEXPR> {
  \}                             { yybegin(YYINITIAL); 
                                   return symbol("CURLYEXPR", AutomatonSym.CURLYEXPR, 
                                   string.toString()); }
  [^\n\r\}\\]+                   { string.append( yytext() ); }
  \\t                            { string.append('\t'); }
  \\n                            { string.append('\n'); }
  \n                             { string.append('\n'); }
  \\r                            { string.append('\r'); }
  \r                             { string.append('\r'); }
  \\\}                           { string.append('}'); }
  \\\\                           { string.append('\\'); }
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
  \\\\                           { string.append('\\'); }
}
<<EOF>> {if (yymoreStreams()) { yypopStream(); filesStack.pop(); } else return symbol("EOF", AutomatonSym.EOF); }
/* error fallback */
[^]                              { error(); }
