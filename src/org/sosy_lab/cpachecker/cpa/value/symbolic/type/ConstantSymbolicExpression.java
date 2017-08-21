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
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * {@link SymbolicExpression} that represents a single constant value of a specific type.
 */
public class ConstantSymbolicExpression extends SymbolicExpression {

  private static final long serialVersionUID = 8720056661933193765L;

  private final NumberInterface value;
  private final Type type;

  /**
   * Create a new <code>ConstantSymbolicExpression</code> object with the given value and type.
   *
   * @param pValue the value of the new object
   * @param pType the type of the value of the new object
   */
  protected ConstantSymbolicExpression(NumberInterface pValue, Type pType) {
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
      final NumberInterface pValue,
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

  public NumberInterface getValue() {
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

  @Override
  public NumberInterface EMPTY() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface UNBOUND() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface BOOLEAN_INTERVAL() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface ZERO() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface ONE() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean intersects(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Number getLow() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Number getHigh() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isGreaterThan(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isGreaterOrEqualThan(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public NumberInterface plus(NumberInterface pInterval) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface minus(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface times(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface divide(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface shiftLeft(NumberInterface pOffset) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface shiftRight(NumberInterface pOffset) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface unsignedDivide(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface unsignedModulo(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface unsignedShiftRight(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface modulo(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isUnbound() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public NumberInterface union(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean contains(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isEmpty() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public NumberInterface negate() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface intersect(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface limitUpperBoundBy(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface limitLowerBoundBy(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface asDecimal() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface asInteger() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Number getNumber() {
    // TODO Auto-generated method stub
    return null;
  }
}
