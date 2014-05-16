/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.invariants.formula;

import java.math.BigInteger;
import java.util.Map;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/**
 * Instances of this class are compound state invariants visitors used to
 * convert the visited invariants formulae into rational formulae. This visitor
 * always coexists with an instance of {@link ToBooleanFormulaVisitor}, which
 * should also be used to obtain an instance of this visitor.
 */
public class ToNumeralFormulaVisitor<T extends NumeralFormula> implements ToFormulaVisitor<CompoundInterval, T> {

  /**
   * The boolean formula manager used.
   */
  private final BooleanFormulaManager bfmgr;

  /**
   * The rational formula manager used.
   */
  private final NumeralFormulaManager<? super T, ? extends T> nfmgr;

  /**
   * The rational formula representing the value zero.
   */
  private final T zero;

  /**
   * The rational formula representing the value one.
   */
  private final T one;

  /**
   * The corresponding compound state invariants formula visitor used to
   * convert visited formulae into boolean formulae.
   */
  private final ToFormulaVisitor<CompoundInterval, BooleanFormula> toBooleanFormulaVisitor;

  /**
   * The formula evaluation visitor used to evaluate compound state invariants
   * formulae to compound states.
   */
  private final FormulaEvaluationVisitor<CompoundInterval> evaluationVisitor;

  /**
   * Creates a new visitor for converting compound state invariants formulae to
   * rational formulae by using the given formula manager,
   * {@link ToBooleanFormulaVisitor} and evaluation visitor.
   *
   * @param pFmgr the formula manager used.
   * @param pNumeralFormualManager the formula manager for numeric types
   * @param pToBooleanFormulaVisitor the compound state invariants formula
   * visitor used to convert invariants formulae to boolean formulae.
   * @param pEvaluationVisitor the formula evaluation visitor used to evaluate
   * compound state invariants formulae to compound states.
   */
  ToNumeralFormulaVisitor(FormulaManagerView pFmgr,
      NumeralFormulaManager<? super T, ? extends T> pNumeralFormualManager,
      ToFormulaVisitor<CompoundInterval, BooleanFormula> pToBooleanFormulaVisitor,
      FormulaEvaluationVisitor<CompoundInterval> pEvaluationVisitor) {
    this.bfmgr = pFmgr.getBooleanFormulaManager();
    this.nfmgr = pNumeralFormualManager;
    this.zero = this.nfmgr.makeNumber(0);
    this.one = this.nfmgr.makeNumber(1);
    this.toBooleanFormulaVisitor = pToBooleanFormulaVisitor;
    this.evaluationVisitor = pEvaluationVisitor;
  }

  /**
   * Evaluates the given compound state invariants formula and tries to convert
   * the resulting value into a rational formula.
   *
   * @param pFormula the formula to evaluate.
   * @param pEnvironment the environment to evaluate the formula in.
   *
   * @return a rational formula representing the evaluation of the given
   * formula or <code>null</code> if the evaluation of the given formula could
   * not be represented as a rational formula.
   */
  private @Nullable
  T evaluate(InvariantsFormula<CompoundInterval> pFormula, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    CompoundInterval value = pFormula.accept(this.evaluationVisitor, pEnvironment);
    if (value.isSingleton()) {
      return this.nfmgr.makeNumber(value.getLowerBound());
    }
    return null;
  }

  /**
   * Interprets the given compound state invariants formula as a boolean
   * formula and then reinterprets the boolean formula as a rational formula.
   *
   * @param pBooleanFormula the compound state invariants formula to interpret
   * as a boolean formula.
   * @param pEnvironment the environment to perform formula evaluations in if
   * they are required.
   *
   * @return a rational formula representing the rational re-interpretation of
   * the boolean formula resulting from interpreting the given formula as a
   * boolean invariants formula or <code>null</code> if any of those
   * interpretations fail.
   */
  private @Nullable
  T fromBooleanFormula(InvariantsFormula<CompoundInterval> pBooleanFormula, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromBooleanFormula(pBooleanFormula.accept(this.toBooleanFormulaVisitor, pEnvironment));
  }

  /**
   * Interprets the given boolean formula as a rational formula by defining
   * that <code>true</code> is <code>1</code> and <code>false</code> is
   * <code>0</code>, so for any given non-<code>null</code> boolean formula
   * <code>b</code> the result is the rational formula equivalent of the
   * expression <code>b ? 1 : 0</code>.
   *
   * @param pBooleanFormula
   *
   * @return a rational formula representing the rational interpretation of the
   * given boolean formula or <code>null</code> if the given boolean formula
   * is <code>null</code>.
   */
  private @Nullable
  T fromBooleanFormula(@Nullable BooleanFormula pBooleanFormula) {
    if (pBooleanFormula == null) {
      return null;
    }
    return this.bfmgr.ifThenElse(pBooleanFormula, this.one, this.zero);
  }

  @Override
  public T visit(Add<CompoundInterval> pAdd, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    T summand1 = pAdd.getSummand1().accept(this, pEnvironment);
    T summand2 = pAdd.getSummand2().accept(this, pEnvironment);
    if (summand1 == null || summand2 == null) {
      return evaluate(pAdd, pEnvironment);
    }
    return this.nfmgr.add(summand1, summand2);
  }

  @Override
  public T visit(BinaryAnd<CompoundInterval> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pAnd, pEnvironment);
  }

  @Override
  public T visit(BinaryNot<CompoundInterval> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pNot, pEnvironment);
  }

  @Override
  public T visit(BinaryOr<CompoundInterval> pOr, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pOr, pEnvironment);
  }

  @Override
  public T visit(BinaryXor<CompoundInterval> pXor, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pXor, pEnvironment);
  }

  @Override
  public T visit(Constant<CompoundInterval> pConstant, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pConstant, pEnvironment);
  }

  @Override
  public T visit(Divide<CompoundInterval> pDivide, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    T numerator = pDivide.getNumerator().accept(this, pEnvironment);
    T denominator = pDivide.getDenominator().accept(this, pEnvironment);
    if (numerator == null || denominator == null) {
      return evaluate(pDivide, pEnvironment);
    }
    return this.nfmgr.divide(numerator, denominator);
  }

  @Override
  public T visit(Equal<CompoundInterval> pEqual, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromBooleanFormula(pEqual, pEnvironment);
  }

  @Override
  public T visit(LessThan<CompoundInterval> pLessThan, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromBooleanFormula(pLessThan, pEnvironment);
  }

  @Override
  public T visit(LogicalAnd<CompoundInterval> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromBooleanFormula(pAnd, pEnvironment);
  }

  @Override
  public T visit(LogicalNot<CompoundInterval> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return fromBooleanFormula(pNot, pEnvironment);
  }

  @Override
  public T visit(Modulo<CompoundInterval> pModulo, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    T numerator = pModulo.getNumerator().accept(this, pEnvironment);
    T denominator = pModulo.getDenominator().accept(this, pEnvironment);
    if (numerator == null || denominator == null) {
      return evaluate(pModulo, pEnvironment);
    }
    return this.nfmgr.modulo(numerator, denominator);
  }

  @Override
  public T visit(Multiply<CompoundInterval> pMultiply, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    T factor1 = pMultiply.getFactor1().accept(this, pEnvironment);
    T factor2 = pMultiply.getFactor2().accept(this, pEnvironment);
    if (factor1 == null || factor2 == null) {
      return evaluate(pMultiply, pEnvironment);
    }
    return this.nfmgr.multiply(factor1, factor2);
  }

  @Override
  public T visit(ShiftLeft<CompoundInterval> pShiftLeft, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pShiftLeft, pEnvironment);
  }

  @Override
  public T visit(ShiftRight<CompoundInterval> pShiftRight, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pShiftRight, pEnvironment);
  }

  @Override
  public T visit(Union<CompoundInterval> pUnion, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return evaluate(pUnion, pEnvironment);
  }

  @Override
  public T visit(Variable<CompoundInterval> pVariable, Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return nfmgr.makeVariable(pVariable.getName());
  }

  @Override
  public BooleanFormula lessThan(T pOp1, T pOp2) {
    return this.nfmgr.lessThan(pOp1, pOp2);
  }

  @Override
  public BooleanFormula equal(T pOp1, T pOp2) {
    return this.nfmgr.equal(pOp1, pOp2);
  }

  @Override
  public BooleanFormula greaterThan(T pOp1, T pOp2) {
    return this.nfmgr.greaterThan(pOp1, pOp2);
  }

  @Override
  public BooleanFormula lessOrEqual(T pOp1, T pOp2) {
    return this.nfmgr.lessOrEquals(pOp1, pOp2);
  }

  @Override
  public BooleanFormula greaterOrEqual(T pOp1, T pOp2) {
    return this.nfmgr.greaterOrEquals(pOp1, pOp2);
  }

  @Override
  public BooleanFormula asBoolean(T pOp1) {
    return this.bfmgr.not(this.nfmgr.equal(pOp1, this.nfmgr.makeNumber(BigInteger.ZERO)));
  }

}