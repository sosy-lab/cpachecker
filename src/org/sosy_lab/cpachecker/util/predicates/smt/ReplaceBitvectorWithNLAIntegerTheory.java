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
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

@Options(prefix = "cpa.predicate")
class ReplaceBitvectorWithNLAIntegerTheory extends BaseManagerView
    implements BitvectorFormulaManager {

  private final BooleanFormulaManager booleanFormulaManager;
  private final IntegerFormulaManager integerFormulaManager;

  ReplaceBitvectorWithNLAIntegerTheory(
      FormulaWrappingHandler pWrappingHandler,
      BooleanFormulaManager pBooleanFormulaManager,
      IntegerFormulaManager pIntegerFormulaManager,
      Configuration pConfig)
      throws InvalidConfigurationException {
    super(pWrappingHandler);
    pConfig.inject(this);
    booleanFormulaManager = pBooleanFormulaManager;
    integerFormulaManager = pIntegerFormulaManager;
  }

  @Option(
      secure = true,
      description =
          "Use signed wraparound when encoding bitvectors as integers, using non-linear arithmetic"
              + " (default: false, use unbounded domain for signed values)")
  private boolean signedWraparound = false;

  IntegerFormula wrapAround(IntegerFormula formula, int size) {
    BigInteger upperLimit = BigInteger.TWO.pow(size);
    IntegerFormula upperLimitFormula = integerFormulaManager.makeNumber(upperLimit);
    return integerFormulaManager.modulo(formula, upperLimitFormula);
  }

  IntegerFormula wrapAroundSigned(IntegerFormula formula, int size) {
    BigInteger lowerSignedLimit = BigInteger.TWO.pow(size - 1).negate();
    BigInteger upperSignedLimit = BigInteger.TWO.pow(size - 1);
    BigInteger upperUnsignedLimit = BigInteger.TWO.pow(size);
    if (signedWraparound) {
      return integerFormulaManager.subtract(
          integerFormulaManager.modulo(
              integerFormulaManager.add(
                  formula, integerFormulaManager.makeNumber(lowerSignedLimit)),
              integerFormulaManager.makeNumber(upperUnsignedLimit)),
          integerFormulaManager.makeNumber(lowerSignedLimit));
    } else {
      // we still need to convert the value to a signed version
      return booleanFormulaManager.ifThenElse(
          integerFormulaManager.greaterOrEquals(
              formula, integerFormulaManager.makeNumber(upperSignedLimit)),
          integerFormulaManager.negate(
              integerFormulaManager.subtract(
                  integerFormulaManager.makeNumber(upperUnsignedLimit), formula)),
          formula);
    }
  }

  private IntegerFormula unwrap(BitvectorFormula pNumber) {
    return (IntegerFormula) super.unwrap(pNumber);
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
    IntegerFormula number = integerFormulaManager.makeNumber(value);
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
                    wrapAroundSigned(unwrap(pNumber1), getLength(pNumber1)),
                    wrapAroundSigned(unwrap(pNumber2), getLength(pNumber2))),
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
      divisor = wrapAroundSigned(divisor, getLength(denominator));
      dividend = wrapAroundSigned(dividend, getLength(numerator));
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
          wrapAroundSigned(unwrap(pNumber1), getLength(pNumber1)),
          wrapAroundSigned(unwrap(pNumber2), getLength(pNumber2)));
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
          wrapAroundSigned(unwrap(pNumber1), getLength(pNumber1)),
          wrapAroundSigned(unwrap(pNumber2), getLength(pNumber2)));
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
          wrapAroundSigned(unwrap(pNumber1), getLength(pNumber1)),
          wrapAroundSigned(unwrap(pNumber2), getLength(pNumber2)));
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
          wrapAroundSigned(unwrap(pNumber1), getLength(pNumber1)),
          wrapAroundSigned(unwrap(pNumber2), getLength(pNumber2)));
    }
  }

  public BooleanFormula addRangeConstraint(
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
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula shiftLeft(BitvectorFormula pNumber, BitvectorFormula pToShift) {
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula rotateLeft(BitvectorFormula number, int toRotate) {
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula rotateLeft(BitvectorFormula number, BitvectorFormula toRotate) {
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula rotateRight(BitvectorFormula number, int toRotate) {
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula rotateRight(BitvectorFormula number, BitvectorFormula toRotate) {
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula concat(BitvectorFormula pFirst, BitvectorFormula pSecound) {
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
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
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula and(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula or(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula xor(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    throw new UnsupportedOperationException("not yet implemented for CPAchecker");
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, IntegerFormula pI) {
    // INT to BV -> just wrap
    return wrap(getBitvectorTypeWithSize(pLength), unwrap(pI));
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
