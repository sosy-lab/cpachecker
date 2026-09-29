// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Keeps the latest set of violation conditions of every successor. A message replaces the set of
 * its sender, unless the received set equals the stored one, in which case there is nothing to do.
 *
 * <p>The {@link PartialReplaceExplorationEngine} caches what it found under every condition and
 * identifies the conditions by identity. A received condition that equals a stored one of any
 * sender is therefore replaced by the stored object, so that only the conditions that are actually
 * new have to be explored. A successor sends all its conditions whenever one of them changes, so
 * without this, one changed condition would make the block explore all of them again.
 */
final class PartialReplaceViolationConditionHandler implements DssViolationConditionHandler {

  private final DssBlockAnalysis analysis;

  private final Map<String, ImmutableList<@NonNull StateAndPrecision>> conditions =
      new LinkedHashMap<>();

  PartialReplaceViolationConditionHandler(DssBlockAnalysis pAnalysis) {
    analysis = pAnalysis;
  }

  @Override
  public DssMessageProcessing store(DssViolationConditionMessage pReceived)
      throws InterruptedException, CPAException {
    ImmutableList<@NonNull StateAndPrecision> received = analysis.deserialize(pReceived);
    DssSingleWorkerStatistics stats = analysis.statistics();
    stats.getStoreViolationConditionStatesTimer().start();
    try {
      String sender = pReceived.getSenderId();
      ImmutableList<@NonNull StateAndPrecision> stored =
          conditions.getOrDefault(sender, ImmutableList.of());
      ImmutableSet<AbstractState> previousConditions = ImmutableSet.copyOf(states());
      ImmutableList<@NonNull StateAndPrecision> merged =
          reuseEqual(received, FluentIterable.concat(conditions.values()).toList());
      if (ImmutableSet.copyOf(merged).equals(ImmutableSet.copyOf(stored))) {
        return DssMessageProcessing.stop();
      }
      conditions.put(sender, merged);
      // Keep ownership per successor, but explore an equivalent condition only once. Withdrawing
      // one owner's copy must leave both the other owner and the cached representative intact.
      return previousConditions.equals(ImmutableSet.copyOf(states()))
          ? DssMessageProcessing.stop()
          : DssMessageProcessing.proceed();
    } finally {
      stats.getStoreViolationConditionStatesTimer().stop();
      stats.getStoreViolationConditionStatesCounter().add(received.size());
    }
  }

  /**
   * Replaces every received condition that equals a stored one by the stored one.
   *
   * @return the received conditions, each either as received or as the equal stored object
   */
  private ImmutableList<@NonNull StateAndPrecision> reuseEqual(
      ImmutableList<@NonNull StateAndPrecision> pReceived,
      ImmutableList<@NonNull StateAndPrecision> pStored)
      throws InterruptedException, CPAException {
    ImmutableSet.Builder<@NonNull StateAndPrecision> merged = ImmutableSet.builder();
    List<StateAndPrecision> representatives = new ArrayList<>(pStored);
    for (StateAndPrecision condition : pReceived) {
      StateAndPrecision equal = condition;
      for (StateAndPrecision candidate : representatives) {
        if (analysis.violationConditionEqual(condition.state(), candidate.state())) {
          equal = candidate;
          break;
        }
      }
      if (equal == condition) {
        representatives.add(equal);
      }
      merged.add(equal);
    }
    return merged.build().asList();
  }

  @Override
  public ImmutableList<StateAndPrecision> getKnownConditions() {
    return FluentIterable.concat(conditions.values()).toList();
  }

  @Override
  public ImmutableList<AbstractState> states() {
    return FluentIterable.concat(conditions.values())
        .transform(StateAndPrecision::state)
        .toSet()
        .asList();
  }
}
