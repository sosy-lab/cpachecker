// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

/**
 * An object for a thread containing an identifier (threadObject) and entry / exit Nodes of the
 * threads to identify which parts of a CFA are executed by the thread.
 */
public class MPORThread {

  /** The pthread_t object. Set to empty for the main thread. */
  public final Optional<CExpression> threadObject;

  /** FunctionEntryNode of the main function (main thread) or start routine (pthreads). */
  public final FunctionEntryNode entryNode;

  /**
   * FunctionExitNode of the main function (main thread) or start routine (pthreads). Can be empty,
   * see {@link FunctionEntryNode#getExitNode()}.
   */
  public final Optional<FunctionExitNode> exitNode;

  private Set<MPORMutex> mutexes = new HashSet<>();

  private Set<MPORJoin> joins = new HashSet<>();

  public MPORThread(
      Optional<CExpression> pThreadObject,
      FunctionEntryNode pEntryNode,
      Optional<FunctionExitNode> pExitNode) {
    threadObject = pThreadObject;
    entryNode = pEntryNode;
    exitNode = pExitNode;
  }

  public boolean isMain() {
    return threadObject.isEmpty();
  }

  public void addMutex(MPORMutex pMutex) {
    mutexes.add(pMutex);
  }

  public Set<MPORMutex> getMutexes() {
    return mutexes;
  }

  public void addJoin(MPORJoin pJoin) {
    joins.add(pJoin);
  }

  public Set<MPORJoin> getJoins() {
    return joins;
  }
}
