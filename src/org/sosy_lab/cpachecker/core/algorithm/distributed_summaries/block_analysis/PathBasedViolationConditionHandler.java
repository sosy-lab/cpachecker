// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis.blockStateOf;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Keeps only the violation conditions of the latest message per sending block: an update discards
 * everything previously received from that block.
 */
final class PathBasedViolationConditionHandler implements DssViolationConditionHandler {

  private final Map<String, Multimap<String, StateAndPrecision>> conditions;

  private final DssBlockAnalysis analysis;

  PathBasedViolationConditionHandler(DssBlockAnalysis pAnalysis) {
    conditions = new LinkedHashMap<>();
    analysis = pAnalysis;
  }

  @Override
  public DssMessageProcessing store(DssViolationConditionMessage pReceived)
      throws InterruptedException {
    analysis
        .getLogger()
        .log(Level.INFO, "Running forward analysis with respect to error condition");
    ImmutableList<@NonNull StateAndPrecision> received = analysis.deserialize(pReceived);
    DssSingleWorkerStatistics stats = analysis.statistics();
    stats.getStoreViolationConditionStatesTimer().start();

    try {
      Multimap<String, StateAndPrecision> mapForSuccessor = conditions.get(pReceived.getSenderId());
      mapForSuccessor.putAll(
          Multimaps.index(received, sap -> blockStateOf(sap.state()).getUniqueId()));
      mapForSuccessor.keySet().stream()
          .filter(k -> !pReceived.getRemainingPreconditions().contains(k))
          .forEach(remainingId -> mapForSuccessor.removeAll(remainingId));
    } finally {
      stats.getStoreViolationConditionStatesTimer().stop();
      stats.getStoreViolationConditionStatesCounter().add(received.size());
    }

    return DssMessageProcessing.proceed();
  }

  @Override
  public boolean isEmpty() {
    return conditions.keySet().stream().allMatch(this::isEmptyFor);
  }

  @Override
  public boolean isEmptyFor(String pSenderId) {
    return !conditions.containsKey(pSenderId) || conditions.get(pSenderId).isEmpty();
  }

  @Override
  public ImmutableList<AbstractState> statesOf(Optional<String> pSenderId) {
    if (pSenderId.isPresent()) {
      return FluentIterable.from(
              conditions.getOrDefault(pSenderId.orElseThrow(), ImmutableListMultimap.of()).values())
          .transform(StateAndPrecision::state)
          .toList();
    }
    return FluentIterable.from(conditions.values())
        .transformAndConcat(Multimap::values)
        .transform(StateAndPrecision::state)
        .toList();
  }
}
