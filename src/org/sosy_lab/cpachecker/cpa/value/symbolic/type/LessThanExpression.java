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
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * {@link BinarySymbolicExpression} representing the 'less than' operation.
 */
public class LessThanExpression extends BinarySymbolicExpression implements BinaryConstraint {

  private static final long serialVersionUID = -711307360731984235L;

  protected LessThanExpression(SymbolicExpression pOperand1, SymbolicExpression pOperand2,
      Type pExpressionType, Type pCalculationType) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType);
  }

  protected LessThanExpression(
      final SymbolicExpression pOperand1,
      final SymbolicExpression pOperand2,
      final Type pExpressionType,
      final Type pCalculationType,
      final MemoryLocation pRepresentedLocation
  ) {
    super(pOperand1, pOperand2, pExpressionType, pCalculationType, pRepresentedLocation);
  }

  @Override
  public LessThanExpression copyForLocation(final MemoryLocation pRepresentedLocation) {
    return new LessThanExpression(getOperand1(), getOperand2(), getType(), getCalculationType(),
        pRepresentedLocation);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String getOperationString() {
    return "<";
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
