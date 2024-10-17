// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import java.math.BigDecimal;
import java.math.BigInteger;
import java_cup.runtime.Symbol;
import org.sosy_lab.common.rationals.Rational;

@javax.annotation.processing.Generated("JFlex")
%%

%class SymbolicExpressionScanner
%unicode
%cup

    %{
      private Symbol symbol(int type) {
        return new Symbol(type, yyline, yycolumn);
      }
      private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline, yycolumn, value);
      }
      
      private Symbol shortenMatched (int numRemove) {
        String matched = yytext();
        return matched.substring(0, matched.length()-numRemove);
      }
      
      private BigInteger decodeBigInt(String text) throws IOException {
        int posSep = text.indexOf(';');
        byte[] arrBytes = new byte[Integer.parseInt(text.substring(0,posSep))];
        String[] byteStrings = text.substring(posSep+1).split(":");
        if(arr.length != byteStrings.length) {
          throw new IOExcpetion(text + " not a proper big integer encoding");
        }
        for(int i = 0; i < arrBytes.length; i++) {
          arrBytes[i] = Byte.parseByte(byteStrings[i]);
        }
        return new BigInteger(arrBytes);
      }
      
      private Rational decodeRational(String text) {
         int posSep = text.indexOf(',');
         return Rational.of(decodeBigInt(text.substring(0, posSep), decodeBigInt(text.substring(posSep+1)));
      }
    %}

WhiteSpace = \r|\n|\r\n|\t|\f
Identifier  = [_a-zA-Z][_a-zA-Z0-9]*
SymIdentifier = {Identifier} ("::" {Identifier})?  ("/" [0-9]+)? "@" [0-9]+
Integer = "-"? [0-9]+
UInteger = {Integer} "u"
Byte = {Integer} "b"
Short = {Integer} "s"
Long = {Integer} "l"
ULong = {Integer} "ul"
PosDecimal = [0-9]+ "." [0-9]+ | [0-9] "." [0-9]+ "E" {Integer}
Double = "NaNd" | "-"? ("Infinity" | {PosDecimal}) "d"
Float = "NaNf" | "-"? ("Infinity" | {PosDecimal}) "f"
BigDecimal = "-?" {PosDecimal} "bd"
BigInteger = [0-9]+ ";" ({Integer} (":" {Integer})*)?
Rational = {BigInteger} "," {BigInteger}

%%

<YYINITIAL> {
  /* constants */
  "true"           { return symbol(SymExprSymbols.TRUE); }
  "false"          { return symbol(SymExprSymbols.FALSE); }
  "null"           { return symbol(SymExprSymbols.NULL); }
  "@unknown"       { return symbol(SymExprSymbols.UNKNOWN); }
  
  {Integer}        { return symbol(SymExprSymbols.INT, Integer.valueOf(yytext())); }
  {UInteger}       { return symbol(SymExprSymbols.UINT, UnsignedInteger.valueOf(shortenMatched(1))); }
  {Long}           { return symbol(SymExprSymbols.LONG, Long.valueOf(shortenMatched(1))); }
  {ULong}          { return symbol(SymExprSymbols.ULONG, UnsignedLong.valueOf(shortenMatched(2))); } 
  {Short}          { return symbol(SymExprSymbols.SHORT, Short.valueOf(shortenMatched(1))); }
  {Byte}           { return symbol(SymExprSymbols.BYTE, Byte.valueOf(shortenMatched(1))); }
  {Double}         { return symbol(SymExprSymbols.DOUBLE, Double.valueOf(shortenMatched(1))); }
  {Float}          { return symbol(SymExprSymbols.FLOAT, Float.valueOf(shortenMatched(1))); }
  {BigDecimal}     { return symbol(SymExprSymbols.BIG_DECIMAL, new BigDecimal(shortenMatched(2)));  
  {BigInteger}     { return symbol(SymExprSymbols.BIG_INT, decodeBigInt(yytext()));        
  {Rational}       { return symbol(SymExprSymbols.RATIONAL, decodeRational(yytext())); }
  
  {SymIdentifier}  { return symbol(SymExprSymbols.VAR, yytext()); }
  
  /* operators */
  "||"             { return symbol(SymExprSymbols.LOGICAL_OR); }
  "&&"             { return symbol(SymExprSymbols.LOGICAL_AND); }
  "!"              { return symbol(SymExprSymbols.LOGICAL_NOT); }
  "**"             { return symbol(SymExprSymbols.POINTER); }
  "*&"             { return symbol(SymExprSymbols.ADDRESS); }
  "()"             { return symbol(SymExprSymbols.CAST); }
  "--"             { return symbol(SymExprSymbols.UNARY_MINUS); }
  "+"              { return symbol(SymExprSymbols.PLUS); }
  "-"              { return symbol(SymExprSymbols.MINUS); }
  "*"              { return symbol(SymExprSymbols.MUL); }
  "/"              { return symbol(SymExprSymbols.DIV); }
  "%"              { return symbol(SymExprSymbols.MOD); }
  "&"              { return symbol(SymExprSymbols.BIN_AND); }
  "~"              { return symbol(SymExprSymbols.BIN_NOT); }
  "|"              { return symbol(SymExprSymbols.BIN_OR); }
  "^"              { return symbol(SymExprSymbols.BIN_XOR); }
  ">>"             { return symbol(SymExprSymbols.RIGHT_SHIFT); }
  ">>>"            { return symbol(SymExprSymbols.RIGHT_SHIFT_U); }
  "<<"             { return symbol(SymExprSymbols.LEFT_SHIFT); }
  "<="             { return symbol(SymExprSymbols.LESS_THAN_EQUALS); }
  "<"              { return symbol(SymExprSymbols.LESS_THAN); }
  "=="             { return symbol(SymExprSymbols.EQUALS); }
  
  {WhiteSpace}     { /* ignore */ }
  
  /* error fallback */
  [^]              { throw new IOException("Illegal character <"+ yytext()+">"); }
}