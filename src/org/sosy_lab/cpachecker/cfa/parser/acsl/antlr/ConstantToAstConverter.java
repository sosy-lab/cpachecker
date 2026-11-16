// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl.antlr;

import com.google.common.base.CharMatcher;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.antlr.v4.runtime.ParserRuleContext;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCharLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslRealLiteralTerm;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.BinaryIntegerConstantContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.DecimalFloatConstantContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.DecimalIntegerConstantContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.HexadecimalEscapeCharConstantContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.HexadecimalFloatConstantContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.HexadecimalIntegerConstantContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.OctalEscapeCharConstantContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.OctalIntegerConstantContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.SimpleCharConstantContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.SimpleEscapeCharConstantContext;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarParser.UniversalCharConstantContext;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslBuiltinLogicType;

/**
 * Converts C constants, whose kind the grammar distinguishes. In ACSL, integer constants denote
 * mathematical integers and floating constants denote real numbers, so their suffixes do not change
 * the value. The prefixes of character constants do not change the value of a single character
 * either.
 */
class ConstantToAstConverter extends AbstractAntlrToAstConverter<AcslLiteralTerm> {

  private static final CharMatcher INTEGER_SUFFIX = CharMatcher.anyOf("uUlL");
  private static final CharMatcher FLOATING_SUFFIX = CharMatcher.anyOf("fFlL");

  ConstantToAstConverter(
      CProgramScope pCProgramScope, AcslScope pAcslScope, FileLocation pInitialFileLocation) {
    super(pCProgramScope, pAcslScope, pInitialFileLocation);
  }

  @Override
  public AcslLiteralTerm visitDecimalIntegerConstant(DecimalIntegerConstantContext ctx) {
    return integer(ctx, 0, 10);
  }

  @Override
  public AcslLiteralTerm visitOctalIntegerConstant(OctalIntegerConstantContext ctx) {
    return integer(ctx, 0, 8);
  }

  @Override
  public AcslLiteralTerm visitHexadecimalIntegerConstant(HexadecimalIntegerConstantContext ctx) {
    return integer(ctx, "0x".length(), 16);
  }

  @Override
  public AcslLiteralTerm visitBinaryIntegerConstant(BinaryIntegerConstantContext ctx) {
    return integer(ctx, "0b".length(), 2);
  }

  @Override
  public AcslLiteralTerm visitDecimalFloatConstant(DecimalFloatConstantContext ctx) {
    return new AcslRealLiteralTerm(
        fileLocationFromContext(ctx),
        AcslBuiltinLogicType.REAL,
        new BigDecimal(FLOATING_SUFFIX.trimTrailingFrom(ctx.getText())));
  }

  @Override
  public AcslLiteralTerm visitHexadecimalFloatConstant(HexadecimalFloatConstantContext ctx) {
    return new AcslRealLiteralTerm(
        fileLocationFromContext(ctx),
        AcslBuiltinLogicType.REAL,
        new BigDecimal(Double.parseDouble(FLOATING_SUFFIX.trimTrailingFrom(ctx.getText()))));
  }

  @Override
  public AcslLiteralTerm visitSimpleCharConstant(SimpleCharConstantContext ctx) {
    String text = ctx.getText();
    return character(ctx, text.charAt(text.length() - "a'".length()));
  }

  @Override
  public AcslLiteralTerm visitSimpleEscapeCharConstant(SimpleEscapeCharConstantContext ctx) {
    String text = ctx.getText();
    char escaped = text.charAt(text.length() - "n'".length());
    return character(
        ctx,
        switch (escaped) {
          case 'a' -> 7;
          case 'b' -> '\b';
          case 'f' -> '\f';
          case 'n' -> '\n';
          case 'r' -> '\r';
          case 't' -> '\t';
          case 'v' -> 11;
          // The grammar allows only ', ", ?, and \ besides the letters above
          default -> escaped;
        });
  }

  @Override
  public AcslLiteralTerm visitOctalEscapeCharConstant(OctalEscapeCharConstantContext ctx) {
    return character(ctx, escapedNumber(ctx, "\\".length(), 8));
  }

  @Override
  public AcslLiteralTerm visitHexadecimalEscapeCharConstant(
      HexadecimalEscapeCharConstantContext ctx) {
    return character(ctx, escapedNumber(ctx, "\\x".length(), 16));
  }

  @Override
  public AcslLiteralTerm visitUniversalCharConstant(UniversalCharConstantContext ctx) {
    return character(ctx, escapedNumber(ctx, "\\u".length(), 16));
  }

  private AcslIntegerLiteralTerm integer(
      ParserRuleContext pContext, int pPrefixLength, int pRadix) {
    String digits = INTEGER_SUFFIX.trimTrailingFrom(pContext.getText()).substring(pPrefixLength);
    return new AcslIntegerLiteralTerm(
        fileLocationFromContext(pContext),
        AcslBuiltinLogicType.INTEGER,
        new BigInteger(digits, pRadix));
  }

  /** Returns the number of the escape sequence that starts with a prefix of the given length. */
  private static int escapedNumber(ParserRuleContext pContext, int pPrefixLength, int pRadix) {
    String text = pContext.getText();
    BigInteger value =
        new BigInteger(
            text.substring(text.indexOf('\\') + pPrefixLength, text.length() - "'".length()),
            pRadix);
    if (value.bitLength() > Character.SIZE) {
      throw new AcslParseRuntimeException("Unsupported character constant " + text);
    }
    return value.intValueExact();
  }

  private AcslCharLiteralTerm character(ParserRuleContext pContext, int pValue) {
    return new AcslCharLiteralTerm(
        fileLocationFromContext(pContext), AcslBuiltinLogicType.INTEGER, (char) pValue);
  }
}
