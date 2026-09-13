// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePreconditionsOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;

public class AlwaysReplaceExplorationEngineTest {

  private final DssBlockAnalysis analysis = mock(DssBlockAnalysis.class);
  private final DistributedConfigurableProgramAnalysis dcpa =
      mock(DistributedConfigurableProgramAnalysis.class);
  private final BlockToProgramLocationMap preconditions = mock(BlockToProgramLocationMap.class);
  private final AbstractState entry = mock(AbstractState.class);
  private final ArgPathAndCondition violation = mock(ArgPathAndCondition.class);
  private final AbstractState condition = mock(AbstractState.class);
  private final DssViolationConditionHandler conditionHandler =
      mock(DssViolationConditionHandler.class);
  private final ArgPathAndCondition fallbackViolation = mock(ArgPathAndCondition.class);
  private final BlockState summaryBlock = mock(BlockState.class);
  private final ARGState summaryState = new ARGState(summaryBlock, null);
  private final StateAndPrecision summary =
      new StateAndPrecision(summaryState, SingletonPrecision.getInstance());
  private final DssBlockAnalysisResult initialResult = mock(DssBlockAnalysisResult.class);
  private AlwaysReplaceExplorationEngine engine;

  @Before
  public void setUp() throws Exception {
    var preconditionHandler = mock(AlwaysReplacePreconditionHandler.class);
    var target = new ARGState(mock(AbstractState.class), null);
    var result = initialResult;
    var precision = SingletonPrecision.getInstance();
    when(summaryBlock.getHinderedByCallstack()).thenReturn(ImmutableSet.of());
    when(analysis.getDcpa()).thenReturn(dcpa);
    when(analysis.getOptions())
        .thenReturn(new DssAnalysisOptions(Configuration.defaultConfiguration()));
    when(analysis.makeStartPrecision()).thenReturn(precision);
    when(analysis.makeStartState(true)).thenReturn(entry);
    when(analysis.combinePrecisions(any())).thenReturn(precision);
    when(preconditionHandler.getPreconditions()).thenReturn(preconditions);
    when(preconditions.getAllProgramPoints()).thenReturn(ImmutableSet.of("entry"));
    when(preconditions.getStatesPerLocation("entry")).thenReturn(ImmutableList.of(entry));
    when(conditionHandler.statesOf(Optional.empty())).thenReturn(ImmutableList.of(condition));
    when(dcpa.computeProgramPointId(condition)).thenReturn("condition");
    when(dcpa.reset(entry)).thenReturn(entry);
    when(analysis.runBlockAnalysis(entry, precision, ImmutableList.of(condition)))
        .thenReturn(result);
    when(result.getFinalLocationStates()).thenReturn(ImmutableSet.of());
    when(result.getAllViolations()).thenReturn(ImmutableSet.of(target));
    when(result.getTargetStates()).thenReturn(ImmutableSet.of(target));
    when(result.getViolationConditionViolations()).thenReturn(ImmutableSet.of());
    when(analysis.pathsFromOrigin(ImmutableSet.of(target))).thenReturn(ImmutableSet.of(violation));
    when(analysis.pathsWithCondition(ImmutableSet.of())).thenReturn(ImmutableSet.of());
    when(analysis.summariesOf(result)).thenReturn(ImmutableList.of(summary));
    when(analysis.deduplicateStatesAndPrecisions(any()))
        .thenAnswer(
            invocation ->
                ImmutableList.copyOf(invocation.<Iterable<StateAndPrecision>>getArgument(0)));
    engine = new AlwaysReplaceExplorationEngine(analysis, preconditionHandler, conditionHandler);
  }

  @Test
  public void unresolvedAnalysisDoesNotPublishUnrefinedExits() throws Exception {
    AnalysisResult result = engine.explore(Optional.empty());
    assertThat(result.summaries()).isEmpty();
    assertThat(result.violationConditions()).containsExactly(violation);
    assertThat(result.blockEndUnreachable()).isFalse();
  }

  @Test
  public void refinedExitsSurviveViolationsFromAnotherEntryState() throws Exception {
    AbstractState otherEntry = mock(AbstractState.class);
    configureRefinedEntry(otherEntry);

    // Both inputs have the same program point, and their processing order must not matter.
    for (var entries :
        ImmutableList.of(
            ImmutableList.of(entry, otherEntry), ImmutableList.of(otherEntry, entry))) {
      when(preconditions.getStatesPerLocation("entry")).thenReturn(entries);
      AnalysisResult result = engine.explore(Optional.empty());
      assertThat(result.summaries()).containsExactly(summary);
      assertThat(result.violationConditions()).containsExactly(violation);
      assertThat(result.blockEndUnreachable()).isFalse();
    }
  }

  private void configureRefinedEntry(AbstractState pEntry) throws Exception {
    DssBlockAnalysisResult refined = mock(DssBlockAnalysisResult.class);
    when(dcpa.reset(pEntry)).thenReturn(pEntry);
    when(analysis.runBlockAnalysis(eq(pEntry), any(), any())).thenReturn(refined);
    when(refined.getAllViolations()).thenReturn(ImmutableSet.of());
    when(refined.getFinalLocationStates()).thenReturn(ImmutableSet.of(summaryState));
    when(analysis.summariesOf(refined)).thenReturn(ImmutableList.of(summary));
  }

  private AbstractState configureFallback(boolean pFeasible) throws Exception {
    AbstractState top = mock(AbstractState.class);
    ARGState fallbackTarget = new ARGState(mock(AbstractState.class), null);
    DssBlockAnalysisResult fallback = mock(DssBlockAnalysisResult.class);
    when(analysis.makeStartState(true)).thenReturn(top);
    when(dcpa.reset(top)).thenReturn(top);
    when(analysis.runBlockAnalysis(eq(top), any(), any())).thenReturn(fallback);
    ImmutableSet<ARGState> targets =
        pFeasible ? ImmutableSet.of(fallbackTarget) : ImmutableSet.of();
    when(fallback.getFinalLocationStates()).thenReturn(ImmutableSet.of());
    when(fallback.getAllViolations()).thenReturn(targets);
    when(fallback.getTargetStates()).thenReturn(targets);
    when(fallback.getViolationConditionViolations()).thenReturn(ImmutableSet.of());
    if (pFeasible) {
      when(analysis.pathsFromOrigin(targets)).thenReturn(ImmutableSet.of(fallbackViolation));
    }
    return top;
  }

  @Test
  public void callstackFallbackSurvivesViolationsFromAnotherEntryState() throws Exception {
    AbstractState otherEntry = mock(AbstractState.class);
    configureRefinedEntry(otherEntry);
    when(summaryBlock.getHinderedByCallstack()).thenReturn(ImmutableSet.of(condition));
    configureFallback(true);

    for (var entries :
        ImmutableList.of(
            ImmutableList.of(entry, otherEntry), ImmutableList.of(otherEntry, entry))) {
      when(preconditions.getStatesPerLocation("entry")).thenReturn(entries);
      AnalysisResult result = engine.explore(Optional.empty());
      assertThat(result.violationConditions()).containsExactly(violation, fallbackViolation);
      assertThat(result.blockEndUnreachable()).isFalse();
    }
  }

  @Test
  public void callstackFallbackSurvivesViolationsWithinTheSameAnalysis() throws Exception {
    when(initialResult.getFinalLocationStates()).thenReturn(ImmutableSet.of(summaryState));
    when(summaryBlock.getHinderedByCallstack()).thenReturn(ImmutableSet.of(condition));
    configureFallback(true);

    AnalysisResult result = engine.explore(Optional.empty());
    assertThat(result.summaries()).isEmpty();
    assertThat(result.violationConditions()).containsExactly(violation, fallbackViolation);
    assertThat(result.blockEndUnreachable()).isFalse();
  }

  @Test
  public void callstackOnlyRejectionPreservesReachableExits() throws Exception {
    configureRefinedEntry(entry);
    when(summaryBlock.getHinderedByCallstack()).thenReturn(ImmutableSet.of(condition));
    configureFallback(true);

    AnalysisResult result = engine.explore(Optional.empty());
    assertThat(result.summaries()).containsExactly(summary);
    assertThat(result.violationConditions()).containsExactly(fallbackViolation);
    assertThat(result.blockEndUnreachable()).isFalse();
  }

  @Test
  public void refutedFallbackPreservesReachableExits() throws Exception {
    configureRefinedEntry(entry);
    when(summaryBlock.getHinderedByCallstack()).thenReturn(ImmutableSet.of(condition));
    configureFallback(false);

    AnalysisResult result = engine.explore(Optional.empty());
    assertThat(result.summaries()).containsExactly(summary);
    assertThat(result.violationConditions()).isEmpty();
    assertThat(result.blockEndUnreachable()).isFalse();
  }

  @Test
  public void refutedApplicableConditionStillAllowsPublishingExit() throws Exception {
    AbstractState otherCondition = mock(AbstractState.class);
    when(conditionHandler.statesOf(Optional.empty()))
        .thenReturn(ImmutableList.of(condition, otherCondition));
    when(dcpa.computeProgramPointId(otherCondition)).thenReturn("condition");
    configureRefinedEntry(entry);
    when(summaryBlock.getHinderedByCallstack()).thenReturn(ImmutableSet.of(condition));
    configureFallback(true);

    AnalysisResult result = engine.explore(Optional.empty());
    assertThat(result.summaries()).containsExactly(summary);
    assertThat(result.violationConditions()).containsExactly(fallbackViolation);
    assertThat(result.blockEndUnreachable()).isFalse();
  }

  @Test
  public void combinesRefinementsSeparatelyForInputsAtTheSameProgramPoint() throws Exception {
    AbstractState otherEntry = mock(AbstractState.class);
    AbstractState otherCondition = mock(AbstractState.class);
    when(conditionHandler.statesOf(Optional.empty()))
        .thenReturn(ImmutableList.of(condition, otherCondition));
    when(dcpa.computeProgramPointId(otherCondition)).thenReturn("other condition");
    when(preconditions.getStatesPerLocation("entry"))
        .thenReturn(ImmutableList.of(entry, otherEntry));
    configureRefinedEntry(otherEntry);

    // The first input violates condition but completes under otherCondition. Its exit must survive
    // even though the second input can be refined under both condition groups together.
    DssBlockAnalysisResult firstComplete = mock(DssBlockAnalysisResult.class);
    when(firstComplete.getAllViolations()).thenReturn(ImmutableSet.of());
    when(firstComplete.getFinalLocationStates()).thenReturn(ImmutableSet.of(summaryState));
    when(analysis.runBlockAnalysis(eq(entry), any(), eq(ImmutableList.of(otherCondition))))
        .thenReturn(firstComplete);
    when(analysis.summariesOf(firstComplete)).thenReturn(ImmutableList.of(summary));

    ARGState combinedState = new ARGState(mock(BlockState.class), null);
    StateAndPrecision combinedSummary =
        new StateAndPrecision(combinedState, SingletonPrecision.getInstance());
    DssBlockAnalysisResult combined = mock(DssBlockAnalysisResult.class);
    when(combined.getAllViolations()).thenReturn(ImmutableSet.of());
    when(combined.getFinalLocationStates()).thenReturn(ImmutableSet.of(combinedState));
    when(analysis.runBlockAnalysis(
            eq(otherEntry), any(), eq(ImmutableList.of(condition, otherCondition))))
        .thenReturn(combined);
    when(analysis.summariesOf(combined)).thenReturn(ImmutableList.of(combinedSummary));

    for (var entries :
        ImmutableList.of(
            ImmutableList.of(entry, otherEntry), ImmutableList.of(otherEntry, entry))) {
      when(preconditions.getStatesPerLocation("entry")).thenReturn(entries);
      AnalysisResult result = engine.explore(Optional.empty());
      assertThat(result.summaries()).containsExactly(summary, combinedSummary);
      assertThat(result.violationConditions()).containsExactly(violation);
      assertThat(result.blockEndUnreachable()).isFalse();
    }
    verify(analysis, never())
        .runBlockAnalysis(eq(entry), any(), eq(ImmutableList.of(condition, otherCondition)));
  }

  @Test
  public void combinesKnownInputRefinementsDespiteSpeculativeViolations() throws Exception {
    AbstractState otherCondition = mock(AbstractState.class);
    when(conditionHandler.statesOf(Optional.empty()))
        .thenReturn(ImmutableList.of(condition, otherCondition));
    when(dcpa.computeProgramPointId(otherCondition)).thenReturn("other condition");
    configureRefinedEntry(entry);
    when(summaryBlock.getHinderedByCallstack()).thenReturn(ImmutableSet.of(condition));
    configureFallback(true);

    // The combined analysis completes without a fallback of its own. Obligations from individual
    // runs still need to be propagated because their speculative callers differ from this input.
    ARGState combinedState = new ARGState(mock(BlockState.class), null);
    StateAndPrecision combinedSummary =
        new StateAndPrecision(combinedState, SingletonPrecision.getInstance());
    DssBlockAnalysisResult combined = mock(DssBlockAnalysisResult.class);
    when(combined.getAllViolations()).thenReturn(ImmutableSet.of());
    when(combined.getFinalLocationStates()).thenReturn(ImmutableSet.of(combinedState));
    when(analysis.runBlockAnalysis(
            eq(entry), any(), eq(ImmutableList.of(condition, otherCondition))))
        .thenReturn(combined);
    when(analysis.summariesOf(combined)).thenReturn(ImmutableList.of(combinedSummary));

    AnalysisResult result = engine.explore(Optional.empty());
    assertThat(result.summaries()).containsExactly(combinedSummary);
    assertThat(result.violationConditions()).containsExactly(fallbackViolation);
    assertThat(result.blockEndUnreachable()).isFalse();
  }

  @Test
  public void inputsShareTheSameSpeculativeCallstackCheck() throws Exception {
    AbstractState otherEntry = mock(AbstractState.class);
    configureRefinedEntry(entry);
    configureRefinedEntry(otherEntry);
    when(preconditions.getStatesPerLocation("entry"))
        .thenReturn(ImmutableList.of(entry, otherEntry));
    when(summaryBlock.getHinderedByCallstack()).thenReturn(ImmutableSet.of(condition));
    AbstractState top = configureFallback(true);

    AnalysisResult result = engine.explore(Optional.empty());
    assertThat(result.summaries()).containsExactly(summary);
    assertThat(result.violationConditions()).containsExactly(fallbackViolation);
    verify(analysis).runBlockAnalysis(eq(entry), any(), any());
    verify(analysis).runBlockAnalysis(eq(otherEntry), any(), any());
    verify(analysis).runBlockAnalysis(eq(top), any(), eq(ImmutableList.of(condition)));
  }

  @Test
  public void aNewUpdateRepeatsSpeculativeChecksInsteadOfUsingOldResults() throws Exception {
    configureRefinedEntry(entry);
    when(summaryBlock.getHinderedByCallstack()).thenReturn(ImmutableSet.of(condition));
    AbstractState top = configureFallback(true);
    assertThat(engine.explore(Optional.empty()).violationConditions())
        .containsExactly(fallbackViolation);

    ARGState newTarget = new ARGState(mock(AbstractState.class), null);
    DssBlockAnalysisResult newFallback = mock(DssBlockAnalysisResult.class);
    ArgPathAndCondition newViolation = mock(ArgPathAndCondition.class);
    when(newFallback.getAllViolations()).thenReturn(ImmutableSet.of(newTarget));
    when(newFallback.getFinalLocationStates()).thenReturn(ImmutableSet.of());
    when(newFallback.getTargetStates()).thenReturn(ImmutableSet.of(newTarget));
    when(newFallback.getViolationConditionViolations()).thenReturn(ImmutableSet.of());
    when(analysis.pathsFromOrigin(ImmutableSet.of(newTarget)))
        .thenReturn(ImmutableSet.of(newViolation));
    when(analysis.runBlockAnalysis(eq(top), any(), any())).thenReturn(newFallback);

    AnalysisResult updated = engine.explore(Optional.empty());
    assertThat(updated.summaries()).containsExactly(summary);
    assertThat(updated.violationConditions()).containsExactly(newViolation);
    verify(analysis, times(2)).runBlockAnalysis(eq(top), any(), any());
  }

  @Test
  public void probesDoNotMaterializeSummaries() throws Exception {
    AbstractState otherCondition = mock(AbstractState.class);
    AbstractState lastCondition = mock(AbstractState.class);
    when(conditionHandler.statesOf(Optional.empty()))
        .thenReturn(ImmutableList.of(condition, otherCondition, lastCondition));
    when(dcpa.computeProgramPointId(otherCondition)).thenReturn("other condition");
    when(dcpa.computeProgramPointId(lastCondition)).thenReturn("last condition");
    when(analysis.runBlockAnalysis(eq(entry), any(), eq(ImmutableList.of(lastCondition))))
        .thenReturn(initialResult);
    DssBlockAnalysisResult probe = mock(DssBlockAnalysisResult.class);
    when(probe.getAllViolations()).thenReturn(ImmutableSet.of());
    when(probe.getFinalLocationStates()).thenReturn(ImmutableSet.of(summaryState));
    when(analysis.runBlockAnalysis(eq(entry), any(), eq(ImmutableList.of(otherCondition))))
        .thenReturn(probe, initialResult);

    // Only otherCondition succeeds during probing. The publishing run finds a violation instead.
    // It must contribute its current result, not an exit retained from the earlier probe.
    AnalysisResult result = engine.explore(Optional.empty());
    assertThat(result.summaries()).isEmpty();
    assertThat(result.violationConditions()).containsExactly(violation);
    verify(analysis, never()).summariesOf(probe);
    verify(analysis, times(2))
        .runBlockAnalysis(eq(entry), any(), eq(ImmutableList.of(otherCondition)));
  }

  @Test
  public void lastRemainingGroupPublishesWithoutRepeatingTheAnalysis() throws Exception {
    AbstractState otherCondition = mock(AbstractState.class);
    when(conditionHandler.statesOf(Optional.empty()))
        .thenReturn(ImmutableList.of(condition, otherCondition));
    when(dcpa.computeProgramPointId(otherCondition)).thenReturn("other condition");
    DssBlockAnalysisResult completed = mock(DssBlockAnalysisResult.class);
    when(completed.getAllViolations()).thenReturn(ImmutableSet.of());
    when(completed.getFinalLocationStates()).thenReturn(ImmutableSet.of(summaryState));
    when(analysis.runBlockAnalysis(eq(entry), any(), eq(ImmutableList.of(otherCondition))))
        .thenReturn(completed);
    when(analysis.summariesOf(completed)).thenReturn(ImmutableList.of(summary));

    AnalysisResult result = engine.explore(Optional.empty());
    assertThat(result.summaries()).containsExactly(summary);
    assertThat(result.violationConditions()).containsExactly(violation);
    verify(analysis).runBlockAnalysis(eq(entry), any(), eq(ImmutableList.of(otherCondition)));
  }

  @Test
  public void aCompletedRunWithoutExitsReportsAnUnreachableEnd() throws Exception {
    DssBlockAnalysisResult unreachable = mock(DssBlockAnalysisResult.class);
    when(unreachable.getAllViolations()).thenReturn(ImmutableSet.of());
    when(unreachable.getFinalLocationStates()).thenReturn(ImmutableSet.of());
    when(analysis.summariesOf(unreachable)).thenReturn(ImmutableList.of());
    when(analysis.runBlockAnalysis(eq(entry), any(), any())).thenReturn(unreachable);

    AnalysisResult result = engine.explore(Optional.empty());
    assertThat(result.summaries()).isEmpty();
    assertThat(result.violationConditions()).isEmpty();
    assertThat(result.blockEndUnreachable()).isTrue();
  }

  @Test
  public void explicitlyCombinedPreconditionsAreAnalyzedAsOneInput() throws Exception {
    AbstractState otherEntry = mock(AbstractState.class);
    AbstractState combinedEntry = mock(AbstractState.class);
    var combine = mock(CombinePreconditionsOperator.class);
    when(analysis.getOptions())
        .thenReturn(
            new DssAnalysisOptions(
                Configuration.builder()
                    .setOption("distributedSummaries.combinePresByHash", "true")
                    .build()));
    when(preconditions.getStatesPerLocation("entry"))
        .thenReturn(ImmutableList.of(entry, otherEntry));
    when(dcpa.reset(otherEntry)).thenReturn(otherEntry);
    when(dcpa.getCombineOperator()).thenReturn(combine);
    when(combine.combinePreconditions(ImmutableList.of(entry, otherEntry)))
        .thenReturn(combinedEntry);
    configureRefinedEntry(combinedEntry);

    AnalysisResult result = engine.explore(Optional.empty());
    assertThat(result.summaries()).containsExactly(summary);
    assertThat(result.violationConditions()).isEmpty();
    verify(analysis, never()).runBlockAnalysis(eq(entry), any(), any());
    verify(analysis, never()).runBlockAnalysis(eq(otherEntry), any(), any());
  }

  @Test
  public void speculativeRunsStillDoNotPublishForwardSummaries() throws Exception {
    when(preconditions.isEmpty()).thenReturn(true);
    when(preconditions.getAllProgramPoints()).thenReturn(ImmutableSet.of());
    AnalysisResult result = engine.explore(Optional.empty());
    assertThat(result.summaries()).isEmpty();
    assertThat(result.violationConditions()).containsExactly(violation);
    assertThat(result.blockEndUnreachable()).isFalse();
  }
}
