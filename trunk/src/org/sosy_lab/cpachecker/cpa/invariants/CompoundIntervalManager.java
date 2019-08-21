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

import java.math.BigInteger;


public interface CompoundIntervalManager {

  CompoundInterval allPossibleValues();

  CompoundInterval bottom();

  CompoundInterval logicalFalse();

  CompoundInterval logicalTrue();

  CompoundInterval union(CompoundInterval pOperand1, CompoundInterval pOperand2);

  CompoundInterval intersect(CompoundInterval pOperand1, CompoundInterval pOperand2);

  boolean doIntersect(CompoundInterval pEvaluate, CompoundInterval pParameter);

  boolean contains(CompoundInterval pContainer, CompoundInterval pElement);

  CompoundInterval lessEqual(CompoundInterval pOperand1, CompoundInterval pOperand2);

  CompoundInterval lessThan(CompoundInterval pOperand1, CompoundInterval pOperand2);

  CompoundInterval greaterEqual(CompoundInterval pOperand1, CompoundInterval pOperand2);

  CompoundInterval greaterThan(CompoundInterval pOperand1, CompoundInterval pOperand2);

  /**
   * Gets a compound interval for the given value.
   *
   * @param pValue the value to convert to a compound interval.
   *
   * @return a compound interval for the given value.
   * @throws IllegalArgumentException if the given value does not fit into the
   * maximum interval range.
   */
  CompoundInterval singleton(long pValue);

  /**
   * Gets a compound interval for the given value.
   *
   * @param pValue the value to convert to a compound interval.
   *
   * @return a compound interval for the given value.
   * @throws IllegalArgumentException if the given value does not fit into the
   * maximum interval range.
   */
  CompoundInterval singleton(BigInteger pValue);

  /**
   * Gets a compound interval for the given value.
   *
   * @param pValue the value to convert to a compound interval.
   *
   * @return a compound interval for the given value.
   * @throws IllegalArgumentException if the given value does not fit into the
   * maximum interval range.
   */
  CompoundInterval singleton(Number pValue);

  /**
   * Gets a compound interval for the given value.
   * If the given value does not fit into the maximum interval range,
   * this function will apply a cast to fit the value into the range.
   *
   * @param pValue the value to convert to a compound interval.
   *
   * @return a compound interval for the given value.
   */
  CompoundInterval castedSingleton(BigInteger pValue);

  CompoundInterval fromBoolean(boolean pValue);

  CompoundInterval add(CompoundInterval pOperand1, CompoundInterval pOperand2);

  CompoundInterval modulo(CompoundInterval pOperand1, CompoundInterval pOperand2);

  CompoundInterval logicalEquals(CompoundInterval pOperand1, CompoundInterval pOperand2);

  CompoundInterval binaryAnd(CompoundInterval pOperand1, CompoundInterval pOperand2);

  CompoundInterval binaryOr(CompoundInterval pOperand1, CompoundInterval pOperand2);

  CompoundInterval binaryXor(CompoundInterval pOperand1, CompoundInterval pOperand2);

  CompoundInterval binaryNot(CompoundInterval pOperand);

  CompoundInterval divide(CompoundInterval pNumerator, CompoundInterval pDenominator);

  CompoundInterval multiply(CompoundInterval pOperand1, CompoundInterval pOperand2);

  CompoundInterval shiftLeft(CompoundInterval pOperand1, CompoundInterval pOperand2);

  CompoundInterval shiftRight(CompoundInterval pOperand1, CompoundInterval pOperand2);

  CompoundInterval span(CompoundInterval pBorderA, CompoundInterval pBorderB);

  CompoundInterval negate(CompoundInterval pToNegate);

  CompoundInterval cast(TypeInfo pTypeInfo, CompoundInterval pToCast);

}
