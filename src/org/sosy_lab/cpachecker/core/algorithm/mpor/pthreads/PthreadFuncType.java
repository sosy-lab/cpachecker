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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

public enum PthreadFuncType {
  // TODO decide which of these are actually relevant for us
  // TODO create barrier logic, see e.g. pthread-divine/barrier_2t.i

  BARRIER_INIT("pthread_barrier_init", Optional.empty(), Optional.empty(), Optional.empty()),
  BARRIER_WAIT("pthread_barrier_wait", Optional.empty(), Optional.empty(), Optional.empty()),
  CANCEL("pthread_cancel", Optional.of(0), Optional.of(false), Optional.empty()),
  DETACH("pthread_detach", Optional.of(0), Optional.of(false), Optional.empty()),
  EXIT("pthread_exit", Optional.empty(), Optional.empty(), Optional.empty()),
  PTHREAD_CREATE("pthread_create", Optional.of(0), Optional.of(true), Optional.empty()),
  PTHREAD_JOIN("pthread_join", Optional.of(0), Optional.of(false), Optional.empty()),
  PTHREAD_MUTEX_INIT("pthread_mutex_init", Optional.empty(), Optional.empty(), Optional.of(0)),
  PTHREAD_MUTEX_LOCK("pthread_mutex_lock", Optional.empty(), Optional.empty(), Optional.of(0)),
  PTHREAD_MUTEX_UNLOCK("pthread_mutex_unlock", Optional.empty(), Optional.empty(), Optional.of(0));

  // TODO unsure about yield, mutex_destroy
  //  pthread_mutex_t amutex = PTHREAD_MUTEX_INITIALIZER; // also used instead of mutex init
  //  pthread_barrier stuff
  //  etc. probably a lot more things
  //  __VERIFIER_atomic_begin and _end will be relevant to identify too

  public final String name;

  /** The index of the pthread_t param if present. */
  private final Optional<Integer> pthreadTIndex;

  private final Optional<Boolean> isPthreadTPointer;

  /** The index of the pthread_mutex_t param if present. */
  private final Optional<Integer> pthreadMutexTIndex;

  PthreadFuncType(
      String pName,
      Optional<Integer> pPthreadTIndex,
      Optional<Boolean> pIsPthreadTPointer,
      Optional<Integer> pPthreadMutexTIndex) {
    // pPthreadTIndex and pIsPthreadTPointer have to be equivalent (both empty or both present)
    checkArgument(pPthreadTIndex.isEmpty() || pIsPthreadTPointer.isPresent());
    checkArgument(pIsPthreadTPointer.isEmpty() || pPthreadTIndex.isPresent());
    name = pName;
    pthreadTIndex = pPthreadTIndex;
    isPthreadTPointer = pIsPthreadTPointer;
    pthreadMutexTIndex = pPthreadMutexTIndex;
  }

  public int getPthreadTIndex() {
    checkArgument(pthreadTIndex.isPresent(), "this PthreadFuncType has no pthread_t param");
    return pthreadTIndex.orElseThrow();
  }

  public boolean isPthreadTPointer() {
    checkArgument(pthreadTIndex.isPresent(), "this PthreadFuncType has no pthread_t param");
    return isPthreadTPointer.orElseThrow();
  }

  public int getPthreadMutexTIndex() {
    checkArgument(pthreadMutexTIndex.isPresent(), "this PthreadFuncType has no pthread_t param");
    return pthreadMutexTIndex.orElseThrow();
  }

  /**
   * Tries to extract the CFunctionCallStatement from pCfaEdge and checks if it is a call to
   * pFuncType.
   *
   * @param pCfaEdge the CFAEdge to be analyzed
   * @param pFuncType the desired FunctionType
   * @return true if pCfaEdge is a call to pFuncType
   */
  public static boolean isCallToPthreadFunc(CFAEdge pCfaEdge, PthreadFuncType pFuncType) {
    return CFAUtils.isCfaEdgeCFunctionCallStatement(pCfaEdge)
        && CFAUtils.getFunctionNameFromCfaEdge(pCfaEdge).equals(pFuncType.name);
  }

  public static boolean isCallToAnyPthreadFunc(CFAEdge pCfaEdge) {
    for (PthreadFuncType funcType : PthreadFuncType.values()) {
      if (isCallToPthreadFunc(pCfaEdge, funcType)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isCallToAnyPthreadFuncWithPthreadTParam(CFAEdge pCfaEdge) {
    for (PthreadFuncType funcType : PthreadFuncType.values()) {
      if (funcType.pthreadTIndex.isPresent()) {
        if (isCallToPthreadFunc(pCfaEdge, funcType)) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean isCallToAnyPthreadFuncWithPthreadMutexTParam(CFAEdge pCfaEdge) {
    for (PthreadFuncType funcType : PthreadFuncType.values()) {
      if (funcType.pthreadMutexTIndex.isPresent()) {
        if (isCallToPthreadFunc(pCfaEdge, funcType)) {
          return true;
        }
      }
    }
    return false;
  }

  public static PthreadFuncType getPthreadFuncType(CFAEdge pEdge) {
    checkArgument(CFAUtils.isCfaEdgeCFunctionCallStatement(pEdge));
    String funcName = CFAUtils.getFunctionNameFromCfaEdge(pEdge);
    for (PthreadFuncType funcType : PthreadFuncType.values()) {
      if (funcType.name.equals(funcName)) {
        return funcType;
      }
    }
    throw new IllegalArgumentException("unrecognized pthread method: " + pEdge.getRawAST());
  }
}
