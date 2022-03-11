// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import com.ibm.icu.math.BigDecimal;
import java.math.BigInteger;

class CompoundFloatingPointIntervalManager implements CompoundIntervalManager {

  private final FloatingPointTypeInfo typeInfo;

  public CompoundFloatingPointIntervalManager(FloatingPointTypeInfo pTypeInfo) {
    typeInfo = pTypeInfo;
  }

  @Override
  public CompoundInterval allPossibleValues() {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval bottom() {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval logicalFalse() {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval logicalTrue() {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval union(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval intersect(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(
        ((CompoundFloatingPointInterval) pOperand1).getTypeInfo());
  }

  @Override
  public boolean doIntersect(CompoundInterval pEvaluate, CompoundInterval pParameter) {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public boolean contains(CompoundInterval pContainer, CompoundInterval pElement) {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public CompoundInterval lessEqual(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval lessThan(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval greaterEqual(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval greaterThan(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval singleton(long pValue) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval singleton(BigInteger pValue) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
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
      return singleton(pValue.longValue());
    }
    if (pValue instanceof Float || pValue instanceof Double) {
      // TODO something something pValue.doubleValue();
      return new CompoundFloatingPointInterval(typeInfo);
    }
    if (pValue instanceof BigDecimal) {
      // TODO
      return new CompoundFloatingPointInterval(typeInfo);
    }
    throw new IllegalArgumentException("Unsupported number: " + pValue);
  }

  @Override
  public CompoundInterval castedSingleton(BigInteger pValue) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval fromBoolean(boolean pValue) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval add(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval modulo(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval logicalEquals(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval binaryAnd(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval binaryOr(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval binaryXor(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval binaryNot(CompoundInterval pOperand) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval divide(CompoundInterval pNumerator, CompoundInterval pDenominator) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval multiply(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval shiftLeft(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval shiftRight(CompoundInterval pOperand1, CompoundInterval pOperand2) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval span(CompoundInterval pBorderA, CompoundInterval pBorderB) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval negate(CompoundInterval pToNegate) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }

  @Override
  public CompoundInterval cast(TypeInfo pTypeInfo, CompoundInterval pToCast) {
    // TODO Auto-generated method stub
    return new CompoundFloatingPointInterval(typeInfo);
  }
}
