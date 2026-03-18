// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.mutex.MutexLock.MutexLockType;

/**
 * Utility class for detecting and extracting information from mutex-related C function calls.
 * Supports both POSIX pthread mutex functions and C11 threading mutex functions.
 */
public final class MutexFunctions {

  private static final ImmutableMap<String, MutexLockType> LOCK_FUNCTIONS =
      ImmutableMap.of(
          "pthread_mutex_lock", MutexLockType.WRITE,
          "mtx_lock", MutexLockType.WRITE
      );

  private static final ImmutableMap<String, MutexLockType> UNLOCK_FUNCTIONS =
      ImmutableMap.of(
          "pthread_mutex_unlock", MutexLockType.WRITE,
          "mtx_unlock", MutexLockType.WRITE
      );

  private static final ImmutableSet<String> INIT_FUNCTIONS =
      ImmutableSet.of("pthread_mutex_init", "mtx_init");

  private static final ImmutableSet<String> DESTROY_FUNCTIONS =
      ImmutableSet.of("pthread_mutex_destroy", "mtx_destroy");

  private static final String ATOMIC_BEGIN = "__VERIFIER_atomic_begin";
  private static final String ATOMIC_END = "__VERIFIER_atomic_end";

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
    return getLockMutex(edge) != null;
  }

  /** Returns {@code true} if the given CFA edge is a mutex unlock function call. */
  public static boolean isUnlockCall(CFAEdge edge) {
    return getUnlockMutex(edge) != null;
  }

  /**
   * If the given CFA edge is a mutex lock call, returns the mutex variable name; otherwise returns
   * {@code null}.
   */
  public static MutexLock getLockMutex(CFAEdge edge) {
    return getMutexLockForFunctionSet(edge, LOCK_FUNCTIONS);
  }

  /**
   * If the given CFA edge is a mutex unlock call, returns the mutex variable name; otherwise
   * returns {@code null}.
   */
  public static MutexLock getUnlockMutex(CFAEdge edge) {
    return getMutexLockForFunctionSet(edge, UNLOCK_FUNCTIONS);
  }

  /** Returns {@code true} if the given function name is a mutex init function. */
  public static boolean isInitFunction(String functionName) {
    return INIT_FUNCTIONS.contains(functionName);
  }

  /** Returns {@code true} if the given function name is a mutex destroy function. */
  public static boolean isDestroyFunction(String functionName) {
    return DESTROY_FUNCTIONS.contains(functionName);
  }

  /** Returns {@code true} if the given function name is {@code __VERIFIER_atomic_begin}. */
  public static boolean isAtomicBegin(String functionName) {
    return ATOMIC_BEGIN.equals(functionName);
  }

  /** Returns {@code true} if the given function name is {@code __VERIFIER_atomic_end}. */
  public static boolean isAtomicEnd(String functionName) {
    return ATOMIC_END.equals(functionName);
  }

  /** Returns {@code true} if the CFA edge is a {@code __VERIFIER_atomic_begin} call. */
  public static boolean isAtomicBeginCall(CFAEdge edge) {
    String name = getFunctionCallName(edge);
    return isAtomicBegin(name);
  }

  /** Returns {@code true} if the CFA edge is a {@code __VERIFIER_atomic_end} call. */
  public static boolean isAtomicEndCall(CFAEdge edge) {
    String name = getFunctionCallName(edge);
    return isAtomicEnd(name);
  }

  /**
   * Extracts the function name from a CFA edge if it is a function call statement, or returns
   * {@code null}.
   */
  public static String getFunctionCallName(CFAEdge edge) {
    if (edge instanceof AStatementEdge sEdge
        && sEdge.getStatement() instanceof AFunctionCall funcCall) {
      AExpression funcNameExpr =
          funcCall.getFunctionCallExpression().getFunctionNameExpression();
      if (funcNameExpr instanceof AIdExpression funcName) {
        return funcName.getName();
      }
    }
    return null;
  }

  private static MutexLock getMutexLockForFunctionSet(
      CFAEdge edge, ImmutableMap<String, MutexLockType> functions) {
    if (edge instanceof AStatementEdge sEdge
        && sEdge.getStatement() instanceof AFunctionCall funcCall) {
      AExpression funcNameExpr =
          funcCall.getFunctionCallExpression().getFunctionNameExpression();
      if (funcNameExpr instanceof AIdExpression funcName) {
        MutexLockType lockType = functions.get(funcName.getName());
        if (lockType != null) {
          var params = funcCall.getFunctionCallExpression().getParameterExpressions();
          if (!params.isEmpty()) {
            return new MutexLock(extractMutexName(params.getFirst()), lockType);
          }
        }
      }
    }
    return null;
  }
}
