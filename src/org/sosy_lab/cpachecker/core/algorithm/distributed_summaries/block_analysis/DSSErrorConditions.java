// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DSSMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSErrorConditionMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

public class DSSErrorConditions {

  private final Multimap<String, AbstractState> errorConditions;
  private final BlockNode blockNode;
  private final DistributedConfigurableProgramAnalysis dcpa;

  public DSSErrorConditions(BlockNode pBlockNode, DistributedConfigurableProgramAnalysis pDCPA) {
    errorConditions = ArrayListMultimap.create();
    blockNode = pBlockNode;
    dcpa = pDCPA;
  }

  public Collection<AbstractState> getErrorConditionsOf(String id) {
    Preconditions.checkArgument(
        errorConditions.containsKey(id),
        "Cannot retrieve error condition from a successor that has not yet reported one.");
    return errorConditions.get(id);
  }

  public AbstractState getLastErrorConditionOf(String id) {
    Preconditions.checkArgument(
        errorConditions.containsKey(id),
        "Cannot retrieve error condition from a successor that has not yet reported one.");
    return Iterables.getLast(errorConditions.get(id));
  }

  public Collection<AbstractState> getErrorConditions() {
    Preconditions.checkArgument(!errorConditions.containsValue(null));
    return ImmutableSet.copyOf(errorConditions.values());
  }

  public DSSMessageProcessing updateErrorCondition(DSSErrorConditionMessage pErrorConditionMessage)
      throws InterruptedException, SolverException {
    AbstractState deserialized = dcpa.getDeserializeOperator().deserialize(pErrorConditionMessage);
    DSSMessageProcessing processing = dcpa.getProceedOperator().processBackward(deserialized);
    if (!processing.shouldProceed()) {
      return processing;
    }
    Preconditions.checkArgument(
        blockNode.getSuccessorIds().contains(pErrorConditionMessage.getBlockId()));
    errorConditions.put(pErrorConditionMessage.getBlockId(), deserialized);
    return processing;
  }

  public void prepareReachedSet(ReachedSet reachedSet) {
    Preconditions.checkArgument(
        !reachedSet.isEmpty(), "ReachedSet cannot be empty when injecting error conditions.");
    prepareReachedSet(reachedSet, errorConditions.values());
  }

  public void prepareReachedSet(ReachedSet reachedSet, String successorId) {
    Preconditions.checkArgument(
        !reachedSet.isEmpty(), "ReachedSet cannot be empty when injecting error conditions.");
    Collection<AbstractState> blockErrorConditions = errorConditions.get(successorId);
    if (blockErrorConditions.isEmpty()) {
      return;
    }
    prepareReachedSet(reachedSet, ImmutableSet.of(getLastErrorConditionOf(successorId)));
  }

  private void prepareReachedSet(ReachedSet reachedSet, Collection<AbstractState> errorCondition) {
    reachedSet.forEach(
        abstractState ->
            Objects.requireNonNull(
                    AbstractStates.extractStateByType(abstractState, BlockState.class))
                .setErrorCondition(errorCondition));
  }

  @Override
  public String toString() {
    return "DSSErrorConditions{"
        + "blockNode="
        + blockNode.getId()
        + ", errorConditions="
        + errorConditions
        + '}';
  }
}
