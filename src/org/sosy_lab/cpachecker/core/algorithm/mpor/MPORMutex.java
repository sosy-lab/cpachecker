// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * An object for a mutex containing the pthread_mutex_t object (not unique) and the set of all
 * CFANodes inside the lock, starting from the CFANode whose next CFAEdge is pthread_mutex_lock and
 * ending with the CFANode(s) whose next CFAEdge is pthread_mutex_unlock.
 */
public class MPORMutex {

  public final CIdExpression pthreadMutexT;

  public final CFANode entryNode;

  private Set<CFANode> cfaNodes = new HashSet<>();

  private Set<CFANode> exitNodes = new HashSet<>();

  // TODO boolean hasNondetUnlock?
  //  if the unlock is conditional (i.e. multiple exitNodes) it has an effect on the
  //  positional preference order: we can only prune the edges up until we encounter multiple
  //  leaving edges in a node

  /**
   * Initializes an MPORMutex with the pthread_mutex_t object and the first CFANode inside the lock.
   * The (exit) CFANodes inside the mutex are not initialized in the constructor, see
   * MPORAlgorithm.assignMutexesToThreads(...).
   *
   * @param pPthreadMutexT the pthread_mutex_t object (not a unique identifier!)
   * @param pEntryNode the entry CFANode of the lock, i.e. the CFANode directly after
   *     pthread_mutex_lock
   */
  public MPORMutex(CIdExpression pPthreadMutexT, CFANode pEntryNode) {
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
