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
package org.sosy_lab.cpachecker.cpa.symbolic;

import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

import com.google.common.base.Optional;

/**
 * A single condition for a {@link SymbolicValue}.
 *
 * <p>A condition consists of a value and one of the
 * following operators that describes the relation to this value:
 * <ul>
 *   <li>less-than (<)</li>
 *   <li>greater-than (>)</li>
 *   <li>less-than-or-equal-to (<=)</li>
 *   <li>greater-than-or-equal-to (>=)</li>
 * </ul></p>
 *
 * <p>Example: '< 5'</p>
 */
public class Condition {

  public enum Operator { LESS, GREATER, LESS_EQUAL, GREATER_EQUAL }

  private final Operator operator;
  private final NumericValue operand;

  /**
   * Creates a new <code>Condition</code> object with the given operator and operand.
   *
   * @param pOperator the operator of the newly created condition
   * @param pOperand the operand of the newly created condition
   */
  public Condition(Operator pOperator, NumericValue pOperand) {
    operator = pOperator;
    operand = pOperand;
  }

  /**
   * Returns whether the given value fulfills this condition.
   *
   * <p>Example in pseudo-code:
   * <pre>
   *   a = new Condition(Operator.LESS, 5);
   *   return a.includes(4)
   * </pre>
   * will return <code>true</code>.</p>
   *
   * @param pValue the value to check against this condition
   * @return <code>true</code> if the given value is in the range of this condition,
   *     <code>false</code> otherwise
   */
  public boolean includes(Value pValue) {
    final double concreteOperand = operand.doubleValue();

    if (pValue.isNumericValue()) {
      final double concreteValue = ((NumericValue) pValue).doubleValue();

      switch (operator) {
        case LESS:
          return concreteValue < concreteOperand;
        case LESS_EQUAL:
          return concreteValue <= concreteOperand;
        case GREATER:
          return concreteValue > concreteOperand;
        case GREATER_EQUAL:
          return concreteValue >= concreteOperand;
        default:
          throw new AssertionError("Unhandled operator " + operator);
      }
    } else if (pValue instanceof SymbolicValue) {
      final SymbolicValue symbolicValue = (SymbolicValue) pValue;
      final Optional<Condition> lesserCondition = symbolicValue.getLesserCondition();
      final Optional<Condition> greaterCondition = symbolicValue.getGreaterCondition();
      boolean isIncluded = true;

      if (lesserCondition.isPresent()) {
        isIncluded = mergeWith(lesserCondition.get()).isPresent();
      }

      if (greaterCondition.isPresent()) {
        isIncluded &= mergeWith(greaterCondition.get()).isPresent();
      }

      return isIncluded;
    } else {
      return false;
    }
  }

  /**
   * Returns whether this condition is a less or less-equal condition.
   *
   * @return <code>true</code> if this condition is a less or less-equal condition, <code>false</code> otherwise
   */
  public boolean isLesserCondition() {
    return isLesserOperator(operator);
  }

  private boolean isLesserOperator(Operator operator) {
    return operator == Operator.LESS || operator == Operator.LESS_EQUAL;
  }

  /**
   * Merges this condition with the given condition.
   *
   *
   * @param pOtherCondition the <code>Condition</code> to merge with this object
   * @return an <code>Optional</code> instance containing a {@link Value} object representing the merge of the two
   *    conditions, if their intersection is not empty. Returns an empty <code>Optional</code> instance, otherwise
   */
  public Optional<Value> mergeWith(Condition pOtherCondition) {
    final double thisOperand = operand.doubleValue();
    final double otherOperand = pOtherCondition.operand.doubleValue();

    if (!(isLesserOperator(operator) ^ isLesserOperator(pOtherCondition.operator))) {
      Condition newCondition;

      if (thisOperand == otherOperand) {
        Operator newOperator = getStrongerOperator(operator, pOtherCondition.operator);

        newCondition = new Condition(newOperator, operand);

      } else if (isLesserOperator(operator)) {
        if (thisOperand < otherOperand) {
          newCondition = this;
        } else {
          newCondition = pOtherCondition;
        }
      } else {
        if (thisOperand > otherOperand) {
          newCondition = this;
        } else {
          newCondition = pOtherCondition;
        }
      }

      return Optional.<Value>of(new SymbolicValue(newCondition));

    } else {
      if (haveSingleIntersection(this, pOtherCondition)) {
        return Optional.<Value>of(operand);
      }

      if (isLesserOperator(operator)) {
        if (thisOperand <= otherOperand) {
          return Optional.absent();
        }

      } else {
        if (thisOperand >= otherOperand) {
          return Optional.absent();
        }
      }

      return Optional.<Value>of(new SymbolicValue(this, pOtherCondition));
    }
  }

  private Operator getStrongerOperator(Operator pOperator1, Operator pOperator2) {
    assert !(isLesserOperator(pOperator1) ^ isLesserOperator(pOperator2));

    if (pOperator1 == Operator.GREATER || pOperator1 == Operator.LESS) {
      return pOperator1;
    } else {
      // either the operators equal or pOperator2 is more restrictive
      return pOperator2;
    }
  }

  private boolean haveSingleIntersection(Condition pCond1, Condition pCond2) {
    Operator op1 = pCond1.operator;
    Operator op2 = pCond2.operator;

    return pCond1.operand.equals(pCond2.operand)
        && (op1 == Operator.GREATER_EQUAL || op1 == Operator.LESS_EQUAL)
        && (op2 == Operator.GREATER_EQUAL || op2 == Operator.LESS_EQUAL);
  }

  @Override
  public String toString() {
    switch (operator) {
      case LESS:
        return "< " + operand;
      case LESS_EQUAL:
        return "<= " + operand;
      case GREATER:
        return "> " + operand;
      case GREATER_EQUAL:
        return ">= " + operand;
      default:
        throw new AssertionError("Unhandled operator: " + operator);
    }
  }
}
