// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * An object for a mutex containing the pthread_mutex_t object (not unique) and the set of all
 * CFANodes inside the lock, starting from the CFANode whose next CFAEdge is pthread_mutex_lock and
 * ending with the CFANode(s) whose next CFAEdge is pthread_mutex_unlock.
 */
public class MPORMutex {

  public final CExpression pthreadMutexT;

  /** The CFANode directly after pthread_mutex_lock. */
  public final CFANode entryNode;

  private Set<CFANode> cfaNodes = new HashSet<>();

  /**
   * Set of CFANodes whose leaving CFAEdges are pthread_mutex_unlocks to pthreadMutexT. Multiple
   * CFANodes can be exitNodes if they are reached in a nondeterministic way.
   */
  private Set<CFANode> exitNodes = new HashSet<>();

  /**
   * Initializes an MPORMutex with the pthread_mutex_t object and the first CFANode inside the lock.
   * The (exit) CFANodes inside the mutex are not initialized in the constructor, see
   * MPORAlgorithm.assignMutexesToThreads(...).
   *
   * @param pPthreadMutexT the pthread_mutex_t object (not a unique identifier!)
   * @param pEntryNode the entry CFANode of the lock, i.e. the CFANode directly after
   *     pthread_mutex_lock
   */
  public MPORMutex(CExpression pPthreadMutexT, CFANode pEntryNode) {
    pthreadMutexT = pPthreadMutexT;
    entryNode = pEntryNode;
  }

  public void addNode(CFANode pCFANode) {
    cfaNodes.add(pCFANode);
  }

  public Set<CFANode> getNodes() {
    return cfaNodes;
  }

  public void addExitNode(CFANode pExitNode) {
    exitNodes.add(pExitNode);
  }

  public Set<CFANode> getExitNodes() {
    return exitNodes;
  }
}