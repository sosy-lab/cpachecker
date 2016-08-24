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

import org.sosy_lab.cpachecker.cpa.invariants.TypeInfo;
import org.sosy_lab.cpachecker.cpa.invariants.Typed;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


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
  public <T> NumeralFormula<T> add(NumeralFormula<T> pSummand1, NumeralFormula<T> pSummand2) {
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
  public <T> NumeralFormula<T> binaryAnd(NumeralFormula<T> pOperand1, NumeralFormula<T> pOperand2) {
    return BinaryAnd.of(pOperand1, pOperand2);
  }

  /**
   * Gets the binary negation of the given formula.
   *
   * @param pToFlip the operand of the bit flip operation.
   *
   * @return the binary negation of the given formula.
   */
  public <T> NumeralFormula<T> binaryNot(NumeralFormula<T> pToFlip) {
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
  public <T> NumeralFormula<T> binaryOr(NumeralFormula<T> pOperand1, NumeralFormula<T> pOperand2) {
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
  public <T> NumeralFormula<T> binaryXor(NumeralFormula<T> pOperand1, NumeralFormula<T> pOperand2) {
    return BinaryXor.of(pOperand1, pOperand2);
  }

  /**
   * Gets a invariants formula representing a constant with the given value.
   *
   * @param pValue the value of the constant.
   *
   * @return a invariants formula representing a constant with the given value.
   */
  public <T extends Typed> NumeralFormula<T> asConstant(T pValue) {
    return Constant.of(pValue);
  }

  /**
   * Gets a invariants formula representing a constant with the given value.
   *
   * @param pTypeInfo the type information for the constant.
   * @param pValue the value of the constant.
   *
   * @return a invariants formula representing a constant with the given value.
   */
  public <T> NumeralFormula<T> asConstant(TypeInfo pTypeInfo, T pValue) {
    return Constant.of(pTypeInfo, pValue);
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
  public <T> NumeralFormula<T> divide(NumeralFormula<T> pNumerator, NumeralFormula<T> pDenominator) {
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
  public <T> BooleanFormula<T> equal(NumeralFormula<T> pOperand1, NumeralFormula<T> pOperand2) {
    if (pOperand1 instanceof Exclusion) {
      return logicalNot(Equal.of(((Exclusion<T>) pOperand1).getExcluded(), pOperand2));
    }
    if (pOperand2 instanceof Exclusion) {
      return logicalNot(Equal.of(pOperand1, ((Exclusion<T>) pOperand2).getExcluded()));
    }
    return Equal.of(pOperand1, pOperand2);
  }

  public <T> NumeralFormula<T> exclude(NumeralFormula<T> pToExclude) {
    return Exclusion.of(pToExclude);
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
  public <T> BooleanFormula<T> greaterThan(NumeralFormula<T> pOperand1, NumeralFormula<T> pOperand2) {
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
  public <T> BooleanFormula<T> greaterThanOrEqual(NumeralFormula<T> pOperand1, NumeralFormula<T> pOperand2) {
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
  public <T> BooleanFormula<T> lessThan(NumeralFormula<T> pOperand1, NumeralFormula<T> pOperand2) {
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
  public <T> BooleanFormula<T> lessThanOrEqual(NumeralFormula<T> pOperand1, NumeralFormula<T> pOperand2) {
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
  public <T> BooleanFormula<T> logicalAnd(BooleanFormula<T> pOperand1, BooleanFormula<T> pOperand2) {
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
  public <T> BooleanFormula<T> logicalNot(BooleanFormula<T> pToNegate) {
    if (pToNegate instanceof LogicalNot) {
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
  public <T> BooleanFormula<T> logicalOr(BooleanFormula<T> pOperand1, BooleanFormula<T> pOperand2) {
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
  public <T> BooleanFormula<T> logicalImplies(BooleanFormula<T> pOperand1, BooleanFormula<T> pOperand2) {
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
  public <T> NumeralFormula<T> modulo(NumeralFormula<T> pNumerator, NumeralFormula<T> pDenominator) {
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
  public <T> NumeralFormula<T> multiply(NumeralFormula<T> pFactor1,
      NumeralFormula<T> pFactor2) {
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
  public <T> NumeralFormula<T> shiftLeft(NumeralFormula<T> pToShift,
      NumeralFormula<T> pShiftDistance) {
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
  public <T> NumeralFormula<T> shiftRight(NumeralFormula<T> pToShift,
      NumeralFormula<T> pShiftDistance) {
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
  public <T> NumeralFormula<T> union(NumeralFormula<T> pOperand1,
      NumeralFormula<T> pOperand2) {
    if (pOperand1.equals(pOperand2)) {
      return pOperand1;
    }
    return Union.of(pOperand1, pOperand2);
  }

  /**
   * Gets an invariants formula representing the variable with the given memory location.
   *
   * @param pTypeInfo the bit vector information for the variable.
   * @param pMemoryLocation the memory location of the variable.
   *
   * @return an invariants formula representing the variable with the given memory location.
   */
  public <T> Variable<T> asVariable(TypeInfo pTypeInfo, MemoryLocation pMemoryLocation) {
    return Variable.of(pTypeInfo, pMemoryLocation);
  }

  /**
   * Gets a numeral formula representing an if-then-else condition-dependent
   * value.
   *
   * @param pCondition the condition the value depends on.
   * @param pPositiveCase the value in case the condition is true.
   * @param pNegativeCase the value in case the condition is false.
   *
   * @return a numeral formula representing an if-then-else condition-dependent
   * value.
   */
  public <T> NumeralFormula<T> ifThenElse(
      BooleanFormula<T> pCondition,
      NumeralFormula<T> pPositiveCase,
      NumeralFormula<T> pNegativeCase) {
    if (BooleanConstant.isTrue(pCondition)) {
      return pPositiveCase;
    }
    if (BooleanConstant.isFalse(pCondition)) {
      return pNegativeCase;
    }
    return IfThenElse.of(pCondition, pPositiveCase, pNegativeCase);
  }

  /**
   * Gets an invariants formula representing the cast of the given
   * operand to the given bit vector.
   *
   * @param pTypeInfo the bit vector to cast the formula to.
   * @param pToCast the invariants formula to cast.
   *
   * @return an invariants formula representing the cast of the given
   * operand to the given bit vector.
   */
  public <T> NumeralFormula<T> cast(TypeInfo pTypeInfo, NumeralFormula<T> pToCast) {
    if (pTypeInfo.equals(pToCast.getTypeInfo())) {
      return pToCast;
    }
    return Cast.of(pTypeInfo, pToCast);
  }

}
