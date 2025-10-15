// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static org.sosy_lab.java_smt.api.FormulaType.getBitvectorTypeWithSize;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.UFManager;

/**
 * Internally, all integers are treated as unsigned unless a signed operator explicitly requires a
 * signed interpretation. For example, creating a negative number actually produces a large unsigned
 * number in the range [{@code 2^(N-1)}, {@code 2^N}) using 2's complement encoding. It is only when
 * a signed operator is applied that the value is interpreted as negative.
 *
 * <p>This behavior is necessary because bitvectors themselves do not carry information about
 * signedness; the interpretation must be imposed externally based on the operations used.
 *
 * <p>A configuration option, {@code cpa.predicate.signedWraparound}, is available to enable
 * wraparound semantics for signed integers as well. When enabled, signed overflow behaves similarly
 * to unsigned overflow, using more expensive arithmetic transformations. If signed overflow is
 * instead treated as undefined behavior (UB), this option is not needed, and a lightweight
 * conditional (ITE) expression is used whenever a signed interpretation is required from an
 * unsigned value.
 *
 * <p>Note that even with {@code signedWraparound} enabled, signed integers are not treated as
 * unbounded. Internally, unsigned wraparound behavior is always assumed for all arithmetic
 * operations.
 *
 * <p>The only operations that differ between signed and unsigned semantics are division and
 * remainder. These operations explicitly encode whether the operands are signed or unsigned,
 * allowing the logic to handle them accordingly. For all other arithmetic operations, the result is
 * unaffected by whether a value is interpreted as a large unsigned number or as a signed 2's
 * complement negative number with the same bit pattern.
 */
@Options(prefix = "cpa.predicate")
class ReplaceBitvectorWithNLAIntegerTheory extends BaseManagerView
    implements BitvectorFormulaManager {

  private final BooleanFormulaManager booleanFormulaManager;
  private final IntegerFormulaManager integerFormulaManager;
  private final UFManager functionManager;

  @Option(
      secure = true,
      description =
          "When a bitwise/shift operation is not supported by the NLA encoding, approximate it"
              + " with an uninterpreted function (UF). If false, the operation fails early by"
              + " throwing UnsupportedOperationException.")
  private boolean approximateBitwiseWithUFs = true;

  ReplaceBitvectorWithNLAIntegerTheory(
      FormulaWrappingHandler pWrappingHandler,
      BooleanFormulaManager pBooleanFormulaManager,
      IntegerFormulaManager pIntegerFormulaManager,
      UFManager pFunctionManager,
      Configuration pConfig)
      throws InvalidConfigurationException {
    super(pWrappingHandler);
    pConfig.inject(this);
    booleanFormulaManager = pBooleanFormulaManager;
    integerFormulaManager = pIntegerFormulaManager;
    functionManager = pFunctionManager;
  }

  IntegerFormula wrapAround(IntegerFormula formula, int size) {
    BigInteger upperLimit = BigInteger.TWO.pow(size);
    IntegerFormula upperLimitFormula = integerFormulaManager.makeNumber(upperLimit);
    return integerFormulaManager.modulo(formula, upperLimitFormula);
  }

  IntegerFormula mapToSignedRange(IntegerFormula formula, int size) {
    BigInteger upperSignedLimit = BigInteger.TWO.pow(size - 1);
    BigInteger upperUnsignedLimit = BigInteger.TWO.pow(size);
    return booleanFormulaManager.ifThenElse(
        integerFormulaManager.greaterOrEquals(
            formula, integerFormulaManager.makeNumber(upperSignedLimit)),
        integerFormulaManager.negate(
            integerFormulaManager.subtract(
                integerFormulaManager.makeNumber(upperUnsignedLimit), formula)),
        formula);
  }

  private IntegerFormula unwrap(BitvectorFormula pNumber) {
    return (IntegerFormula) super.unwrap(pNumber);
  }

  private FunctionDeclaration<IntegerFormula> createUnaryFunction(String name) {
    return functionManager.declareUF(
        name, integerFormulaManager.getFormulaType(), integerFormulaManager.getFormulaType());
  }

  private FunctionDeclaration<IntegerFormula> createBinaryFunction(String name) {
    return functionManager.declareUF(
        name,
        integerFormulaManager.getFormulaType(),
        integerFormulaManager.getFormulaType(),
        integerFormulaManager.getFormulaType());
  }

  private BitvectorFormula makeUf(
      FormulaType<BitvectorFormula> retType,
      FunctionDeclaration<IntegerFormula> decl,
      BitvectorFormula... args) {
    List<org.sosy_lab.java_smt.api.Formula> uargs =
        unwrap(java.util.Arrays.<org.sosy_lab.java_smt.api.Formula>asList(args));
    return wrap(retType, functionManager.callUF(decl, uargs));
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, long pI) {
    return makeBitvector(pLength, BigInteger.valueOf(pI));
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, BigInteger pI) {
    assert pI.bitLength() <= pLength
        : String.format("numeral value %s is too big for bitvector of length %d.", pI, pLength);
    BigInteger value;
    if (pI.compareTo(BigInteger.ZERO) < 0) {
      // Convert to two's complement as an unsigned long value
      value = pI.add(BigInteger.ONE.shiftLeft(pLength));
    } else {
      value = pI;
    }
    IntegerFormula number = wrapAround(integerFormulaManager.makeNumber(value), pLength);
    return wrap(getBitvectorTypeWithSize(pLength), number);
  }

  @Override
  public BitvectorFormula makeVariable(int pLength, String pVar) {
    return makeVariable(getBitvectorTypeWithSize(pLength), pVar);
  }

  @Override
  public BitvectorFormula makeVariable(BitvectorType type, String pVar) {
    final IntegerFormula newVar = integerFormulaManager.makeVariable(pVar);
    return wrap(type, newVar);
  }

  @Override
  public int getLength(BitvectorFormula pNumber) {
    return ((BitvectorType) getFormulaType(pNumber)).getSize();
  }

  @Override
  public BitvectorFormula negate(BitvectorFormula pNumber) {
    return wrap(
        getFormulaType(pNumber),
        wrapAround(integerFormulaManager.negate(unwrap(pNumber)), getLength(pNumber)));
  }

  @Override
  public BitvectorFormula add(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(
        getFormulaType(pNumber1),
        wrapAround(
            integerFormulaManager.add(unwrap(pNumber1), unwrap(pNumber2)), getLength(pNumber1)));
  }

  @Override
  public BitvectorFormula subtract(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(
        getFormulaType(pNumber1),
        wrapAround(
            integerFormulaManager.subtract(unwrap(pNumber1), unwrap(pNumber2)),
            getLength(pNumber1)));
  }

  @Override
  public BitvectorFormula divide(
      BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(
        getFormulaType(pNumber1),
        wrapAround(
            !pSigned
                ? integerFormulaManager.divide(unwrap(pNumber1), unwrap(pNumber2))
                : getC99ReplacementForSMTlib2Division(
                    mapToSignedRange(unwrap(pNumber1), getLength(pNumber1)),
                    mapToSignedRange(unwrap(pNumber2), getLength(pNumber2))),
            getLength(pNumber1)));
  }

  @Override
  public BitvectorFormula smodulo(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    // Note: signed bv modulo behaves differently compared to int modulo or bv remainder!
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula remainder(
      BitvectorFormula numerator, BitvectorFormula denominator, boolean signed) {
    assert getLength(numerator) == getLength(denominator)
        : "Expect operators to have the same size";

    IntegerFormula divisor = unwrap(denominator);
    IntegerFormula dividend = unwrap(numerator);

    if (signed) {
      divisor = mapToSignedRange(divisor, getLength(denominator));
      dividend = mapToSignedRange(dividend, getLength(numerator));
      final IntegerFormula zero = integerFormulaManager.makeNumber(0);
      final IntegerFormula additionalUnit =
          booleanFormulaManager.ifThenElse(
              integerFormulaManager.greaterOrEquals(divisor, zero),
              integerFormulaManager.negate(divisor),
              divisor);

      final IntegerFormula mod = integerFormulaManager.modulo(dividend, divisor);

      return wrap(
          getFormulaType(numerator),
          wrapAround(
              booleanFormulaManager.ifThenElse(
                  booleanFormulaManager.or(
                      integerFormulaManager.greaterOrEquals(dividend, zero),
                      integerFormulaManager.equal(mod, zero)),
                  mod,
                  integerFormulaManager.add(mod, additionalUnit)),
              getLength(numerator)));
    } else {
      return wrap(getFormulaType(numerator), integerFormulaManager.modulo(dividend, divisor));
    }
  }

  /**
   * @see BitvectorFormulaManagerView#divide(BitvectorFormula, BitvectorFormula, boolean)
   */
  private IntegerFormula getC99ReplacementForSMTlib2Division(
      final IntegerFormula f1, final IntegerFormula f2) {

    final IntegerFormula zero = integerFormulaManager.makeNumber(0);
    final IntegerFormula additionalUnit =
        booleanFormulaManager.ifThenElse(
            integerFormulaManager.greaterOrEquals(f2, zero),
            integerFormulaManager.makeNumber(1),
            integerFormulaManager.makeNumber(-1));
    final IntegerFormula div = integerFormulaManager.divide(f1, f2);

    // IF   first operand is positive or is divisible by second operand
    // THEN return plain division --> here C99 is equal to SMTlib2
    // ELSE divide and add an additional unit towards the nearest infinity.

    return booleanFormulaManager.ifThenElse(
        booleanFormulaManager.or(
            integerFormulaManager.greaterOrEquals(f1, zero),
            integerFormulaManager.equal(integerFormulaManager.multiply(div, f2), f1)),
        div,
        integerFormulaManager.add(div, additionalUnit));
  }

  @Override
  public BitvectorFormula multiply(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return wrap(
        getFormulaType(pNumber1),
        wrapAround(
            integerFormulaManager.multiply(unwrap(pNumber1), unwrap(pNumber2)),
            getLength(pNumber1)));
  }

  @Override
  public BooleanFormula equal(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    return integerFormulaManager.equal(unwrap(pNumber1), unwrap(pNumber2));
  }

  @Override
  public BooleanFormula greaterThan(
      BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    if (!pSigned) {
      return integerFormulaManager.greaterThan(unwrap(pNumber1), unwrap(pNumber2));
    } else {
      return integerFormulaManager.greaterThan(
          mapToSignedRange(unwrap(pNumber1), getLength(pNumber1)),
          mapToSignedRange(unwrap(pNumber2), getLength(pNumber2)));
    }
  }

  @Override
  public BooleanFormula greaterOrEquals(
      BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    if (!pSigned) {
      return integerFormulaManager.greaterOrEquals(unwrap(pNumber1), unwrap(pNumber2));
    } else {
      return integerFormulaManager.greaterOrEquals(
          mapToSignedRange(unwrap(pNumber1), getLength(pNumber1)),
          mapToSignedRange(unwrap(pNumber2), getLength(pNumber2)));
    }
  }

  @Override
  public BooleanFormula lessThan(
      BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    if (!pSigned) {
      return integerFormulaManager.lessThan(unwrap(pNumber1), unwrap(pNumber2));
    } else {
      return integerFormulaManager.lessThan(
          mapToSignedRange(unwrap(pNumber1), getLength(pNumber1)),
          mapToSignedRange(unwrap(pNumber2), getLength(pNumber2)));
    }
  }

  @Override
  public BooleanFormula lessOrEquals(
      BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean pSigned) {
    assert getLength(pNumber1) == getLength(pNumber2) : "Expect operators to have the same size";
    if (!pSigned) {
      return integerFormulaManager.lessOrEquals(unwrap(pNumber1), unwrap(pNumber2));
    } else {
      return integerFormulaManager.lessOrEquals(
          mapToSignedRange(unwrap(pNumber1), getLength(pNumber1)),
          mapToSignedRange(unwrap(pNumber2), getLength(pNumber2)));
    }
  }

  public BooleanFormula makeRangeConstraint(
      BitvectorFormula term, BigInteger start, BigInteger end) {
    if (start.compareTo(BigInteger.ZERO) < 0) {
      end = end.subtract(start);
      start = BigInteger.ZERO;
    }
    assert end.compareTo(BigInteger.ZERO) >= 0 : "Expect end of range to always be positive";
    return booleanFormulaManager.and(
        integerFormulaManager.lessOrEquals(integerFormulaManager.makeNumber(start), unwrap(term)),
        integerFormulaManager.lessOrEquals(unwrap(term), integerFormulaManager.makeNumber(end)));
  }

  @Override
  public BitvectorFormula shiftRight(
      BitvectorFormula pNumber, BitvectorFormula pToShift, boolean signed) {
    if (approximateBitwiseWithUFs) {
      FunctionDeclaration<IntegerFormula> decl = createBinaryFunction("_shiftRight_approx");
      return makeUf(getFormulaType(pNumber), decl, pNumber, pToShift);
    }
    throw new UnsupportedOperationException("shiftRight not implemented for NLA encoding");
  }

  @Override
  public BitvectorFormula shiftLeft(BitvectorFormula pNumber, BitvectorFormula pToShift) {
    if (approximateBitwiseWithUFs) {
      FunctionDeclaration<IntegerFormula> decl = createBinaryFunction("_shiftLeft_approx");
      return makeUf(getFormulaType(pNumber), decl, pNumber, pToShift);
    }
    throw new UnsupportedOperationException("shiftLeft not implemented for NLA encoding");
  }

  @Override
  public BitvectorFormula rotateLeft(BitvectorFormula number, int toRotate) {
    if (approximateBitwiseWithUFs) {
      FunctionDeclaration<IntegerFormula> decl = createUnaryFunction("_rotateLeft_const_approx");
      return makeUf(getFormulaType(number), decl, number);
    }
    throw new UnsupportedOperationException("rotateLeft not implemented for NLA encoding");
  }

  @Override
  public BitvectorFormula rotateLeft(BitvectorFormula number, BitvectorFormula toRotate) {
    if (approximateBitwiseWithUFs) {
      FunctionDeclaration<IntegerFormula> decl = createBinaryFunction("_rotateLeft_approx");
      return makeUf(getFormulaType(number), decl, number, toRotate);
    }
    throw new UnsupportedOperationException("rotateLeft not implemented for NLA encoding");
  }

  @Override
  public BitvectorFormula rotateRight(BitvectorFormula number, int toRotate) {
    if (approximateBitwiseWithUFs) {
      FunctionDeclaration<IntegerFormula> decl = createUnaryFunction("_rotateRight_const_approx");
      return makeUf(getFormulaType(number), decl, number);
    }
    throw new UnsupportedOperationException("rotateRight not implemented for NLA encoding");
  }

  @Override
  public BitvectorFormula rotateRight(BitvectorFormula number, BitvectorFormula toRotate) {
    if (approximateBitwiseWithUFs) {
      FunctionDeclaration<IntegerFormula> decl = createBinaryFunction("_rotateRight_approx");
      return makeUf(getFormulaType(number), decl, number, toRotate);
    }
    throw new UnsupportedOperationException("rotateRight not implemented for NLA encoding");
  }

  @Override
  public BitvectorFormula concat(BitvectorFormula pFirst, BitvectorFormula pSecound) {
    if (approximateBitwiseWithUFs) {
      FunctionDeclaration<IntegerFormula> decl = createBinaryFunction("_concat_approx");
      return makeUf(getFormulaType(pFirst), decl, pFirst, pSecound);
    }
    throw new UnsupportedOperationException("concat not implemented for NLA encoding");
  }

  @Override
  public BitvectorFormula extract(BitvectorFormula pNumber, int pMsb, int pLsb) {
    if (pLsb != 0) {
      throw new UnsupportedOperationException("not yet implemented for CPAchecker");
    }
    return wrap(getBitvectorTypeWithSize(pMsb + 1), wrapAround(unwrap(pNumber), pMsb + 1));
  }

  @Override
  public BitvectorFormula extend(BitvectorFormula pNumber, int pExtensionBits, boolean pSigned) {
    int width = getLength(pNumber);
    IntegerFormula x = unwrap(pNumber);

    if (!pSigned) {
      return wrap(getBitvectorTypeWithSize(width + pExtensionBits), x);
    } else {
      BigInteger nextUpperLimit = BigInteger.ONE.shiftLeft(width + pExtensionBits);
      BigInteger currentUpperLimit = BigInteger.ONE.shiftLeft(width);
      BigInteger currentSignedUpperLimit = BigInteger.ONE.shiftLeft(width - 1);

      IntegerFormula diffUpperLimit =
          integerFormulaManager.makeNumber(nextUpperLimit.subtract(currentUpperLimit));
      IntegerFormula currentSignedUpperLimitFormula =
          integerFormulaManager.makeNumber(currentSignedUpperLimit);

      BooleanFormula isNegative =
          integerFormulaManager.greaterOrEquals(x, currentSignedUpperLimitFormula);
      IntegerFormula xSigned =
          booleanFormulaManager.ifThenElse(
              isNegative, integerFormulaManager.add(x, diffUpperLimit), x);
      return wrap(getBitvectorTypeWithSize(width + pExtensionBits), xSigned);
    }
  }

  @Override
  public BitvectorFormula not(BitvectorFormula pBits) {
    if (approximateBitwiseWithUFs) {
      FunctionDeclaration<IntegerFormula> decl = createUnaryFunction("_not_approx");
      return makeUf(getFormulaType(pBits), decl, pBits);
    }
    throw new UnsupportedOperationException("not operation not implemented for NLA encoding");
  }

  @Override
  public BitvectorFormula and(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    if (approximateBitwiseWithUFs) {
      FunctionDeclaration<IntegerFormula> decl = createBinaryFunction("_and_approx");
      return makeUf(getFormulaType(pBits1), decl, pBits1, pBits2);
    }
    throw new UnsupportedOperationException("and operation not implemented for NLA encoding");
  }

  @Override
  public BitvectorFormula or(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    if (approximateBitwiseWithUFs) {
      FunctionDeclaration<IntegerFormula> decl = createBinaryFunction("_or_approx");
      return makeUf(getFormulaType(pBits1), decl, pBits1, pBits2);
    }
    throw new UnsupportedOperationException("or operation not implemented for NLA encoding");
  }

  @Override
  public BitvectorFormula xor(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    if (approximateBitwiseWithUFs) {
      FunctionDeclaration<IntegerFormula> decl = createBinaryFunction("_xor_approx");
      return makeUf(getFormulaType(pBits1), decl, pBits1, pBits2);
    }
    throw new UnsupportedOperationException("xor operation not implemented for NLA encoding");
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, IntegerFormula pI) {
    // INT to BV -> just wrap
    return wrap(getBitvectorTypeWithSize(pLength), unwrap(wrapAround(pI, pLength)));
  }

  @Override
  public IntegerFormula toIntegerFormula(BitvectorFormula pI, boolean pSigned) {
    final IntegerFormula unwrapped = unwrap(pI);
    if (integerFormulaManager.getFormulaType().equals(FormulaType.IntegerType)) {
      return unwrapped;
    } else {
      return integerFormulaManager.floor(unwrapped);
    }
  }

  @Override
  public BooleanFormula distinct(List<BitvectorFormula> pBits) {
    if (pBits.isEmpty()) {
      return booleanFormulaManager.makeTrue();
    }
    int bitsize = getLength(pBits.get(0));
    pBits.forEach(
        bit ->
            Preconditions.checkArgument(
                bitsize == getLength(bit), "Expect operators to have the same size"));
    return integerFormulaManager.distinct(Lists.transform(pBits, this::unwrap));
  }
}
