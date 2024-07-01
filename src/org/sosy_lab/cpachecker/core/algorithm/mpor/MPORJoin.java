// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * An object for a pthread_join call containing thread waited on and the CFANode whose next leaving
 * CFAEdge is the pthread_join calls.
 */
public class MPORJoin {

  public final MPORThread threadToTerminate;

  public final CFANode preJoinNode;

  /**
   * Creates a new MPORJoin.
   *
   * @param pThreadToTerminate the thread that is waited on for termination
   * @param pPreJoinNode the CFANode right before the pthread_join call (the next CFANode is reached
   *     if pThreadToTerminate is at the exit CFANode of its start routine)
   */
  public MPORJoin(MPORThread pThreadToTerminate, CFANode pPreJoinNode) {
    threadToTerminate = pThreadToTerminate;
    preJoinNode = pPreJoinNode;
  }
}
