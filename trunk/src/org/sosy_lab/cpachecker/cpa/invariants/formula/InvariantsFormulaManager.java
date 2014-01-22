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


/**
 * The singleton instance of this class provides operations for obtaining
 * invariants formulae.
 */
public enum InvariantsFormulaManager {

  /**
   * The invariants formula manager singleton instance.
   */
  INSTANCE;

  /**
   * Gets the sum of the given formulae as a formula.
   *
   * @param pSummand1 the first summand.
   * @param pSummand2 the second summand.
   *
   * @return the sum of the given formulae.
   */
  public <T> InvariantsFormula<T> add(InvariantsFormula<T> pSummand1, InvariantsFormula<T> pSummand2) {
    return Add.of(pSummand1, pSummand2);
  }

  /**
   * Gets the binary and operation over the given operands.
   *
   * @param pOperand1 the first operand.
   * @param pOperand2 the second operand.
   *
   * @return the binary and operation over the given operands.
   */
  public <T> InvariantsFormula<T> binaryAnd(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return BinaryAnd.of(pOperand1, pOperand2);
  }

  /**
   * Gets the binary negation of the given formula.
   *
   * @param pToFlip the operand of the bit flip operation.
   *
   * @return the binary negation of the given formula.
   */
  public <T> InvariantsFormula<T> binaryNot(InvariantsFormula<T> pToFlip) {
    return BinaryNot.of(pToFlip);
  }

  /**
   * Gets an invariants formula representing the binary or operation over the
   * given operands.
   *
   * @param pOperand1 the first operand.
   * @param pOperand2 the second operand.
   *
   * @return an invariants formula representing the binary or operation over the
   * given operands.
   */
  public <T> InvariantsFormula<T> binaryOr(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return BinaryOr.of(pOperand1, pOperand2);
  }

  /**
   * Gets an invariants formula representing the binary exclusive or operation
   * over the given operands.
   *
   * @param pOperand1 the first operand.
   * @param pOperand2 the second operand.
   *
   * @return an invariants formula representing the binary exclusive or operation
   * over the given operands.
   */
  public <T> InvariantsFormula<T> binaryXor(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return BinaryXor.of(pOperand1, pOperand2);
  }

  /**
   * Gets a invariants formula representing a constant with the given value.
   *
   * @param pValue the value of the constant.
   *
   * @return a invariants formula representing a constant with the given value.
   */
  public <T> InvariantsFormula<T> asConstant(T pValue) {
    return Constant.of(pValue);
  }

  /**
   * Gets an invariants formula representing the division of the given
   * numerator formula by the given denominator formula.
   *
   * @param pNumerator the numerator of the fraction.
   * @param pDenominator the denominator of the fraction.
   *
   * @return an invariants formula representing the division of the given
   * numerator formula by the given denominator formula.
   */
  public <T> InvariantsFormula<T> divide(InvariantsFormula<T> pNumerator, InvariantsFormula<T> pDenominator) {
    return Divide.of(pNumerator, pDenominator);
  }

  /**
   * Gets an invariants formula representing the equation over the given
   * formulae.
   *
   * @param pOperand1 the first operand of the equation.
   * @param pOperand2 the second operand of the equation.
   *
   * @return an invariants formula representing the equation of the given
   * operands.
   */
  public <T> InvariantsFormula<T> equal(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return Equal.of(pOperand1, pOperand2);
  }

  /**
   * Gets an invariants formula representing a greater-than inequation over the
   * given operands.
   *
   * @param pOperand1 the left operand of the inequation.
   * @param pOperand2 the right operand of the inequation.
   *
   * @return an invariants formula representing a greater-than inequation over
   * the given operands.
   */
  public <T> InvariantsFormula<T> greaterThan(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return lessThan(pOperand2, pOperand1);
  }

  /**
   * Gets an invariants formula representing a greater-than or equal inequation
   * over the given operands.
   *
   * @param pOperand1 the left operand of the inequation.
   * @param pOperand2 the right operand of the inequation.
   *
   * @return an invariants formula representing a greater-than or equal
   * inequation over the given operands.
   */
  public <T> InvariantsFormula<T> greaterThanOrEqual(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return logicalNot(lessThan(pOperand1, pOperand2));
  }

  /**
   * Gets an invariants formula representing a less-than inequation over the
   * given operands.
   *
   * @param pOperand1 the left operand of the inequation.
   * @param pOperand2 the right operand of the inequation.
   *
   * @return an invariants formula representing a less-than inequation over the
   * given operands.
   */
  public <T> InvariantsFormula<T> lessThan(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return LessThan.of(pOperand1, pOperand2);
  }

  /**
   * Gets an invariants formula representing a less-than or equal inequation
   * over the given operands.
   *
   * @param pOperand1 the left operand of the inequation.
   * @param pOperand2 the right operand of the inequation.
   *
   * @return an invariants formula representing a less-than or equal inequation
   * over the given operands.
   */
  public <T> InvariantsFormula<T> lessThanOrEqual(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return greaterThanOrEqual(pOperand2, pOperand1);
  }

  /**
   * Gets an invariants formula representing the logical conjunction over the
   * given operands.
   *
   * @param pOperand1 the first operand of the conjunction.
   * @param pOperand2 the second operand of the conjunction.
   *
   * @return an invariants formula representing the logical conjunction over the
   * given operands.
   */
  public <T> InvariantsFormula<T> logicalAnd(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return LogicalAnd.of(pOperand1, pOperand2);
  }

  /**
   * Gets an invariants formula representing the logical negation of the given
   * operand.
   *
   * @param pToNegate the invariants formula to negate.
   *
   * @return an invariants formula representing the logical negation of the given
   * operand.
   */
  public <T> InvariantsFormula<T> logicalNot(InvariantsFormula<T> pToNegate) {
    if (pToNegate instanceof LogicalNot<?>) {
      return ((LogicalNot<T>) pToNegate).getNegated();
    }
    return LogicalNot.of(pToNegate);
  }

  /**
   * Gets an invariants formula representing the logical disjunction over the
   * given operands.
   *
   * @param pOperand1 the first operand of the disjunction.
   * @param pOperand2 the second operand of the disjunction.
   *
   * @return an invariants formula representing the logical disjunction over
   * the given operands.
   */
  public <T> InvariantsFormula<T> logicalOr(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return logicalNot(LogicalAnd.of(logicalNot(pOperand1), logicalNot(pOperand2)));
  }

  /**
   * Gets an invariants formula representing a logical implication over the
   * given operands, meaning that the first operand implies the second operand.
   *
   * @param pOperand1 the implication assumption.
   * @param pOperand2 the implication conclusion.
   *
   * @return an invariants formula representing a logical implication over the
   * given operands, meaning that the first operand implies the second operand.
   */
  public <T> InvariantsFormula<T> logicalImplies(InvariantsFormula<T> pOperand1, InvariantsFormula<T> pOperand2) {
    return logicalNot(LogicalAnd.of(pOperand1, logicalNot(pOperand2)));
  }

  /**
   * Gets an invariants formula representing the modulo operation over the
   * given operands.
   *
   * @param pNumerator the numerator of the fraction.
   * @param pDenominator the denominator of the fraction.
   * @return an invariants formula representing the modulo operation over the
   * given operands.
   */
  public <T> InvariantsFormula<T> modulo(InvariantsFormula<T> pNumerator, InvariantsFormula<T> pDenominator) {
    return Modulo.of(pNumerator, pDenominator);
  }

  /**
   * Gets an invariants formula representing the multiplication of the given
   * factors.
   *
   * @param pFactor1 the first factor.
   * @param pFactor2 the second factor.
   *
   * @return an invariants formula representing the multiplication of the given
   * factors.
   */
  public <T> InvariantsFormula<T> multiply(InvariantsFormula<T> pFactor1,
      InvariantsFormula<T> pFactor2) {
    return Multiply.of(pFactor1, pFactor2);
  }

  /**
   * Gets an invariants formula representing the left shift of the first given
   * operand by the second given operand.
   *
   * @param pToShift the operand to be shifted.
   * @param pShiftDistance the shift distance.
   *
   * @return an invariants formula representing the left shift of the first
   * given operand by the second given operand.
   */
  public <T> InvariantsFormula<T> shiftLeft(InvariantsFormula<T> pToShift,
      InvariantsFormula<T> pShiftDistance) {
    return ShiftLeft.of(pToShift, pShiftDistance);
  }

  /**
   * Gets an invariants formula representing the right shift of the first given
   * operand by the second given operand.
   *
   * @param pToShift the operand to be shifted.
   * @param pShiftDistance the shift distance.
   *
   * @return an invariants formula representing the right shift of the first
   * given operand by the second given operand.
   */
  public <T> InvariantsFormula<T> shiftRight(InvariantsFormula<T> pToShift,
      InvariantsFormula<T> pShiftDistance) {
    return ShiftRight.of(pToShift, pShiftDistance);
  }

  /**
   * Gets an invariants formula representing the union of the given invariants
   * formulae.
   *
   * @param pOperand1 the first operand.
   * @param pOperand2 the second operand.
   *
   * @return an invariants formula representing the union of the given invariants
   * formulae.
   */
  public <T> InvariantsFormula<T> union(InvariantsFormula<T> pOperand1,
      InvariantsFormula<T> pOperand2) {
    if (pOperand1.equals(pOperand2)) {
      return pOperand1;
    }
    return Union.of(pOperand1, pOperand2);
  }

  /**
   * Gets an invariants formula representing the variable with the given name.
   *
   * @param pName the name of the variable.
   *
   * @return an invariants formula representing the variable with the given name.
   */
  public <T> Variable<T> asVariable(String pName) {
    return Variable.of(pName);
  }

}
