// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORCreate;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORJoin;
import org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order.MPORMutex;

/**
 * An object for a thread containing an identifier (threadObject) and entry / exit Nodes of the
 * threads to identify which parts of a CFA are executed by the thread.
 */
public class MPORThread {

  public final int id;

  /** The pthread_t object. Set to empty for the main thread. */
  public final Optional<CExpression> threadObject;

  /** FunctionEntryNode of the main function (main thread) or start routine (pthreads). */
  public final FunctionEntryNode entryNode;

  /**
   * FunctionExitNode of the main function (main thread) or start routine (pthreads). Can be empty,
   * see {@link FunctionEntryNode#getExitNode()}.
   */
  public final FunctionExitNode exitNode;

  public final ImmutableSet<CFANode> nodes;

  public final ImmutableSet<CFAEdge> edges;

  public final ImmutableSet<MPORCreate> creates;

  public final ImmutableSet<MPORMutex> mutexes;

  public final ImmutableSet<MPORJoin> joins;

  protected MPORThread(
      int pId,
      Optional<CExpression> pThreadObject,
      FunctionEntryNode pEntryNode,
      FunctionExitNode pExitNode,
      ImmutableSet<CFANode> pNodes,
      ImmutableSet<CFAEdge> pEdges,
      ImmutableSet<MPORCreate> pCreates,
      ImmutableSet<MPORMutex> pMutexes,
      ImmutableSet<MPORJoin> pJoins) {

    id = pId;
    threadObject = pThreadObject;
    entryNode = pEntryNode;
    exitNode = pExitNode;
    nodes = pNodes;
    edges = pEdges;
    creates = pCreates;
    mutexes = pMutexes;
    joins = pJoins;
  }

  public boolean isMain() {
    return threadObject.isEmpty();
  }
}
