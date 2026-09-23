// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
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

  /** The conditions the block explores, with equivalent ones kept only once. */
  private ImmutableList<StateAndPrecision> conditionsToExplore = ImmutableList.of();

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
      String sender = pReceived.getSenderId();
      ImmutableList<@NonNull StateAndPrecision> storedForSender =
          ImmutableList.copyOf(conditions.getStatesAndPrecisionsForKey(sender));
      // Each message replaces what we remember from this sender. Keep that separate from the
      // other senders: if two successors report the same condition, an update from one of them
      // must not erase the condition that still belongs to the other. Both directions matter,
      // because removing a condition is an update as well, so this asks for set equality.
      if (analysis.statesEqual(received, storedForSender)) {
        return DssMessageProcessing.stop();
      }

      ImmutableListMultimap<Object, @NonNull StateAndPrecision> programPointToState =
          Multimaps.index(received, sap -> analysis.getDcpa().computeProgramPointId(sap.state()));
      conditions.overwriteStatesForKey(sender, programPointToState);

      // What the block has to explore is the set of conditions over all senders, so an update is
      // only worth re-exploring if it changes that set. A condition another successor has already
      // reported adds nothing: exploring the block again would repeat work that is already done.
      ImmutableList<StateAndPrecision> updatedConditionsToExplore =
          analysis.deduplicateStatesAndPrecisions(conditions.getStatesAndPrecisions());
      boolean conditionSetUnchanged =
          analysis.statesEqual(updatedConditionsToExplore, conditionsToExplore);
      conditionsToExplore = updatedConditionsToExplore;
      return conditionSetUnchanged
          ? DssMessageProcessing.stop()
          : DssMessageProcessing.proceed();
    } finally {
      stats.getStoreViolationConditionStatesTimer().stop();
      stats.getStoreViolationConditionStatesCounter().add(received.size());
    }
  }

  @Override
  public ImmutableList<AbstractState> states() {
    // The conditions to explore, not the raw entries: two successors can report the same condition,
    // and the block gains nothing from exploring it once per successor.
    return transformedImmutableListCopy(conditionsToExplore, StateAndPrecision::state);
  }

  BlockToProgramLocationMap getConditions() {
    return conditions;
  }
}
