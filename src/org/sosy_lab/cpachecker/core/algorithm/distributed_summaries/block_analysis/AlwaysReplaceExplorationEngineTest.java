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

  private void configureFallback(boolean pFeasible) throws Exception {
    AbstractState top = mock(AbstractState.class);
    ARGState fallbackTarget = new ARGState(mock(AbstractState.class), null);
    DssBlockAnalysisResult fallback = mock(DssBlockAnalysisResult.class);
    when(analysis.makeStartState(true)).thenReturn(top);
    when(dcpa.reset(top)).thenReturn(top);
    when(analysis.runBlockAnalysis(eq(top), any(), any())).thenReturn(fallback);
    ImmutableSet<ARGState> targets =
        pFeasible ? ImmutableSet.of(fallbackTarget) : ImmutableSet.of();
    when(fallback.getAllViolations()).thenReturn(targets);
    when(fallback.getTargetStates()).thenReturn(targets);
    when(fallback.getViolationConditionViolations()).thenReturn(ImmutableSet.of());
    if (pFeasible) {
      when(analysis.pathsFromOrigin(targets)).thenReturn(ImmutableSet.of(fallbackViolation));
    }
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
  public void callstackOnlyRejectionDoesNotPublishUnrefinedExits() throws Exception {
    configureRefinedEntry(entry);
    when(summaryBlock.getHinderedByCallstack()).thenReturn(ImmutableSet.of(condition));
    configureFallback(true);

    AnalysisResult result = engine.explore(Optional.empty());
    assertThat(result.summaries()).isEmpty();
    assertThat(result.violationConditions()).containsExactly(fallbackViolation);
    assertThat(result.blockEndUnreachable()).isFalse();
  }

  @Test
  public void omittingUnrelatedExitsDoesNotReportUnreachableBlockEnd() throws Exception {
    configureRefinedEntry(entry);
    when(summaryBlock.getHinderedByCallstack()).thenReturn(ImmutableSet.of(condition));
    configureFallback(false);

    AnalysisResult result = engine.explore(Optional.empty());
    assertThat(result.summaries()).isEmpty();
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
  public void speculativeRunsStillDoNotPublishForwardSummaries() throws Exception {
    when(preconditions.isEmpty()).thenReturn(true);
    when(preconditions.getAllProgramPoints()).thenReturn(ImmutableSet.of());
    AnalysisResult result = engine.explore(Optional.empty());
    assertThat(result.summaries()).isEmpty();
    assertThat(result.violationConditions()).containsExactly(violation);
    assertThat(result.blockEndUnreachable()).isFalse();
  }
}
