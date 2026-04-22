// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class BlockNode extends BlockNodeWithoutGraphInformation {
  private final ImmutableSet<String> predecessorIds;
  private final ImmutableSet<String> loopPredecessorIds;
  private final ImmutableSet<String> nonLoopPredecessors;
  private final ImmutableSet<String> successorIds;
  private final ImmutableSet<String> loopSuccessorIds;
  private final ImmutableSet<String> nonLoopSuccessorIds;
  private final CFANode violationConditionLocation;

  BlockNode(
      String pId,
      CFANode pFirst,
      CFANode pLast,
      ImmutableSet<CFANode> pNodes,
      ImmutableSet<CFAEdge> pEdges,
      ImmutableSet<String> pPredecessorIds,
      ImmutableSet<String> pLoopPredecessorIds,
      ImmutableSet<String> pSuccessorIds,
      ImmutableSet<String> pLoopSuccessorIds) {
    this(
        pId,
        pFirst,
        pLast,
        pNodes,
        pEdges,
        pPredecessorIds,
        pLoopPredecessorIds,
        pSuccessorIds,
        pLoopSuccessorIds,
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
      ImmutableSet<String> pLoopSuccessorIds,
      CFANode pViolationConditionLocation) {
    super(pId, pFirst, pLast, pNodes, pEdges);
    predecessorIds = pPredecessorIds;
    loopPredecessorIds = pLoopPredecessorIds;
    nonLoopPredecessors = Sets.difference(predecessorIds, loopPredecessorIds).immutableCopy();
    successorIds = pSuccessorIds;
    loopSuccessorIds = pLoopSuccessorIds;
    nonLoopSuccessorIds = Sets.difference(successorIds, pLoopSuccessorIds).immutableCopy();
    violationConditionLocation = pViolationConditionLocation;
  }

  public boolean isAbstractionPossible() {
    return !getFinalLocation().equals(getViolationConditionLocation());
  }

  public boolean hasLoopPredecessor(String pId) {
    return loopPredecessorIds.contains(pId);
  }

  public boolean allPredecessorsAreLoopPredecessors() {
    return predecessorIds.equals(loopPredecessorIds);
  }

  public boolean hasLoopSuccessor(String pId) {
    return loopSuccessorIds.contains(pId);
  }

  public boolean allSuccessorsAreLoopSuccessors() {
    return successorIds.equals(loopSuccessorIds);
  }

  public BlockNode withMappedIds(
      Function<String, String> idMapper,
      Predicate<String> predecessorFilter,
      Function<String, String> predecessorMapper,
      Predicate<String> successorFilter,
      Function<String, String> successorMapper) {

    ImmutableSet<String> mappedPredecessors =
        predecessorIds.stream()
            .filter(predecessorFilter)
            .map(predecessorMapper)
            .collect(ImmutableSet.toImmutableSet());

    ImmutableSet<String> mappedLoopPredecessors =
        loopPredecessorIds.stream()
            .filter(predecessorFilter)
            .map(predecessorMapper)
            .collect(ImmutableSet.toImmutableSet());

    ImmutableSet<String> mappedSuccessors =
        successorIds.stream()
            .filter(successorFilter)
            .map(successorMapper)
            .collect(ImmutableSet.toImmutableSet());

    ImmutableSet<String> mappedLoopSuccessors =
        loopSuccessorIds.stream()
            .filter(successorFilter)
            .map(successorMapper)
            .collect(ImmutableSet.toImmutableSet());


    return new BlockNode(
        idMapper.apply(getId()),
        getInitialLocation(),
        getFinalLocation(),
        getNodes(),
        getEdges(),
        mappedPredecessors,
        mappedLoopPredecessors,
        mappedSuccessors,
        mappedLoopSuccessors);
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

  public ImmutableSet<String> getNonLoopPredecessors() {
    return nonLoopPredecessors;
  }

  public ImmutableSet<String> getSuccessorIds() {
    return successorIds;
  }

  public ImmutableSet<String> getLoopSuccessorIds() {
    return loopSuccessorIds;
  }

  public ImmutableSet<String> getNonLoopSuccessorIds() {
    return nonLoopSuccessorIds;
  }
}
