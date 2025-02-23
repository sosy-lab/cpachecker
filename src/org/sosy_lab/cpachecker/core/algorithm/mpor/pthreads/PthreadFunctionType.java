// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * Contains mostly methods from the pthread standard, though we also include e.g. {@code
 * __VERIFIER_atomic_begin} here.
 */
public enum PthreadFunctionType {

  // TODO create barrier logic, see e.g. pthread-divine/barrier_2t.i

  PTHREAD_BARRIER_INIT(
      "pthread_barrier_init",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_BARRIER_WAIT(
      "pthread_barrier_wait",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_CANCEL(
      "pthread_cancel",
      false,
      false,
      Optional.of(0),
      Optional.of(false),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_COND_SIGNAL(
      "pthread_cond_signal",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_COND_WAIT(
      "pthread_cond_wait",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.of(1),
      Optional.empty()),
  PTHREAD_CREATE(
      "pthread_create",
      true,
      true,
      Optional.of(0),
      Optional.of(true),
      Optional.empty(),
      Optional.of(2)),
  PTHREAD_DETACH(
      "pthread_detach",
      false,
      false,
      Optional.of(0),
      Optional.of(false),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_EQUAL(
      "pthread_equal",
      false,
      false,
      // TODO this method has pthread_t at index 1 and 2 -> add list as param later
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_EXIT(
      "pthread_exit",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_JOIN(
      "pthread_join",
      true,
      true,
      Optional.of(0),
      Optional.of(false),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_KILL(
      "pthread_kill",
      false,
      false,
      // TODO actually want Optional.of(0) here, but it causes an ExceptionInInitializerError
      //  -> fix once we support pthread_kill
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_MUTEX_DESTROY(
      "pthread_mutex_destroy",
      true,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.of(0),
      Optional.empty()),
  PTHREAD_MUTEX_INIT(
      "pthread_mutex_init",
      true,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.of(0),
      Optional.empty()),
  PTHREAD_MUTEX_LOCK(
      "pthread_mutex_lock",
      true,
      true,
      Optional.empty(),
      Optional.empty(),
      Optional.of(0),
      Optional.empty()),
  PTHREAD_MUTEX_UNLOCK(
      "pthread_mutex_unlock",
      true,
      true,
      Optional.empty(),
      Optional.empty(),
      Optional.of(0),
      Optional.empty()),
  PTHREAD_ONCE(
      "pthread_once",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_RWLOCK_RDLOCK(
      "pthread_rwlock_rdlock",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_RWLOCK_TRYRDLOCK(
      "pthread_rwlock_tryrdlock",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_RWLOCK_UNLOCK(
      "pthread_rwlock_unlock",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_RWLOCK_WRLOCK(
      "pthread_rwlock_wrlock",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_SELF(
      "pthread_self",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  __VERIFIER_ATOMIC_BEGIN(
      "__VERIFIER_atomic_begin",
      true,
      true,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  __VERIFIER_ATOMIC_END(
      "__VERIFIER_atomic_end",
      true,
      true,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty());

  // TODO unsure about pthread_yield
  //  pthread_mutex_t amutex = PTHREAD_MUTEX_INITIALIZER; // also used instead of mutex init
  //  pthread_barrier stuff
  //  etc. probably a lot more things

  public final String name;

  public final boolean isSupported;

  /**
   * If this function is explicitly handled in the sequentialization, i.e. contains case block code
   * simulating the function's behavior.
   */
  public final boolean isExplicitlyHandled;

  /** The index of the pthread_t param if present. */
  private final Optional<Integer> pthreadTIndex;

  private final Optional<Boolean> isPthreadTPointer;

  /** The index of the pthread_mutex_t param if present. */
  private final Optional<Integer> pthreadMutexTIndex;

  private final Optional<Integer> startRoutineIndex;

  // TODO maybe its best to create a class that states the type of the pthread object, the index /
  //  indices and whether it is never / always / sometimes a pointer
  //  then just create an immutablelist with the desired properties of the function
  PthreadFunctionType(
      String pName,
      boolean pIsSupported,
      boolean pIsExplicitlyHandled,
      Optional<Integer> pPthreadTIndex,
      Optional<Boolean> pIsPthreadTPointer,
      Optional<Integer> pPthreadMutexTIndex,
      Optional<Integer> pStartRoutineIndex) {

    // pPthreadTIndex and pIsPthreadTPointer have to be equivalent (both empty or both present)
    checkArgument(pPthreadTIndex.isEmpty() || pIsPthreadTPointer.isPresent());
    checkArgument(pIsPthreadTPointer.isEmpty() || pPthreadTIndex.isPresent());
    // if the function is not supported, it cannot be explicitly handled
    checkArgument(pIsSupported || !pIsExplicitlyHandled);

    name = pName;
    isSupported = pIsSupported;
    isExplicitlyHandled = pIsExplicitlyHandled;
    pthreadTIndex = pPthreadTIndex;
    isPthreadTPointer = pIsPthreadTPointer;
    pthreadMutexTIndex = pPthreadMutexTIndex;
    startRoutineIndex = pStartRoutineIndex;
  }

  public boolean hasPthreadTIndex() {
    return pthreadTIndex.isPresent();
  }

  public int getPthreadTIndex() {
    checkArgument(pthreadTIndex.isPresent(), "this PthreadFuncType has no pthread_t param");
    return pthreadTIndex.orElseThrow();
  }

  // TODO problem: even if the function declaration states that pthread_t is a pointer, the address
  //  may not necessarily be passed on afaik. need more tests here (same with start_routines)
  public boolean isPthreadTPointer() {
    checkArgument(pthreadTIndex.isPresent(), "this PthreadFuncType has no pthread_t param");
    return isPthreadTPointer.orElseThrow();
  }

  public boolean hasPthreadMutexTIndex() {
    return pthreadMutexTIndex.isPresent();
  }

  public int getPthreadMutexTIndex() {
    checkArgument(
        pthreadMutexTIndex.isPresent(), "this PthreadFuncType has no pthread_mutex_t param");
    return pthreadMutexTIndex.orElseThrow();
  }

  public int getStartRoutineIndex() {
    checkArgument(startRoutineIndex.isPresent(), "this PthreadFuncType has no start_routine param");
    return startRoutineIndex.orElseThrow();
  }

  /**
   * Tries to extract the {@link CFunctionCallStatement} from pEdge and returns true if it is a call
   * to pFuncType.
   */
  public static boolean callsPthreadFunc(CFAEdge pEdge, PthreadFunctionType pFuncType) {
    return CFAUtils.isCfaEdgeCFunctionCall(pEdge)
        && CFAUtils.getFunctionNameFromCfaEdge(pEdge).equals(pFuncType.name);
  }

  public static boolean callsAnyPthreadFunc(CFAEdge pEdge) {
    for (PthreadFunctionType funcType : PthreadFunctionType.values()) {
      if (callsPthreadFunc(pEdge, funcType)) {
        return true;
      }
    }
    return false;
  }

  public static boolean callsAnyPthreadFuncWithPthreadT(CFAEdge pEdge) {
    for (PthreadFunctionType funcType : PthreadFunctionType.values()) {
      if (funcType.pthreadTIndex.isPresent()) {
        if (callsPthreadFunc(pEdge, funcType)) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean callsAnyPthreadFuncWithPthreadMutexT(CFAEdge pEdge) {
    for (PthreadFunctionType funcType : PthreadFunctionType.values()) {
      if (funcType.pthreadMutexTIndex.isPresent()) {
        if (callsPthreadFunc(pEdge, funcType)) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean callsAnyPthreadFuncWithStartRoutine(CFAEdge pEdge) {
    for (PthreadFunctionType funcType : PthreadFunctionType.values()) {
      if (funcType.startRoutineIndex.isPresent()) {
        if (callsPthreadFunc(pEdge, funcType)) {
          return true;
        }
      }
    }
    return false;
  }

  public static PthreadFunctionType getPthreadFuncType(CFAEdge pEdge) {
    checkArgument(CFAUtils.isCfaEdgeCFunctionCall(pEdge));
    String funcName = CFAUtils.getFunctionNameFromCfaEdge(pEdge);
    for (PthreadFunctionType funcType : PthreadFunctionType.values()) {
      if (funcType.name.equals(funcName)) {
        return funcType;
      }
    }
    throw new IllegalArgumentException("unrecognized pthread method: " + pEdge.getRawAST());
  }
}
