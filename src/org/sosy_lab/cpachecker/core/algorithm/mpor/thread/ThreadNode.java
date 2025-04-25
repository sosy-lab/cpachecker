// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;

public class ThreadNode {

  public final int threadId;

  /** The corresponding CFANode of the input CFA. */
  public final CFANode cfaNode;

  /** The corresponding program counter of the CFANode. */
  public final int pc;

  /** Not all nodes have a calling context, e.g. {@code main()} function statements. */
  protected final Optional<ThreadEdge> callContext;

  /** The list of context-sensitive return leaving edges of this ThreadNode. */
  private final List<ThreadEdge> leavingEdges;

  protected ThreadNode(
      int pThreadId,
      CFANode pCfaNode,
      int pPc,
      Optional<ThreadEdge> pCallContext,
      List<ThreadEdge> pLeavingEdges) {

    threadId = pThreadId;
    cfaNode = pCfaNode;
    pc = pPc;
    callContext = pCallContext;
    leavingEdges = pLeavingEdges;
  }

  public ThreadEdge firstLeavingEdge() {
    checkArgument(!leavingEdges.isEmpty(), "cannot get first leaving edge, list is empty");
    return leavingEdges.get(0);
  }

  public ImmutableList<ThreadEdge> leavingEdges() {
    return ImmutableList.copyOf(leavingEdges);
  }

  protected void pruneLeavingEdge(ThreadEdge pThreadEdge) {
    checkArgument(leavingEdges.contains(pThreadEdge), "pThreadEdge not in threadEdges");
    checkArgument(
        pThreadEdge.cfaEdge instanceof CFunctionReturnEdge,
        "only CFunctionReturnEdges can be pruned");

    leavingEdges.remove(pThreadEdge);
  }
}
