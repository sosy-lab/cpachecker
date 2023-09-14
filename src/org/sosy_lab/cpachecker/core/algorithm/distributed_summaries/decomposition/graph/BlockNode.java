// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class BlockNode extends BlockNodeWithoutGraphInformation {
  private final ImmutableSet<String> predecessorIds;
  private final ImmutableSet<String> loopPredecessorIds;
  private final ImmutableSet<String> successorIds;
  private final CFANode abstractionLocation;

  BlockNode(
      String pId,
      CFANode pFirst,
      CFANode pLast,
      ImmutableSet<CFANode> pNodes,
      ImmutableSet<CFAEdge> pEdges,
      ImmutableSet<String> pPredecessorIds,
      ImmutableSet<String> pLoopPredecessorIds,
      ImmutableSet<String> pSuccessorIds) {
    this(
        pId,
        pFirst,
        pLast,
        pNodes,
        pEdges,
        pPredecessorIds,
        pLoopPredecessorIds,
        pSuccessorIds,
        pLast);
  }

  BlockNode(
      String pId,
      CFANode pFirst,
      CFANode pLast,
      ImmutableSet<CFANode> pNodes,
      ImmutableSet<CFAEdge> pEdges,
      ImmutableSet<String> pPredecessorIds,
      ImmutableSet<String> pLoopPredecessorIds,
      ImmutableSet<String> pSuccessorIds,
      CFANode pAbstractionLocation) {
    super(pId, pFirst, pLast, pNodes, pEdges);
    predecessorIds = pPredecessorIds;
    loopPredecessorIds = pLoopPredecessorIds;
    successorIds = pSuccessorIds;
    abstractionLocation = pAbstractionLocation;
  }

  public boolean isAbstractionPossible() {
    return !getLast().equals(getAbstractionLocation());
  }

  @Override
  public CFANode getAbstractionLocation() {
    return abstractionLocation;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BlockNode) {
      // based on id (but ensures same class)
      return super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    // based on id
    return super.hashCode();
  }

  @Override
  public String toString() {
    return "BlockNode{"
        + "id="
        + getId()
        + ", first="
        + getFirst()
        + ", last="
        + getLast()
        + ", pred="
        + predecessorIds
        + ", succ="
        + successorIds
        + ", loopPred="
        + loopPredecessorIds
        + ", code="
        + getCode()
        + ", nodes="
        + getNodes()
        + '}';
  }

  public boolean isRoot() {
    return getPredecessorIds().isEmpty();
  }

  public ImmutableSet<String> getPredecessorIds() {
    return predecessorIds;
  }

  public ImmutableSet<String> getLoopPredecessorIds() {
    return loopPredecessorIds;
  }

  public ImmutableSet<String> getSuccessorIds() {
    return successorIds;
  }
}
