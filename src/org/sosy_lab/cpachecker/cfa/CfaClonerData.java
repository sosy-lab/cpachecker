// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.IdentityHashMap;
import java.util.OptionalInt;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/** Data maintained by a CFA cloner for the elements it created. */
public final class CfaClonerData {

  private final IdentityHashMap<CFANode, CFANode> originalNodes = new IdentityHashMap<>();
  private final IdentityHashMap<CFAEdge, CFAEdge> originalEdges = new IdentityHashMap<>();
  private final IdentityHashMap<CFANode, Integer> threadIds = new IdentityHashMap<>();

  public synchronized void registerNode(CFANode pClone, CFANode pOriginal, int pThreadId) {
    checkArgument(pClone != pOriginal);
    checkState(originalNodes.put(pClone, checkNotNull(pOriginal)) == null);
    threadIds.put(pClone, pThreadId);
  }

  public synchronized void registerEdge(CFAEdge pClone, CFAEdge pOriginal) {
    checkArgument(pClone != pOriginal);
    checkState(originalEdges.put(pClone, checkNotNull(pOriginal)) == null);
  }

  public synchronized CFANode getOriginal(CFANode pNode) {
    return checkNotNull(originalNodes.get(pNode), "Missing original node for clone %s", pNode);
  }

  public synchronized CFAEdge getOriginal(CFAEdge pEdge) {
    return checkNotNull(originalEdges.get(pEdge), "Missing original edge for clone %s", pEdge);
  }

  public synchronized OptionalInt getThreadId(CFANode pNode) {
    Integer threadId = threadIds.get(pNode);
    return threadId == null ? OptionalInt.empty() : OptionalInt.of(threadId);
  }
}
