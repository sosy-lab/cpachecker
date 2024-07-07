// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORThread;

/**
 * An object for a pthread_join call containing thread waited on and the CFANode whose next leaving
 * CFAEdge is the pthread_join calls.
 */
public class MPORJoin {

  /** The MPORThread with the pthread_t object specified in the pthread_join call. */
  public final MPORThread threadToTerminate;

  /** The CFANode whose leaving CFAEdge is a call to pthread_join. */
  public final CFANode preJoinNode;

  /** The CFAEdge that is a call to pthread_join. */
  public final CFAEdge joinEdge;

  /**
   * Creates a new MPORJoin.
   *
   * @param pThreadToTerminate the thread that is waited on for termination
   * @param pPreJoinNode the CFANode right before the pthread_join call (the next CFANode is reached
   *     if pThreadToTerminate is at the exit CFANode of its start routine)
   */
  public MPORJoin(MPORThread pThreadToTerminate, CFANode pPreJoinNode, CFAEdge pJoinEdge) {
    threadToTerminate = pThreadToTerminate;
    preJoinNode = pPreJoinNode;
    joinEdge = pJoinEdge;
  }
}
