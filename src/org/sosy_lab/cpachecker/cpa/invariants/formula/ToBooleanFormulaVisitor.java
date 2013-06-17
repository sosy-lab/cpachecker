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
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/**
 * Instances of this class are compound state invariants visitors used to
 * convert the visited invariants formulae into boolean formulae. This visitor
 * always coexists with an instance of {@link ToFormulaVisitor} for value type
 * formulae.
 */
public class ToBooleanFormulaVisitor<ValueFormulaType> implements ToFormulaVisitor<CompoundState, BooleanFormula> {

  /**
   * The boolean formula manager used.
   */
  private final BooleanFormulaManager bfmgr;

  /**
   * The formula evaluation visitor used to evaluate compound state invariants
   * formulae to compound states.
   */
  private final FormulaEvaluationVisitor<CompoundState> evaluationVisitor;

  /**
   * The corresponding compound state invariants formula visitor used to
   * convert visited formulae into formulae of the value type.
   */
  private final ToFormulaVisitor<CompoundState, ValueFormulaType> toValueFormulaVisitor;

  public static ToFormulaVisitor<CompoundState, BooleanFormula> getVisitor(FormulaManagerView pFmgr,
        FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor,
        boolean useBitvectors) {

    final ToFormulaVisitorWrapper<CompoundState, BooleanFormula> wrapper = new ToFormulaVisitorWrapper<>();
    final ToBooleanFormulaVisitor<?> result;
    if (useBitvectors) {
      result = new ToBooleanFormulaVisitor<>(
          pFmgr,
          new ToBitvectorFormulaVisitor(pFmgr, wrapper, pEvaluationVisitor),
          pEvaluationVisitor);
      wrapper.setInner(result);
    } else {
      result = new ToBooleanFormulaVisitor<>(
          pFmgr,
          new ToRationalFormulaVisitor(pFmgr, wrapper, pEvaluationVisitor),
          pEvaluationVisitor);
      wrapper.setInner(result);
    }
    return wrapper.getWrapped();

  }

  /**
   * Creates a new visitor for converting compound state invariants formulae to
   * boolean formulae by using the given formula manager, value to formula
   * converter and evaluation visitor.
   *
   * @param pFmgr the formula manager used.
   * @param evaluationVisitor the evaluation visitor used to evaluate compound
   * state invariants formulae to compound states.
   */
  public ToBooleanFormulaVisitor(FormulaManagerView pFmgr,
      ToFormulaVisitor<CompoundState, ValueFormulaType> pToValueFormulaVisitor,
      FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    this.bfmgr = pFmgr.getBooleanFormulaManager();
    this.evaluationVisitor = pEvaluationVisitor;
    this.toValueFormulaVisitor = pToValueFormulaVisitor;
  }

  /**
   * Gets the corresponding visitor for converting compound state invariants
   * formulae to value type formulae.
   *
   * @return a visitor for converting compound state invariants formulae to
   * value type formulae.
   */
  public ToFormulaVisitor<CompoundState, ValueFormulaType> getValueVisitor() {
    return this.toValueFormulaVisitor;
  }

  /**
   * Interprets the given value invariants formula as a boolean formula or
   * <code>null</code> if it was not possible to interpret the formula as a
   * boolean formula.
   *
   * @param pValueFormula the value invariants formula to interpret.
   * @param pEnvironment the environment used to evaluate the given formula or
   * parts of it in if necessary.
   *
   * @return a boolean interpretation of the given value invariants formula
   * or <code>null</code> if it was not possible to interpret the formula as a
   * boolean formula.
   */
  private @Nullable BooleanFormula fromValueFormula(InvariantsFormula<CompoundState> pValueFormula, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    ValueFormulaType valueFormula = pValueFormula.accept(getValueVisitor(), pEnvironment);
    if (valueFormula == null) {
      return evaluateAsBoolean(pValueFormula, pEnvironment);
    }
    return fromValueFormula(valueFormula);
  }

  /**
   * Interprets the given value formula as a boolean formula or
   * <code>null</code> if the given formula was <code>null</code>.
   *
   * @param pValueFormula the value formula to interpret.
   * @return a boolean interpretation of the given value formula or
   * <code>null</code> if the given formula was <code>null</code>.
   */
  private @Nullable BooleanFormula fromValueFormula(@Nullable ValueFormulaType pValueFormula) {
    if (pValueFormula == null) {
      return null;
    }
    /*
     * Zero stands for false, everything else for true, so a value formula
     * r can be represented as a boolean formula as (r != zero) or
     * (!(r == zero)).
     */
    return this.bfmgr.not(getValueVisitor().equal(pValueFormula, getValueVisitor().getZero()));
  }

  /**
   * Tries to evaluate the given compound state invariants formula as a boolean
   * value, which means that the formula is evaluated using the evaluation
   * visitor and if it represents definitely either <code>true</code> or
   * <code>false</code>, the corresponding boolean formula is returned;
   * otherwise <code>null</code> is returned.
   *
   * @param pValueFormula the compound state invariants formula to evaluate.
   * @param pEnvironment the environment to evaluate the formula in.
   * @return a boolean formula representing the evaluated formula or
   * <code>null</code> if the evaluated formula does not clearly represent
   * either <code>true</code> or <code>false</code>.
   */
  private BooleanFormula evaluateAsBoolean(InvariantsFormula<CompoundState> pValueFormula, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    CompoundState value = pValueFormula.accept(this.evaluationVisitor, pEnvironment);
    if (value.isDefinitelyFalse()) {
      return this.bfmgr.makeBoolean(false);
    }
    if (value.isDefinitelyTrue()) {
      return this.bfmgr.makeBoolean(true);
    }
    return null;
  }

  @Override
  public BooleanFormula visit(Add<CompoundState> pAdd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromValueFormula(pAdd, pEnvironment);
  }

  @Override
  public BooleanFormula visit(BinaryAnd<CompoundState> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromValueFormula(pAnd, pEnvironment);
  }

  @Override
  public BooleanFormula visit(BinaryNot<CompoundState> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromValueFormula(pNot, pEnvironment);
  }

  @Override
  public BooleanFormula visit(BinaryOr<CompoundState> pOr, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromValueFormula(pOr, pEnvironment);
  }

  @Override
  public BooleanFormula visit(BinaryXor<CompoundState> pXor, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromValueFormula(pXor, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Constant<CompoundState> pConstant, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromValueFormula(pConstant, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Divide<CompoundState> pDivide, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromValueFormula(pDivide, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Equal<CompoundState> pEqual, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    ValueFormulaType operand1 = pEqual.getOperand1().accept(getValueVisitor(), pEnvironment);
    ValueFormulaType operand2 = pEqual.getOperand2().accept(getValueVisitor(), pEnvironment);
    if (operand1 == null && operand2 == null) {
      return evaluateAsBoolean(pEqual, pEnvironment);
    }
    if (operand1 == null || operand2 == null) {
      final ValueFormulaType left;
      final InvariantsFormula<CompoundState> right;
      if (operand1 != null) {
        left = operand1;
        right = pEqual.getOperand2();
      } else {
        left = operand2;
        right = pEqual.getOperand1();
      }
      CompoundState rightValue = right.accept(evaluationVisitor, pEnvironment);
      if (rightValue.isBottom()) {
        return this.bfmgr.makeBoolean(false);
      }
      BooleanFormula bf = this.bfmgr.makeBoolean(true);
      for (SimpleInterval interval : rightValue.getIntervals()) {
        if (interval.isSingleton()) {
          ValueFormulaType value = getValueFormula(interval.getLowerBound().longValue(), pEnvironment);
          bf = this.bfmgr.and(bf, getValueVisitor().equal(left, value));
        } else {
          if (interval.hasLowerBound()) {
            ValueFormulaType lb = getValueFormula(interval.getLowerBound().longValue(), pEnvironment);
            bf = this.bfmgr.and(bf, getValueVisitor().greaterOrEqual(left, lb));
          }
          if (interval.hasUpperBound()) {
            ValueFormulaType ub = getValueFormula(interval.getUpperBound().longValue(), pEnvironment);
            bf = this.bfmgr.and(bf, getValueVisitor().lessOrEqual(left, ub));
          }
        }
      }
      return bf;
    }
    return getValueVisitor().equal(operand1, operand2);
  }

  private ValueFormulaType getValueFormula(long pValue, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.singleton(pValue)).accept(getValueVisitor(), pEnvironment);
  }

  @Override
  public BooleanFormula visit(LessThan<CompoundState> pLessThan, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    ValueFormulaType operand1 = pLessThan.getOperand1().accept(getValueVisitor(), pEnvironment);
    ValueFormulaType operand2 = pLessThan.getOperand2().accept(getValueVisitor(), pEnvironment);
    if (operand1 == null && operand2 == null) {
      return evaluateAsBoolean(pLessThan, pEnvironment);
    }
    if (operand1 == null || operand2 == null) {
      final ValueFormulaType left;
      final InvariantsFormula<CompoundState> right;
      final boolean lessThan;
      if (operand1 != null) {
        left = operand1;
        right = pLessThan.getOperand2();
        lessThan = true;
      } else {
        left = operand2;
        right = pLessThan.getOperand1();
        lessThan = false;
      }
      CompoundState rightValue = right.accept(evaluationVisitor, pEnvironment);
      if (rightValue.isBottom()) {
        return this.bfmgr.makeBoolean(false);
      }
      if (lessThan) {
        if (rightValue.hasUpperBound()) {
          return getValueVisitor().lessThan(left, getValueFormula(rightValue.getUpperBound().longValue(), pEnvironment));
        }
      } else {
        if (rightValue.hasLowerBound()) {
          return getValueVisitor().greaterThan(left, getValueFormula(rightValue.getLowerBound().longValue(), pEnvironment));
        }
      }
      return null;
    }
    return getValueVisitor().lessThan(operand1, operand2);
  }

  @Override
  public BooleanFormula visit(LogicalAnd<CompoundState> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    BooleanFormula operand1 = pAnd.getOperand1().accept(this, pEnvironment);
    BooleanFormula operand2 = pAnd.getOperand2().accept(this, pEnvironment);
    if (operand1 == null || operand2 == null) {
      return evaluateAsBoolean(pAnd, pEnvironment);
    }
    return this.bfmgr.and(operand1, operand2);
  }

  @Override
  public BooleanFormula visit(LogicalNot<CompoundState> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    BooleanFormula operand = pNot.getNegated().accept(this, pEnvironment);
    if (operand == null) {
      return evaluateAsBoolean(pNot, pEnvironment);
    }
    return this.bfmgr.not(operand);
  }

  @Override
  public BooleanFormula visit(Modulo<CompoundState> pModulo, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromValueFormula(pModulo, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Multiply<CompoundState> pMultiply, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromValueFormula(pMultiply, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Negate<CompoundState> pNegate, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromValueFormula(pNegate, pEnvironment);
  }

  @Override
  public BooleanFormula visit(ShiftLeft<CompoundState> pShiftLeft, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromValueFormula(pShiftLeft, pEnvironment);
  }

  @Override
  public BooleanFormula visit(ShiftRight<CompoundState> pShiftRight, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromValueFormula(pShiftRight, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Union<CompoundState> pUnion, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromValueFormula(pUnion, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Variable<CompoundState> pVariable, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return this.bfmgr.makeVariable(pVariable.getName());
  }

  @Override
  public BooleanFormula getZero() {
    return this.bfmgr.makeBoolean(false);
  }

  @Override
  public BooleanFormula getOne() {
    return this.bfmgr.makeBoolean(true);
  }

  @Override
  public BooleanFormula lessThan(BooleanFormula pOp1, BooleanFormula pOp2) {
    return this.bfmgr.and(this.bfmgr.not(pOp1), pOp2);
  }

  @Override
  public BooleanFormula equal(BooleanFormula pOp1, BooleanFormula pOp2) {
    return this.bfmgr.equivalence(pOp1, pOp2);
  }

  @Override
  public BooleanFormula greaterThan(BooleanFormula pOp1, BooleanFormula pOp2) {
    return this.bfmgr.and(pOp1, this.bfmgr.not(pOp2));
  }

  @Override
  public BooleanFormula lessOrEqual(BooleanFormula pOp1, BooleanFormula pOp2) {
    return this.bfmgr.not(greaterThan(pOp1, pOp2));
  }

  @Override
  public BooleanFormula greaterOrEqual(BooleanFormula pOp1, BooleanFormula pOp2) {
    return this.bfmgr.not(lessThan(pOp1, pOp2));
  }

}
