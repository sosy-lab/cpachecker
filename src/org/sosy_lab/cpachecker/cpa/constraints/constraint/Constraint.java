/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.constraints.FormulaCreator;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

import com.google.common.base.Optional;

/**
 * A single constraint.
 *
 * <p>A constraint consists of two values and one of the
 * following operators that describes the relation between these values:
 * <ul>
 *   <li>less-than (<)</li>
 *   <li>less-than-or-equal-to (<=)</li>
 *   <li>equals</li>
 * </ul></p>
 *
 * <p>Example: '2 < 5'</p>
 */
public class Constraint implements Value {

  public Formula transformToFormula(FormulaCreator<? extends Formula> pFormulaCreator) {
    Formula formula;

    switch (operator) {
      case LESS:
        formula = pFormulaCreator.createLess(leftOperand, rightOperand);
        break;
      case LESS_EQUAL:
        formula = pFormulaCreator.createLessOrEqual(leftOperand, rightOperand);
        break;
      case EQUAL:
        formula = pFormulaCreator.createEqual(leftOperand, rightOperand);
        break;
      default:
        throw new AssertionError("Unhandled operator " + operator);
    }

    if (!positiveConstraint) {
      formula = pFormulaCreator.createNot(formula);
    }

    return formula;
  }

  public enum Operator { LESS, LESS_EQUAL, EQUAL }

  private final Operator operator;
  private final ConstraintOperand leftOperand;
  private final ConstraintOperand rightOperand;

  private final boolean positiveConstraint;

  /**
   * Creates a new <code>Constraint</code> object with the given operator and operand.
   *
   * @param pLeftOperand the left operand of the constraint
   * @param pOperator the operator of the constraint
   * @param pRightOperand the right operand of the constraint
   */
  Constraint(ConstraintOperand pLeftOperand, Operator pOperator, ConstraintOperand pRightOperand) {

    operator = pOperator;
    leftOperand = pLeftOperand;
    rightOperand = pRightOperand;
    positiveConstraint = true;
  }

  Constraint(ConstraintOperand pLeftOperand, Operator pOperator, ConstraintOperand pRightOperand, boolean pIsPositive) {
    operator = pOperator;
    leftOperand = pLeftOperand;
    rightOperand = pRightOperand;
    positiveConstraint = pIsPositive;
  }


  public boolean includes(Constraint pOtherConstraint) {
    return false;
    /* We currently have no way to create Ranges as an operand can be any kind of (unresolvable) formula
    final Range thisRange = new Range(this);
    final Range otherRange = new Range(pOtherConstraint);

    return thisRange.includes(otherRange);*/
  }

  /**
   * Merges this condition with the given condition.
   *
   * @param pOtherConstraint the <code>Condition</code> to merge with this object
   * @return an <code>Optional</code> instance containing a {@link Value} object representing the merge of the two
   *    conditions, if their intersection is knowingly not empty.
   *    Returns an empty <code>Optional</code> instance, otherwise
   */
  public Optional<Value> mergeWith(Constraint pOtherConstraint) {
    Constraint newConstraint;

    if (haveSingleIntersection(this, pOtherConstraint)) {
      newConstraint = new Constraint(leftOperand, Operator.EQUAL, rightOperand);

    } else { // TODO add more cases
      return Optional.absent();
    }

    return Optional.<Value>of(newConstraint);
  }

  private boolean haveSingleIntersection(Constraint pCond1, Constraint pCond2) {
    Operator op1 = pCond1.operator;
    Operator op2 = pCond2.operator;

    return !(op1 == Operator.LESS || op2 == Operator.LESS)
        && ((pCond1.leftOperand.equals(pCond2.rightOperand) && pCond1.rightOperand.equals(pCond2.leftOperand))
            || (pCond1.leftOperand.equals(pCond2.leftOperand) && pCond1.rightOperand.equals(pCond2.rightOperand)
              && (op1 == Operator.EQUAL || op2 == Operator.EQUAL)));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;

    } else if (!(o instanceof Constraint)) {
      return false;

    } else {
      Constraint that = (Constraint) o;

      return positiveConstraint == that.positiveConstraint
          && leftOperand.equals(that.leftOperand)
          && operator == that.operator
          && rightOperand.equals(that.rightOperand);
    }
  }

  @Override
  public int hashCode() {
    int result = operator.hashCode();

    result = 2 * result + (positiveConstraint ? 1 : 0);
    result = 31 * result + leftOperand.hashCode();
    result = 31 * result + rightOperand.hashCode();

    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    if (!positiveConstraint) {
      sb.append("not (");
    }

    switch (operator) {
      case LESS:
        sb.append(leftOperand).append(" < ").append(rightOperand);
        break;
      case LESS_EQUAL:
        sb.append(leftOperand).append(" <= ").append(rightOperand);
        break;
      case EQUAL:
        sb.append(leftOperand).append(" == ").append(rightOperand);
        break;
      default:
        throw new AssertionError("Unhandled operator: " + operator);
    }

    if (!positiveConstraint) {
      sb.append(")");
    }

    return sb.toString();
  }

  @Override
  public boolean isNumericValue() {
    return false;
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  @Override
  public boolean isExplicitlyKnown() {
    return false;
  }

  @Override
  public NumericValue asNumericValue() {
    return null;
  }

  @Override
  public Long asLong(CType type) {
    return null;
  }

/* We currently have no way to create Ranges as an operand can be any kind of (unresolvable) formula
  private static class Range {
    private double min;
    private double max;
    private boolean isOpen;

    public Range(Constraint pConstraint) {
      if (pConstraint.operator == Operator.EQUAL) {
        isOpen = false;

        if (pConstraint.leftOperand.isNumericValue()) {
          min = ((NumericValue) pConstraint.leftOperand).doubleValue();
        } else if (pConstraint.rightOperand.isNumericValue()) {
          min = ((NumericValue) pConstraint.rightOperand).doubleValue();
        } else {
          throw new AssertionError("No numeric value for range creation");
        }

        max = min;

      } else {
        isOpen = pConstraint.operator == Operator.LESS;

        if (pConstraint.leftOperand.isNumericValue()) {
          min = ((NumericValue) pConstraint.leftOperand).doubleValue();
        } else {
          min = Double.NEGATIVE_INFINITY;
        }

        if (pConstraint.rightOperand.isNumericValue()) {
          max = ((NumericValue) pConstraint.rightOperand).doubleValue();
        } else {
          max = Double.POSITIVE_INFINITY;
        }
      }
    }

    public boolean includes(Range pRange) {
      boolean isIncluded = true;

      if (min > pRange.min) {
        return false;
      }

      if (max < pRange.max) {
        return false;
      }

      if (min == pRange.min && min != Double.NEGATIVE_INFINITY) {
        isIncluded = !isOpen || pRange.isOpen;
      }

      if (max == pRange.max && max != Double.POSITIVE_INFINITY) {
        isIncluded &= !isOpen || pRange.isOpen;
      }

      return isIncluded;
    }
  }*/
}
