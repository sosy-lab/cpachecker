// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static java.lang.Character.isDigit;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

/**
 * This Class contains functions, that convert literals (chars, numbers) from C-source into
 * CPAchecker-format.
 */
// Deprecated because of the temporary use of the CFloatNative class. This will be replaced by
// CFloatImpl as soon as available.
class ASTLiteralConverter {

  private final MachineModel machine;

  private final ParseContext parseContext;

  ASTLiteralConverter(MachineModel pMachineModel, ParseContext pParseContext) {
    machine = pMachineModel;
    parseContext = pParseContext;
  }

  private void check(boolean assertion, String msg, IASTNode astNode)
      throws CFAGenerationRuntimeException {
    if (!assertion) {
      throw parseContext.parseError(msg, astNode);
    }
  }

  /** This function converts literals like chars or numbers. */
  CLiteralExpression convert(
      final IASTLiteralExpression e, final CType type, final FileLocation fileLoc) {
    if (!(type instanceof CSimpleType)
        && (e.getKind() != IASTLiteralExpression.lk_string_literal)) {
      throw parseContext.parseError("Invalid type " + type + " for literal expression", e);
    }

    String valueStr = String.valueOf(e.getValue());
    if (valueStr.endsWith("i") || valueStr.endsWith("j")) {
      return handleImaginaryNumber(fileLoc, (CSimpleType) type, e, valueStr);
    }

    return switch (e.getKind()) {
      case IASTLiteralExpression.lk_char_constant ->
          new CCharLiteralExpression(fileLoc, type, parseCharacterLiteral(valueStr, e));
      case IASTLiteralExpression.lk_integer_constant -> parseIntegerLiteral(fileLoc, valueStr, e);
      case IASTLiteralExpression.lk_float_constant -> parseFloatLiteral(fileLoc, type, valueStr, e);
      case IASTLiteralExpression.lk_string_literal ->
          new CStringLiteralExpression(fileLoc, valueStr);
      default -> throw parseContext.parseError("Unknown literal", e);
    };
  }

  private CImaginaryLiteralExpression handleImaginaryNumber(
      FileLocation fileLoc, CSimpleType type, IASTLiteralExpression exp, String valueStr) {
    valueStr = valueStr.substring(0, valueStr.length() - 1);
    type =
        new CSimpleType(
            type.isConst(),
            type.isVolatile(),
            type.getType(),
            type.hasLongSpecifier(),
            type.hasShortSpecifier(),
            type.hasSignedSpecifier(),
            type.hasUnsignedSpecifier(),
            type.hasComplexSpecifier(),
            true,
            type.hasLongLongSpecifier());
    switch (exp.getKind()) {
      case IASTLiteralExpression.lk_char_constant -> {
        return new CImaginaryLiteralExpression(
            fileLoc,
            type,
            new CCharLiteralExpression(fileLoc, type, parseCharacterLiteral(valueStr, exp)));
      }
      case IASTLiteralExpression.lk_integer_constant -> {
        CLiteralExpression intLiteralExp = parseIntegerLiteral(fileLoc, valueStr, exp);
        return new CImaginaryLiteralExpression(
            fileLoc, intLiteralExp.getExpressionType(), intLiteralExp);
      }
      case IASTLiteralExpression.lk_float_constant -> {
        CLiteralExpression floatLiteralExp = parseFloatLiteral(fileLoc, type, valueStr, exp);
        return new CImaginaryLiteralExpression(
            fileLoc, floatLiteralExp.getExpressionType(), floatLiteralExp);
      }
      default -> throw parseContext.parseError("Unknown imaginary literal", exp);
    }
  }

  @VisibleForTesting
  CLiteralExpression parseIntegerLiteral(
      FileLocation pFileLoc, String pValueStr, IASTLiteralExpression pExp) {

    // Get the suffix that is specified in the literal
    Suffix denotedSuffix = extractDenotedSuffix(pValueStr, pExp);
    ConstantType type = parseType(pValueStr);
    BigInteger integerValue =
        parseRawIntegerValue(
            type, pValueStr.substring(0, pValueStr.length() - denotedSuffix.getLength()), pExp);

    ImmutableList<Suffix> suffixCandiates = getSuffixCandidates(denotedSuffix, type);
    Suffix actualRequiredSuffix =
        getLeastRepresentedTypeForValue(integerValue, machine, suffixCandiates, pExp);

    int bits = machine.getSizeof(actualRequiredSuffix.getType()) * machine.getSizeofCharInBits();
    if (actualRequiredSuffix.isSigned() && integerValue.testBit(bits - 1)) {
      throw parseContext.parseError(
          "Type must not be signed while simultaneously having its most significant bit set", pExp);
    }

    // Assure that the bits of the expression fit into the computed type
    // by comparing them against a mask whose lowest bits are set to one (e.g. 2^32-1 or 2^64-1)
    BigInteger mask = BigInteger.ZERO.setBit(bits).subtract(BigInteger.ONE);
    assert integerValue.and(mask).bitLength() <= bits;

    return new CIntegerLiteralExpression(pFileLoc, actualRequiredSuffix.getType(), integerValue);
  }

  @VisibleForTesting
  CFloatLiteralExpression parseFloatLiteral(
      FileLocation pFileLoc, CType pType, String pValueStr, IASTLiteralExpression pExp) {
    String input = Ascii.toLowerCase(pValueStr);

    // According to section 6.4.4.2 "Floating constants" of the C standard,
    // an unsuffixed floating constant has type double. If suffixed by the letter f or F, it has
    // type float. If suffixed by the letter l or L, it has type long double.
    FloatValue.Format format;
    if (input.endsWith("l")) {
      input = input.substring(0, input.length() - 1);
      format = FloatValue.Format.Float80;
    } else if (input.endsWith("f")) {
      input = input.substring(0, input.length() - 1);
      format = FloatValue.Format.Float32;
    } else {
      format = FloatValue.Format.Float64;
    }

    FloatValue value;
    try {
      // FloatValue.fromString allows some inputs that would not be legal in a C program.
      // Specifically, it will parse special values like "inf" or "nan" and allows a "+" or "-" sign
      // in front of the number. This is not a problem as the Eclipse CDT parser follows the C
      // standard and will not recognize such floating point literals.
      // For all inputs that we do encounter in this function FloatValue.fromString behaves exactly
      // as specified by the C standard.
      value = FloatValue.fromString(format, input);
    } catch (IllegalArgumentException e) {
      throw parseContext.parseError(
          String.format("unable to parse floating point literal (%s)", pValueStr), pExp);
    }

    // Round the parsed value to the target type
    value = value.withPrecision(FloatValue.Format.fromCType(machine, pType));

    return new CFloatLiteralExpression(pFileLoc, machine, pType, value);
  }

  @VisibleForTesting
  char parseCharacterLiteral(String s, final IASTLiteralExpression e) {
    check(s.length() >= 3, "invalid character literal (too short)", e);
    if (s.charAt(0) == 'L' || s.charAt(0) == 'u' || s.charAt(0) == 'U') {
      try {
        return parseCharacterLiteral(s.substring(1), e);
      } catch (CFAGenerationRuntimeException ex) {
        throw parseContext.parseError("Unsupported wide character literal", e);
      }
    }

    check(
        s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'',
        "character literal without quotation marks",
        e);
    s = s.substring(1, s.length() - 1); // remove the surrounding quotation marks ''

    final char result;
    if (s.length() == 1) {
      result = s.charAt(0);
      check(result != '\\', "invalid quoting sequence", e);

    } else {
      check(s.charAt(0) == '\\', "character literal too long", e);
      // quoted character literal
      s = s.substring(1); // remove leading backslash \
      check(s.length() >= 1, "invalid quoting sequence", e);

      final char c = s.charAt(0);
      if (c == 'x' || c == 'X') {
        // something like '\xFF'
        s = s.substring(1); // remove leading x
        check(!s.isEmpty() && s.length() <= 3, "character literal with illegal hex number", e);
        try {
          result = (char) Integer.parseInt(s, 16);
          check(result <= 0xFF, "hex escape sequence out of range", e);
        } catch (NumberFormatException exception) {
          throw parseContext.parseError("character literal with illegal hex number", e);
        }

      } else if (isDigit(c)) {
        // something like '\000'
        check(s.length() <= 3, "character literal with illegal octal number", e);
        try {
          result = (char) Integer.parseInt(s, 8);
          check(result <= 0xFF, "octal escape sequence out of range", e);
        } catch (NumberFormatException exception) {
          throw parseContext.parseError("character literal with illegal octal number", e);
        }

      } else {
        // something like '\n'
        check(s.length() == 1, "character literal too long", e);
        result =
            switch (c) {
              case 'a' -> 7;
              case 'b' -> '\b';
              case 'f' -> '\f';
              case 'n' -> '\n';
              case 'r' -> '\r';
              case 't' -> '\t';
              case 'v' -> 11;
              case '"' -> '\"';
              case '\'' -> '\'';
              case '\\' -> '\\';
              default -> throw parseContext.parseError("unknown character literal", e);
            };
      }
    }
    return result;
  }

  private ConstantType parseType(String pRawValue) {
    if (pRawValue.startsWith("0x") || pRawValue.startsWith("0X")) {
      return ConstantType.HEXADECIMAL;

    } else if (pRawValue.startsWith("0b") || pRawValue.startsWith("0B")) {
      return ConstantType.BINARY;

    } else if (pRawValue.startsWith("0")) {
      return ConstantType.OCTAL;

    } else {
      return ConstantType.DECIMAL;
    }
  }

  private BigInteger parseRawIntegerValue(ConstantType type, String s, final IASTNode e) {
    BigInteger result;
    try {
      result =
          switch (type) {
            case BINARY -> new BigInteger(s.substring(2), 2); // remove "0b" from the string
            case OCTAL -> new BigInteger(s, 8);
            case DECIMAL -> new BigInteger(s, 10);
            case HEXADECIMAL -> new BigInteger(s.substring(2), 16); // remove "0x" from the string
          };
    } catch (NumberFormatException exception) {
      throw parseContext.parseError("invalid number", e);
    }
    check(result.compareTo(BigInteger.ZERO) >= 0, "invalid number", e);

    return result;
  }

  private Suffix extractDenotedSuffix(String pIntegerLiteral, IASTNode pExpression) {
    Suffix suffix = Suffix.NONE;
    int last = pIntegerLiteral.length() - 1;

    if (pIntegerLiteral.charAt(last) == 'U' || pIntegerLiteral.charAt(last) == 'u') {
      last--;
      suffix = Suffix.U;
    }
    if (pIntegerLiteral.charAt(last) == 'L' || pIntegerLiteral.charAt(last) == 'l') {
      last--;
      // one 'L' is a long int
      suffix = suffix == Suffix.NONE ? Suffix.L : Suffix.UL;
    }
    if (pIntegerLiteral.charAt(last) == 'L' || pIntegerLiteral.charAt(last) == 'l') {
      last--;
      // two 'L' are a long long int
      suffix = suffix == Suffix.L ? Suffix.LL : Suffix.ULL;
    }
    if (pIntegerLiteral.charAt(last) == 'U' || pIntegerLiteral.charAt(last) == 'u') {
      if (!suffix.isSigned()) {
        throw parseContext.parseError(
            "invalid duplicate modifier U in integer literal", pExpression);
      }
      last--;
      suffix = suffix == Suffix.L ? Suffix.UL : Suffix.ULL;
    }
    return suffix;
  }

  /**
   * Compute the integer type that is at least required to fully represent the integer value.
   * According to section 6.4.4.1 "Integer constants" of the C standard, the type must not be lower
   * than what is specified in the code (i.e., what is stored in param 'denotedSuffix' here)
   *
   * @param pDenotedSuffix the suffix that is denoted in the program
   * @param pType the type of the constant, see {@link ConstantType}
   * @return the list of possible suffixes as specified in the C standard
   */
  private ImmutableList<Suffix> getSuffixCandidates(Suffix pDenotedSuffix, ConstantType pType) {

    // For reference, the list of the C standard is copy and pasted below for convenience:
    /*
     *  Suffix        |  Decimal Constant            |  Octal or Hexadecimal Constant
     *  --------------+------------------------------+-------------------------------
     *  --------------+------------------------------+-------------------------------
     *  none          |  int                         |  int
     *                |  long int                    |  unsigned int
     *                |  long long int               |  long int
     *                |                              |  unsigned long int
     *                |                              |  long long int
     *                |                              |  unsigned long long int
     *  --------------+------------------------------+-------------------------------
     * u or U         |  unsigned int                |  unsigned int
     *                |  unsigned long int           |  unsigned long int
     *                |  unsigned long long int      |  unsigned long long int
     *  --------------+------------------------------+-------------------------------
     * l or L         |  long int                    |  long int
     *                |  long long int               |  unsigned long int
     *                |                              |  long long int
     *                |                              |  unsigned long long int
     *  --------------+------------------------------+-------------------------------
     * Both u or U    |  unsigned long int           |  unsigned long int
     * and l or L     |  unsigned long long int      |  unsigned long long int
     *  --------------+------------------------------+-------------------------------
     * ll or LL       |  long long int               |  long long int
     *                |                              |  unsigned long long int
     *  --------------+------------------------------+-------------------------------
     * Both u or U    |  unsigned long long int      |  unsigned long long int
     * and ll or LL   |                              |
     */

    Stream<Suffix> stream =
        Arrays.stream(Suffix.values()).filter(x -> x.compareTo(pDenotedSuffix) >= 0);

    switch (pDenotedSuffix) {
      case NONE, L, LL -> {
        if (pType == ConstantType.DECIMAL) {
          stream = stream.filter(Suffix::isSigned);
        }
      }
      case U, UL, ULL -> stream = stream.filter(x -> !x.isSigned());
    }

    return stream.collect(ImmutableList.toImmutableList());
  }

  /**
   * Implement section 6.4.4.1 "Integer constants" of the C standard:
   *
   * <p>For each kind of literal suffix (including no suffix), there is an ordered list of potential
   * types, and the first matching type of the list should be selected.
   *
   * @param pMachineModel the machine model.
   * @param pValue the literal value.
   * @param pExp the ASTLiteralExpression containing the literal.
   * @return the size that will be used to represent the value.
   */
  private Suffix getLeastRepresentedTypeForValue(
      BigInteger pValue,
      MachineModel pMachineModel,
      ImmutableList<Suffix> pSuffixes,
      IASTLiteralExpression pExp) {
    Preconditions.checkState(!pSuffixes.isEmpty(), "List with possible suffixes must not be empty");

    int numberOfBits = pValue.bitLength();
    int actualCandidateBitSize = -1;
    for (Suffix suffix : pSuffixes) {
      CSimpleType candidate = suffix.getType();
      int candidateBitSize =
          pMachineModel.getSizeof(candidate) * pMachineModel.getSizeofCharInBits();
      actualCandidateBitSize = suffix.isSigned() ? candidateBitSize - 1 : candidateBitSize;
      if (actualCandidateBitSize >= numberOfBits) {
        return suffix;
      }
    }

    // TODO: Value is too large to be represented by an unsigned long long int.
    // Thus, it is either an extended integer type (such as _int128), or the integer constant has no
    // type, meaning it is ill-formed. This is not yet handled here however.
    assert actualCandidateBitSize > 0 && numberOfBits > actualCandidateBitSize;
    throw new CFAGenerationRuntimeException(
        String.format(
            "Integer value is too large to be represented by the highest possible type (unsigned"
                + " long long int): %s.",
            pExp));
  }

  private enum ConstantType {
    BINARY,
    OCTAL,
    DECIMAL,
    HEXADECIMAL,
  }

  private enum Suffix {
    NONE {

      @Override
      public boolean isSigned() {
        return true;
      }

      @Override
      public CSimpleType getType() {
        return CNumericTypes.INT;
      }

      @Override
      public int getLength() {
        return 0;
      }
    },

    U {

      @Override
      public boolean isSigned() {
        return false;
      }

      @Override
      public CSimpleType getType() {
        return CNumericTypes.UNSIGNED_INT;
      }

      @Override
      public int getLength() {
        return 1;
      }
    },

    L {

      @Override
      public boolean isSigned() {
        return true;
      }

      @Override
      public CSimpleType getType() {
        return CNumericTypes.LONG_INT;
      }

      @Override
      public int getLength() {
        return 1;
      }
    },

    UL {

      @Override
      public boolean isSigned() {
        return false;
      }

      @Override
      public CSimpleType getType() {
        return CNumericTypes.UNSIGNED_LONG_INT;
      }

      @Override
      public int getLength() {
        return 2;
      }
    },

    LL {

      @Override
      public boolean isSigned() {
        return true;
      }

      @Override
      public CSimpleType getType() {
        return CNumericTypes.LONG_LONG_INT;
      }

      @Override
      public int getLength() {
        return 2;
      }
    },

    ULL {

      @Override
      public boolean isSigned() {
        return false;
      }

      @Override
      public CSimpleType getType() {
        return CNumericTypes.UNSIGNED_LONG_LONG_INT;
      }

      @Override
      public int getLength() {
        return 3;
      }
    };

    abstract boolean isSigned();

    abstract CSimpleType getType();

    abstract int getLength();
  }
}
