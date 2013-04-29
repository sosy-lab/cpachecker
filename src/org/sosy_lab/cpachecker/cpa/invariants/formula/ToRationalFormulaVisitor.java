/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.Map;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormulaManager;

/**
 * Instances of this class are compound state invariants visitors used to
 * convert the visited invariants formulae into rational formulae. This visitor
 * always coexists with an instance of {@link ToBooleanFormulaVisitor}, which
 * should also be used to obtain an instance of this visitor.
 */
public class ToRationalFormulaVisitor implements ToFormulaVisitor<CompoundState, RationalFormula> {

  /**
   * The boolean formula manager used.
   */
  private final BooleanFormulaManager bfmgr;

  /**
   * The rational formula manager used.
   */
  private final RationalFormulaManager rfmgr;

  /**
   * The rational formula representing the value zero.
   */
  private final RationalFormula zero;

  /**
   * The rational formula representing the value one.
   */
  private final RationalFormula one;

  /**
   * The corresponding compound state invariants formula visitor used to
   * convert visited formulae into boolean formulae.
   */
  private final ToFormulaVisitor<CompoundState, BooleanFormula> toBooleanFormulaVisitor;

  /**
   * The formula evaluation visitor used to evaluate compound state invariants
   * formulae to compound states.
   */
  private final FormulaEvaluationVisitor<CompoundState> evaluationVisitor;

  /**
   * Creates a new visitor for converting compound state invariants formulae to
   * rational formulae by using the given formula manager,
   * {@link ToBooleanFormulaVisitor} and evaluation visitor.
   *
   * @param pFmgr the formula manager used.
   * @param pToBooleanFormulaVisitor the compound state invariants formula
   * visitor used to convert invariants formulae to boolean formulae.
   * @param pEvaluationVisitor the formula evaluation visitor used to evaluate
   * compound state invariants formulae to compound states.
   */
  ToRationalFormulaVisitor(FormulaManager pFmgr,
      ToFormulaVisitor<CompoundState, BooleanFormula> pToBooleanFormulaVisitor,
      FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    this.bfmgr = pFmgr.getBooleanFormulaManager();
    this.rfmgr = pFmgr.getRationalFormulaManager();
    this.zero = this.rfmgr.makeNumber(0);
    this.one = this.rfmgr.makeNumber(1);
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
  private @Nullable RationalFormula evaluate(InvariantsFormula<CompoundState> pFormula, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    CompoundState value = pFormula.accept(this.evaluationVisitor, pEnvironment);
    if (value.isSingleton()) {
      return this.rfmgr.makeNumber(value.getLowerBound().longValue());
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
  private @Nullable RationalFormula fromBooleanFormula(InvariantsFormula<CompoundState> pBooleanFormula, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
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
  private @Nullable RationalFormula fromBooleanFormula(@Nullable BooleanFormula pBooleanFormula) {
    if (pBooleanFormula == null) {
      return null;
    }
    return this.bfmgr.ifThenElse(pBooleanFormula, this.one, this.zero);
  }

  @Override
  public RationalFormula visit(Add<CompoundState> pAdd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    RationalFormula summand1 = pAdd.getSummand1().accept(this, pEnvironment);
    RationalFormula summand2 = pAdd.getSummand2().accept(this, pEnvironment);
    if (summand1 == null || summand2 == null) {
      return evaluate(pAdd, pEnvironment);
    }
    return this.rfmgr.add(summand1, summand2);
  }

  @Override
  public RationalFormula visit(BinaryAnd<CompoundState> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return evaluate(pAnd, pEnvironment);
  }

  @Override
  public RationalFormula visit(BinaryNot<CompoundState> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return evaluate(pNot, pEnvironment);
  }

  @Override
  public RationalFormula visit(BinaryOr<CompoundState> pOr, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return evaluate(pOr, pEnvironment);
  }

  @Override
  public RationalFormula visit(BinaryXor<CompoundState> pXor, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return evaluate(pXor, pEnvironment);
  }

  @Override
  public RationalFormula visit(Constant<CompoundState> pConstant, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return evaluate(pConstant, pEnvironment);
  }

  @Override
  public RationalFormula visit(Divide<CompoundState> pDivide, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    RationalFormula numerator = pDivide.getNumerator().accept(this, pEnvironment);
    RationalFormula denominator = pDivide.getDenominator().accept(this, pEnvironment);
    if (numerator == null || denominator == null) {
      return evaluate(pDivide, pEnvironment);
    }
    return this.rfmgr.divide(numerator, denominator);
  }

  @Override
  public RationalFormula visit(Equal<CompoundState> pEqual, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromBooleanFormula(pEqual, pEnvironment);
  }

  @Override
  public RationalFormula visit(LessThan<CompoundState> pLessThan, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromBooleanFormula(pLessThan, pEnvironment);
  }

  @Override
  public RationalFormula visit(LogicalAnd<CompoundState> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromBooleanFormula(pAnd, pEnvironment);
  }

  @Override
  public RationalFormula visit(LogicalNot<CompoundState> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromBooleanFormula(pNot, pEnvironment);
  }

  @Override
  public RationalFormula visit(Modulo<CompoundState> pModulo, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    RationalFormula numerator = pModulo.getNumerator().accept(this, pEnvironment);
    RationalFormula denominator = pModulo.getDenominator().accept(this, pEnvironment);
    if (numerator == null || denominator == null) {
      return evaluate(pModulo, pEnvironment);
    }
    return this.rfmgr.modulo(numerator, denominator);
  }

  @Override
  public RationalFormula visit(Multiply<CompoundState> pMultiply, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    RationalFormula factor1 = pMultiply.getFactor1().accept(this, pEnvironment);
    RationalFormula factor2 = pMultiply.getFactor2().accept(this, pEnvironment);
    if (factor1 == null || factor2 == null) {
      return evaluate(pMultiply, pEnvironment);
    }
    return this.rfmgr.multiply(factor1, factor2);
  }

  @Override
  public RationalFormula visit(Negate<CompoundState> pNegate, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    RationalFormula negated = pNegate.getNegated().accept(this, pEnvironment);
    if (negated == null) {
      return evaluate(pNegate, pEnvironment);
    }
    return this.rfmgr.negate(negated);
  }

  @Override
  public RationalFormula visit(ShiftLeft<CompoundState> pShiftLeft, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return evaluate(pShiftLeft, pEnvironment);
  }

  @Override
  public RationalFormula visit(ShiftRight<CompoundState> pShiftRight, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return evaluate(pShiftRight, pEnvironment);
  }

  @Override
  public RationalFormula visit(Union<CompoundState> pUnion, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return evaluate(pUnion, pEnvironment);
  }

  @Override
  public RationalFormula visit(Variable<CompoundState> pVariable, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return rfmgr.makeVariable(pVariable.getName());
  }

  @Override
  public RationalFormula getZero() {
    return zero;
  }

  @Override
  public RationalFormula getOne() {
    return one;
  }

  @Override
  public BooleanFormula lessThan(RationalFormula pOp1, RationalFormula pOp2) {
    return this.rfmgr.lessThan(pOp1, pOp2);
  }

  @Override
  public BooleanFormula equal(RationalFormula pOp1, RationalFormula pOp2) {
    return this.rfmgr.equal(pOp1, pOp2);
  }

  @Override
  public BooleanFormula greaterThan(RationalFormula pOp1, RationalFormula pOp2) {
    return this.rfmgr.greaterThan(pOp1, pOp2);
  }

  @Override
  public BooleanFormula lessOrEqual(RationalFormula pOp1, RationalFormula pOp2) {
    return this.rfmgr.lessOrEquals(pOp1, pOp2);
  }

  @Override
  public BooleanFormula greaterOrEqual(RationalFormula pOp1, RationalFormula pOp2) {
    return this.rfmgr.greaterOrEquals(pOp1, pOp2);
  }

}
