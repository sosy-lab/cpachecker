// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.NumeralFormulaManager;

public class NumeralFormulaManagerView<
        ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula>
    extends BaseManagerView implements NumeralFormulaManager<ParamFormulaType, ResultFormulaType> {

  protected final NumeralFormulaManager<ParamFormulaType, ResultFormulaType> numeralFormulaManager;
  protected final BooleanFormulaManager booleanFormulaManager;

  NumeralFormulaManagerView(
      FormulaWrappingHandler pWrappingHandler,
      NumeralFormulaManager<ParamFormulaType, ResultFormulaType> pNumeralManager,
      BooleanFormulaManager pBooleanFormulaManager) {
    super(pWrappingHandler);
    numeralFormulaManager = checkNotNull(pNumeralManager);
    booleanFormulaManager = checkNotNull(pBooleanFormulaManager);
  }

  @Override
  public ResultFormulaType negate(ParamFormulaType pNumber) {
    return numeralFormulaManager.negate(pNumber);
  }

  @Override
  public ResultFormulaType add(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return numeralFormulaManager.add(pNumber1, pNumber2);
  }

  @Override
  public ResultFormulaType sum(List<ParamFormulaType> operands) {
    return numeralFormulaManager.sum(operands);
  }

  @Override
  public ResultFormulaType subtract(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return numeralFormulaManager.subtract(pNumber1, pNumber2);
  }

  @Override
  public ResultFormulaType divide(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return numeralFormulaManager.divide(pNumber1, pNumber2);
  }

  @SuppressWarnings("unchecked")
  public ResultFormulaType cDivide(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    if (numeralFormulaManager instanceof ReplaceIntegerWithBitvectorTheory) {
      // FIXME: This uses the correct BV divide as in this case the divide method below would
      // default to the BV divide which would lead to a non C conform division (as the BV division
      // is already correct)
      return numeralFormulaManager.divide(pNumber1, pNumber2);
    }
    final ResultFormulaType zero = numeralFormulaManager.makeNumber(0);
    final ResultFormulaType additionalUnit =
        booleanFormulaManager.ifThenElse(
            numeralFormulaManager.greaterOrEquals(pNumber2, (ParamFormulaType) zero),
            numeralFormulaManager.makeNumber(1),
            numeralFormulaManager.makeNumber(-1));
    final ResultFormulaType div = numeralFormulaManager.divide(pNumber1, pNumber2);

    // IF   first operand is positive or is divisible by second operand
    // THEN return plain division --> here C99 is equal to SMTlib2
    // ELSE divide and add an additional unit towards the nearest infinity.

    return booleanFormulaManager.ifThenElse(
        booleanFormulaManager.or(
            greaterOrEquals(pNumber1, (ParamFormulaType) zero),
            numeralFormulaManager.equal(
                (ParamFormulaType) numeralFormulaManager.multiply((ParamFormulaType) div, pNumber2),
                pNumber1)),
        div,
        numeralFormulaManager.add((ParamFormulaType) div, (ParamFormulaType) additionalUnit));
  }

  @Override
  public ResultFormulaType multiply(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return numeralFormulaManager.multiply(pNumber1, pNumber2);
  }

  @Override
  public BooleanFormula equal(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return numeralFormulaManager.equal(pNumber1, pNumber2);
  }

  @Override
  public BooleanFormula greaterThan(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return numeralFormulaManager.greaterThan(pNumber1, pNumber2);
  }

  @Override
  public BooleanFormula greaterOrEquals(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return numeralFormulaManager.greaterOrEquals(pNumber1, pNumber2);
  }

  @Override
  public BooleanFormula lessThan(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return numeralFormulaManager.lessThan(pNumber1, pNumber2);
  }

  @Override
  public BooleanFormula lessOrEquals(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return numeralFormulaManager.lessOrEquals(pNumber1, pNumber2);
  }

  @Override
  public BooleanFormula distinct(List<ParamFormulaType> pNumbers) {
    return numeralFormulaManager.distinct(pNumbers);
  }

  @Override
  public ResultFormulaType makeNumber(long pI) {
    return numeralFormulaManager.makeNumber(pI);
  }

  @Override
  public ResultFormulaType makeNumber(BigInteger pI) {
    return numeralFormulaManager.makeNumber(pI);
  }

  @Override
  public ResultFormulaType makeNumber(String pI) {
    return numeralFormulaManager.makeNumber(pI);
  }

  @Override
  public ResultFormulaType makeNumber(double pNumber) {
    return numeralFormulaManager.makeNumber(pNumber);
  }

  @Override
  public ResultFormulaType makeNumber(BigDecimal pNumber) {
    return numeralFormulaManager.makeNumber(pNumber);
  }

  @Override
  public ResultFormulaType makeNumber(Rational pRational) {
    return numeralFormulaManager.makeNumber(pRational);
  }

  @Override
  public ResultFormulaType makeVariable(String pVar) {
    return numeralFormulaManager.makeVariable(pVar);
  }

  public ResultFormulaType makeVariable(String pVar, int idx) {
    return numeralFormulaManager.makeVariable(FormulaManagerView.makeName(pVar, idx));
  }

  @Override
  public FormulaType<ResultFormulaType> getFormulaType() {
    return numeralFormulaManager.getFormulaType();
  }

  @Override
  public IntegerFormula floor(ParamFormulaType pFormula) {
    return numeralFormulaManager.floor(pFormula);
  }
}
