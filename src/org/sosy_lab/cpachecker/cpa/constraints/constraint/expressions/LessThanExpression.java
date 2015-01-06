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
package org.sosy_lab.cpachecker.cpa.constraints.constraint.expressions;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintVisitor;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.BinaryConstraint;

/**
 * {@link BinaryConstraintExpression} representing the 'less than' operation.
 */
public class LessThanExpression extends BinaryConstraintExpression implements BinaryConstraint {

  protected LessThanExpression(ConstraintExpression pOperand1, ConstraintExpression pOperand2,
      Type pExpressionType, Type pCalculationType) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(ConstraintExpressionVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public <T> T accept(ConstraintVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public LessThanExpression copyWithExpressionType(Type pExpressionType) {
    return new LessThanExpression(getOperand1(), getOperand2(), pExpressionType, getCalculationType());
  }

  @Override
  public String toString() {
    return "(" + getOperand1() + " < " + getOperand2() + ")";
  }
}
