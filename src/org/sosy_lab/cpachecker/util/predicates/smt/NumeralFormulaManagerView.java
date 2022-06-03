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
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.NumeralFormulaManager;

public class NumeralFormulaManagerView<
        ParamFormulaType extends NumeralFormula, ResultFormulaType extends NumeralFormula>
    extends BaseManagerView implements NumeralFormulaManager<ParamFormulaType, ResultFormulaType> {

  protected final NumeralFormulaManager<ParamFormulaType, ResultFormulaType> manager;

  NumeralFormulaManagerView(
      FormulaWrappingHandler pWrappingHandler,
      NumeralFormulaManager<ParamFormulaType, ResultFormulaType> pManager) {
    super(pWrappingHandler);
    this.manager = checkNotNull(pManager);
  }

  @Override
  public ResultFormulaType negate(ParamFormulaType pNumber) {
    return manager.negate(pNumber);
  }

  @Override
  public ResultFormulaType add(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return manager.add(pNumber1, pNumber2);
  }

  @Override
  public ResultFormulaType sum(List<ParamFormulaType> operands) {
    return manager.sum(operands);
  }

  @Override
  public ResultFormulaType subtract(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return manager.subtract(pNumber1, pNumber2);
  }

  @Override
  public ResultFormulaType divide(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return manager.divide(pNumber1, pNumber2);
  }

  @Override
  public ResultFormulaType multiply(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return manager.multiply(pNumber1, pNumber2);
  }

  @Override
  public BooleanFormula equal(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return manager.equal(pNumber1, pNumber2);
  }

  @Override
  public BooleanFormula greaterThan(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return manager.greaterThan(pNumber1, pNumber2);
  }

  @Override
  public BooleanFormula greaterOrEquals(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return manager.greaterOrEquals(pNumber1, pNumber2);
  }

  @Override
  public BooleanFormula lessThan(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return manager.lessThan(pNumber1, pNumber2);
  }

  @Override
  public BooleanFormula lessOrEquals(ParamFormulaType pNumber1, ParamFormulaType pNumber2) {
    return manager.lessOrEquals(pNumber1, pNumber2);
  }

  @Override
  public BooleanFormula distinct(List<ParamFormulaType> pNumbers) {
    return manager.distinct(pNumbers);
  }

  @Override
  public ResultFormulaType makeNumber(long pI) {
    return manager.makeNumber(pI);
  }

  @Override
  public ResultFormulaType makeNumber(BigInteger pI) {
    return manager.makeNumber(pI);
  }

  @Override
  public ResultFormulaType makeNumber(String pI) {
    return manager.makeNumber(pI);
  }

  @Override
  public ResultFormulaType makeNumber(double pNumber) {
    return manager.makeNumber(pNumber);
  }

  @Override
  public ResultFormulaType makeNumber(BigDecimal pNumber) {
    return manager.makeNumber(pNumber);
  }

  @Override
  public ResultFormulaType makeNumber(Rational pRational) {
    return manager.makeNumber(pRational);
  }

  @Override
  public ResultFormulaType makeVariable(String pVar) {
    return manager.makeVariable(pVar);
  }

  public ResultFormulaType makeVariable(String pVar, int idx) {
    return manager.makeVariable(FormulaManagerView.makeName(pVar, idx));
  }

  @Override
  public FormulaType<ResultFormulaType> getFormulaType() {
    return manager.getFormulaType();
  }

  @Override
  public IntegerFormula floor(ParamFormulaType pFormula) {
    return manager.floor(pFormula);
  }
}
