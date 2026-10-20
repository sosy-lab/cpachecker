// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Groups preconditions by the block that sent them, so that every postcondition update replaces
 * everything previously received from that block.
 *
 * <p>The block stops as soon as an update carries only states it already knows for the sending
 * block. What the stored preconditions are then explored from is decided by {@link
 * AlwaysReplaceExplorationEngine}, which re-explores every known precondition on every round.
 */
final class AlwaysReplacePreconditionHandler implements DssPreconditionHandler {

  /** Stands in for the (nonexistent) predecessor of the root block. */
  private static final String ROOT_KEY = "root";

  private final BlockToProgramLocationMap preconditions;

  private final DssBlockAnalysis analysis;

  AlwaysReplacePreconditionHandler(DssBlockAnalysis pAnalysis) throws InterruptedException {
    analysis = pAnalysis;
    if (analysis.getBlock().isRoot()) {
      // the root block has no predecessor to receive a precondition from, so it starts from the
      // unconstrained entry state under the synthetic key ROOT_KEY
      preconditions = new BlockToProgramLocationMap(analysis.getDcpa(), ImmutableSet.of(ROOT_KEY));
      preconditions.addStateForKey(
          ROOT_KEY,
          new StateAndPrecision(analysis.makeStartState(false), analysis.makeStartPrecision()));
    } else {
      preconditions =
          new BlockToProgramLocationMap(
              analysis.getDcpa(), analysis.getBlock().getPredecessorIds());
    }
  }

  @Override
  public DssMessageProcessing store(DssPostConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException {
    if (pReceived.indicatesUnreachableBlockEnd()) {
      String sender = pReceived.getSenderId();
      // Re-analysing on an update that changes nothing republishes the very same message. Around a
      // cycle in the block graph that never terminates => stop.
      boolean alreadyRecorded =
          preconditions.isMarkedUnreachable(sender) && preconditions.isEmpty(sender);
      preconditions.markUnreachable(sender);
      if (alreadyRecorded) {
        return DssMessageProcessing.stop();
      }
      if (preconditions.isEmpty(sender) && !preconditions.isUnreachable()) {
        return DssMessageProcessing.stop();
      }
      preconditions.clearKey(sender);
      return DssMessageProcessing.proceed();
    }
    ImmutableList<@NonNull StateAndPrecision> received = analysis.deserialize(pReceived);
    preconditions.markReachable(pReceived.getSenderId());
    ImmutableListMultimap<Object, @NonNull StateAndPrecision> programPointToState =
        Multimaps.index(received, sap -> analysis.getDcpa().computeProgramPointId(sap.state()));
    DssSingleWorkerStatistics stats = analysis.statistics();
    stats.getStorePreconditionStatesTimer().start();
    try {
      DssMessageProcessing processing = analysis.shouldProceedForward(received);
      if (!processing.shouldProceed()) {
        return processing;
      }

      preconditions.removeStatesWithIgnoreCallstackIfMorePrecise(pReceived.getSenderId(), received);

      boolean stop = true;

      for (Object programPoint : programPointToState.keySet()) {
        ImmutableList<@NonNull StateAndPrecision> statesAtLocation =
            programPointToState.get(programPoint);
        if (!analysis.statesEqual(
            statesAtLocation,
            preconditions.getStatesAndPrecisionsForKeyAndId(
                pReceived.getSenderId(), programPoint))) {
          preconditions.overwriteStatesForKey(
              pReceived.getSenderId(), programPoint, statesAtLocation);
          stop = false;
        }
      }

      if (stop) {
        // All states are equal, no need to proceed
        return DssMessageProcessing.stop();
      }
      return DssMessageProcessing.proceed();
    } finally {
      stats.getStorePreconditionStatesTimer().stop();
      stats.getStorePreconditionStatesCounter().add(received.size());
    }
  }

  @Override
  public ImmutableList<@NonNull StateAndPrecision> getKnownPreconditions() {
    return preconditions.getStatesAndPrecisions();
  }

  @Override
  public void violationConditionsChanged() {
    // nothing to do, the block is always explored from all known preconditions
  }

  /**
   * The stored preconditions, grouped by sending block and program point, for the {@link
   * AlwaysReplaceExplorationEngine} that explores them.
   */
  BlockToProgramLocationMap getPreconditions() {
    return preconditions;
  }
}
