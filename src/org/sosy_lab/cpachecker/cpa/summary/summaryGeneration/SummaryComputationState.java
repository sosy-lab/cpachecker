/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.summary.summaryGeneration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PriorityProvidingState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * State representing a (partially) computed function summary.
 */
class SummaryComputationState implements
                              AbstractState,
                              Partitionable,
                              Targetable,
                              PriorityProvidingState,
                              AbstractStateWithLocation,
                              Graphable
{

  /**
   * Block associated with the summary.
   */
  private final Block block;
  private final AbstractState entryState;
  private final Precision entryPrecision;
  private final @Nullable AbstractState callingContext;
  private final @Nullable SummaryComputationState parent;

  /**
   * Calling edge from {@code parent} to the entrance of {@code block}.
   */
  private final @Nullable CFAEdge callEdge;

  /**
   */
  private final transient ReachedSet reached;

  /**
   * Transient properties derived from reached set.
   */
  private final transient boolean hasWaitingState;
  private final transient boolean isTarget;
  private final transient ImmutableSet<Property> violatedProperties;
  private final transient int reachedSize;

  /**
   * Meta-information for visualization.
   * todo: change to collection for performance
   */
  private final Set<SummaryComputationState> children = new HashSet<>(1);
  private transient SummaryComputationState coveredBy = null;
  private final transient Set<SummaryComputationState> coveredByThis = new HashSet<>();
  private final int stateId;
  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();
  private transient boolean fullyExplored = false;
  private transient Summary generatedSummary = null;

  private SummaryComputationState(
      Block pBlock,
      Optional<AbstractState> pCallingContext,
      AbstractState pEntryState,
      Precision pEntryPrecision,
      ReachedSet pReached,
      boolean pHasWaitingState,
      boolean pIsTarget,
      ImmutableSet<Property> pViolatedProperties,
      int pReachedSize,
      @Nullable SummaryComputationState pParent,
      @Nullable CFAEdge pCallEdge) {

    block = pBlock;
    entryState = pEntryState;
    entryPrecision = pEntryPrecision;
    callingContext = pCallingContext.orElse(null);
    reached = pReached;
    hasWaitingState = pHasWaitingState;
    isTarget = pIsTarget;
    violatedProperties = pViolatedProperties;
    reachedSize = pReachedSize;
    callEdge = pCallEdge;
    parent = pParent;
    if (parent != null) {
      parent.addToChildren(this);
    }
    stateId = idGenerator.getFreshId();
  }

  /**
   * Set that no more successors are necessary.
   */
  void setFullyExplored() {
    fullyExplored = true;
  }

  void setGeneratedSummary(Summary pGeneratedSummary) {
    generatedSummary = pGeneratedSummary;
  }

  public int getStateId() {
    return stateId;
  }

  public Collection<SummaryComputationState> getChildren() {
    return Collections.unmodifiableSet(children);
  }

  private void addToChildren(SummaryComputationState pChild) {
    children.add(pChild);
  }

  void setCoveredBy(SummaryComputationState pCoveredBy) {
    coveredBy = pCoveredBy;
    coveredBy.coveredByThis.add(this);
  }

  public boolean isCovered() {
    return coveredBy != null;
  }

  public Set<SummaryComputationState> getCoveredByThis() {
    return Collections.unmodifiableSet(coveredByThis);
  }

  int getReachedSize() {
    return reachedSize;
  }

  /**
   * @return calling context for the summary,
   * empty for the entry function.
   */
  Optional<AbstractState> getCallingContext() {
    return Optional.ofNullable(callingContext);
  }

  /**
   * @return syntactical bounds for the summarized block.
   */
  Block getBlock() {
    return block;
  }

  /**
   * @return function name of the summarized function.
   */
  String getFunctionName() {
    return AbstractStates.extractLocation(entryState).getFunctionName();
  }

  /**
   * Reached set, representing the state of the summary computation.
   * Optimization which avoid the redundant computation,
   * as the summary can be recomputed from the
   * {@link #getEntryState()} and {@link #get}.
   *
   * @return reached set representing computation in progress for summary derivation.
   */
  ReachedSet getReached() {
    return reached;
  }

  /**
   * @return state associated with {@link org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode},
   * first state in the function.
   */
  AbstractState getEntryState() {
    return entryState;
  }

  CFANode getEntryLocation() {
    return AbstractStates.extractLocation(entryState);
  }

  /**
   * @return precision associated with {@link #getEntryState()}.
   */
  Precision getEntryPrecision() {
    return entryPrecision;
  }

  public static SummaryComputationState initial(
      Block pBlock,
      AbstractState pEntryState,
      Precision pPrecision,
      ReachedSet pReached
  ) {
    return new SummaryComputationState(
        pBlock,
        Optional.empty(),
        pEntryState,
        pPrecision,
        pReached,
        false,
        false,
        ImmutableSet.of(),
        1,
        null,
        null);
  }

  public static SummaryComputationState of(
      Block pBlock,
      AbstractState pCallingContext,
      AbstractState pEntryState,
      Precision pPrecision,
      ReachedSet pReached,
      boolean pInnerAnalysisHasWaitingState,
      boolean pIsTarget,
      Set<Property> pViolatedProperties,
      SummaryComputationState parent,
      CFAEdge pCallEdge
  ) {
    return new SummaryComputationState(
        pBlock,
        Optional.of(pCallingContext),
        pEntryState,
        pPrecision,
        pReached,
        pInnerAnalysisHasWaitingState, pIsTarget,
        ImmutableSet.copyOf(pViolatedProperties),
        pReached.size(),
        parent,
        pCallEdge);
  }

  /**
   * Update the transient information associated with the currently performed
   * computation in the reached set.
   */
  SummaryComputationState withUpdatedTargetable(
      boolean pInnerAnalysisHasWaitingState,
      boolean pIsTarget,
      Set<Property> pViolatedProperties,
      int pReachedSize
  ) {
    return new SummaryComputationState(
        block,
        Optional.ofNullable(callingContext),
        entryState,
        entryPrecision,
        reached,
        pInnerAnalysisHasWaitingState, pIsTarget,
        ImmutableSet.copyOf(pViolatedProperties),
        pReachedSize,
        parent,
        callEdge);
  }

  SummaryComputationState withNewReachedSize(int pReachedSize) {
    return new SummaryComputationState(
        block,
        Optional.ofNullable(callingContext),
        entryState,
        entryPrecision,
        reached,
        hasWaitingState,
        isTarget,
        violatedProperties,
        pReachedSize,
        this, // todo?
        callEdge);
  }

  @Override
  public Object getPartitionKey() {
    return block;
  }

  @Override
  public String toString() {
    return "SummaryComputationState{" +
        "node=" +
        ", entryState=" + entryState +
        ", entryPrecision=" + entryPrecision +
        '}';
  }

  @Override
  public boolean isTarget() {
    return isTarget;
  }

  @Nonnull
  @Override
  public Set<Property> getViolatedProperties() {
    return violatedProperties;
  }

  public boolean hasWaitingState() {
    return hasWaitingState;
  }

  /**
   * States with larger callstack should be considered <b>first</b>.
   */
  @Override
  public int getPriority() {
    if (parent == null) {
      return 0;
    } else {
      // todo: store, do not recompute.
      if (parent.getEntryState() == entryState) {

        // todo: covers all cases?
        return parent.getPriority();
      }
      return 1 + parent.getPriority();
    }
  }

  @Override
  public String toDOTLabel() {
    String out = String.format("Summary (size=%d, depth=%s): entry=%s",
        reachedSize,
        getPriority(),
        ((Graphable) entryState).toDOTLabel()
    );
    if (generatedSummary != null) {
      out += "\n generated=" + generatedSummary.toString();
    }
    return out;
  }

  @Override
  public boolean shouldBeHighlighted() {
    return isTarget;
  }

  @Override
  public CFANode getLocationNode() {
    return block.getStartNode();
  }

  @Override
  public Iterable<CFANode> getLocationNodes() {
    return Collections.singleton(getLocationNode());
  }

  @Override
  public Iterable<CFAEdge> getOutgoingEdges() {

    // todo: this should probably reflect something different: namely, all function
    // calls from this location that lead to edges being computed.
    return block.getStartNode().getLeavingEdges().collect(Collectors.toList());
  }

  @Override
  public Iterable<CFAEdge> getIngoingEdges() {
    return block.getStartNode().getEnteringEdges().collect(Collectors.toList());
  }

  SummaryComputationState getParent() {
    return parent;
  }

  CFAEdge getCallEdge() {
    return callEdge;
  }

  public List<CFAEdge> getEdgesToChild(SummaryComputationState pSuccessorState) {
    if (pSuccessorState.getParent() == this
        && pSuccessorState.getCallEdge() != null) {
      return ImmutableList.of(pSuccessorState.getCallEdge());
    } else {
      return ImmutableList.of();
    }
  }

  public boolean isFullyExplored() {
    return fullyExplored;
  }
}
