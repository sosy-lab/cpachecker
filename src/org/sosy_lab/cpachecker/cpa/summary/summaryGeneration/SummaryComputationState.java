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

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

/**
 * State representing a (partially) computed function summary.
 */
class SummaryComputationState
    implements AbstractState,
               Partitionable,
               Targetable {

  /**
   * Entry node associated with this computation.
   */
  private final CFANode node;

  private final AbstractState entryState;
  private final Precision entryPrecision;

  /**
   * Reached set, representing the state of the summary computation.
   * Optimization which avoid the redundant computation,
   * as the summary can be recomputed from the
   * {@code entryState} and {@code entryPrecision}.
   */
  private final transient ReachedSet reached;

  SummaryComputationState(
      CFANode pNode,
      AbstractState pEntryState, Precision pEntryPrecision, ReachedSet pReached) {
    node = pNode;
    entryState = pEntryState;
    entryPrecision = pEntryPrecision;
    reached = pReached;
  }

  public CFANode getNode() {
    return node;
  }

  public String getFunctionName() {
    return node.getFunctionName();
  }

  public ReachedSet getReached() {
    return reached;
  }

  public AbstractState getEntryState() {
    return entryState;
  }

  /**
   * Update the reached set associated with the state.
   */
  SummaryComputationState withNewReachedSet(ReachedSet pReached) {
    return new SummaryComputationState(
        node, entryState, entryPrecision, pReached
    );
  }

  @Override
  public Object getPartitionKey() {

    // Partition by the function for which we compute this summary.
    return node;
  }

  @Override
  public boolean equals(@Nullable Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    SummaryComputationState that = (SummaryComputationState) pO;
    return Objects.equals(node, that.node) &&
        Objects.equals(entryState, that.entryState) &&
        Objects.equals(entryPrecision, that.entryPrecision);
  }

  @Override
  public int hashCode() {
    return Objects.hash(node, entryState, entryPrecision);
  }

  @Override
  public String toString() {
    return "SummaryComputationState{" +
        "node=" + node +
        ", entryState=" + entryState +
        ", entryPrecision=" + entryPrecision +
        '}';
  }

  @Override
  public boolean isTarget() {

    // State is targetable if any of the contained states is targetable.
    return reached.asCollection().stream().anyMatch(s -> s instanceof Targetable);
  }

  @Nonnull
  @Override
  public Set<Property> getViolatedProperties() throws IllegalStateException {

    // Collect violated properties for all target states inside.
    return reached.asCollection().stream().filter(
        s -> s instanceof Targetable
    ).flatMap(s -> ((Targetable) s).getViolatedProperties().stream()).collect(Collectors.toSet());
  }
}
