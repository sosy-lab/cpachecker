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

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundState;

/**
 * The singleton instance of this class is a compound state invariants formula
 * visitor used to partially evaluate compound state invariants formulae to
 * eliminate complex expressions consisting only of constants.
 */
public enum PartialEvaluator implements ParameterizedInvariantsFormulaVisitor<CompoundState, FormulaEvaluationVisitor<CompoundState>, InvariantsFormula<CompoundState>> {

  /**
   * The partial evaluator singleton instance.
   */
  INSTANCE;

  /**
   * An invariants formula representing the top state.
   */
  private static final InvariantsFormula<CompoundState> TOP =
      InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.top());

  /**
   * An invariants formula representing the bottom state.
   */
  private static final InvariantsFormula<CompoundState> BOTTOM =
      InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.bottom());

  /**
   * A visitor for compound state invariants formulae used to determine whether
   * or not the visited formula is a genuine boolean formula.
   */
  private static final IsBooleanFormulaVisitor<CompoundState> IS_BOOLEAN_FORMULA_VISITOR =
      new IsBooleanFormulaVisitor<>();

  /**
   * A map representing an empty environment; since only constants are
   * evaluated, no real environment is required.
   */
  private static final Map<String, InvariantsFormula<CompoundState>> EMPTY_ENVIRONMENT =
      Collections.emptyMap();

  @Override
  public InvariantsFormula<CompoundState> visit(Add<CompoundState> pAdd, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> summand1 = pAdd.getSummand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> summand2 = pAdd.getSummand2().accept(this, pEvaluationVisitor);
    // If both summands are constants, calculate a new constant
    if (summand1 instanceof Constant<?> && summand2 instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pAdd.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    // If one of the summands is constant zero, return the other summand
    // If one of the summands is top or bottom, return it
    Constant<CompoundState> c = null;
    InvariantsFormula<CompoundState> other = null;
    if (summand1 instanceof Constant<?>) {
      c = (Constant<CompoundState>) summand1;
      other = summand2;
    } else if (summand2 instanceof Constant<?>) {
      c = (Constant<CompoundState>) summand2;
      other = summand1;
    }
    if (c != null && other != null) {
      CompoundState value = c.getValue();
      if (value.isSingleton() && value.getValue().equals(BigInteger.ZERO)) {
        return other;
      }
      if (value.isTop() || value.isBottom()) {
        return c;
      }
      PushSummandVisitor<CompoundState> psv = new PushSummandVisitor<>(pEvaluationVisitor);
      other = other.accept(psv, value);
      if (psv.isSummandConsumed()) {
        other = other.accept(this, pEvaluationVisitor);
      }
      return other;
    }
    // If the summands did not change, return the original statement
    if (summand1 == pAdd.getSummand1() && summand2 == pAdd.getSummand2()) {
      return pAdd;
    }
    // If one or both summands change but cannot be further evaluated,
    // return a new addition formula for both
    return InvariantsFormulaManager.INSTANCE.add(summand1, summand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(BinaryAnd<CompoundState> pAnd, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand1 = pAnd.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> operand2 = pAnd.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pAnd.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    if (operand1 == pAnd.getOperand1() && operand2 == pAnd.getOperand2()) {
      return pAnd;
    }
    return InvariantsFormulaManager.INSTANCE.binaryAnd(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(BinaryNot<CompoundState> pNot, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand = pNot.getFlipped().accept(this, pEvaluationVisitor);
    if (operand instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pNot.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    if (operand == pNot.getFlipped()) {
      return pNot;
    }
    return InvariantsFormulaManager.INSTANCE.binaryNot(operand);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(BinaryOr<CompoundState> pOr, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand1 = pOr.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> operand2 = pOr.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pOr.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    if (operand1 == pOr.getOperand1() && operand2 == pOr.getOperand2()) {
      return pOr;
    }
    return InvariantsFormulaManager.INSTANCE.binaryOr(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(BinaryXor<CompoundState> pXor, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand1 = pXor.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> operand2 = pXor.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pXor.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    if (operand1 == pXor.getOperand1() && operand2 == pXor.getOperand2()) {
      return pXor;
    }
    return InvariantsFormulaManager.INSTANCE.binaryXor(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Constant<CompoundState> pConstant, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    return pConstant;
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Divide<CompoundState> pDivide, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> numerator = pDivide.getNumerator().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> denominator = pDivide.getDenominator().accept(this, pEvaluationVisitor);
    if (numerator instanceof Constant<?> && denominator instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pDivide.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    // Division by 1 yields the numerator, by -1 the negated numerator
    if (denominator instanceof Constant<?>) {
      Constant<CompoundState> c = (Constant<CompoundState>) denominator;
      CompoundState value = c.getValue();
      if (value.isSingleton()) {
        if (value.getValue().equals(BigInteger.ONE)) {
          return numerator;
        } else if (value.getValue().equals(BigInteger.valueOf(-1))) {
          return InvariantsFormulaManager.INSTANCE.negate(numerator);
        }
      }
    }
    if (numerator == pDivide.getNumerator() && denominator == pDivide.getDenominator()) {
      return pDivide;
    }
    return InvariantsFormulaManager.INSTANCE.divide(numerator, denominator);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Equal<CompoundState> pEqual, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand1 = pEqual.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> operand2 = pEqual.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pEqual.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    /*
     * If a boolean formula is operand in an equation with true, the boolean
     * formula itself can be returned, because the "== true" has does not
     * change the value of the expression. Almost the same goes for equations
     * with false, only that the other operand has to be negated then.
     */
    CompoundState c = null;
    InvariantsFormula<CompoundState> other = null;
    if (operand1 instanceof Constant<?>
      && (pEqual.getOperand2().accept(IS_BOOLEAN_FORMULA_VISITOR)
          || operand2.accept(IS_BOOLEAN_FORMULA_VISITOR))) {
      c = operand1.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT);
      other = operand2;
    } else if (operand2 instanceof Constant<?>
      && (pEqual.getOperand1().accept(IS_BOOLEAN_FORMULA_VISITOR)
          || operand1.accept(IS_BOOLEAN_FORMULA_VISITOR))) {
      c = operand2.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT);
      other = operand1;
    }
    if (c != null && other != null) {
      if (c.isDefinitelyTrue()) {
        return other;
      }
      if (c.isDefinitelyFalse()) {
        return InvariantsFormulaManager.INSTANCE.logicalNot(other).accept(this, pEvaluationVisitor);
      }
    }
    if (operand1 == pEqual.getOperand1() && operand2 == pEqual.getOperand2()) {
      return pEqual;
    }
    return InvariantsFormulaManager.INSTANCE.equal(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(LessThan<CompoundState> pLessThan, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand1 = pLessThan.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> operand2 = pLessThan.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pLessThan.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    if (operand1 == pLessThan.getOperand1() && operand2 == pLessThan.getOperand2()) {
      return pLessThan;
    }
    return InvariantsFormulaManager.INSTANCE.lessThan(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(LogicalAnd<CompoundState> pAnd, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand1 = pAnd.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> operand2 = pAnd.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pAnd.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    // If one of the operands is false, return false;
    // if one of the operands is true, return the other one
    InvariantsFormula<CompoundState> constant = null;
    InvariantsFormula<CompoundState> other = null;
    if (operand1 instanceof Constant<?>) {
      constant = operand1;
      other = operand2;
    } else if (operand2 instanceof Constant<?>) {
      constant = operand2;
      other = operand1;
    }
    if (constant != null && other != null) {
      CompoundState constantValue = ((Constant<CompoundState>) constant).getValue();
      if (constantValue.isDefinitelyFalse()) {
        return constant;
      } else if (constantValue.isDefinitelyTrue()) {
        return other;
      }
    }
    if (operand1 == pAnd.getOperand1() && operand2 == pAnd.getOperand2()) {
      return pAnd;
    }
    return InvariantsFormulaManager.INSTANCE.logicalAnd(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(LogicalNot<CompoundState> pNot, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand = pNot.getNegated().accept(this, pEvaluationVisitor);
    if (operand instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pNot.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    // The negation of a negation yields the inner operand
    if (operand instanceof LogicalNot<?>) {
      return ((LogicalNot<CompoundState>) operand).getNegated();
    }
    if (operand == pNot.getNegated()) {
      return pNot;
    }
    return InvariantsFormulaManager.INSTANCE.logicalNot(operand);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Modulo<CompoundState> pModulo, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> numerator = pModulo.getNumerator().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> denominator = pModulo.getDenominator().accept(this, pEvaluationVisitor);
    if (numerator instanceof Constant<?> && denominator instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pModulo.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    // If the denominator is 1 or -1, modulo must yield 0
    if (denominator instanceof Constant<?>) {
      Constant<CompoundState> c = (Constant<CompoundState>) denominator;
      CompoundState value = c.getValue();
      if (value.isSingleton() && (value.getValue().equals(BigInteger.ONE)
          || value.getValue().equals(BigInteger.valueOf(-1)))) {
        return InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.singleton(1));
      }
    }
    if (numerator == pModulo.getNumerator() && denominator == pModulo.getDenominator()) {
      return pModulo;
    }
    return InvariantsFormulaManager.INSTANCE.modulo(numerator, denominator);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Multiply<CompoundState> pMultiply, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> factor1 = pMultiply.getFactor1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> factor2 = pMultiply.getFactor2().accept(this, pEvaluationVisitor);
    if (factor1 instanceof Constant<?> && factor2 instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pMultiply.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    // Multiplication by 1 yields the other factor,
    // by -1 the negated other factor, by 0 it yields 0.
    Constant<CompoundState> c = null;
    InvariantsFormula<CompoundState> otherFactor = null;
    if (factor1 instanceof Constant<?>) {
      c = (Constant<CompoundState>) factor1;
      otherFactor = factor2;
    } else if (factor2 instanceof Constant<?>) {
      c = (Constant<CompoundState>) factor2;
      otherFactor = factor1;
    }
    if (c != null && otherFactor != null) {
      CompoundState state = c.getValue();
      if (state.isSingleton()) {
        BigInteger value = state.getValue();
        if (value.equals(BigInteger.ONE)) {
          return otherFactor;
        }
        if (value.equals(BigInteger.valueOf(-1))) {
          return InvariantsFormulaManager.INSTANCE.negate(otherFactor);
        }
        if (value.equals(BigInteger.ZERO)) {
          return InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.singleton(BigInteger.ZERO));
        }
      }
    }
    if (factor1 == pMultiply.getFactor1() && factor2 == pMultiply.getFactor2()) {
      return pMultiply;
    }
    return InvariantsFormulaManager.INSTANCE.multiply(factor1, factor2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Negate<CompoundState> pNegate, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand = pNegate.getNegated().accept(this, pEvaluationVisitor);
    if (operand instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pNegate.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    if (operand == pNegate.getNegated()) {
      return pNegate;
    }
    return InvariantsFormulaManager.INSTANCE.negate(operand);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(ShiftLeft<CompoundState> pShiftLeft, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> shifted = pShiftLeft.getShifted().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> shiftDistance = pShiftLeft.getShiftDistance().accept(this, pEvaluationVisitor);
    if (shifted instanceof Constant<?> && shiftDistance instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pShiftLeft.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    // If the shift distance is zero, return the left operand
    if (shiftDistance instanceof Constant<?>) {
      Constant<CompoundState> c = (Constant<CompoundState>) shiftDistance;
      if (c.getValue().isSingleton() && c.getValue().getValue().equals(BigInteger.ZERO)) {
        return shifted;
      }
    }
    if (shifted == pShiftLeft.getShifted() && shiftDistance == pShiftLeft.getShiftDistance()) {
      return pShiftLeft;
    }
    return InvariantsFormulaManager.INSTANCE.shiftLeft(shifted, shiftDistance);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(ShiftRight<CompoundState> pShiftRight, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> shifted = pShiftRight.getShifted().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> shiftDistance = pShiftRight.getShiftDistance().accept(this, pEvaluationVisitor);
    if (shifted instanceof Constant<?> && shiftDistance instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pShiftRight.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    // If the shift distance is zero, return the left operand
    if (shiftDistance instanceof Constant<?>) {
      Constant<CompoundState> c = (Constant<CompoundState>) shiftDistance;
      if (c.getValue().isSingleton() && c.getValue().getValue().equals(BigInteger.ZERO)) {
        return shifted;
      }
    }
    if (shifted == pShiftRight.getShifted() && shiftDistance == pShiftRight.getShiftDistance()) {
      return pShiftRight;
    }
    return InvariantsFormulaManager.INSTANCE.shiftRight(shifted, shiftDistance);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Union<CompoundState> pUnion, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    InvariantsFormula<CompoundState> operand1 = pUnion.getOperand1().accept(this, pEvaluationVisitor);
    InvariantsFormula<CompoundState> operand2 = pUnion.getOperand2().accept(this, pEvaluationVisitor);
    if (operand1 instanceof Constant<?> && operand2 instanceof Constant<?>) {
      return InvariantsFormulaManager.INSTANCE.asConstant(pUnion.accept(pEvaluationVisitor, EMPTY_ENVIRONMENT));
    }
    // Union with top yields top
    if (operand1.equals(TOP) || operand2.equals(TOP)) {
      return TOP;
    }
    // Union with bottom yields the other operand
    if (operand1.equals(BOTTOM)) {
      return operand2;
    }
    if (operand2.equals(BOTTOM)) {
      return operand1;
    }
    if (operand1 == pUnion.getOperand1() && operand2 == pUnion.getOperand2()) {
      return pUnion;
    }
    return InvariantsFormulaManager.INSTANCE.union(operand1, operand2);
  }

  @Override
  public InvariantsFormula<CompoundState> visit(Variable<CompoundState> pVariable, FormulaEvaluationVisitor<CompoundState> pEvaluationVisitor) {
    return pVariable;
  }

}
