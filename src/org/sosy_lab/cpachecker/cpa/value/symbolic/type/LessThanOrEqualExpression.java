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
package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.BinaryConstraint;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * {@link BinarySymbolicExpression} representing the 'less than/equal' operation.
 */
public class LessThanOrEqualExpression extends BinarySymbolicExpression implements BinaryConstraint {

  private static final long serialVersionUID = -805938567569160336L;

  protected LessThanOrEqualExpression(SymbolicExpression pOperand1, SymbolicExpression pOperand2,
      Type pExpressionType, Type pCalculationType) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType);
  }

  protected LessThanOrEqualExpression(
      final SymbolicExpression pOperand1,
      final SymbolicExpression pOperand2,
      final Type pExpressionType,
      final Type pCalculationType,
      final MemoryLocation pRepresentedLocation
  ) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType, pRepresentedLocation);
  }

  @Override
  public LessThanOrEqualExpression copyForLocation(final MemoryLocation pRepresentedLocation) {
    return new LessThanOrEqualExpression(getOperand1(), getOperand2(), getType(),
        getCalculationType(), pRepresentedLocation);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String getOperationString() {
    return "<=";
  }
}
