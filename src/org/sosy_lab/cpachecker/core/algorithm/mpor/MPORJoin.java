// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class MPORJoin {

  public final MPORThread threadToTerminate;

  public final CFANode preJoinNode;

  public MPORJoin(MPORThread pThreadToTerminate, CFANode pPreJoinNode) {
    threadToTerminate = pThreadToTerminate;
    preJoinNode = pPreJoinNode;
  }
}
