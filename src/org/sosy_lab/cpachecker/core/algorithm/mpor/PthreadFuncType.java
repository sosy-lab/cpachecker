// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

public enum PthreadFuncType {
  // TODO decide which of these are actually relevant for us
  // TODO create barrier logic, see e.g. pthread-divine/barrier_2t.i

  BARRIER_INIT("pthread_barrier_init"),
  BARRIER_WAIT("pthread_barrier_wait"),
  CANCEL("pthread_cancel", Optional.of(0), Optional.of(false)),
  DETACH("pthread_detach", Optional.of(0), Optional.of(false)),
  EXIT("pthread_exit"),
  PTHREAD_CREATE("pthread_create", Optional.of(0), Optional.of(true)),
  PTHREAD_JOIN("pthread_join", Optional.of(0), Optional.of(false)),
  PTHREAD_MUTEX_INIT("pthread_mutex_init"),
  PTHREAD_MUTEX_LOCK("pthread_mutex_lock"),
  PTHREAD_MUTEX_UNLOCK("pthread_mutex_unlock");

  // TODO unsure about yield, mutex_destroy
  //  pthread_mutex_t amutex = PTHREAD_MUTEX_INITIALIZER; // also used instead of mutex init
  //  pthread_barrier stuff
  //  etc. probably a lot more things
  //  __VERIFIER_atomic_begin and _end will be relevant to identify too

  public final String name;

  /** The index of the pthread_t parameter, can be empty. */
  public final Optional<Integer> pthreadTIndex;

  public final Optional<Boolean> isPthreadTPointer;

  PthreadFuncType(
      String pName, Optional<Integer> pPthreadTIndex, Optional<Boolean> pIsPthreadTPointer) {
    name = pName;
    pthreadTIndex = pPthreadTIndex;
    isPthreadTPointer = pIsPthreadTPointer;
  }

  PthreadFuncType(String pName) {
    name = pName;
    pthreadTIndex = Optional.empty();
    isPthreadTPointer = Optional.empty();
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
