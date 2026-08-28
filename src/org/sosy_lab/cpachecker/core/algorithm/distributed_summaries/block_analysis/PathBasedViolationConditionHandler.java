// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis.blockStateOf;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssDebugUtils;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Keeps only the violation conditions of the latest message per sending block: an update discards
 * everything previously received from that block.
 */
final class PathBasedViolationConditionHandler implements DssViolationConditionHandler {

  private final Map<String, Multimap<String, StateAndPrecision>> conditions;

  private final DssBlockAnalysis analysis;

  /** Conditions that still need to be explored, with equivalent ones kept only once. */
  private ImmutableList<StateAndPrecision> conditionsToExplore = ImmutableList.of();

  PathBasedViolationConditionHandler(DssBlockAnalysis pAnalysis) {
    conditions = new LinkedHashMap<>();
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
    ImmutableList<StateAndPrecision> updatedConditionsToExplore =
        analysis.deduplicateStatesAndPrecisions(
            conditions.values().stream().flatMap(m -> m.values().stream()).toList());
    boolean globalConditionSetUnchanged =
        analysis.allCovered(updatedConditionsToExplore, conditionsToExplore)
            && analysis.allCovered(conditionsToExplore, updatedConditionsToExplore);
    conditionsToExplore = updatedConditionsToExplore;
    return globalConditionSetUnchanged
        ? DssMessageProcessing.stop()
        : DssMessageProcessing.proceed();
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
    return transformedImmutableListCopy(conditionsToExplore, StateAndPrecision::state);
  }

  /** Renders {@link #conditions} and {@link #conditionsToExplore} for debugging. */
  @Override
  public String toString() {
    List<List<String>> conditionRows = new ArrayList<>();
    for (Entry<String, Multimap<String, StateAndPrecision>> bySender : conditions.entrySet()) {
      String sender = bySender.getKey();
      for (Entry<String, StateAndPrecision> byPrecondition : bySender.getValue().entries()) {
        conditionRows.add(
            ImmutableList.of(
                sender,
                byPrecondition.getKey(),
                DssDebugUtils.oneLine(byPrecondition.getValue().state())));
        sender = "";
      }
    }
    String conditionsBody =
        conditionRows.isEmpty()
            ? "<none>"
            : DssDebugUtils.table(
                ImmutableList.of("from", "preconditionId", "state"), conditionRows);

    List<List<String>> toExploreRows = new ArrayList<>();
    int index = 0;
    for (StateAndPrecision stateAndPrecision : conditionsToExplore) {
      toExploreRows.add(
          ImmutableList.of(
              Integer.toString(index++), DssDebugUtils.oneLine(stateAndPrecision.state())));
    }
    String toExploreBody =
        toExploreRows.isEmpty()
            ? "<none>"
            : DssDebugUtils.table(ImmutableList.of("#", "state"), toExploreRows);

    String body =
        "conditions ("
            + conditionRows.size()
            + " states from "
            + conditions.size()
            + " senders):\n"
            + DssDebugUtils.indent("  ", conditionsBody)
            + "\n\nconditions to explore ("
            + conditionsToExplore.size()
            + "):\n"
            + DssDebugUtils.indent("  ", toExploreBody);
    return DssDebugUtils.box("ViolationConditions of Block " + analysis.getBlock().getId(), body);
  }
}
