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
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

/**
 * A single constraint.
 *
 * <p>A constraint describes a relation between two {@link ConstraintOperand}s that has to be true or false,
 * depending on the constraint.</p>
 *
 * <p>Possible examples would be relations like <code>'5 < 10'</code> or <code>'n == 10'</code></p>
 */
public abstract class Constraint implements Value {

  private final ConstraintOperand leftOperand;
  private final ConstraintOperand rightOperand;

  private final boolean positiveConstraint;

  /**
   * Creates a new <code>Constraint</code> object with the given operator and operand.
   *
   * @param pLeftOperand the left operand of the constraint
   * @param pRightOperand the right operand of the constraint
   */
  Constraint(ConstraintOperand pLeftOperand, ConstraintOperand pRightOperand) {
    leftOperand = pLeftOperand;
    rightOperand = pRightOperand;
    positiveConstraint = true;
  }

  Constraint(ConstraintOperand pLeftOperand, ConstraintOperand pRightOperand, boolean pIsPositive) {
    leftOperand = pLeftOperand;
    rightOperand = pRightOperand;
    positiveConstraint = pIsPositive;
  }

  /**
   * Accepts the given {@link ConstraintVisitor}.
   *
   * @param pVisitor the visitor to accept
   * @param <T> the return type of the given visitor
   *
   * @return the value returned by the visitor's visit method
   */
  public abstract <T> T accept(ConstraintVisitor<T> pVisitor);

  public ConstraintOperand getLeftOperand() {
    return leftOperand;
  }

  public ConstraintOperand getRightOperand() {
    return rightOperand;
  }

  /**
   * Returns whether this constraint is positive.
   * Not positive (that is, negative) constraints represent a NOT.
   *
   * @return <code>true</code> if this constraint is positive, <code>false</code> otherwise.
   */
  public boolean isPositiveConstraint() {
    return positiveConstraint;
  }

  public boolean includes(Constraint pOtherConstraint) {
    return false;
    /* We currently have no way to create Ranges as an operand can be any kind of (unresolvable) formula
    final Range thisRange = new Range(this);
    final Range otherRange = new Range(pOtherConstraint);

    return thisRange.includes(otherRange);*/
  }

  /*
  /**
   * Merges this condition with the given condition.
   *
   * @param pOtherConstraint the <code>Condition</code> to merge with this object
   * @return an <code>Optional</code> instance containing a {@link Value} object representing the merge of the two
   *    conditions, if their intersection is knowingly not empty.
   *    Returns an empty <code>Optional</code> instance, otherwise
   */
  /*
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
  */

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;

    } else if (!(o.getClass().equals(getClass()))) {
      return false;

    } else {
      Constraint that = (Constraint) o;

      return positiveConstraint == that.positiveConstraint
          && leftOperand.equals(that.leftOperand)
          && rightOperand.equals(that.rightOperand);
    }
  }

  @Override
  public int hashCode() {
    int result = getClass().hashCode();

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

    sb.append(leftOperand).append(" " + getOperatorAsString() + " ").append(rightOperand);

    if (!positiveConstraint) {
      sb.append(")");
    }

    return sb.toString();
  }

  /**
   * Returns the constraint's operator as a string.
   *
   * @return the constraint operator's string representation
   */
  protected abstract String getOperatorAsString();

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
