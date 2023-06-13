// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
   * @return a compound interval for the given value.
   * @throws IllegalArgumentException if the given value does not fit into the maximum interval
   *     range.
   */
  CompoundInterval singleton(long pValue);

  /**
   * Gets a compound interval for the given value.
   *
   * @param pValue the value to convert to a compound interval.
   * @return a compound interval for the given value.
   * @throws IllegalArgumentException if the given value does not fit into the maximum interval
   *     range.
   */
  CompoundInterval singleton(BigInteger pValue);

  /**
   * Gets a compound interval for the given value.
   *
   * @param pValue the value to convert to a compound interval.
   * @return a compound interval for the given value.
   * @throws IllegalArgumentException if the given value does not fit into the maximum interval
   *     range.
   */
  CompoundInterval singleton(Number pValue);

  /**
   * Gets a compound interval for the given value. If the given value does not fit into the maximum
   * interval range, this function will apply a cast to fit the value into the range.
   *
   * @param pValue the value to convert to a compound interval.
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
