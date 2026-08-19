// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;


import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Keeps only the violation conditions of the latest message per sending block: an update discards
 * everything previously received from that block.
 */
final class AlwaysReplaceViolationConditionHandler implements DssViolationConditionHandler {

  private final BlockToProgramLocationMap conditions;

  private final DssBlockAnalysis analysis;

  AlwaysReplaceViolationConditionHandler(DssBlockAnalysis pAnalysis) {
    conditions =
        new BlockToProgramLocationMap(pAnalysis.getDcpa(), pAnalysis.getBlock().getSuccessorIds());
    analysis = pAnalysis;
  }

  @Override
  public DssMessageProcessing store(DssViolationConditionMessage pReceived)
      throws InterruptedException, CPAException {
    analysis
        .getLogger()
        .log(Level.INFO, "Running forward analysis with respect to error condition");
    ImmutableList<@NonNull StateAndPrecision> received = analysis.deserialize(pReceived);
    DssSingleWorkerStatistics stats = analysis.statistics();
    stats.getStoreViolationConditionStatesTimer().start();

    try {
      // A condition counts as known only if an equal one is already stored. Comparing the
      // witnesses alone would only compare the path through the block graph, so a refined
      // condition for a path that was already reported would be discarded: the block would stop
      // although the sender learned something new, which lets a reachable violation go unnoticed.
      if (analysis.countCovered(received, ImmutableList.copyOf(conditions.getStatesAndPrecisions()))
          == received.size()) {
        return DssMessageProcessing.stop();
      }
      String sender = pReceived.getSenderId();
      conditions.overwriteStatesForKey(sender, received);
      return DssMessageProcessing.proceed();
    } finally {
      stats.getStoreViolationConditionStatesTimer().stop();
      stats.getStoreViolationConditionStatesCounter().add(received.size());
    }
  }

  @Override
  public boolean isEmpty() {
    return conditions.getStates().isEmpty();
  }

  @Override
  public boolean isEmptyFor(String pSenderId) {
    return conditions.isEmpty(pSenderId);
  }

  @Override
  public ImmutableList<AbstractState> statesOf(Optional<String> pSenderId) {
    return ImmutableList.copyOf(
        pSenderId.map(conditions::getStatesForKey).orElse(conditions.getStates()));
  }

  public BlockToProgramLocationMap getConditions() {
    return conditions;
  }
}
