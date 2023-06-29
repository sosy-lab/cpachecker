// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import com.google.common.base.Preconditions;

/** This is just a plain formula with two operands and one operator. */
abstract class AbstractBinaryFormula<ConstantType> extends AbstractFormula<ConstantType> {

  private final NumeralFormula<ConstantType> operand1;

  private final NumeralFormula<ConstantType> operand2;

  private final String operator;

  // isCommutative is TRUE for "=", "+", "*" and FALSE for "-", "/", "<".
  private final boolean isCommutative;

  /**
   * Creates a new formula with two operands.
   *
   * @param pOperand1 the first operand.
   * @param pOperand2 the second operand.
   */
  AbstractBinaryFormula(
      String pOperator,
      boolean pIsCommutative,
      NumeralFormula<ConstantType> pOperand1,
      NumeralFormula<ConstantType> pOperand2) {
    super(pOperand1.getTypeInfo());
    Preconditions.checkNotNull(pOperator);
    Preconditions.checkArgument(pOperand1.getTypeInfo().equals(pOperand2.getTypeInfo()));
    operator = pOperator;
    isCommutative = pIsCommutative;
    operand1 = pOperand1;
    operand2 = pOperand2;
  }

  public NumeralFormula<ConstantType> getOperand1() {
    return operand1;
  }

  public NumeralFormula<ConstantType> getOperand2() {
    return operand2;
  }

  @Override
  @SuppressWarnings("EqualsGetClass") // on purpose, case-class structure with single equals()
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    // equality for subclasses
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AbstractBinaryFormula<?> other = (AbstractBinaryFormula<?>) o;

    if (getTypeInfo().equals(other.getTypeInfo())
        && operator.equals(other.operator)
        && isCommutative == other.isCommutative) {

      if (isCommutative) {
        return (getOperand1().equals(other.getOperand1())
                && getOperand2().equals(other.getOperand2()))
            || (getOperand1().equals(other.getOperand2())
                && getOperand2().equals(other.getOperand1()));
      } else {
        return getOperand1().equals(other.getOperand1())
            && getOperand2().equals(other.getOperand2());
      }
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return 31 * operator.hashCode()
        + getOperand1().hashCode() * getOperand2().hashCode()
        + 43 * getTypeInfo().hashCode();
  }

  @Override
  public String toString() {
    return String.format("(%s %s %s)", getOperand1(), operator, getOperand2());
  }
}
