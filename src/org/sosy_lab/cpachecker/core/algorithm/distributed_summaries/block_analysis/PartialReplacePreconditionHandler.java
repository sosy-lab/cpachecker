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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Keeps the latest precondition set of every predecessor and skips predecessors whose set is
 * covered by the set of another one.
 *
 * <p>A predecessor is <em>covered</em> if every one of its states is subsumed by some state of
 * another predecessor. Exploring the block from a covered set cannot produce anything that
 * exploring from the covering set does not already produce, so only the <em>active</em>, i.e.,
 * uncovered, predecessors are explored by the {@link PartialReplaceExplorationEngine}. Every
 * covered predecessor remembers which active predecessor covers it, so that an update of the
 * covering predecessor re-checks exactly the predecessors that relied on it. A covering predecessor
 * is always active, so there are no chains of coverage to follow.
 *
 * <p>This handler ignores program points. It is meant for the inlining decomposition, where all
 * preconditions of a block are at the same program point anyway.
 */
final class PartialReplacePreconditionHandler implements DssPreconditionHandler {

  /** Stands in for the (nonexistent) predecessor of the root block. */
  private static final String ROOT_KEY = "root";

  private final DssBlockAnalysis analysis;

  /** Every predecessor this block can receive a postcondition from. */
  private final ImmutableSet<String> predecessors;

  /**
   * The latest precondition set of every predecessor that sent one. A predecessor whose block end
   * is unreachable is stored with the empty set.
   *
   * <p>An entry is replaced only if the received set differs from the stored one, so the {@link
   * PartialReplaceExplorationEngine} can tell by identity whether its cached exploration of a set
   * is still up to date.
   */
  private final Map<String, ImmutableList<@NonNull StateAndPrecision>> preconditions =
      new LinkedHashMap<>();

  /** Maps every covered predecessor to the active predecessor that covers it. */
  private final Map<String, String> coveredBy = new LinkedHashMap<>();

  PartialReplacePreconditionHandler(DssBlockAnalysis pAnalysis) throws InterruptedException {
    analysis = pAnalysis;
    if (analysis.getBlock().isRoot()) {
      // the root block has no predecessor to receive a precondition from, so it starts from the
      // unconstrained entry state under the synthetic key ROOT_KEY
      predecessors = ImmutableSet.of(ROOT_KEY);
      preconditions.put(
          ROOT_KEY,
          ImmutableList.of(
              new StateAndPrecision(
                  analysis.makeStartState(false), analysis.makeStartPrecision())));
    } else {
      predecessors = ImmutableSet.copyOf(analysis.getBlock().getPredecessorIds());
    }
  }

  @Override
  public DssMessageProcessing store(DssPostConditionMessage pReceived)
      throws InterruptedException, SolverException, CPAException {
    String sender = pReceived.getSenderId();
    ImmutableList<@NonNull StateAndPrecision> received =
        pReceived.indicatesUnreachableBlockEnd()
            ? ImmutableList.of()
            : analysis.deserialize(pReceived);
    DssSingleWorkerStatistics stats = analysis.statistics();
    stats.getStorePreconditionStatesTimer().start();
    try {
      if (!received.isEmpty()) {
        DssMessageProcessing processing = analysis.shouldProceedForward(received);
        if (!processing.shouldProceed()) {
          return processing;
        }
      }
      ImmutableList<@NonNull StateAndPrecision> previous = preconditions.get(sender);
      if (previous != null && analysis.statesEqual(previous, received)) {
        // Re-analysing on an update that changes nothing republishes the very same message.
        // Around a cycle in the block graph that never terminates.
        return DssMessageProcessing.stop();
      }
      boolean wasActive = previous != null && !coveredBy.containsKey(sender);
      preconditions.put(sender, received);
      return updateCoverage(sender, received, wasActive)
          ? DssMessageProcessing.proceed()
          : DssMessageProcessing.stop();
    } finally {
      stats.getStorePreconditionStatesTimer().stop();
      stats.getStorePreconditionStatesCounter().add(received.size());
    }
  }

  /**
   * Re-establishes the coverage relation after the precondition set of {@code pSender} changed.
   *
   * @param pWasActive whether the previous set of {@code pSender} was explored, i.e., whether it
   *     contributes to what this block last published
   * @return whether the set of active predecessors or one of their precondition sets changed, i.e.,
   *     whether what the block publishes has to be recomputed
   */
  private boolean updateCoverage(
      String pSender, ImmutableList<@NonNull StateAndPrecision> pReceived, boolean pWasActive)
      throws CPAException, InterruptedException {
    // the predecessors that relied on the previous set of the sender
    ImmutableList<String> dependents =
        FluentIterable.from(coveredBy.entrySet())
            .filter(e -> e.getValue().equals(pSender))
            .transform(Entry::getKey)
            .toList();
    coveredBy.remove(pSender);

    Optional<String> coverer = findCoverer(pSender, pReceived, pSender);
    coverer.ifPresent(c -> coveredBy.put(pSender, c));
    // An active sender has to be explored from its new set. A sender that just became covered
    // needs no exploration, but what it contributed so far has to be withdrawn.
    boolean changed = coverer.isEmpty() || pWasActive;

    for (String dependent : dependents) {
      ImmutableList<@NonNull StateAndPrecision> states = preconditions.get(dependent);
      if (analysis.allCovered(states, pReceived)) {
        // Still covered by the sender. If the sender itself is covered now, the dependent is
        // covered by the same predecessor (subsumption is transitive), which keeps every covering
        // predecessor active.
        coverer.ifPresent(c -> coveredBy.put(dependent, c));
        continue;
      }
      coveredBy.remove(dependent);
      // the sender was just shown not to cover the dependent, so there is no need to ask again
      Optional<String> otherCoverer = findCoverer(dependent, states, pSender);
      if (otherCoverer.isPresent()) {
        coveredBy.put(dependent, otherCoverer.orElseThrow());
      } else {
        changed = true;
      }
    }
    return changed;
  }

  /**
   * The first active predecessor other than {@code pPredecessor} and {@code pExcluded} whose
   * precondition set covers {@code pStates}.
   */
  private Optional<String> findCoverer(
      String pPredecessor, ImmutableList<@NonNull StateAndPrecision> pStates, String pExcluded)
      throws CPAException, InterruptedException {
    for (Entry<String, ImmutableList<@NonNull StateAndPrecision>> candidate :
        preconditions.entrySet()) {
      String key = candidate.getKey();
      if (key.equals(pPredecessor) || key.equals(pExcluded) || coveredBy.containsKey(key)) {
        continue;
      }
      if (analysis.allCovered(pStates, candidate.getValue())) {
        return Optional.of(key);
      }
    }
    return Optional.empty();
  }

  @Override
  public ImmutableList<@NonNull StateAndPrecision> getKnownPreconditions() {
    return FluentIterable.concat(preconditions.values()).toList();
  }

  /**
   * The precondition sets of all active predecessors, i.e., the ones that have sent something and
   * are not covered by another predecessor. The {@link PartialReplaceExplorationEngine} explores
   * exactly these.
   */
  ImmutableMap<String, ImmutableList<@NonNull StateAndPrecision>> getActivePreconditions() {
    return ImmutableMap.copyOf(
        Maps.filterKeys(preconditions, predecessor -> !coveredBy.containsKey(predecessor)));
  }

  /**
   * Whether some predecessor has not sent anything yet. Such a predecessor does not restrict the
   * block entry, so the default exploration also probes the unconstrained start state.
   */
  boolean isAnyPredecessorSilent() {
    return !preconditions.keySet().containsAll(predecessors);
  }

  /** Whether every predecessor has reported its block end to be unreachable. */
  boolean isUnreachable() {
    return !isAnyPredecessorSilent()
        && preconditions.values().stream().allMatch(ImmutableList::isEmpty);
  }

  /** The predecessor covering the given one, if any. Exposed for tests. */
  Optional<String> getCoverer(String pPredecessor) {
    return Optional.ofNullable(coveredBy.get(pPredecessor));
  }
}
