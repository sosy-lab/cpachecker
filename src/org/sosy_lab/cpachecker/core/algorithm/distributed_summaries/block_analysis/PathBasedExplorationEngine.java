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
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Explores the block from the contexts a {@link PathBasedPreconditionHandler} holds, under all
 * violation conditions at once.
 *
 * <p>After a postcondition arrived, only the contexts it added or changed are explored; after a
 * violation condition arrived, all of them are. Each context publishes its postcondition, or
 * retracts it if the block end turned out to be unreachable. Like with {@link
 * AlwaysReplaceExplorationEngine}, the forward wave only starts once the root block received a
 * violation condition, and a block that may still miss contexts is explored speculatively from the
 * unconstrained entry state, so the search first proceeds backwards from the violations.
 */
final class PathBasedExplorationEngine implements DssExplorationEngine {

  private final DssBlockAnalysis analysis;
  private final PathBasedPreconditionHandler preconditions;
  private final PathBasedViolationConditionHandler violationConditions;

  /**
   * What exploring each context found so far, which stays valid as long as the context does not
   * change (see {@link Findings}).
   */
  private final Map<BlockGraphPath, Findings> findingsOfContexts = new HashMap<>();

  /** What exploring the speculative start state found so far. */
  private Findings speculativeFindings = new Findings();

  PathBasedExplorationEngine(
      DssBlockAnalysis pAnalysis,
      PathBasedPreconditionHandler pPreconditions,
      PathBasedViolationConditionHandler pViolationConditions) {
    analysis = pAnalysis;
    preconditions = pPreconditions;
    violationConditions = pViolationConditions;
  }

  /**
   * Reports the violations that originate inside the block, found from the unconstrained entry
   * state.
   *
   * <p>No postcondition is published yet, not even by the root block, whose only context is that
   * very state: like with {@link AlwaysReplaceExplorationEngine}, the forward wave starts only once
   * the root block has received a violation condition. Until then, the blocks search backwards from
   * the violations by exploring speculatively from the unconstrained entry state, which finds a
   * counterexample without unrolling any loop forward, and a program in which no violation
   * condition ever reaches the root block needs no forward wave at all.
   */
  @Override
  public AnalysisResult exploreInitially() throws CPAException, InterruptedException {
    StateAndPrecision start =
        preconditions
            .getSpeculativeStart()
            .orElseGet(() -> Iterables.getOnlyElement(preconditions.getKnownPreconditions()));
    DssBlockAnalysisResult result =
        analysis.runInitialBlockAnalysis(
            analysis.getDcpa().reset(start.state()), start.precision());
    // This run carries no violation condition, so every violation it finds originates inside this
    // block and is reported from its origin.
    return AnalysisResult.ofViolationConditions(
        analysis.pathsFromOrigin(result.getAllViolations()));
  }

  /**
   * Explores the contexts that changed, and, if the violation conditions changed, checks every
   * other context against the conditions it has not been checked against yet.
   *
   * <p>A context whose states did not change was already checked against the conditions it had seen
   * before, and whether a condition is violated from these states does not depend on the other
   * conditions. So only the new conditions have to be explored, and the violations found for the
   * old ones are kept (see {@link Findings}). The violation conditions of a context are published
   * completely whenever something about them changed, because a predecessor replaces all conditions
   * that stem from one of this block's contexts at once.
   */
  @Override
  public AnalysisResult explore(boolean pViolationConditionsChanged)
      throws CPAException, InterruptedException {
    ImmutableSet<BlockGraphPath> changed = preconditions.consumeChangedContexts();
    ImmutableSet<BlockGraphPath> toExplore =
        pViolationConditionsChanged ? preconditions.getAllContexts() : changed;
    ImmutableList<AbstractState> conditions = violationConditions.states();
    Set<AbstractState> current = Sets.newIdentityHashSet();
    current.addAll(conditions);
    ImmutableList.Builder<StateAndPrecision> summaries = ImmutableList.builder();
    ImmutableSet.Builder<ArgPathAndCondition> violations = ImmutableSet.builder();

    // Like AlwaysReplaceExplorationEngine, every context is explored with the precision of all
    // contexts combined: what one context's refinements found helps the others, which in
    // particular lets the context that a loop carries back into the block learn from the context
    // entering it.
    Precision precision =
        preconditions.getPreconditionsWithParked().isEmpty()
            ? analysis.makeStartPrecision()
            : analysis.combinePrecisions(preconditions.getPreconditionsWithParked());
    findingsOfContexts.keySet().removeIf(path -> !preconditions.hasContext(path));
    for (BlockGraphPath path : toExplore) {
      if (!preconditions.hasContext(path)) {
        // removed while exploring, as a descendant of a context without postcondition
        continue;
      }
      boolean isChanged = changed.contains(path);
      if (isChanged) {
        // the findings for the previous states of the path do not apply to the new ones
        findingsOfContexts.put(path, new Findings());
      }
      Findings findings = findingsOfContexts.computeIfAbsent(path, p -> new Findings());
      boolean lostViolations = findings.retainAll(current);
      ImmutableList<AbstractState> unchecked = findings.unchecked(conditions);
      boolean foundViolations = false;
      if (isChanged || !unchecked.isEmpty()) {
        ImmutableList.Builder<StateAndPrecision> summariesOfContext = ImmutableList.builder();
        for (StateAndPrecision precondition : preconditions.getStates(path)) {
          DssBlockAnalysisResult result =
              analysis.runBlockAnalysis(
                  analysis.getDcpa().reset(precondition.state()), precision, unchecked);
          ImmutableList<StateAndPrecision> summariesOfRun = analysis.summariesOf(result);
          summariesOfContext.addAll(summariesOfRun);
          foundViolations |= findings.record(analysis, unchecked, result, summariesOfRun);
        }
        // The postcondition is published even if a violation was found: withholding it would leave
        // the entries of the successors without an over-approximation of what this block knows.
        // Such a run may have refined the precision, so it is published even if the context
        // itself did not change; a successor discards it if it is not stronger than what it has.
        ImmutableList<StateAndPrecision> postcondition = summariesOfContext.build();
        if (postcondition.isEmpty()) {
          // The block end is unreachable from this context. A successor may still hold what it
          // derived from an earlier generation of the context, which has to go.
          preconditions.contextProducedNoPostcondition(path);
        }
        summaries.addAll(postcondition);
      }
      if (foundViolations || lostViolations) {
        violations.addAll(findings.violations());
      }
    }

    Set<AbstractState> hindered = Sets.newIdentityHashSet();
    for (Findings findings : findingsOfContexts.values()) {
      hindered.addAll(findings.hindered);
    }
    violations.addAll(exploreSpeculatively(conditions, hindered, precision));
    return publish(summaries.build(), violations.build());
  }

  /**
   * Explores the block from the unconstrained entry state if it may miss contexts, or if the
   * callstack of every known context hinders some conditions. The postcondition of such a run
   * describes no context and is not published. The root block never explores speculatively: a
   * violation it reports counts as a counterexample.
   *
   * @return the violations found from the speculative start state if they changed, all of them,
   *     because a predecessor replaces the conditions of that state at once, or nothing otherwise
   */
  private ImmutableSet<ArgPathAndCondition> exploreSpeculatively(
      ImmutableList<AbstractState> pConditions,
      Set<AbstractState> pHinderedConditions,
      Precision pPrecision)
      throws CPAException, InterruptedException {
    Optional<StateAndPrecision> speculativeStart = preconditions.getSpeculativeStart();
    ImmutableList<AbstractState> speculativeConditions =
        preconditions.mayMissContexts()
            // a context this block does not know yet may reach any of the conditions
            ? pConditions
            // A condition that the callstack of every known context hinders would never be
            // propagated, although a context whose callstack is not fully known may fit it (see
            // DssCallstackTransferRelation).
            : ImmutableList.copyOf(pHinderedConditions);
    if (speculativeStart.isEmpty() || speculativeConditions.isEmpty()) {
      preconditions.setExploredSpeculatively(false);
      // once speculation starts again, it has to check every condition again
      speculativeFindings = new Findings();
      return ImmutableSet.of();
    }
    preconditions.setExploredSpeculatively(true);
    Set<AbstractState> current = Sets.newIdentityHashSet();
    current.addAll(speculativeConditions);
    boolean lostViolations = speculativeFindings.retainAll(current);
    ImmutableList<AbstractState> unchecked = speculativeFindings.unchecked(speculativeConditions);
    boolean foundViolations = false;
    if (!unchecked.isEmpty()) {
      DssBlockAnalysisResult result =
          analysis.runBlockAnalysis(
              analysis.getDcpa().reset(speculativeStart.orElseThrow().state()),
              pPrecision,
              unchecked);
      foundViolations = speculativeFindings.record(analysis, unchecked, result, ImmutableList.of());
    }
    return foundViolations || lostViolations ? speculativeFindings.violations() : ImmutableSet.of();
  }

  /**
   * What exploring the states of one context found so far. It stays valid as long as these states
   * do not change: whether a violation condition is violated from the states does not depend on the
   * other conditions, so the states only have to be checked against conditions they were not
   * checked against yet. Conditions are identified by identity, as the violation-condition handler
   * hands them out.
   */
  private static final class Findings {

    /** The conditions the states were checked against. */
    private final Set<AbstractState> checked = Sets.newIdentityHashSet();

    /** The paths that violate a condition, per condition. */
    private final IdentityHashMap<AbstractState, Set<ArgPathAndCondition>> violationsPerCondition =
        new IdentityHashMap<>();

    /** The paths to a target inside the block, which violate the property on every condition. */
    private final Set<ArgPathAndCondition> violationsFromOrigin = new LinkedHashSet<>();

    /**
     * The conditions that the callstack of the states hindered, see DssCallstackTransferRelation.
     */
    private final Set<AbstractState> hindered = Sets.newIdentityHashSet();

    /** The given conditions that the states were not checked against yet. */
    ImmutableList<AbstractState> unchecked(List<AbstractState> pConditions) {
      return FluentIterable.from(pConditions).filter(c -> !checked.contains(c)).toList();
    }

    /**
     * Forgets everything about conditions that are not among the given ones anymore.
     *
     * @return whether violations of such a condition were known
     */
    boolean retainAll(Set<AbstractState> pCurrent) {
      checked.retainAll(pCurrent);
      hindered.retainAll(pCurrent);
      return violationsPerCondition.keySet().removeIf(condition -> !pCurrent.contains(condition));
    }

    /**
     * Records what a run of the states against the given conditions found.
     *
     * @return whether the run found a violation
     */
    boolean record(
        DssBlockAnalysis pAnalysis,
        Collection<AbstractState> pConditions,
        DssBlockAnalysisResult pResult,
        Collection<StateAndPrecision> pSummaries) {
      checked.addAll(pConditions);
      for (StateAndPrecision summary : pSummaries) {
        hindered.addAll(summary.getBlockState().getHinderedByCallstack());
      }
      for (ARGState violation : pResult.getViolationConditionViolations()) {
        AbstractState condition =
            Iterables.getOnlyElement(
                DssBlockAnalysis.blockStateOf(violation).getViolationConditions());
        violationsPerCondition
            .computeIfAbsent(condition, c -> new LinkedHashSet<>())
            .addAll(pAnalysis.pathsWithCondition(ImmutableList.of(violation)));
      }
      violationsFromOrigin.addAll(pAnalysis.pathsFromOrigin(pResult.getTargetStates()));
      return !pResult.getAllViolations().isEmpty();
    }

    /** All violations known for the current conditions. */
    ImmutableSet<ArgPathAndCondition> violations() {
      return ImmutableSet.<ArgPathAndCondition>builder()
          .addAll(Iterables.concat(violationsPerCondition.values()))
          .addAll(violationsFromOrigin)
          .build();
    }
  }

  /**
   * Assembles what one exploration publishes: the postconditions, the violations, and the contexts
   * that the successors have to drop even without a postcondition.
   */
  private AnalysisResult publish(
      ImmutableList<StateAndPrecision> pSummaries, ImmutableSet<ArgPathAndCondition> pViolations)
      throws CPAException, InterruptedException {
    // Deduplicated per path only: equality of states ignores the path, and a receiver keys its
    // preconditions by path, so a summary dropped as a duplicate of another path's summary would
    // never reach the receiver under its own path.
    ImmutableList.Builder<StateAndPrecision> deduplicated = ImmutableList.builder();
    for (Collection<StateAndPrecision> samePath :
        Multimaps.index(pSummaries, StateAndPrecision::getBlockGraphPath).asMap().values()) {
      deduplicated.addAll(analysis.deduplicateStatesAndPrecisions(samePath));
    }
    return new AnalysisResult(
        deduplicated.build(), pViolations, false, preconditions.consumeRetractedContexts());
  }
}
