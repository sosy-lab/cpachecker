// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis.blockStateOf;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;

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
      conditions.putIfAbsent(pReceived.getSenderId(), ArrayListMultimap.create());
      Multimap<String, StateAndPrecision> mapForSuccessor = conditions.get(pReceived.getSenderId());
      ImmutableListMultimap<String, @NonNull StateAndPrecision> vcsByID =
          indexByPrecondition(received);
      // replace all newly received keys
      vcsByID.keySet().forEach(id -> mapForSuccessor.removeAll(id));
      mapForSuccessor.putAll(vcsByID);
      List<String> toRemove =
          mapForSuccessor.keySet().stream()
              .filter(k -> !pReceived.getRemainingPreconditions().contains(k))
              .toList();
      toRemove.forEach(remainingId -> mapForSuccessor.removeAll(remainingId));
    } finally {
      stats.getStoreViolationConditionStatesTimer().stop();
      stats.getStoreViolationConditionStatesCounter().add(received.size());
    }
    return received.isEmpty() ? DssMessageProcessing.stop() : DssMessageProcessing.proceed();
  }

  /**
   * Indexes the received violation conditions by the id of the precondition of the sender they were
   * computed for.
   *
   * <p>Violation conditions that reach the same program point may be combined into a single state,
   * whose id then combines the ids of all preconditions it stems from. Such a state is indexed
   * under each of these ids, so that it is replaced as soon as any of them is superseded or gone.
   */
  private ImmutableListMultimap<String, @NonNull StateAndPrecision> indexByPrecondition(
      List<@NonNull StateAndPrecision> pViolationConditions) {
    ImmutableListMultimap.Builder<String, @NonNull StateAndPrecision> vcsByID =
        ImmutableListMultimap.builder();
    for (StateAndPrecision violationCondition : pViolationConditions) {
      for (String id :
          BlockState.splitUniqueId(blockStateOf(violationCondition.state()).getUniqueId())) {
        vcsByID.put(id, violationCondition);
      }
    }
    return vcsByID.build();
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
    // a combined violation condition is stored under each of the precondition ids it stems from,
    // so it has to be reported only once
    if (pSenderId.isPresent()) {
      return FluentIterable.from(
              conditions.getOrDefault(pSenderId.orElseThrow(), ImmutableListMultimap.of()).values())
          .transform(StateAndPrecision::state)
          .toSet()
          .asList();
    }
    return FluentIterable.from(conditions.values())
        .transformAndConcat(Multimap::values)
        .transform(StateAndPrecision::state)
        .toSet()
        .asList();
  }
}
