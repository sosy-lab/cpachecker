// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;

/**
 * Don't use this because it is convenient! This is for its special use-case only! There are no
 * guarantees that this implementation is not changed or deleted at some point!
 */
public class OverflowFunctionReturnAndCastCalculationResult {

  private final CExpression functionReturn;
  private final CExpression castCalculationResult;

  private OverflowFunctionReturnAndCastCalculationResult(
      CExpression pFunctionReturn, CExpression pCastCalculationResult) {
    functionReturn = checkNotNull(pFunctionReturn);
    checkArgument(
        pFunctionReturn.getExpressionType().getCanonicalType().equals(CNumericTypes.BOOL));
    castCalculationResult = checkNotNull(pCastCalculationResult);
  }

  /**
   * Carries the {@link CExpression}s modeling the boolean result of GCC built-in functions (e.g.
   * {@code bool __builtin_sadd_overflow(int a, int b, int *res)}) in 'functionReturn' and the
   * internal calculation result after casting in 'castCalculationResult' that is assigned to res
   * (i.e. {@code *res = castCalculationResult;}).
   *
   * <p>Don't use this because it is convenient! This is for its special use-case only! There are no
   * guarantees that this class/implementation is not changed or deleted at some point!
   */
  public static OverflowFunctionReturnAndCastCalculationResult of(
      CExpression functionReturn, CExpression castCalculationResult) {
    return new OverflowFunctionReturnAndCastCalculationResult(
        functionReturn, castCalculationResult);
  }

  /**
   * Returns the {@link CExpression} modeling the boolean return value of GCC built-in overflow
   * functions (e.g. {@code bool __builtin_sadd_overflow(int a, int b, int *res)}).
   */
  public CExpression getFunctionReturn() {
    return functionReturn;
  }

  /**
   * Returns the {@link CExpression} modeling the internal calculation result, after casting, that
   * needs to be assigned to the 'res' argument (i.e. {@code *res = getCastCalculationResult();}) of
   * GCC built-in overflow functions (e.g. {@code bool __builtin_sadd_overflow(int a, int b, int
   * *res)}).
   */
  public CExpression getCastCalculationResult() {
    return castCalculationResult;
  }
}
