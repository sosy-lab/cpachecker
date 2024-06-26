// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

public enum PthreadFunctionType {
  CANCEL("pthread_cancel"),
  CREATE("pthread_create"),
  DETACH("pthread_detach"),
  EXIT("pthread_exit"),
  JOIN("pthread_join"),
  MUTEX_INIT("pthread_mutex_init"),
  MUTEX_LOCK("pthread_mutex_lock"),
  MUTEX_UNLOCK("pthread_mutex_unlock");
  // TODO unsure about yield, mutex_destroy
  //  pthread_mutex_t amutex = PTHREAD_MUTEX_INITIALIZER; // also used instead of mutex init
  //  pthread_barrier stuff
  //  etc. probably a lot more things

  public final String name;

  PthreadFunctionType(String pName) {
    this.name = pName;
  }

  /** Return true if the given CFAEdge is a call to the given pthread function */
  public static boolean isEdgeCallToFunctionType(
      CFAEdge pCfaEdge, PthreadFunctionType pPthreadFunctionType) {
    return CFAUtils.isCfaEdgeCFunctionCallStatement(pCfaEdge)
        && CFAUtils.getCFunctionCallStatementFromCfaEdge(pCfaEdge)
            .getFunctionCallExpression()
            .getFunctionNameExpression()
            .toASTString()
            .equals(pPthreadFunctionType.name);
  }
}
