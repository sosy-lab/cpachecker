// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class ThreadFunctionReturnValue {

  private final CFAEdge threadFunction;
  private final boolean success;

  ThreadFunctionReturnValue(CFAEdge pThreadFunction, boolean pSuccess) {
    threadFunction = pThreadFunction;
    success = pSuccess;
  }

  public CFAEdge getThreadFunction() {
    return threadFunction;
  }

  public boolean isSuccess() {
    return success;
  }
}
