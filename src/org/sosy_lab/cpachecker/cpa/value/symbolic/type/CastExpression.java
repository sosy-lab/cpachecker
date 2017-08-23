/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
 * {@link SymbolicExpression} representing a cast.
 */
public class CastExpression extends UnarySymbolicExpression {

  private static final long serialVersionUID = 3928318112889309143L;

  /**
   * Create a new <code>CastExpression</code> with the given operand and {@link Type}.
   *
   * The given <code>Type</code> represents the type the operand should be casted to.
   * No checks for compatibility between operand type and
   *
   * @param pOperand the operand to cast
   * @param pType the type to cast the operand to
   */
  protected CastExpression(SymbolicExpression pOperand, Type pType) {
    super(pOperand, pType);
  }

  /**
   * Create a new <code>CastExpression</code> with the given operand and {@link Type} representing
   * the value of the given memory location.
   *
   * The given <code>Type</code> represents the type the operand should be casted to.
   * No checks for compatibility between operand type and
   *
   * @param pOperand the operand to cast
   * @param pType the type to cast the operand to
   * @param pRepresentedLocation the memory location this cast expression represents
   */
  protected CastExpression(
      final SymbolicExpression pOperand,
      final Type pType,
      final MemoryLocation pRepresentedLocation
  ) {
    super(pOperand, pType, pRepresentedLocation);
  }

  @Override
  public CastExpression copyForLocation(MemoryLocation pRepresentedLocation) {
    return new CastExpression(getOperand(), getType(), pRepresentedLocation);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String toString() {
    return "Cast[" + getType() + ", " + getOperand() + "]";
  }

  @Override
  public String getOperationString() {
    return "Cast>";
  }
}
