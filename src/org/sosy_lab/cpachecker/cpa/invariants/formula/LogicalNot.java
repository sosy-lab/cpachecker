// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import com.google.common.base.Preconditions;

/** Instances of this class represent logical negations of invariants formulae. */
public class LogicalNot<ConstantType> implements BooleanFormula<ConstantType> {

  /** The formula logically negated by this formula. */
  private final BooleanFormula<ConstantType> negatedFormula;

  /**
   * Creates a new logical negation of the given formula.
   *
   * @param pToNegate the formula to logically negate.
   */
  private LogicalNot(BooleanFormula<ConstantType> pToNegate) {
    Preconditions.checkNotNull(pToNegate);
    this.negatedFormula = pToNegate;
  }

  /**
   * The formula logically negated by this formula.
   *
   * @return the formula logically negated by this formula.
   */
  public BooleanFormula<ConstantType> getNegated() {
    return this.negatedFormula;
  }

  @Override
  public String toString() {
    BooleanFormula<ConstantType> negated = getNegated();
    if (negated instanceof LogicalNot) {
      return ((LogicalNot<?>) negated).getNegated().toString();
    }
    if (negated instanceof Equal<?>) {
      Equal<?> equation = (Equal<?>) negated;
      return String.format("(%s != %s)", equation.getOperand1(), equation.getOperand2());
    }
    if (negated instanceof LessThan<?>) {
      LessThan<?> lessThan = (LessThan<?>) negated;
      return String.format("(%s >= %s)", lessThan.getOperand1(), lessThan.getOperand2());
    }
    if (negated instanceof LogicalAnd<?>) {
      LogicalAnd<?> and = (LogicalAnd<?>) negated;
      final String left;
      if (and.getOperand1() instanceof LogicalNot) {
        left = ((LogicalNot<?>) and.getOperand1()).getNegated().toString();
      } else {
        left = String.format("(!%s)", and.getOperand1());
      }
      final String right;
      if (and.getOperand2() instanceof LogicalNot) {
        right = ((LogicalNot<?>) and.getOperand2()).getNegated().toString();
      } else {
        right = String.format("(!%s)", and.getOperand2());
      }
      return String.format("(%s || %s)", left, right);
    }
    return String.format("(!%s)", negated);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof LogicalNot) {
      return getNegated().equals(((LogicalNot<?>) o).getNegated());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return -getNegated().hashCode();
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
   * Gets an invariants formula representing the logical negation of the given operand.
   *
   * @param pToNegate the invariants formula to negate.
   * @return an invariants formula representing the logical negation of the given operand.
   */
  static <ConstantType> LogicalNot<ConstantType> of(BooleanFormula<ConstantType> pToNegate) {
    return new LogicalNot<>(pToNegate);
  }
}
