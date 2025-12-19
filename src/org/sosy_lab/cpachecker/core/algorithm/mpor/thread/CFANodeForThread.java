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
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;

public class CFANodeForThread {

  private static int currentId = 0;

  private static int getNewId() {
    return currentId++;
  }

  public static void resetId() {
    currentId = 0;
  }

  public final int id;

  public final int threadId;

  /** The corresponding CFANode of the input CFA. */
  public final CFANode cfaNode;

  /** The corresponding program counter of the CFANode. */
  public final int pc;

  /** Not all nodes have a calling context, e.g. {@code main()} function statements. */
  public final Optional<CFAEdgeForThread> callContext;

  /** The list of context-sensitive return leaving edges of this ThreadNode. */
  private final List<CFAEdgeForThread> leavingEdges;

  public final boolean isInAtomicBlock;

  CFANodeForThread(
      int pThreadId,
      CFANode pCfaNode,
      int pPc,
      Optional<CFAEdgeForThread> pCallContext,
      List<CFAEdgeForThread> pLeavingEdges,
      boolean pIsInAtomicBlock) {

    id = getNewId();
    threadId = pThreadId;
    cfaNode = pCfaNode;
    pc = pPc;
    callContext = pCallContext;
    leavingEdges = pLeavingEdges;
    isInAtomicBlock = pIsInAtomicBlock;
  }

  public CFAEdgeForThread firstLeavingEdge() {
    checkArgument(!leavingEdges.isEmpty(), "cannot get first leaving edge, list is empty");
    return leavingEdges.getFirst();
  }

  public ImmutableList<CFAEdgeForThread> leavingEdges() {
    return ImmutableList.copyOf(leavingEdges);
  }

  void pruneLeavingEdge(CFAEdgeForThread pThreadEdge) {
    checkArgument(leavingEdges.contains(pThreadEdge), "pThreadEdge not in threadEdges");
    checkArgument(
        pThreadEdge.cfaEdge instanceof CFunctionReturnEdge,
        "only CFunctionReturnEdges can be pruned");

    leavingEdges.remove(pThreadEdge);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, threadId, cfaNode, pc, isInAtomicBlock);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof CFANodeForThread other
        && id == other.id
        && threadId == other.threadId
        && cfaNode.equals(other.cfaNode)
        && pc == other.pc
        && isInAtomicBlock == other.isInAtomicBlock;
  }
}
