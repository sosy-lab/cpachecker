// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;

public class BlockNode extends BlockNodeWithoutGraphInformation {
  private final Optional<CallstackState> knownEntryCallstack;

  /** A complete entry stack, only present after a successful context preanalysis. */
  public Optional<CallstackState> getKnownEntryCallstack() {
    return knownEntryCallstack;
  }

  /** Returns a copy, keeping shared decomposition caches independent of this analysis run. */
  public BlockNode withKnownEntryCallstack(CallstackState pStack) {
    return new BlockNode(
        getId(),
        getInitialLocation(),
        getFinalLocation(),
        getNodes(),
        getEdges(),
        predecessorIds,
        successorIds,
        violationConditionLocation,
        Optional.of(pStack));
  }

  private final ImmutableSet<String> predecessorIds;
  private final ImmutableSet<String> successorIds;
  private final CFANode violationConditionLocation;

  public BlockNode(
      String pId,
      CFANode pFirst,
      CFANode pLast,
      ImmutableSet<CFANode> pNodes,
      ImmutableSet<CFAEdge> pEdges,
      ImmutableSet<String> pPredecessorIds,
      ImmutableSet<String> pSuccessorIds) {
    this(pId, pFirst, pLast, pNodes, pEdges, pPredecessorIds, pSuccessorIds, pLast);
  }

  public BlockNode(
      String pId,
      CFANode pFirst,
      CFANode pLast,
      ImmutableSet<CFANode> pNodes,
      ImmutableSet<CFAEdge> pEdges,
      ImmutableSet<String> pPredecessorIds,
      ImmutableSet<String> pSuccessorIds,
      CFANode pViolationConditionLocation) {
    this(
        pId,
        pFirst,
        pLast,
        pNodes,
        pEdges,
        pPredecessorIds,
        pSuccessorIds,
        pViolationConditionLocation,
        Optional.empty());
  }

  private BlockNode(
      String pId,
      CFANode pFirst,
      CFANode pLast,
      ImmutableSet<CFANode> pNodes,
      ImmutableSet<CFAEdge> pEdges,
      ImmutableSet<String> pPredecessorIds,
      ImmutableSet<String> pSuccessorIds,
      CFANode pViolationConditionLocation,
      Optional<CallstackState> pKnownEntryCallstack) {
    super(pId, pFirst, pLast, pNodes, pEdges);
    knownEntryCallstack = pKnownEntryCallstack;
    predecessorIds = pPredecessorIds;
    successorIds = pSuccessorIds;
    violationConditionLocation = pViolationConditionLocation;
  }

  public boolean isAbstractionPossible() {
    return !getFinalLocation().equals(getViolationConditionLocation());
  }

  @Override
  public CFANode getViolationConditionLocation() {
    return violationConditionLocation;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof BlockNode other && super.equals(other);
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
        + getInitialLocation()
        + ", last="
        + getFinalLocation()
        + ", pred="
        + predecessorIds
        + ", succ="
        + successorIds
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

  public ImmutableSet<String> getSuccessorIds() {
    return successorIds;
  }
}
