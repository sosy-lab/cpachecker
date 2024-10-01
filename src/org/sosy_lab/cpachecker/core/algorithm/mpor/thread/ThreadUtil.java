// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.PthreadFuncType;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class ThreadUtil {

  public static MPORThread extractThreadFromPthreadCreate(
      ImmutableSet<MPORThread> pThreads, CFAEdge pEdge) {
    checkArgument(
        PthreadFuncType.isCallToPthreadFunc(pEdge, PthreadFuncType.PTHREAD_CREATE),
        "pEdge must be call to pthread_create");
    CExpression pthreadTParam = CFAUtils.getParameterAtIndex(pEdge, 0);
    CExpression pthreadT = CFAUtils.getValueFromPointer(pthreadTParam);
    return getThreadByObject(pThreads, Optional.of(pthreadT));
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
