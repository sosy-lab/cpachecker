// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.WithholdingStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Explores the block from the contexts a {@link PathBasedPreconditionHandler} holds, under all
 * violation conditions at once.
 *
 * <p>After a postcondition arrived, only the contexts it added or changed are explored; after a
 * violation condition arrived, all of them are. Each context publishes its postcondition, retracts
 * it if the block end turned out to be unreachable, or withholds it if a violation was found. The
 * search thus proceeds backwards from the violations first, like with {@link
 * AlwaysReplaceExplorationEngine}: postconditions spread only from contexts without violations, and
 * a block that may miss contexts explores speculatively from the unconstrained entry state.
 */
final class PathBasedExplorationEngine implements DssExplorationEngine {

  private final DssBlockAnalysis analysis;
  private final PathBasedPreconditionHandler preconditions;
  private final PathBasedViolationConditionHandler violationConditions;

  /**
   * Whether the block was explored speculatively under the current violation conditions since it
   * last started to miss contexts.
   */
  private boolean speculationUpToDate = false;

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

  @Override
  public AnalysisResult explore(boolean pViolationConditionsChanged)
      throws CPAException, InterruptedException {
    ImmutableSet<BlockGraphPath> changed = preconditions.consumeChangedContexts();
    // Every context has to be checked against new conditions, not only the changed ones. Such a run
    // may refine the precision against the new conditions, so its postcondition is published as
    // well, even if the context itself did not change.
    ImmutableSet<BlockGraphPath> toExplore =
        pViolationConditionsChanged ? preconditions.getAllContexts() : changed;
    ImmutableList<AbstractState> conditions = violationConditions.states();
    // conditions that no explored context could be checked against, because the callstack of the
    // context does not fit the one the condition expects at the block end
    Set<AbstractState> hindered = new LinkedHashSet<>();
    ImmutableList.Builder<StateAndPrecision> summaries = ImmutableList.builder();
    ImmutableSet.Builder<ArgPathAndCondition> violations = ImmutableSet.builder();

    // Like AlwaysReplaceExplorationEngine, every context is explored with the precision of all
    // contexts combined: what one context's refinements found helps the others, which in
    // particular lets the context that a loop carries back into the block learn from the context
    // entering it.
    Precision precision =
        preconditions.getKnownPreconditions().isEmpty()
            ? analysis.makeStartPrecision()
            : analysis.combinePrecisions(preconditions.getKnownPreconditions());
    for (BlockGraphPath path : toExplore) {
      if (!preconditions.hasContext(path)) {
        // removed while exploring, as a descendant of a context without postcondition
        continue;
      }
      ImmutableList.Builder<StateAndPrecision> summariesOfContext = ImmutableList.builder();
      boolean foundViolation = false;
      for (StateAndPrecision precondition : preconditions.getStates(path)) {
        DssBlockAnalysisResult result =
            analysis.runBlockAnalysis(
                analysis.getDcpa().reset(precondition.state()), precision, conditions);

        ImmutableList<StateAndPrecision> summariesOfRun = analysis.summariesOf(result);
        summariesOfContext.addAll(summariesOfRun);
        for (StateAndPrecision summary : summariesOfRun) {
          hindered.addAll(summary.getBlockState().getHinderedByCallstack());
        }

        // TODO we only want to combine violations with the same precondition id
        if (!result.getAllViolations().isEmpty()) {
          foundViolation = true;
          violations.addAll(analysis.pathsWithCondition(result.getViolationConditionViolations()));
          violations.addAll(analysis.pathsFromOrigin(result.getTargetStates()));
        }
      }
      ImmutableList<StateAndPrecision> postcondition = summariesOfContext.build();
      preconditions.setWithheld(path, foundViolation);
      if (foundViolation) {
        // Like AlwaysReplaceExplorationEngine, a context with a violation publishes no
        // postcondition until the violation is resolved, which lets the search proceed backwards
        // instead of unrolling the program forward. The successors keep what they have, and the
        // blocks downstream explore speculatively as long as they learn that this block withholds
        // a postcondition (see PathBasedPreconditionHandler#mayMissContexts).
        continue;
      }
      if (postcondition.isEmpty()) {
        // The block end is unreachable from this context. A successor may still hold what it
        // derived from an earlier generation of the context, which has to go.
        preconditions.contextProducedNoPostcondition(path);
      }
      summaries.addAll(postcondition);
    }

    violations.addAll(
        exploreSpeculatively(pViolationConditionsChanged, conditions, hindered, precision));
    return publish(summaries.build(), violations.build());
  }

  /**
   * Explores the block from the unconstrained entry state if it may miss contexts, or if the
   * callstack of every known context hinders some conditions, and returns the violations found. The
   * postcondition of such a run describes no context and is not published. The root block never
   * explores speculatively: a violation it reports counts as a counterexample.
   */
  private ImmutableSet<ArgPathAndCondition> exploreSpeculatively(
      boolean pViolationConditionsChanged,
      ImmutableList<AbstractState> pConditions,
      Set<AbstractState> pHinderedConditions,
      Precision pPrecision)
      throws CPAException, InterruptedException {
    Optional<StateAndPrecision> speculativeStart = preconditions.getSpeculativeStart();
    ImmutableList<AbstractState> speculativeConditions;
    boolean mayMissContexts = preconditions.mayMissContexts();
    if (mayMissContexts && (pViolationConditionsChanged || !speculationUpToDate)) {
      // A context this block does not know may reach the conditions. This is repeated whenever the
      // conditions change or the block starts to miss contexts again.
      speculativeConditions = pConditions;
      speculationUpToDate = true;
    } else {
      speculationUpToDate &= mayMissContexts;
      // A condition that the callstack of every known context hinders would never be propagated,
      // although a context whose callstack is not fully known may fit it (see
      // DssCallstackTransferRelation).
      speculativeConditions = ImmutableList.copyOf(pHinderedConditions);
    }
    boolean explore = !speculativeConditions.isEmpty() && speculativeStart.isPresent();
    preconditions.setExploredSpeculatively(explore);
    if (!explore) {
      return ImmutableSet.of();
    }
    DssBlockAnalysisResult result =
        analysis.runBlockAnalysis(
            analysis.getDcpa().reset(speculativeStart.orElseThrow().state()),
            pPrecision,
            speculativeConditions);
    return ImmutableSet.<ArgPathAndCondition>builder()
        .addAll(analysis.pathsWithCondition(result.getViolationConditionViolations()))
        .addAll(analysis.pathsFromOrigin(result.getTargetStates()))
        .build();
  }

  /**
   * Assembles what one exploration publishes: the postconditions, the violations, and the update of
   * the contexts that the successors have to learn about even without a postcondition.
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
    ImmutableList<BlockGraphPath> retracted = preconditions.consumeRetractedContexts();
    Optional<ImmutableMap<String, WithholdingStatus>> withholding =
        preconditions.consumeWithholdingToAnnounce();
    Optional<ContextUpdate> update =
        retracted.isEmpty() && withholding.isEmpty()
            ? Optional.empty()
            : Optional.of(new ContextUpdate(retracted, withholding.orElse(ImmutableMap.of())));
    return new AnalysisResult(deduplicated.build(), pViolations, false, update);
  }
}
