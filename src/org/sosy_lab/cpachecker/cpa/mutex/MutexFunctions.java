// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Utility class for detecting and extracting information from mutex-related C function calls.
 * Supports both POSIX pthread mutex functions and C11 threading mutex functions.
 */
public final class MutexFunctions {

  private static final ImmutableSet<String> LOCK_FUNCTIONS =
      ImmutableSet.of("pthread_mutex_lock", "mtx_lock");

  private static final ImmutableSet<String> UNLOCK_FUNCTIONS =
      ImmutableSet.of("pthread_mutex_unlock", "mtx_unlock");

  private static final ImmutableSet<String> INIT_FUNCTIONS =
      ImmutableSet.of("pthread_mutex_init", "mtx_init");

  private static final ImmutableSet<String> DESTROY_FUNCTIONS =
      ImmutableSet.of("pthread_mutex_destroy", "mtx_destroy");

  private MutexFunctions() {}

  /**
   * Extracts the mutex variable name from a function argument expression. Handles both {@code
   * &mutex} (address-of) and plain {@code mutex} (pointer) argument styles.
   *
   * @return the mutex variable name, or {@code null} if the expression is not a recognized pattern
   */
  public static String extractMutexName(AExpression expr) {
    if (expr instanceof CUnaryExpression unary
        && unary.getOperator() == UnaryOperator.AMPER
        && unary.getOperand() instanceof CIdExpression id) {
      return id.getName();
    }
    if (expr instanceof CIdExpression id) {
      return id.getName();
    }
    return null;
  }

  /** Returns {@code true} if the given CFA edge is a mutex lock function call. */
  public static boolean isLockCall(CFAEdge edge) {
    return getLockMutexName(edge) != null;
  }

  /**
   * If the given CFA edge is a mutex lock call, returns the mutex variable name; otherwise returns
   * {@code null}.
   */
  public static String getLockMutexName(CFAEdge edge) {
    return getMutexNameForFunctionSet(edge, LOCK_FUNCTIONS);
  }

  /** Returns {@code true} if the given function name is a mutex lock function. */
  public static boolean isLockFunction(String functionName) {
    return LOCK_FUNCTIONS.contains(functionName);
  }

  /** Returns {@code true} if the given function name is a mutex unlock function. */
  public static boolean isUnlockFunction(String functionName) {
    return UNLOCK_FUNCTIONS.contains(functionName);
  }

  /** Returns {@code true} if the given function name is a mutex init function. */
  public static boolean isInitFunction(String functionName) {
    return INIT_FUNCTIONS.contains(functionName);
  }

  /** Returns {@code true} if the given function name is a mutex destroy function. */
  public static boolean isDestroyFunction(String functionName) {
    return DESTROY_FUNCTIONS.contains(functionName);
  }

  private static String getMutexNameForFunctionSet(
      CFAEdge edge, ImmutableSet<String> functionNames) {
    if (edge instanceof AStatementEdge sEdge
        && sEdge.getStatement() instanceof AFunctionCall funcCall) {
      AExpression funcNameExpr =
          funcCall.getFunctionCallExpression().getFunctionNameExpression();
      if (funcNameExpr instanceof AIdExpression funcName
          && functionNames.contains(funcName.getName())) {
        var params = funcCall.getFunctionCallExpression().getParameterExpressions();
        if (!params.isEmpty()) {
          return extractMutexName(params.get(0));
        }
      }
    }
    return null;
  }
}
