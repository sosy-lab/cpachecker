/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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

import com.ibm.icu.math.BigDecimal;

import java.math.BigInteger;


public class CompoundFloatingPointIntervalManager implements CompoundIntervalManager {

  private final FloatingPointTypeInfo typeInfo;

  public CompoundFloatingPointIntervalManager(FloatingPointTypeInfo pTypeInfo) {
    this.typeInfo = pTypeInfo;
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
    return new CompoundFloatingPointInterval(((CompoundFloatingPointInterval) pOperand1).getTypeInfo());
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
