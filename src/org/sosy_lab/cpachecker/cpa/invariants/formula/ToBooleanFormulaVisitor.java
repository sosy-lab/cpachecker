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
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormulaManager;

/**
 * Instances of this class are compound state invariants visitors used to
 * convert the visited invariants formulae into boolean formulae. This visitor
 * always coexists with an instance of {@link ToRationalFormulaVisitor}.
 */
public class ToBooleanFormulaVisitor implements ParameterizedInvariantsFormulaVisitor<CompoundState, Map<? extends String, ? extends InvariantsFormula<CompoundState>>, BooleanFormula> {

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
   * The corresponding compound state invariants formula visitor used to
   * convert visited formulae into rational formulae.
   */
  private final ToRationalFormulaVisitor toRationalFormulaVisitor;

  /**
   * The formula evaluation visitor used to evaluate compound state invariants
   * formulae to compound states.
   */
  private final FormulaEvaluationVisitor<CompoundState> evaluationVisitor;

  private boolean isCurrentFormulaNegated = false;

  /**
   * Creates a new visitor for converting compound state invariants formulae to
   * boolean formulae by using the given formula manager and evaluation
   * visitor.
   *
   * @param pFmgr the formula manager used.
   * @param evaluationVisitor the evaluation visitor used to evaluate compound
   * state invariants formulae to compound states.
   */
  public ToBooleanFormulaVisitor(FormulaManager pFmgr,
      FormulaEvaluationVisitor<CompoundState> evaluationVisitor) {
    this.bfmgr = pFmgr.getBooleanFormulaManager();
    this.rfmgr = pFmgr.getRationalFormulaManager();
    this.zero = this.rfmgr.makeNumber(0);
    this.evaluationVisitor = evaluationVisitor;
    this.toRationalFormulaVisitor = new ToRationalFormulaVisitor(pFmgr, this, evaluationVisitor);
  }

  /**
   * Gets the corresponding visitor for converting compound state invariants
   * formulae to rational formulae.
   *
   * @return a visitor for converting compound state invariants formulae to
   * rational formulae.
   */
  public ToRationalFormulaVisitor getToRationalFormulaVisitor() {
    return this.toRationalFormulaVisitor;
  }

  /**
   * Interprets the given rational invariants formula as a boolean formula or
   * <code>null</code> if it was not possible to interpret the formula as a
   * boolean formula.
   *
   * @param pRationalFormula the rational invariants formula to interpret.
   * @param pEnvironment the environment used to evaluate the given formula or
   * parts of it in if necessary.
   *
   * @return a boolean interpretation of the given rational invariants formula
   * or <code>null</code> if it was not possible to interpret the formula as a
   * boolean formula.
   */
  private @Nullable BooleanFormula fromRationalFormula(InvariantsFormula<CompoundState> pRationalFormula, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    RationalFormula rationalFormula = pRationalFormula.accept(this.toRationalFormulaVisitor, pEnvironment);
    if (rationalFormula == null) {
      return evaluateAsBoolean(pRationalFormula, pEnvironment);
    }
    return fromRationalFormula(rationalFormula);
  }

  /**
   * Interprets the given rational formula as a boolean formula or
   * <code>null</code> if the given formula was <code>null</code>.
   *
   * @param pRationalFormula the rational formula to interpret.
   * @return a boolean interpretation of the given rational formula or
   * <code>null</code> if the given formula was <code>null</code>.
   */
  private @Nullable BooleanFormula fromRationalFormula(@Nullable RationalFormula pRationalFormula) {
    if (pRationalFormula == null) {
      return null;
    }
    /*
     * Zero stands for false, everything else for true, so a rational formula
     * r can be represented as a boolean formula as (r != zero) or
     * (!(r == zero)).
     */
    return this.bfmgr.not(this.rfmgr.equal(pRationalFormula, this.zero));
  }

  /**
   * Tries to evaluate the given compound state invariants formula as a boolean
   * value, which means that the formula is evaluated using the evaluation
   * visitor and if it represents definitely either <code>true</code> or
   * <code>false</code>, the corresponding boolean formula is returned;
   * otherwise <code>null</code> is returned.
   *
   * @param pRationalFormula the compound state invariants formula to evaluate.
   * @param pEnvironment the environment to evaluate the formula in.
   * @return a boolean formula representing the evaluated formula or
   * <code>null</code> if the evaluated formula does not clearly represent
   * either <code>true</code> or <code>false</code>.
   */
  private BooleanFormula evaluateAsBoolean(InvariantsFormula<CompoundState> pRationalFormula, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    CompoundState value = pRationalFormula.accept(this.evaluationVisitor, pEnvironment);
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
    return fromRationalFormula(pAdd, pEnvironment);
  }

  @Override
  public BooleanFormula visit(BinaryAnd<CompoundState> pAnd, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromRationalFormula(pAnd, pEnvironment);
  }

  @Override
  public BooleanFormula visit(BinaryNot<CompoundState> pNot, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromRationalFormula(pNot, pEnvironment);
  }

  @Override
  public BooleanFormula visit(BinaryOr<CompoundState> pOr, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromRationalFormula(pOr, pEnvironment);
  }

  @Override
  public BooleanFormula visit(BinaryXor<CompoundState> pXor, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromRationalFormula(pXor, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Constant<CompoundState> pConstant, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromRationalFormula(pConstant, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Divide<CompoundState> pDivide, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromRationalFormula(pDivide, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Equal<CompoundState> pEqual, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    RationalFormula operand1 = pEqual.getOperand1().accept(this.toRationalFormulaVisitor, pEnvironment);
    RationalFormula operand2 = pEqual.getOperand2().accept(this.toRationalFormulaVisitor, pEnvironment);
    if (operand1 == null && operand2 == null) {
      return evaluateAsBoolean(pEqual, pEnvironment);
    }
    if (operand1 == null || operand2 == null) {
      final RationalFormula left;
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
          RationalFormula value = this.rfmgr.makeNumber(interval.getLowerBound().longValue());
          bf = this.bfmgr.and(bf, this.rfmgr.equal(left, value));
        } else {
          if (interval.hasLowerBound()) {
            RationalFormula lb = this.rfmgr.makeNumber(interval.getLowerBound().longValue());
            bf = this.bfmgr.and(bf, this.rfmgr.greaterOrEquals(left, lb));
          }
          if (interval.hasUpperBound()) {
            RationalFormula ub = this.rfmgr.makeNumber(interval.getUpperBound().longValue());
            bf = this.bfmgr.and(bf, this.rfmgr.greaterOrEquals(left, ub));
          }
        }
      }
      return bf;
    }
    return this.rfmgr.equal(operand1, operand2);
  }

  @Override
  public BooleanFormula visit(LessThan<CompoundState> pLessThan, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    /*
     * In integer arithmetics, x < 1 means x <= 0. In rational arithmetics,
     * however, x < 1 would allow 0.5 as a possible value for x which is
     * clearly not less or equal to 0. Compound states work on integer values,
     * but the resulting rational formulae work on rational values, of course.
     * Therefore op1 < op2 must be encoded as op1 <= (op2 - 1) by this visitor,
     * so the information provided by the integer arithmetics the formula was
     * created in is not lost in this conversion.
     * However, if this formula occurs in a negated formula, every "x < y" is
     * actually a "y <= x" already when evaluated, so in that case, the operator
     * should be translated in the straightforward way.
     */
    RationalFormula operand1 = pLessThan.getOperand1().accept(this.toRationalFormulaVisitor, pEnvironment);
    RationalFormula operand2 = pLessThan.getOperand2().accept(this.toRationalFormulaVisitor, pEnvironment);
    if (operand1 == null && operand2 == null) {
      return evaluateAsBoolean(pLessThan, pEnvironment);
    }
    if (operand1 == null || operand2 == null) {
      final RationalFormula left;
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
          if (this.isCurrentFormulaNegated) {
            return this.rfmgr.lessThan(left, this.rfmgr.makeNumber(rightValue.getUpperBound().longValue()));
          }
          return this.rfmgr.lessOrEquals(left, this.rfmgr.makeNumber(rightValue.getUpperBound().longValue() - 1));
        }
      } else {
        if (rightValue.hasLowerBound()) {
          if (this.isCurrentFormulaNegated) {
            return this.rfmgr.greaterThan(left, this.rfmgr.makeNumber(rightValue.getLowerBound().longValue()));
          }
          return this.rfmgr.greaterOrEquals(left, this.rfmgr.makeNumber(rightValue.getLowerBound().longValue() + 1));
        }
      }
      return null;
    }
    if (this.isCurrentFormulaNegated) {
      return this.rfmgr.lessThan(operand1, operand2);
    }
    return this.rfmgr.lessOrEquals(operand1, rfmgr.subtract(operand2, rfmgr.makeNumber(1)));
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
    this.isCurrentFormulaNegated = !this.isCurrentFormulaNegated;
    BooleanFormula operand = pNot.getNegated().accept(this, pEnvironment);
    this.isCurrentFormulaNegated = !this.isCurrentFormulaNegated;
    if (operand == null) {
      return evaluateAsBoolean(pNot, pEnvironment);
    }
    return this.bfmgr.not(operand);
  }

  @Override
  public BooleanFormula visit(Modulo<CompoundState> pModulo, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromRationalFormula(pModulo, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Multiply<CompoundState> pMultiply, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromRationalFormula(pMultiply, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Negate<CompoundState> pNegate, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromRationalFormula(pNegate, pEnvironment);
  }

  @Override
  public BooleanFormula visit(ShiftLeft<CompoundState> pShiftLeft, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromRationalFormula(pShiftLeft, pEnvironment);
  }

  @Override
  public BooleanFormula visit(ShiftRight<CompoundState> pShiftRight, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromRationalFormula(pShiftRight, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Union<CompoundState> pUnion, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return fromRationalFormula(pUnion, pEnvironment);
  }

  @Override
  public BooleanFormula visit(Variable<CompoundState> pVariable, Map<? extends String, ? extends InvariantsFormula<CompoundState>> pEnvironment) {
    return this.bfmgr.makeVariable(pVariable.getName());
  }

}
