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

import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Represents a unary {@link SymbolicExpression}. Represents all <code>SymbolicExpression</code>s
 * that consist of
 * only one operand.
 */
public abstract class UnarySymbolicExpression extends SymbolicExpression {

  private static final long serialVersionUID = -2727356523115713518L;

  private final SymbolicExpression operand;
  private final Type type;

  public UnarySymbolicExpression(SymbolicExpression pOperand, Type pType) {
    operand = pOperand;
    type = pType;
  }

  public UnarySymbolicExpression(
      final SymbolicExpression pOperand,
      final Type pType,
      final MemoryLocation pRepresentedLocation
  ) {
    super(pRepresentedLocation);
    operand = pOperand;
    type = pType;
  }

  @Override
  public Type getType() {
    return type;
  }

  public SymbolicExpression getOperand() {
    return operand;
  }

  @Override
  public boolean isTrivial() {
    return operand.isTrivial();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UnarySymbolicExpression that = (UnarySymbolicExpression) o;

    return super.equals(that)
        && operand.equals(that.operand) && type.equals(that.type);
  }

  @Override
  public int hashCode() {
    return super.hashCode() + Objects.hash(getClass(), operand, type);
  }

  @Override
  public String getRepresentation() {
    if (getRepresentedLocation().isPresent()) {
      return getRepresentedLocation().get().toString();

    } else {
      return getOperationString() + operand.getRepresentation();
    }
  }

  @Override
  public String toString() {
    return getOperationString() + "(" + operand + ")";
  }

  public abstract String getOperationString();
}
