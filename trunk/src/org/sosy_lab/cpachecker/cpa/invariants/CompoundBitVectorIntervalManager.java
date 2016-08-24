/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.invariants;

import com.google.common.base.Preconditions;

import java.math.BigInteger;
import java.util.Objects;


public class CompoundBitVectorIntervalManager implements CompoundIntervalManager {

  private final BitVectorInfo info;

  private final boolean allowSignedWrapAround;

  private final OverflowEventHandler overflowEventHandler;

  public CompoundBitVectorIntervalManager(BitVectorInfo pInfo, boolean pAllowSignedWrapAround, OverflowEventHandler pOverflowEventHandler) {
    Preconditions.checkNotNull(pInfo);
    this.info = pInfo;
    this.allowSignedWrapAround = pAllowSignedWrapAround;
    this.overflowEventHandler = pOverflowEventHandler;
  }

  @Override
  public int hashCode() {
    return Objects.hash(info, allowSignedWrapAround);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof CompoundBitVectorIntervalManager) {
      CompoundBitVectorIntervalManager other = (CompoundBitVectorIntervalManager) pOther;
      return allowSignedWrapAround == other.allowSignedWrapAround
          && info.equals(other.info);
    }
    return false;
  }

  @Override
  public CompoundInterval allPossibleValues() {
    return CompoundBitVectorInterval.of(info.getRange());
  }

  @Override
  public CompoundInterval bottom() {
    return CompoundBitVectorInterval.bottom(info);
  }

  @Override
  public CompoundInterval logicalFalse() {
    return CompoundBitVectorInterval.logicalFalse(info);
  }

  @Override
  public CompoundInterval logicalTrue() {
    return CompoundBitVectorInterval.logicalTrue(info);
  }

  @Override
  public CompoundInterval union(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.unionWith(operand2);
  }

  @Override
  public boolean contains(CompoundInterval pContainer, CompoundInterval pElement) {
    checkOperands(pContainer, pElement);
    CompoundBitVectorInterval container = (CompoundBitVectorInterval) pContainer;
    CompoundBitVectorInterval element = (CompoundBitVectorInterval) pElement;
    return container.contains(element);
  }

  @Override
  public CompoundInterval lessEqual(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.lessEqual(operand2);
  }

  @Override
  public CompoundInterval lessThan(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.lessThan(operand2);
  }

  @Override
  public CompoundInterval greaterEqual(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.greaterEqual(operand2);
  }

  @Override
  public CompoundInterval greaterThan(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.greaterThan(operand2);
  }

  @Override
  public CompoundInterval singleton(long pValue) {
    return CompoundBitVectorInterval.singleton(info, pValue);
  }

  @Override
  public CompoundInterval singleton(BigInteger pValue) {
    return CompoundBitVectorInterval.singleton(info, pValue);
  }

  @Override
  public CompoundInterval singleton(Number pValue) {
    if (pValue instanceof BigInteger) {
      return singleton((BigInteger) pValue);
    }
    if (pValue instanceof Long
        || pValue instanceof Integer
        || pValue instanceof Short
        || pValue instanceof Byte) {
      return singleton((long) pValue);
    }
    throw new IllegalArgumentException("Unsupported number: " + pValue);
  }

  @Override
  public CompoundInterval castedSingleton(BigInteger pValue) {
    return CompoundBitVectorInterval.cast(info, pValue, pValue, allowSignedWrapAround, OverflowEventHandler.EMPTY);
  }

  @Override
  public CompoundInterval fromBoolean(boolean pValue) {
    return CompoundBitVectorInterval.fromBoolean(info, pValue);
  }

  @Override
  public CompoundInterval intersect(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.intersectWith(operand2);
  }

  @Override
  public CompoundInterval modulo(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.modulo(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval add(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.add(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval logicalEquals(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.logicalEquals(operand2);
  }

  @Override
  public CompoundInterval binaryAnd(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.binaryAnd(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval binaryOr(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.binaryOr(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval binaryXor(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.binaryXor(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval binaryNot(CompoundInterval pOperand) {
    checkOperand(pOperand);
    CompoundBitVectorInterval operand = (CompoundBitVectorInterval) pOperand;
    return operand.binaryNot(allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval divide(CompoundInterval pNumerator, CompoundInterval pDenominator) {
    checkOperands(pNumerator, pDenominator);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pNumerator;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pDenominator;
    return operand1.divide(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval multiply(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.multiply(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval shiftLeft(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.shiftLeft(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval shiftRight(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.shiftRight(operand2, allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public boolean doIntersect(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return operand1.intersectsWith(operand2);
  }

  @Override
  public CompoundInterval span(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperands(pOperand1, pOperand2);
    CompoundBitVectorInterval operand1 = (CompoundBitVectorInterval) pOperand1;
    CompoundBitVectorInterval operand2 = (CompoundBitVectorInterval) pOperand2;
    return CompoundBitVectorInterval.span(operand1, operand2);
  }

  @Override
  public CompoundInterval negate(CompoundInterval pToNegate) {
    checkOperand(pToNegate);
    return ((CompoundBitVectorInterval) pToNegate).negate(allowSignedWrapAround, overflowEventHandler);
  }

  @Override
  public CompoundInterval cast(TypeInfo pInfo, CompoundInterval pToCast) {
    if (!(pInfo instanceof BitVectorInfo)) {
      throw new IllegalArgumentException(
          "Unsupported target type: Not a compound bit vector interval.");
    }
    if (pToCast instanceof CompoundBitVectorInterval) {
      return ((CompoundBitVectorInterval) pToCast)
          .cast((BitVectorInfo) pInfo, allowSignedWrapAround, OverflowEventHandler.EMPTY);
    }
    // TODO be more precise
    return allPossibleValues();
  }

  private static void checkOperand(CompoundInterval pOperand) {
    if (!(pOperand instanceof CompoundBitVectorInterval)) {
      throw new IllegalArgumentException("Operand is not a compound bit vector interval.");
    }
  }

  private static void checkOperands(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    checkOperand(pOperand1);
    checkOperand(pOperand2);
  }

}
