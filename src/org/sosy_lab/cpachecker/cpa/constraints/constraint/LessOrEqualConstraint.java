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

import org.sosy_lab.cpachecker.cpa.constraints.ConstraintVisitor;

/**
 * A less-or-equal-Constraint.
 *
 * <p>Example: <code>'n <= 5'</code>.</p>
 */
public class LessOrEqualConstraint extends Constraint {

  /**
   * Creates a new <code>LessOrEqualConstraint</code> object with the given operator and operand.
   *
   * @param pLeftOperand the left operand of the constraint
   * @param pRightOperand the right operand of the constraint
   */
  LessOrEqualConstraint(ConstraintOperand pLeftOperand,
      ConstraintOperand pRightOperand) {
    super(pLeftOperand, pRightOperand);
  }

  /**
   * Creates a new positive or negative <code>LessorEqualConstraint</code> object with the given operator and operand.
   *
   * @param pLeftOperand the left operand of the constraint
   * @param pRightOperand the right operand of the constraint
   * @param pIsPositive whether the constraint is positive or negative
   */
  LessOrEqualConstraint(ConstraintOperand pLeftOperand,
      ConstraintOperand pRightOperand, boolean pIsPositive) {
    super(pLeftOperand, pRightOperand, pIsPositive);
  }

  @Override
  public <T> T accept(ConstraintVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  protected String getOperatorAsString() {
    return "<=";
  }
}
