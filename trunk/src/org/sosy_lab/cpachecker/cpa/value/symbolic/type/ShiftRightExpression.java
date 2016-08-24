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
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * {@link BinarySymbolicExpression} representing the 'shift right' operation.
 *
 * <p>There is no differentiation between signed and unsigned shifts.</p>
 */
public class ShiftRightExpression extends BinarySymbolicExpression {

  private static final long serialVersionUID = -9068365554036095329L;

  public enum ShiftType {
    SIGNED,
    UNSIGNED
  }

  private final ShiftType shiftType;

  protected ShiftRightExpression(SymbolicExpression pOperand1, SymbolicExpression pOperand2,
      Type pExpressionType, Type pCalculationType, ShiftType pShiftType) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType);
    shiftType = pShiftType;
  }

  protected ShiftRightExpression(
      final ShiftType pShiftType,
      final SymbolicExpression pOperand1,
      final SymbolicExpression pOperand2,
      final Type pExpressionType,
      final Type pCalculationType,
      final MemoryLocation pRepresentedLocation
  ) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType, pRepresentedLocation);
    shiftType = pShiftType;
  }

  @Override
  public ShiftRightExpression copyForLocation(final MemoryLocation pRepresentedLocation) {
    return new ShiftRightExpression(shiftType, getOperand1(), getOperand2(), getType(),
        getCalculationType(), pRepresentedLocation);
  }

  public boolean isSigned() {
    return shiftType == ShiftType.SIGNED;
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String getOperationString() {
    if (isSigned()) {
      return ">>";
    } else {
      return ">>>";
    }
  }
}
