// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.ThreadId;

/**
 * The per-thread clones of the CFA of one analysis (see {@link ConcurrentCfaCloner}). Each thread
 * ID's clone is created on first access. One instance is owned by each {@link ConcurrentCPA}, so
 * analyses never share clones, and it is used only by that analysis's (single) thread.
 */
final class ConcurrentCfaClones {

  private final CFA cfa;

  private final Map<ThreadId, ConcurrentCfaCloner> clonersByThreadId = new HashMap<>();

  ConcurrentCfaClones(CFA pCfa) {
    cfa = checkNotNull(pCfa);
  }

  /** Returns the (original) CFA that is cloned. */
  CFA getCfa() {
    return cfa;
  }

  /**
   * Returns the clone of the given node for the given thread ID. If the node is already a clone, it
   * is returned as-is.
   */
  CFANode getClonedNode(CFANode pNode, ThreadId pThreadId) {
    return clonersByThreadId
        .computeIfAbsent(pThreadId, threadId -> ConcurrentCfaCloner.cloneCfa(threadId, cfa))
        .getClonedNode(pNode);
  }
}
