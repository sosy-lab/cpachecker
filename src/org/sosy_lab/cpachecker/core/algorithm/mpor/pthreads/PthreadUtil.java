// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class PthreadUtil {

  public static MPORThread extractThread(ImmutableSet<MPORThread> pThreads, CFAEdge pEdge) {
    checkArgument(
        PthreadFuncType.isCallToAnyPthreadFuncWithPthreadTParam(pEdge),
        "pEdge must be call to a pthread method with a pthread_t param");

    PthreadFuncType funcType = PthreadFuncType.getPthreadFuncType(pEdge);
    CExpression pthreadTParam = CFAUtils.getParameterAtIndex(pEdge, funcType.getPthreadTIndex());

    if (funcType.isPthreadTPointer()) {
      return getThreadByObject(pThreads, Optional.of(CFAUtils.getValueFromAddress(pthreadTParam)));
    } else {
      return getThreadByObject(pThreads, Optional.of(pthreadTParam));
    }
  }

  public static CExpression extractPthreadT(CFAEdge pEdge) {
    checkArgument(
        PthreadFuncType.isCallToAnyPthreadFuncWithPthreadTParam(pEdge),
        "pEdge must be call to a pthread method with a pthread_t param");

    PthreadFuncType funcType = PthreadFuncType.getPthreadFuncType(pEdge);
    CExpression pthreadT = CFAUtils.getParameterAtIndex(pEdge, funcType.getPthreadTIndex());

    if (funcType.isPthreadTPointer()) {
      return CFAUtils.getValueFromAddress(pthreadT);
    } else {
      return pthreadT;
    }
  }

  public static CExpression extractPthreadMutexT(CFAEdge pEdge) {
    checkArgument(
        PthreadFuncType.isCallToAnyPthreadFuncWithPthreadMutexTParam(pEdge),
        "pEdge must be call to a pthread method with a pthread_mutex_t param");

    PthreadFuncType funcType = PthreadFuncType.getPthreadFuncType(pEdge);
    CExpression pthreadMutexT =
        CFAUtils.getParameterAtIndex(pEdge, funcType.getPthreadMutexTIndex());

    return CFAUtils.getValueFromAddress(pthreadMutexT);
  }

  /** Searches the given map of MPORThreads for the given thread object. */
  private static MPORThread getThreadByObject(
      ImmutableSet<MPORThread> pThreads, Optional<CExpression> pThreadObject) {
    for (MPORThread rThread : pThreads) {
      if (rThread.threadObject.equals(pThreadObject)) {
        return rThread;
      }
    }
    throw new IllegalArgumentException("no MPORThread with pThreadObject found in pThreads");
  }
}
