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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
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

  private static final String ATOMIC_BEGIN = "__VERIFIER_atomic_begin";
  private static final String ATOMIC_END = "__VERIFIER_atomic_end";

  private MutexFunctions() {}

  /**
   * Extracts a canonical key for the storage location a mutex-function argument expression
   * addresses. Handles both {@code &lvalue} (address-of) and plain {@code lvalue} (already a
   * pointer) argument styles.
   *
   * <p>Beyond a plain variable, this also resolves array elements and struct fields reached through
   * a chain of <b>literal</b> array indices and <b>non-pointer</b> field accesses (e.g. {@code
   * &arr[0].field}), using the same canonical-key scheme as {@code
   * ThreadFunctions#canonicalHandleLvalueKey} in {@code cpa.por} (duplicated here rather than
   * shared: {@code cpa.por} already depends on {@code cpa.mutex}, so importing the other way would
   * create a package cycle). Such a path denotes the same storage location on every evaluation, so
   * two occurrences with the same key are provably the same mutex without needing runtime
   * information. A path that goes through a runtime-computed index (a loop variable, say) or a
   * pointer dereference (e.g. {@code s->mutex}, which could alias in ways this syntactic check
   * cannot rule out) returns {@code null}: the mutex cannot be statically identified.
   *
   * <p>Callers must treat a {@code null} result as "this is not a resolvable mutex operation" and
   * must NOT build a {@link MutexLock} with a null handle (see {@link
   * #getMutexLockForFunctionSet}): the sound fallback for an unresolvable mutex expression is to
   * not model the operation at all, so POR/OC lose some reduction power on this lock but never
   * unsoundly prune a real interleaving.
   *
   * @return a canonical key for the mutex's storage location, or {@code null} if the expression is
   *     not a recognized/statically-resolvable pattern
   */
  public static @Nullable String extractMutexName(AExpression expr) {
    if (!(expr instanceof CExpression cExpr)) {
      return null;
    }
    CExpression unwrapped = unwrapCasts(cExpr);
    if (unwrapped instanceof CUnaryExpression unary && unary.getOperator() == UnaryOperator.AMPER) {
      CExpression operand = unwrapCasts(unary.getOperand());
      return operand instanceof CLeftHandSide lvalue ? canonicalLvalueKey(lvalue) : null;
    }
    return unwrapped instanceof CLeftHandSide lvalue ? canonicalLvalueKey(lvalue) : null;
  }

  /**
   * Canonical-key computation for lvalues whose storage location is statically known. Mirrors
   * {@code ThreadFunctions#canonicalLvalueKey} in {@code cpa.por} exactly (see that method's
   * javadoc for the full rationale); keep the two in sync if either changes.
   */
  private static @Nullable String canonicalLvalueKey(CLeftHandSide lvalue) {
    if (lvalue instanceof CIdExpression id) {
      return id.getDeclaration().getQualifiedName();
    }
    if (lvalue instanceof CArraySubscriptExpression subscript
        && unwrapCasts(subscript.getArrayExpression()) instanceof CLeftHandSide array
        && subscript.getSubscriptExpression() instanceof CIntegerLiteralExpression literal) {
      String arrayKey = canonicalLvalueKey(array);
      return arrayKey == null ? null : arrayKey + "[" + literal.getValue() + "]";
    }
    if (lvalue instanceof CFieldReference field
        && !field.isPointerDereference()
        && unwrapCasts(field.getFieldOwner()) instanceof CLeftHandSide owner) {
      String ownerKey = canonicalLvalueKey(owner);
      return ownerKey == null ? null : ownerKey + "." + field.getFieldName();
    }
    return null;
  }

  /** Strips any (possibly nested) {@link CCastExpression} wrapper(s) around {@code expr}. */
  private static CExpression unwrapCasts(CExpression expr) {
    CExpression current = expr;
    while (current instanceof CCastExpression cast) {
      current = cast.getOperand();
    }
    return current;
  }

  /** Returns {@code true} if the given CFA edge is a mutex lock function call. */
  public static boolean isLockCall(CFAEdge edge) {
    return getLockMutex(edge) != null || isAtomicBeginCall(edge);
  }

  /** Returns {@code true} if the given CFA edge is a mutex unlock function call. */
  public static boolean isUnlockCall(CFAEdge edge) {
    return getUnlockMutex(edge) != null || isAtomicEndCall(edge);
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
      AExpression funcNameExpr = funcCall.getFunctionCallExpression().getFunctionNameExpression();
      if (funcNameExpr instanceof AIdExpression funcName) {
        return funcName.getName();
      }
    }
    return null;
  }

  private static @Nullable MutexLock getMutexLockForFunctionSet(
      CFAEdge edge, ImmutableMap<String, MutexLockType> functions) {
    if (edge instanceof AStatementEdge sEdge
        && sEdge.getStatement() instanceof AFunctionCall funcCall) {
      AExpression funcNameExpr = funcCall.getFunctionCallExpression().getFunctionNameExpression();
      if (funcNameExpr instanceof AIdExpression funcName) {
        MutexLockType lockType = functions.get(funcName.getName());
        if (lockType != null) {
          var params = funcCall.getFunctionCallExpression().getParameterExpressions();
          if (!params.isEmpty()) {
            String handle = extractMutexName(params.getFirst());
            if (handle == null) {
              // Sound fallback: the mutex expression's storage location could not be resolved
              // statically (e.g. a symbolic array index, or a pointer dereference which could
              // alias). Rather than building a MutexLock with a null handle, this edge is simply
              // not modelled as a mutex operation at all. That costs some reduction power (POR/OC
              // explore more interleavings than strictly necessary around this lock) but can
              // never prune away a real interleaving.
              return null;
            }
            return new MutexLock(handle, lockType);
          }
        }
      }
    }
    return null;
  }
}
