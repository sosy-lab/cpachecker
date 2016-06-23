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
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * {@link SymbolicExpression} that represents a single constant value of a specific type.
 */
public class ConstantSymbolicExpression extends SymbolicExpression {

  private static final long serialVersionUID = 8720056661933193765L;

  private final Value value;
  private final Type type;

  /**
   * Create a new <code>ConstantSymbolicExpression</code> object with the given value and type.
   *
   * @param pValue the value of the new object
   * @param pType the type of the value of the new object
   */
  protected ConstantSymbolicExpression(Value pValue, Type pType) {
    value = pValue;
    type = pType;
  }

  /**
   * Create a new <code>ConstantSymbolicExpression</code> object with the given value and type
   * representing the given memory location.
   *
   * @param pValue the value of the new object
   * @param pType the type of the value of the new object
   * @param pRepresentedLocation the memory location this symbolic expression represents
   */
  protected ConstantSymbolicExpression(
      final Value pValue,
      final Type pType,
      final MemoryLocation pRepresentedLocation
  ) {
    super(pRepresentedLocation);
    value = pValue;
    type = pType;
  }

  @Override
  public SymbolicExpression copyForLocation(MemoryLocation pRepresentedLocation) {
    return new ConstantSymbolicExpression(value, type, pRepresentedLocation);
  }

  @Override
  public String getRepresentation() {
    if (getRepresentedLocation().isPresent()) {
      return getRepresentedLocation().get().toString();
    } else {
      return toString();
    }
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  public Value getValue() {
    return value;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public boolean isTrivial() {
    return !(value instanceof SymbolicValue);
  }

  @Override
  public String toString() {
    return "SymEx[" + value.toString() + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConstantSymbolicExpression that = (ConstantSymbolicExpression) o;

    return super.equals(o)
        && Objects.equals(type, that.type) && Objects.equals(value, that.value);

  }

  @Override
  public int hashCode() {
    int result = value.hashCode();
    result = type != null ? 31 * result + type.hashCode()
        : result;
    return result;
  }
}
