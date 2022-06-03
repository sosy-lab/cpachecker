// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import com.google.common.base.Preconditions;

/**
 * Instances of this class represent equations over invariants formulae.
 *
 * @param <ConstantType> the type of the constants used in the formula.
 */
public class Equal<ConstantType> implements BooleanFormula<ConstantType> {

  /** The first operand. */
  private final NumeralFormula<ConstantType> operand1;

  /** The second operand. */
  private final NumeralFormula<ConstantType> operand2;

  /**
   * Creates a new equation over the given operands.
   *
   * @param pOperand1 the first operand of the equation.
   * @param pOperand2 the first operand of the equation.
   */
  private Equal(NumeralFormula<ConstantType> pOperand1, NumeralFormula<ConstantType> pOperand2) {
    Preconditions.checkArgument(pOperand1.getTypeInfo().equals(pOperand2.getTypeInfo()));
    this.operand1 = pOperand1;
    this.operand2 = pOperand2;
  }

  public NumeralFormula<ConstantType> getOperand1() {
    return operand1;
  }

  public NumeralFormula<ConstantType> getOperand2() {
    return operand2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Equal) {
      Equal<?> other = (Equal<?>) o;
      return (getOperand1().equals(other.getOperand1())
              && getOperand2().equals(other.getOperand2()))
          || (getOperand1().equals(other.getOperand2())
              && getOperand2().equals(other.getOperand1()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    // needs to be symmetric, see equals()
    return getOperand1().hashCode() + getOperand2().hashCode();
  }

  @Override
  public String toString() {
    return String.format("(%s == %s)", getOperand1(), getOperand2());
  }

  @Override
  public <ReturnType> ReturnType accept(BooleanFormulaVisitor<ConstantType, ReturnType> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <ReturnType, ParamType> ReturnType accept(
      ParameterizedBooleanFormulaVisitor<ConstantType, ParamType, ReturnType> pVisitor,
      ParamType pParameter) {
    return pVisitor.visit(this, pParameter);
  }

  /**
   * Gets an invariants formula representing the equation over the given formulae.
   *
   * @param pOperand1 the first operand of the equation.
   * @param pOperand2 the second operand of the equation.
   * @return an invariants formula representing the equation of the given operands.
   */
  static <ConstantType> Equal<ConstantType> of(
      NumeralFormula<ConstantType> pOperand1, NumeralFormula<ConstantType> pOperand2) {
    return new Equal<>(pOperand1, pOperand2);
  }
}
