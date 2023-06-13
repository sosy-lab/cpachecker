// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

class ReplaceIntegerWithBitvectorTheory extends BaseManagerView implements IntegerFormulaManager {

  private final BitvectorFormulaManager bvFormulaManager;
  private final BooleanFormulaManager booleanFormulaManager;
  private final int bitsize;

  @Options(prefix = "cpa.predicate")
  static class ReplaceIntegerEncodingOptions {

    ReplaceIntegerEncodingOptions(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }

    @Option(secure = true, description = "The bitsize is used to encode integers as bitvectors.")
    private int bitsize = 32;

    public int getBitsize() {
      return bitsize;
    }
  }

  ReplaceIntegerWithBitvectorTheory(
      FormulaWrappingHandler pWrappingHandler,
      BitvectorFormulaManager pReplacementManager,
      BooleanFormulaManager pBooleanFormulaManager,
      ReplaceIntegerEncodingOptions pOptions) {
    super(pWrappingHandler);
    booleanFormulaManager = Preconditions.checkNotNull(pBooleanFormulaManager);
    bvFormulaManager = Preconditions.checkNotNull(pReplacementManager);
    bitsize = pOptions.getBitsize();
  }

  @SuppressWarnings("unchecked")
  private BitvectorFormula unwrap(IntegerFormula pNumber) {
    return (BitvectorFormula) super.unwrap(pNumber);
  }

  @Override
  public IntegerFormula makeNumber(long pNumber) {
    return makeNumber(BigInteger.valueOf(pNumber));
  }

  @Override
  public IntegerFormula makeNumber(BigInteger pNumber) {
    return wrap(FormulaType.IntegerType, bvFormulaManager.makeBitvector(bitsize, pNumber));
  }

  @Override
  public IntegerFormula makeNumber(double pNumber) {
    return makeNumber(BigDecimal.valueOf(pNumber));
  }

  @Override
  public IntegerFormula makeNumber(BigDecimal pNumber) {
    return wrap(
        FormulaType.IntegerType, bvFormulaManager.makeBitvector(bitsize, pNumber.toBigInteger()));
  }

  @Override
  public IntegerFormula makeNumber(String pI) {
    return makeNumber(new BigInteger(pI));
  }

  @Override
  public IntegerFormula makeNumber(Rational pRational) {
    return makeNumber(pRational.longValue());
  }

  @Override
  public IntegerFormula makeVariable(String pVar) {
    return wrap(FormulaType.IntegerType, bvFormulaManager.makeVariable(bitsize, pVar));
  }

  @Override
  public IntegerFormula negate(IntegerFormula pNumber) {
    return wrap(FormulaType.IntegerType, bvFormulaManager.negate(unwrap(pNumber)));
  }

  @Override
  public IntegerFormula add(IntegerFormula pNumber1, IntegerFormula pNumber2) {
    return wrap(FormulaType.IntegerType, bvFormulaManager.add(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public IntegerFormula sum(List<IntegerFormula> pOperands) {
    IntegerFormula result = makeNumber(0);
    for (IntegerFormula operand : pOperands) {
      result = add(result, operand);
    }
    return result;
  }

  @Override
  public IntegerFormula subtract(IntegerFormula pNumber1, IntegerFormula pNumber2) {
    return wrap(
        FormulaType.IntegerType, bvFormulaManager.subtract(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public IntegerFormula divide(IntegerFormula pNumber1, IntegerFormula pNumber2) {
    return wrap(
        FormulaType.IntegerType, bvFormulaManager.divide(unwrap(pNumber1), unwrap(pNumber2), true));
  }

  @Override
  public IntegerFormula multiply(IntegerFormula pNumber1, IntegerFormula pNumber2) {
    return wrap(
        FormulaType.IntegerType, bvFormulaManager.multiply(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public BooleanFormula equal(IntegerFormula pNumber1, IntegerFormula pNumber2) {
    return wrap(
        FormulaType.BooleanType, bvFormulaManager.equal(unwrap(pNumber1), unwrap(pNumber2)));
  }

  @Override
  public BooleanFormula distinct(List<IntegerFormula> pNumbers) {
    // BV manager does not support distinct, we implement this directly
    List<BooleanFormula> r = new ArrayList<>();
    for (int i = 0; i < pNumbers.size(); i++) {
      for (int j = 0; j < i; j++) {
        r.add(booleanFormulaManager.not(equal(pNumbers.get(i), pNumbers.get(j))));
      }
    }
    return booleanFormulaManager.and(r);
  }

  @Override
  public BooleanFormula greaterThan(IntegerFormula pNumber1, IntegerFormula pNumber2) {
    return wrap(
        FormulaType.BooleanType,
        bvFormulaManager.greaterThan(unwrap(pNumber1), unwrap(pNumber2), true));
  }

  @Override
  public BooleanFormula greaterOrEquals(IntegerFormula pNumber1, IntegerFormula pNumber2) {
    return wrap(
        FormulaType.BooleanType,
        bvFormulaManager.greaterOrEquals(unwrap(pNumber1), unwrap(pNumber2), true));
  }

  @Override
  public BooleanFormula lessThan(IntegerFormula pNumber1, IntegerFormula pNumber2) {
    return wrap(
        FormulaType.BooleanType,
        bvFormulaManager.lessThan(unwrap(pNumber1), unwrap(pNumber2), true));
  }

  @Override
  public BooleanFormula lessOrEquals(IntegerFormula pNumber1, IntegerFormula pNumber2) {
    return wrap(
        FormulaType.BooleanType,
        bvFormulaManager.lessOrEquals(unwrap(pNumber1), unwrap(pNumber2), true));
  }

  @Override
  public IntegerFormula floor(IntegerFormula pFormula) {
    return pFormula;
  }

  @Override
  public BooleanFormula modularCongruence(
      IntegerFormula pNumber1, IntegerFormula pNumber2, BigInteger pModulo) {
    // ((_ divisible n) x)   <==>   (= x (* n (div x n)))
    if (BigInteger.ZERO.compareTo(pModulo) < 0) {
      IntegerFormula n = makeNumber(pModulo);
      IntegerFormula x = subtract(pNumber1, pNumber2);
      return equal(x, multiply(n, divide(x, n)));
    }
    return booleanFormulaManager.makeTrue();
  }

  @Override
  public BooleanFormula modularCongruence(
      IntegerFormula pNumber1, IntegerFormula pNumber2, long pN) {
    return modularCongruence(pNumber1, pNumber2, BigInteger.valueOf(pN));
  }

  @Override
  public IntegerFormula modulo(IntegerFormula pNumber1, IntegerFormula pNumber2) {
    return wrap(
        FormulaType.IntegerType, bvFormulaManager.modulo(unwrap(pNumber1), unwrap(pNumber2), true));
  }
}
