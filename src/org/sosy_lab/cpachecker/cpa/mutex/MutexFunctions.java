// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.sosy_lab.cpachecker.util.CFAUtils.getFunctionCallName;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cpa.mutex.MutexLock.MutexLockType;

/**
 * Utility class for detecting and extracting information from mutex-related C function calls.
 * Supports both POSIX pthread mutex functions and C11 threading mutex functions.
 */
public final class MutexFunctions {

  private static final ImmutableMap<String, MutexLockType> LOCK_FUNCTIONS =
      ImmutableMap.of(
          "pthread_mutex_lock", MutexLockType.BOTH,
          "mtx_lock", MutexLockType.BOTH,
          "pthread_rwlock_rdlock", MutexLockType.READ,
          "pthread_rwlock_wrlock", MutexLockType.WRITE);

  private static final ImmutableMap<String, MutexLockType> UNLOCK_FUNCTIONS =
      ImmutableMap.of(
          "pthread_mutex_unlock", MutexLockType.BOTH,
          "mtx_unlock", MutexLockType.BOTH,
          "pthread_rwlock_unlock", MutexLockType.BOTH);

  /**
   * Non-blocking / timed lock-acquisition functions. Each either acquires the lock and returns 0,
   * or fails to acquire it (lock busy, or timeout) and returns a non-zero error code. Modelling
   * them requires branching on that outcome, which is why they are kept separate from {@link
   * #LOCK_FUNCTIONS} (whose members unconditionally acquire).
   */
  private static final ImmutableMap<String, MutexLockType> TRYLOCK_FUNCTIONS =
      ImmutableMap.of(
          "pthread_mutex_trylock", MutexLockType.BOTH,
          "pthread_mutex_timedlock", MutexLockType.BOTH,
          "mtx_trylock", MutexLockType.BOTH,
          "mtx_timedlock", MutexLockType.BOTH,
          "pthread_rwlock_tryrdlock", MutexLockType.READ,
          "pthread_rwlock_timedrdlock", MutexLockType.READ,
          "pthread_rwlock_trywrlock", MutexLockType.WRITE,
          "pthread_rwlock_timedwrlock", MutexLockType.WRITE);

  private static final ImmutableSet<String> INIT_FUNCTIONS =
      ImmutableSet.of("pthread_mutex_init", "mtx_init");

  private static final ImmutableSet<String> DESTROY_FUNCTIONS =
      ImmutableSet.of("pthread_mutex_destroy", "mtx_destroy");

  private static final String ATOMIC_PREFIX = "__VERIFIER_atomic";
  private static final String ATOMIC_BEGIN = "__VERIFIER_atomic_begin";
  private static final String ATOMIC_END = "__VERIFIER_atomic_end";

  private MutexFunctions() {}

  public static Optional<String> extractMutexName(AExpression expr) {
    if (expr instanceof CUnaryExpression unary
        && unary.getOperator() == UnaryOperator.AMPER
        && unary.getOperand() instanceof CIdExpression id) {
      return Optional.of(id.getName());
    }
    if (expr instanceof CIdExpression id) {
      return Optional.of(id.getName());
    }
    return Optional.empty();
  }

  /** Returns {@code true} if the given CFA edge is a mutex lock function call. */
  public static boolean isLockCall(CFAEdge edge) {
    return getLockMutex(edge).isPresent() || isAtomicBegin(edge);
  }

  /** Returns {@code true} if the given CFA edge is a mutex unlock function call. */
  public static boolean isUnlockCall(CFAEdge edge) {
    return getUnlockMutex(edge).isPresent() || isAtomicEnd(edge);
  }

  /**
   * If the given CFA edge is a mutex lock call, returns the mutex variable name; otherwise returns
   * {@code null}.
   */
  public static Optional<MutexLock> getLockMutex(CFAEdge edge) {
    return getMutexLockForFunctionSet(edge, LOCK_FUNCTIONS);
  }

  /**
   * If the given CFA edge is a mutex unlock call, returns the mutex variable name; otherwise
   * returns {@code null}.
   */
  public static Optional<MutexLock> getUnlockMutex(CFAEdge edge) {
    return getMutexLockForFunctionSet(edge, UNLOCK_FUNCTIONS);
  }

  /**
   * Returns {@code true} if the given function name is a mutex/rwlock lock function (regardless of
   * whether its mutex argument can be resolved to a concrete object).
   */
  public static boolean isLockFunction(String functionName) {
    return LOCK_FUNCTIONS.containsKey(functionName);
  }

  /**
   * Returns {@code true} if the given function name is a mutex/rwlock unlock function (regardless
   * of whether its mutex argument can be resolved to a concrete object).
   */
  public static boolean isUnlockFunction(String functionName) {
    return UNLOCK_FUNCTIONS.containsKey(functionName);
  }

  /**
   * Returns {@code true} if the given lock function name takes a <em>read</em> (shared) lock, such
   * as {@code pthread_rwlock_rdlock}; read-locked sections of one lock may overlap each other.
   */
  public static boolean isReadLockFunction(String functionName) {
    return LOCK_FUNCTIONS.get(functionName) == MutexLock.MutexLockType.READ;
  }

  /**
   * Returns {@code true} if the given function name is a non-blocking/timed lock-acquisition
   * function (e.g. {@code pthread_mutex_trylock}), which acquires the lock only when it returns 0.
   */
  public static boolean isTrylockFunction(String functionName) {
    return TRYLOCK_FUNCTIONS.containsKey(functionName);
  }

  /**
   * Returns {@code true} if the given try/timed lock-acquisition function takes a <em>read</em>
   * (shared) lock, such as {@code pthread_rwlock_tryrdlock}.
   */
  public static boolean isReadTrylockFunction(String functionName) {
    return TRYLOCK_FUNCTIONS.get(functionName) == MutexLock.MutexLockType.READ;
  }

  /** Returns {@code true} if the given function name is a mutex init function. */
  public static boolean isInitFunction(String functionName) {
    return INIT_FUNCTIONS.contains(functionName);
  }

  /** Returns {@code true} if the given function name is a mutex destroy function. */
  public static boolean isDestroyFunction(String functionName) {
    return DESTROY_FUNCTIONS.contains(functionName);
  }

  /** Returns {@code true} if the CFA edge is a {@code __VERIFIER_atomic_begin} call. */
  public static boolean isAtomicBegin(CFAEdge edge) {
    Optional<String> name = getFunctionCallName(edge);
    return ATOMIC_BEGIN.equals(name.orElse(null))
        || (edge.getPredecessor() instanceof FunctionEntryNode
            && edge.getPredecessor().getFunctionName().startsWith(ATOMIC_PREFIX));
  }

  /** Returns {@code true} if the CFA edge is a {@code __VERIFIER_atomic_end} call. */
  public static boolean isAtomicEnd(CFAEdge edge) {
    Optional<String> name = getFunctionCallName(edge);
    return ATOMIC_END.equals(name.orElse(null))
        || (edge.getSuccessor() instanceof FunctionExitNode
            && edge.getPredecessor().getFunctionName().startsWith(ATOMIC_PREFIX));
  }

  private static Optional<MutexLock> getMutexLockForFunctionSet(
      CFAEdge edge, ImmutableMap<String, MutexLockType> functions) {
    if (edge instanceof AStatementEdge sEdge
        && sEdge.getStatement() instanceof AFunctionCall funcCall) {
      AExpression funcNameExpr = funcCall.getFunctionCallExpression().getFunctionNameExpression();
      if (funcNameExpr instanceof AIdExpression funcName) {
        MutexLockType lockType = functions.get(funcName.getName());
        if (lockType != null) {
          var params = funcCall.getFunctionCallExpression().getParameterExpressions();
          if (!params.isEmpty()) {
            Optional<String> handle = extractMutexName(params.getFirst());
            if (handle.isEmpty()) {
              throw new UnsupportedOperationException("Cannot statically determine mutex handle.");
            }
            return of(new MutexLock(handle.get(), lockType));
          }
        }
      }
    }
    return empty();
  }
}
