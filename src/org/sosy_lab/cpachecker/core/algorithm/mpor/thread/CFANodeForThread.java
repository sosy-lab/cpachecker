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
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;

public class CFANodeForThread {

  /**
   * The block types that a {@link CFANodeForThread} can be a part of. A block is a sequence of
   * nodes that are grouped together and need to be identified to handle their semantics.
   */
  public enum BlockType {
    ATOMIC,
    PTHREAD_ONCE_ROUTINE,
  }

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
  private final CFANode cfaNode;

  /** The corresponding program counter of the CFANode. */
  public final int pc;

  public final SeqCallContext callContext;

  private final List<CFAEdgeForThread> enteringEdges;

  /** The list of context-sensitive return leaving edges of this ThreadNode. */
  private final List<CFAEdgeForThread> leavingEdges;

  public final ImmutableMap<SeqCallContext, BlockType> blockTypes;

  CFANodeForThread(
      int pThreadId,
      CFANode pCfaNode,
      int pPc,
      SeqCallContext pCallContext,
      List<CFAEdgeForThread> pLeavingEdges,
      ImmutableMap<SeqCallContext, BlockType> pBlockTypes) {

    checkNumLeavingEdges(pCfaNode, pLeavingEdges);

    id = getNewId();
    threadId = pThreadId;
    cfaNode = pCfaNode;
    pc = pPc;
    callContext = pCallContext;
    enteringEdges = new ArrayList<>();
    leavingEdges = pLeavingEdges;
    blockTypes = pBlockTypes;
  }

  private CFANodeForThread(
      int pId,
      int pThreadId,
      CFANode pCfaNode,
      int pPc,
      SeqCallContext pCallContext,
      List<CFAEdgeForThread> pEnteringEdges,
      List<CFAEdgeForThread> pLeavingEdges,
      ImmutableMap<SeqCallContext, BlockType> pBlockTypes) {

    checkNumLeavingEdges(pCfaNode, pLeavingEdges);

    id = pId;
    threadId = pThreadId;
    cfaNode = pCfaNode;
    pc = pPc;
    callContext = pCallContext;
    enteringEdges = pEnteringEdges;
    leavingEdges = pLeavingEdges;
    blockTypes = pBlockTypes;
  }

  private static void checkNumLeavingEdges(CFANode pCfaNode, List<CFAEdgeForThread> pLeavingEdges) {
    checkArgument(pLeavingEdges.size() <= 3 || pCfaNode instanceof FunctionExitNode);
  }

  public CFAEdgeForThread firstLeavingEdge() {
    checkArgument(!leavingEdges.isEmpty(), "cannot get first leaving edge, list is empty");
    return leavingEdges.getFirst();
  }

  public ImmutableList<CFAEdgeForThread> enteringEdges() {
    return ImmutableList.copyOf(enteringEdges);
  }

  public ImmutableList<CFAEdgeForThread> leavingEdges() {
    return ImmutableList.copyOf(leavingEdges);
  }

  public CFANode getCfaNode() {
    return cfaNode;
  }

  void pruneLeavingEdge(CFAEdgeForThread pThreadEdge) {
    checkArgument(leavingEdges.contains(pThreadEdge), "pThreadEdge not in threadEdges");
    checkArgument(
        pThreadEdge.cfaEdge instanceof CFunctionReturnEdge,
        "only CFunctionReturnEdges can be pruned");

    leavingEdges.remove(pThreadEdge);
  }

  void addEnteringEdge(CFAEdgeForThread pEnteringEdge) {
    checkArgument(pEnteringEdge.getSuccessor().equals(this));
    enteringEdges.add(pEnteringEdge);
  }

  void addLeavingEdge(CFAEdgeForThread pLeavingEdge) {
    checkArgument(pLeavingEdge.getPredecessor().equals(this));
    leavingEdges.add(pLeavingEdge);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, threadId, cfaNode, pc, blockTypes);
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
        && blockTypes.equals(other.blockTypes);
  }
}
