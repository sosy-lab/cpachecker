// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DSSMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSPostConditionMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class DSSPreconditions {

  private final Map<String, DSSPrecondition> preconditions;
  private final BlockNode block;
  private final DistributedConfigurableProgramAnalysis dcpa;

  public DSSPreconditions(BlockNode pBlock, DistributedConfigurableProgramAnalysis pDCPA) {
    preconditions = new HashMap<>();
    block = pBlock;
    dcpa = pDCPA;
    pBlock
        .getPredecessorIds()
        .forEach(
            id ->
                preconditions.put(
                    id, DSSPrecondition.empty(block.getLoopPredecessorIds().contains(id))));
  }

  private boolean isCovered(DSSPostConditionMessage pMessage1, DSSPostConditionMessage pMessage2)
      throws InterruptedException, CPAException {
    if (pMessage1 == null || pMessage2 == null) {
      return false;
    }
    AbstractState state1 = dcpa.getDeserializeOperator().deserialize(pMessage1);
    AbstractState state2 = dcpa.getDeserializeOperator().deserialize(pMessage2);
    return dcpa.getCoverageCheck().isCovered(state1, state2);
  }

  public DSSMessageProcessing updatePrecondition(DSSPostConditionMessage pMessage)
      throws SolverException, InterruptedException, CPAException {
    AbstractState deserialized = dcpa.getDeserializeOperator().deserialize(pMessage);
    DSSMessageProcessing processing = dcpa.getProceedOperator().processForward(deserialized);
    if (!processing.shouldProceed()) {
      return processing;
    }
    Preconditions.checkArgument(block.getPredecessorIds().contains(pMessage.getBlockId()));
    DSSPostConditionMessage old = preconditions.get(pMessage.getBlockId()).getMessage();
    boolean ignore =
        block.getLoopPredecessorIds().contains(pMessage.getBlockId()) && dcpa.isTop(deserialized);
    preconditions.put(
        pMessage.getBlockId(), new DSSPrecondition(pMessage, isCovered(pMessage, old), ignore));
    return processing;
  }

  public boolean isFixpointReached() {
    return preconditions.values().stream().allMatch(DSSPrecondition::isSound);
  }

  @Override
  public String toString() {
    return "DSSPreconditions{"
        + "preconditions="
        + preconditions
        + ", block="
        + block.getId()
        + '}';
  }

  /**
   * Prepare the reached set for next analysis by merging all received BPC messages into a non-empty
   * set of start states.
   *
   * @throws CPAException thrown in merge or stop operation runs into an error
   * @throws InterruptedException thrown if thread is interrupted unexpectedly.
   */
  public void prepareReachedSet(ReachedSet reachedSet) throws CPAException, InterruptedException {
    // simulate merge and stop for all states ending up at block#getStartNode
    reachedSet.clear();

    for (DSSPrecondition precondition : preconditions.values()) {
      if (precondition.defaultToTop()) {
        reachedSet.clear();
        break;
      }
      if (precondition.shouldBeIgnored()) {
        continue;
      }
      Preconditions.checkState(
          !precondition.isEmpty(), "Precondition is empty but should not be ignored.");
      DSSPostConditionMessage message = precondition.getMessage();
      AbstractState value = dcpa.getDeserializeOperator().deserialize(message);
      Precision precision = dcpa.getDeserializePrecisionOperator().deserializePrecision(message);
      if (reachedSet.isEmpty()) {
        reachedSet.add(value, precision);
      } else {
        // CPA algorithm
        for (AbstractState abstractState : ImmutableSet.copyOf(reachedSet)) {
          AbstractState merged =
              dcpa.getCPA().getMergeOperator().merge(value, abstractState, precision);
          if (!merged.equals(abstractState)) {
            reachedSet.remove(abstractState);
            reachedSet.add(merged, precision);
          }
        }
        if (!dcpa.getCPA()
            .getStopOperator()
            .stop(
                value,
                reachedSet.getReached(block.getFirst()),
                dcpa.getInitialPrecision(
                    block.getFirst(), StateSpacePartition.getDefaultPartition()))) {
          reachedSet.add(value, precision);
        }
      }
    }

    if (reachedSet.isEmpty()) {
      reachedSet.add(
          dcpa.getCPA()
              .getInitialState(block.getFirst(), StateSpacePartition.getDefaultPartition()),
          dcpa.getCPA()
              .getInitialPrecision(block.getFirst(), StateSpacePartition.getDefaultPartition()));
    }
  }

  private static class DSSPrecondition {

    private final DSSPostConditionMessage currentMessage;
    private final boolean isSound;
    private final boolean ignore;

    private DSSPrecondition(DSSPostConditionMessage pMessage, boolean pIsSound, boolean pIgnore) {
      currentMessage = pMessage;
      isSound = pIsSound;
      ignore = pIgnore;
    }

    private static DSSPrecondition empty(boolean pIgnore) {
      return new DSSPrecondition(null, false, pIgnore);
    }

    public DSSPostConditionMessage getMessage() {
      return currentMessage;
    }

    public boolean isSound() {
      return isSound;
    }

    public boolean isEmpty() {
      return currentMessage == null;
    }

    public boolean shouldBeIgnored() {
      return ignore;
    }

    public boolean defaultToTop() {
      return !shouldBeIgnored() && isEmpty();
    }

    @Override
    public String toString() {
      return "DSSPrecondition{"
          + "currentMessage="
          + currentMessage
          + ", isSound="
          + isSound
          + ", ignore="
          + ignore
          + '}';
    }
  }
}
