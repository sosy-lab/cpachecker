// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.total_strict_order;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * An object for a pthread_join call containing thread waited on and the CFANode whose next leaving
 * CFAEdge is the pthread_join calls.
 */
public class MPORJoin {

  /**
   * The MPORThread with the pthread_t object specified in the pthread_join call, i.e. the thread
   * whose termination is waited on.
   */
  public final CExpression targetThread;

  /** The CFANode whose leaving CFAEdge is a call to pthread_join. */
  public final CFANode preJoinNode;

  /** The CFAEdge that is a call to pthread_join. */
  public final CFAEdge joinEdge;

  /**
   * Creates a new MPORJoin.
   *
   * @param pTargetThread the thread that is waited on for termination
   * @param pPreJoinNode the CFANode right before the pthread_join call (the next CFANode is reached
   *     if pThreadToTerminate is at the exit CFANode of its start routine)
   */
  public MPORJoin(CExpression pTargetThread, CFANode pPreJoinNode, CFAEdge pJoinEdge) {
    targetThread = pTargetThread;
    preJoinNode = pPreJoinNode;
    joinEdge = pJoinEdge;
  }
}
