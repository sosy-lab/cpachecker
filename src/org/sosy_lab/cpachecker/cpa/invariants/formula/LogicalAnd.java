// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

/** Instances of this class represent logical conjunctions over invariants formulae. */
public class LogicalAnd<ConstantType> implements BooleanFormula<ConstantType> {

  /** The first operand. */
  private final BooleanFormula<ConstantType> operand1;

  /** The second operand. */
  private final BooleanFormula<ConstantType> operand2;

  /**
   * Creates a new conjunction over the given operands.
   *
   * @param pOperand1 the first operand of the conjunction.
   * @param pOperand2 the second operand of the conjunction.
   */
  private LogicalAnd(
      BooleanFormula<ConstantType> pOperand1, BooleanFormula<ConstantType> pOperand2) {
    this.operand1 = pOperand1;
    this.operand2 = pOperand2;
  }

  public BooleanFormula<ConstantType> getOperand1() {
    return operand1;
  }

  public BooleanFormula<ConstantType> getOperand2() {
    return operand2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof LogicalAnd) {
      LogicalAnd<?> other = (LogicalAnd<?>) o;
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
    return String.format("(%s && %s)", getOperand1(), getOperand2());
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
   * Gets an invariants formula representing the logical conjunction over the given operands.
   *
   * @param pOperand1 the first operand of the conjunction.
   * @param pOperand2 the second operand of the conjunction.
   * @return an invariants formula representing the logical conjunction over the given operands.
   */
  static <ConstantType> LogicalAnd<ConstantType> of(
      BooleanFormula<ConstantType> pOperand1, BooleanFormula<ConstantType> pOperand2) {
    return new LogicalAnd<>(pOperand1, pOperand2);
  }
}
