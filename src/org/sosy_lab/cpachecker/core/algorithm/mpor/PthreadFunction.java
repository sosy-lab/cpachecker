// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;

public enum PthreadFunction {
  CREATE("pthread_create");
  // TODO more pthread functions

  public final String name;

  private PthreadFunction(String pName) {
    this.name = pName;
  }

  /**
   * @return true if the given CFAEdge is a call to the given pthread function
   */
  public static boolean isEdgeCallToPthreadFunction(CFAEdge pCFAEdge, PthreadFunction pPthreadFunction) {
    return pCFAEdge.getEdgeType().equals(CFAEdgeType.FunctionCallEdge)
        && pCFAEdge.getRawStatement().contains(pPthreadFunction.name);
  }
}
