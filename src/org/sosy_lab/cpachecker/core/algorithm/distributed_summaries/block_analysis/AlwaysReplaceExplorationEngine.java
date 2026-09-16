// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysis.blockStateOf;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Explores the block from all preconditions an {@link AlwaysReplacePreconditionHandler} holds, one
 * input state at a time.
 *
 * <p>Preconditions and violation conditions are both grouped by program point, and every
 * combination of one input state with one condition group becomes its own CPA run. Because a
 * program-point hash covers the callstack, conditions of different call contexts of the same block
 * land in different groups, so exploring per group keeps the contexts apart.
 */
final class AlwaysReplaceExplorationEngine implements DssExplorationEngine {

  private final DssBlockAnalysis analysis;
  private final AlwaysReplacePreconditionHandler preconditionHandler;
  private final DssViolationConditionHandler violationConditions;

  AlwaysReplaceExplorationEngine(
      DssBlockAnalysis pAnalysis,
      AlwaysReplacePreconditionHandler pPreconditionHandler,
      DssViolationConditionHandler pViolationConditions) {
    analysis = pAnalysis;
    preconditionHandler = pPreconditionHandler;
    violationConditions = pViolationConditions;
  }

  @Override
  public AnalysisResult exploreInitially() throws CPAException, InterruptedException {
    DssBlockAnalysisResult result =
        analysis.runInitialBlockAnalysis(
            analysis.makeStartState(true), analysis.makeStartPrecision());

    if (result.getAllViolations().isEmpty()) {
      // The initial run publishes only the violations that originate inside the block. Its
      // postcondition stays unpublished, matching explore(Optional), which explores nothing as
      // long as no violation condition is known.
      return AnalysisResult.empty();
    }
    return AnalysisResult.ofViolationConditions(
        analysis.pathsFromOrigin(result.getAllViolations()));
  }

  /**
   * Explores the block from each known input under the different exit contexts and merges what the
   * individual rounds found.
   */
  @Override
  public AnalysisResult explore(Optional<String> pViolationConditionSender)
      throws CPAException, InterruptedException {
    pViolationConditionSender.ifPresent(
        sender ->
            checkArgument(
                !violationConditions.isEmptyFor(sender),
                "No violation condition found for sender ID: %s",
                sender));
    if (violationConditions.isEmpty()) {
      return AnalysisResult.empty();
    }
    BlockToProgramLocationMap preconditions = preconditionHandler.getPreconditions();
    if (pViolationConditionSender.isEmpty() && preconditions.isUnreachable()) {
      // every predecessor reported an unreachable block end, so this block cannot be entered
      return AnalysisResult.unreachableBlockEnd();
    }
    ImmutableListMultimap<Object, AbstractState> conditionsPerLocation;
    if (analysis.getOptions().callStackStateRequiresStateReset()) {
      conditionsPerLocation =
          ImmutableListMultimap.<Object, AbstractState>builder()
              .putAll(0, violationConditions.statesOf(Optional.empty()))
              .build();
    } else {
      // Violation conditions are grouped by program point exactly like preconditions are. Because a
      // program-point hash covers the callstack, conditions of different call contexts of the same
      // block land in different groups. Exploring a block under all of them at once would mix those
      // contexts: with inlining every call site would be its own block, but here one block is
      // shared by all of them, so the contexts have to be kept apart by exploring per group
      // instead.
      conditionsPerLocation =
          Multimaps.index(
              violationConditions.statesOf(Optional.empty()),
              condition -> analysis.getDcpa().computeProgramPointId(condition));
    }

    Precision precisionOfAnalysis =
        preconditions.isEmpty()
            ? analysis.makeStartPrecision()
            : analysis.combinePrecisions(preconditions.getStatesAndPrecisions());

    Exploration exploration = new Exploration(conditionsPerLocation, precisionOfAnalysis);
    for (Object preconditionProgramPoint : preconditions.getAllProgramPoints()) {
      Collection<AbstractState> inputs =
          preconditions.getStatesPerLocation(preconditionProgramPoint);
      if (analysis.getOptions().combinePreconditionsByHash()) {
        inputs =
            ImmutableList.of(
                analysis
                    .getDcpa()
                    .getCombineOperator()
                    .combinePreconditions(
                        transformedImmutableListCopy(inputs, analysis.getDcpa()::reset)));
      }
      for (AbstractState input : inputs) {
        exploration.exploreInput(input);
      }
    }
    if (preconditions.isEmpty() || preconditions.isAnyPredecessorTrulyEmpty()) {
      // A predecessor with no known state does not restrict the entry.
      exploration.requestUnconstrainedEntry();
    }
    return exploration.finish();
  }

  private enum RunMode {
    PROBE,
    PUBLISH,
    SPECULATIVE
  }

  /**
   * Lives for one invocation of {@link #explore(Optional)}. Only the final output and condition
   * group identifiers survive individual CPA runs; no probe states or results are retained.
   */
  private final class Exploration {

    private final ImmutableListMultimap<Object, AbstractState> conditions;
    private final Precision precision;
    private final ImmutableSet.Builder<StateAndPrecision> summaries = ImmutableSet.builder();
    private final ImmutableSet.Builder<ArgPathAndCondition> violations = ImmutableSet.builder();
    private final Set<ImmutableSet<Object>> pendingFallbacks = new LinkedHashSet<>();

    Exploration(ImmutableListMultimap<Object, AbstractState> pConditions, Precision pPrecision) {
      conditions = pConditions;
      precision = pPrecision;
    }

    void exploreInput(AbstractState pInput) throws CPAException, InterruptedException {
      if (conditions.keySet().size() == 1) {
        // No selection is needed for a single exit context, so avoid a separate probe.
        run(
            pInput,
            ImmutableSet.of(Iterables.getOnlyElement(conditions.keySet())),
            RunMode.PUBLISH);
        return;
      }

      Set<Object> successfulGroups = new LinkedHashSet<>();
      Object lastGroup = Iterables.getLast(conditions.keySet());
      for (Object group : conditions.keySet()) {
        if (successfulGroups.isEmpty() && group.equals(lastGroup)) {
          // Earlier groups contributed no exits. This last run cannot be replaced by a combination,
          // so consume its output directly without retaining a probe or repeating the analysis.
          run(pInput, ImmutableSet.of(group), RunMode.PUBLISH);
          return;
        }
        if (run(pInput, ImmutableSet.of(group), RunMode.PROBE)) {
          successfulGroups.add(group);
        }
      }
      ImmutableSet<Object> selectedGroups = ImmutableSet.copyOf(successfulGroups);
      if (!selectedGroups.isEmpty()) {
        // Recompute a complete postcondition for this same input. Even if only one group succeeded,
        // rerun it instead of retaining its ARG and precision while probing the other groups.
        run(pInput, selectedGroups, RunMode.PUBLISH);
      }
    }

    void requestUnconstrainedEntry() {
      pendingFallbacks.add(conditions.keySet());
    }

    AnalysisResult finish() throws CPAException, InterruptedException {
      // Requests from probes survive their replacement, including requests from unresolved probes.
      // Each distinct check runs once, and this set is discarded when this invocation finishes.
      for (ImmutableSet<Object> groups : pendingFallbacks) {
        run(analysis.makeStartState(true), groups, RunMode.SPECULATIVE);
      }
      Set<StateAndPrecision> finalSummaries = summaries.build();
      Set<ArgPathAndCondition> finalViolations = violations.build();
      return new AnalysisResult(
          analysis.deduplicateStatesAndPrecisions(finalSummaries),
          finalViolations,
          finalSummaries.isEmpty() && finalViolations.isEmpty());
    }

    /**
     * Consumes a run while its reached set is current. Only publishable outputs and fallback
     * identifiers are kept. The return value says whether this input completed with reachable
     * exits.
     */
    @CanIgnoreReturnValue
    private boolean run(AbstractState pInput, ImmutableSet<Object> pGroups, RunMode pMode)
        throws CPAException, InterruptedException {
      DssBlockAnalysisResult result =
          analysis.runBlockAnalysis(
              analysis.getDcpa().reset(pInput),
              precision,
              FluentIterable.from(pGroups).transformAndConcat(conditions::get).toList());
      boolean completed = result.getAllViolations().isEmpty();
      if (!completed) {
        violations.addAll(analysis.pathsWithCondition(result.getViolationConditionViolations()));
        violations.addAll(analysis.pathsFromOrigin(result.getTargetStates()));
      } else if (pMode == RunMode.PUBLISH) {
        // Publish all reachable exits of the selected analysis, including exits with incompatible
        // callstacks. Suppressing these exits can lose progress needed by other caller contexts.
        summaries.addAll(analysis.finalLocationStatesOf(result));
      }

      if (pMode != RunMode.SPECULATIVE
          && result.getFinalLocationStates().stream()
              .anyMatch(state -> !blockStateOf(state).getHinderedByCallstack().isEmpty())) {
        // A violation on another path does not discharge a callstack-rejected obligation.
        pendingFallbacks.add(pGroups);
      }
      return completed && !result.getFinalLocationStates().isEmpty();
    }
  }
}
