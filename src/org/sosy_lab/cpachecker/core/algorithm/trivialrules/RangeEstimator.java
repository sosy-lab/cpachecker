// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * Computes an {@link IntegerRange} that over-approximates the values of an expression, from nothing
 * but the types of the program and the values of the variables that are constant (cf. {@link
 * ProgramFacts#constants()}).
 *
 * <p>The estimator abstains (returns {@code null}) whenever it cannot argue about an expression, so
 * every answer is an over-approximation of the values the expression really has. Two facts make it
 * work without any analysis of the program:
 *
 * <ul>
 *   <li>A C expression of an integer type has a value of that type, so the range of the type is
 *       always an answer. This is what makes the estimator useful for narrow types: an expression
 *       of type {@code unsigned char} is in [0, 255], whatever the program computes.
 *   <li>{@code &&}, {@code ||} and {@code ?:} do not exist in the AST of CPAchecker (the CFA
 *       contains a branching instead), so every subexpression of an expression is evaluated, and
 *       there is no need to reason about short-circuit evaluation.
 * </ul>
 *
 * <p>Signed overflow is undefined behavior, so the estimator does not invent a value for an
 * operation whose mathematical result leaves the range of its signed calculation type: it abstains,
 * which lets the rules abstain as well. For unsigned types the wrap-around is defined, and the
 * range of the type is a sound answer.
 */
final class RangeEstimator {

  private final MachineModel machineModel;
  private final ImmutableMap<String, BigInteger> constants;

  RangeEstimator(MachineModel pMachineModel, ImmutableMap<String, BigInteger> pConstants) {
    machineModel = pMachineModel;
    constants = pConstants;
  }

  /** The range of the given expression, or {@code null} if we cannot say anything about it. */
  @Nullable IntegerRange rangeOf(CExpression pExpression) {
    if (pExpression instanceof CIntegerLiteralExpression literal) {
      return IntegerRange.of(literal.getValue());

    } else if (pExpression instanceof CCharLiteralExpression literal) {
      char character = literal.getCharacter();
      if (character <= 127) {
        // The value of a character of the basic character set is the same in every representation.
        return IntegerRange.of(character, character);
      }
      return rangeOfType(pExpression.getExpressionType());

    } else if (pExpression instanceof CIdExpression id) {
      if (id.getDeclaration() instanceof CEnumerator enumerator) {
        return IntegerRange.of(enumerator.getValue());
      }
      if (id.getDeclaration() != null) {
        BigInteger constant = constants.get(id.getDeclaration().getQualifiedName());
        if (constant != null) {
          return IntegerRange.of(constant);
        }
      }
      return rangeOfType(pExpression.getExpressionType());

    } else if (pExpression instanceof CCastExpression cast) {
      return convertTo(cast.getExpressionType(), rangeOf(cast.getOperand()));

    } else if (pExpression instanceof CUnaryExpression unary) {
      return convertTo(unary.getExpressionType(), resultOfOperation(unary));

    } else if (pExpression instanceof CBinaryExpression binary) {
      IntegerRange inCalculationType =
          convertTo(binary.getCalculationType(), resultOfOperation(binary));
      return convertTo(binary.getExpressionType(), inCalculationType);
    }

    // Every other expression of an integer type has a value of that type.
    return rangeOfType(pExpression.getExpressionType());
  }

  /** The range of the values of the given type, or {@code null} if it is not an integer type. */
  @Nullable IntegerRange rangeOfType(CType pType) {
    CSimpleType type = asIntegerType(pType);
    if (type == null) {
      return null;
    }
    return new IntegerRange(
        machineModel.getMinimalIntegerValue(type), machineModel.getMaximalIntegerValue(type));
  }

  /** Whether the given type is a signed integer type, for which an overflow is undefined. */
  boolean isSignedIntegerType(CType pType) {
    CSimpleType type = asIntegerType(pType);
    return type != null && machineModel.isSigned(type);
  }

  /** Whether the expression has a value other than zero for every value of its operands. */
  boolean isAlwaysTrue(CExpression pExpression) {
    IntegerRange range = rangeOf(pExpression);
    return range != null && !range.contains(BigInteger.ZERO);
  }

  /** Whether the expression has the value zero for every value of its operands. */
  boolean isAlwaysFalse(CExpression pExpression) {
    IntegerRange range = rangeOf(pExpression);
    return range != null && BigInteger.ZERO.equals(range.exactValue());
  }

  /**
   * The mathematical result of the given operation, i.e., the result before it is converted to the
   * calculation type of the operation. This is the value that has to be compared with the range of
   * that type in order to find out whether the operation overflows.
   */
  @Nullable IntegerRange resultOfOperation(CBinaryExpression pExpression) {
    IntegerRange operand1 = rangeOf(pExpression.getOperand1());
    IntegerRange operand2 = rangeOf(pExpression.getOperand2());
    if (operand1 == null || operand2 == null) {
      return null;
    }
    CType calculationType = pExpression.getCalculationType();
    if (rangeOfType(calculationType) == null) {
      // Not an integer operation, e.g. floating-point or pointer arithmetic.
      return null;
    }
    if (!isSignedIntegerType(calculationType)
        && !(operand1.isNonNegative() && operand2.isNonNegative())) {
      // The operands are converted to the unsigned calculation type first, which wraps around for
      // a negative value. We do not model that conversion.
      return null;
    }
    return switch (pExpression.getOperator()) {
      case PLUS -> operand1.plus(operand2);
      case MINUS -> operand1.minus(operand2);
      case MULTIPLY -> operand1.times(operand2);
      case DIVIDE -> divide(operand1, operand2);
      case REMAINDER -> remainder(operand1, operand2);
      case SHIFT_LEFT -> shiftLeft(operand1, operand2, calculationType);
      case SHIFT_RIGHT -> shiftRight(operand1, operand2, calculationType);
      case BITWISE_AND -> bitwiseAnd(operand1, operand2);
      case BITWISE_OR, BITWISE_XOR -> bitwiseOrXor(operand1, operand2);
      case EQUALS, NOT_EQUALS, LESS_THAN, LESS_EQUAL, GREATER_THAN, GREATER_EQUAL ->
          compare(pExpression.getOperator(), operand1, operand2);
    };
  }

  /** The mathematical result of the given operation, cf. {@link #resultOfOperation}. */
  @Nullable IntegerRange resultOfOperation(CUnaryExpression pExpression) {
    return switch (pExpression.getOperator()) {
      case MINUS -> {
        IntegerRange operand = rangeOf(pExpression.getOperand());
        yield operand == null ? null : operand.negate();
      }
      case TILDE -> {
        IntegerRange operand = rangeOf(pExpression.getOperand());
        // ~x == -x - 1
        yield operand == null ? null : operand.negate().minus(IntegerRange.of(1, 1));
      }
      // The address of an object is not an integer, and the value of sizeof and __alignof__ is
      // handled by the range of their type (size_t).
      case AMPER, SIZEOF, ALIGNOF -> null;
    };
  }

  /**
   * Convert the given result of an operation to the given type. A value that does not fit into a
   * signed type is undefined behavior, so we do not invent a value for it; the wrap-around of an
   * unsigned type is defined, so the range of the type is a sound answer there.
   */
  private @Nullable IntegerRange convertTo(CType pType, @Nullable IntegerRange pRange) {
    IntegerRange typeRange = rangeOfType(pType);
    if (pRange == null) {
      // Whatever the expression computes, its value is a value of its type.
      return typeRange;
    }
    if (typeRange == null) {
      return null;
    }
    if (pRange.isWithin(typeRange)) {
      return pRange;
    }
    return isSignedIntegerType(pType) ? null : typeRange;
  }

  private @Nullable CSimpleType asIntegerType(CType pType) {
    CType canonical = pType.getCanonicalType();
    CSimpleType simple = null;
    if (canonical instanceof CSimpleType type) {
      simple = type;
    } else if (canonical instanceof CEnumType type) {
      simple = type.getCompatibleType();
    }
    if (simple == null || !simple.getType().isIntegerType()) {
      return null;
    }
    return simple;
  }

  private static @Nullable IntegerRange divide(IntegerRange pDividend, IntegerRange pDivisor) {
    if (pDivisor.contains(BigInteger.ZERO)) {
      return null;
    }
    // The divisor has a fixed sign, so the extreme values are among the results for the extreme
    // operands. BigInteger.divide truncates towards zero, just like C.
    BigInteger a = pDividend.low().divide(pDivisor.low());
    BigInteger b = pDividend.low().divide(pDivisor.high());
    BigInteger c = pDividend.high().divide(pDivisor.low());
    BigInteger d = pDividend.high().divide(pDivisor.high());
    return new IntegerRange(a.min(b).min(c).min(d), a.max(b).max(c).max(d));
  }

  private static @Nullable IntegerRange remainder(IntegerRange pDividend, IntegerRange pDivisor) {
    if (pDivisor.contains(BigInteger.ZERO)) {
      return null;
    }
    BigInteger dividend = pDividend.exactValue();
    BigInteger divisor = pDivisor.exactValue();
    if (dividend != null && divisor != null) {
      return IntegerRange.of(dividend.remainder(divisor));
    }
    // The result has the sign of the dividend and its magnitude is smaller than the one of the
    // divisor.
    BigInteger magnitude =
        pDivisor
            .low()
            .abs()
            .max(pDivisor.high().abs())
            .subtract(BigInteger.ONE)
            .max(BigInteger.ZERO);
    BigInteger low = pDividend.low().signum() < 0 ? magnitude.negate() : BigInteger.ZERO;
    BigInteger high = pDividend.high().signum() > 0 ? magnitude : BigInteger.ZERO;
    return new IntegerRange(low, high);
  }

  private @Nullable IntegerRange shiftLeft(
      IntegerRange pOperand, IntegerRange pShift, CType pCalculationType) {
    if (!isValidShift(pOperand, pShift, pCalculationType)) {
      return null;
    }
    // x << s == x * 2^s, and both operands are non-negative.
    return new IntegerRange(
        pOperand.low().shiftLeft(pShift.low().intValueExact()),
        pOperand.high().shiftLeft(pShift.high().intValueExact()));
  }

  private @Nullable IntegerRange shiftRight(
      IntegerRange pOperand, IntegerRange pShift, CType pCalculationType) {
    if (!isValidShift(pOperand, pShift, pCalculationType)) {
      return null;
    }
    return new IntegerRange(
        pOperand.low().shiftRight(pShift.high().intValueExact()),
        pOperand.high().shiftRight(pShift.low().intValueExact()));
  }

  /** Whether shifting the given operand by the given number of bits is defined behavior. */
  private boolean isValidShift(IntegerRange pOperand, IntegerRange pShift, CType pCalculationType) {
    if (!pOperand.isNonNegative() || !pShift.isNonNegative()) {
      // Shifting a negative value, or shifting by a negative number of bits, is not defined.
      return false;
    }
    CSimpleType type = asIntegerType(pCalculationType);
    return type != null
        && pShift.high().compareTo(BigInteger.valueOf(machineModel.getSizeofInBits(type))) < 0;
  }

  private static @Nullable IntegerRange bitwiseAnd(IntegerRange pOperand1, IntegerRange pOperand2) {
    // Masking with a non-negative constant bounds the result, also for a negative operand, because
    // the operation works on the two's-complement representation that CPAchecker assumes.
    BigInteger mask1 = pOperand1.exactValue();
    if (mask1 != null && mask1.signum() >= 0) {
      return new IntegerRange(BigInteger.ZERO, mask1);
    }
    BigInteger mask2 = pOperand2.exactValue();
    if (mask2 != null && mask2.signum() >= 0) {
      return new IntegerRange(BigInteger.ZERO, mask2);
    }
    if (pOperand1.isNonNegative() && pOperand2.isNonNegative()) {
      return new IntegerRange(BigInteger.ZERO, pOperand1.high().min(pOperand2.high()));
    }
    return null;
  }

  private static @Nullable IntegerRange bitwiseOrXor(
      IntegerRange pOperand1, IntegerRange pOperand2) {
    if (!pOperand1.isNonNegative() || !pOperand2.isNonNegative()) {
      return null;
    }
    // The result needs at most as many bits as the larger operand.
    int bits = Math.max(pOperand1.high().bitLength(), pOperand2.high().bitLength());
    return new IntegerRange(
        BigInteger.ZERO, BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE));
  }

  private static IntegerRange compare(
      BinaryOperator pOperator, IntegerRange pOperand1, IntegerRange pOperand2) {
    boolean isTrue;
    boolean isFalse;
    switch (pOperator) {
      case LESS_THAN -> {
        isTrue = pOperand1.high().compareTo(pOperand2.low()) < 0;
        isFalse = pOperand1.low().compareTo(pOperand2.high()) >= 0;
      }
      case LESS_EQUAL -> {
        isTrue = pOperand1.high().compareTo(pOperand2.low()) <= 0;
        isFalse = pOperand1.low().compareTo(pOperand2.high()) > 0;
      }
      case GREATER_THAN -> {
        isTrue = pOperand1.low().compareTo(pOperand2.high()) > 0;
        isFalse = pOperand1.high().compareTo(pOperand2.low()) <= 0;
      }
      case GREATER_EQUAL -> {
        isTrue = pOperand1.low().compareTo(pOperand2.high()) >= 0;
        isFalse = pOperand1.high().compareTo(pOperand2.low()) < 0;
      }
      case EQUALS -> {
        isTrue =
            pOperand1.exactValue() != null && pOperand1.exactValue().equals(pOperand2.exactValue());
        isFalse =
            pOperand1.high().compareTo(pOperand2.low()) < 0
                || pOperand2.high().compareTo(pOperand1.low()) < 0;
      }
      case NOT_EQUALS -> {
        isFalse =
            pOperand1.exactValue() != null && pOperand1.exactValue().equals(pOperand2.exactValue());
        isTrue =
            pOperand1.high().compareTo(pOperand2.low()) < 0
                || pOperand2.high().compareTo(pOperand1.low()) < 0;
      }
      default -> throw new AssertionError("not a comparison: " + pOperator);
    }
    if (isTrue) {
      return IntegerRange.of(1, 1);
    }
    return isFalse ? IntegerRange.of(0, 0) : IntegerRange.of(0, 1);
  }
}
