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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

public class PartialReplaceExplorationEngineTest {

  @Test
  public void refinementOfLaterSourceRefreshesEarlierSummariesBeforePublication() throws Exception {
    DssBlockAnalysis analysis = mock(DssBlockAnalysis.class);
    DistributedConfigurableProgramAnalysis dcpa =
        mock(DistributedConfigurableProgramAnalysis.class);
    PartialReplacePreconditionHandler preconditions = mock(PartialReplacePreconditionHandler.class);
    PartialReplaceViolationConditionHandler conditions =
        mock(PartialReplaceViolationConditionHandler.class);
    Precision precision = mock(Precision.class);
    AbstractState first = mock(AbstractState.class);
    AbstractState second = mock(AbstractState.class);
    StateAndPrecision oldSummary = new StateAndPrecision(mock(AbstractState.class), precision);
    StateAndPrecision refinedSummary = new StateAndPrecision(mock(AbstractState.class), precision);
    StateAndPrecision secondSummary = new StateAndPrecision(mock(AbstractState.class), precision);
    var firstGroup = ImmutableList.of(new StateAndPrecision(first, precision));
    var secondGroup = ImmutableList.of(new StateAndPrecision(second, precision));
    when(preconditions.getActivePreconditions())
        .thenReturn(ImmutableMap.of("first", firstGroup, "second", secondGroup));
    when(preconditions.getKnownPreconditions())
        .thenReturn(ImmutableList.of(firstGroup.getFirst(), secondGroup.getFirst()));
    when(conditions.states()).thenReturn(ImmutableList.of());
    when(analysis.getDcpa()).thenReturn(dcpa);
    when(analysis.statistics()).thenReturn(new DssSingleWorkerStatistics("test"));
    when(dcpa.reset(any())).thenAnswer(i -> i.getArgument(0));
    when(analysis.combinePrecisions(any())).thenReturn(precision);
    when(analysis.combineStates(any()))
        .thenAnswer(i -> ImmutableList.copyOf(i.<Collection<AbstractState>>getArgument(0)));
    when(analysis.combineSummaries(any()))
        .thenAnswer(i -> ImmutableList.copyOf(i.<Collection<StateAndPrecision>>getArgument(0)));
    when(analysis.pathsWithCondition(any())).thenReturn(ImmutableSet.of());
    when(analysis.pathsFromOrigin(any())).thenReturn(ImmutableSet.of());
    DssBlockAnalysisResult coarse = mock(DssBlockAnalysisResult.class);
    DssBlockAnalysisResult refined = mock(DssBlockAnalysisResult.class);
    DssBlockAnalysisResult other = mock(DssBlockAnalysisResult.class);
    when(analysis.summariesOf(coarse)).thenReturn(ImmutableList.of(oldSummary));
    when(analysis.summariesOf(refined)).thenReturn(ImmutableList.of(refinedSummary));
    when(analysis.summariesOf(other)).thenReturn(ImmutableList.of(secondSummary));
    AtomicLong version = new AtomicLong();
    when(analysis.explorationPrecisionVersion()).thenAnswer(i -> version.get());
    when(analysis.runBlockAnalysis(any(), any(), any()))
        .thenAnswer(
            i -> {
              if (i.getArgument(0) == first) {
                return version.get() == 0 ? coarse : refined;
              }
              version.set(1);
              return other;
            });
    PartialReplaceExplorationEngine engine =
        new PartialReplaceExplorationEngine(analysis, preconditions, conditions);

    var result = engine.explore(false);
    assertThat(result.summaries()).containsExactly(refinedSummary, secondSummary);
    assertThat(engine.explore(false).summaries()).isEmpty();
  }

  @Test
  public void exactExplorationIsReabstractedWithoutReexplorationAfterPredicateUpdate()
      throws Exception {
    DssBlockAnalysis analysis = mock(DssBlockAnalysis.class);
    var dcpa = mock(DistributedConfigurableProgramAnalysis.class);
    var preconditions = mock(PartialReplacePreconditionHandler.class);
    var conditions = mock(PartialReplaceViolationConditionHandler.class);
    var precision = mock(Precision.class);
    var input = mock(AbstractState.class);
    var summary = new StateAndPrecision(mock(AbstractState.class), precision);
    var group = ImmutableList.of(new StateAndPrecision(input, precision));
    var explored = mock(DssBlockAnalysisResult.class);
    AtomicLong boundaryVersion = new AtomicLong();
    when(analysis.getDcpa()).thenReturn(dcpa);
    when(analysis.statistics()).thenReturn(new DssSingleWorkerStatistics("test"));
    when(analysis.usesExactBoundaryRefinement()).thenReturn(true);
    when(analysis.precisionVersion()).thenAnswer(i -> boundaryVersion.get());
    when(analysis.explorationPrecisionVersion()).thenReturn(0L);
    when(preconditions.getActivePreconditions()).thenReturn(ImmutableMap.of("source", group));
    when(preconditions.getKnownPreconditions()).thenReturn(group);
    when(conditions.states()).thenReturn(ImmutableList.of());
    when(dcpa.reset(any())).thenAnswer(i -> i.getArgument(0));
    when(analysis.combinePrecisions(any())).thenReturn(precision);
    when(analysis.combineStates(any())).thenReturn(ImmutableList.of(input));
    when(analysis.runBlockAnalysis(any(), any(), any())).thenReturn(explored);
    when(analysis.summariesOf(explored)).thenReturn(ImmutableList.of(summary));
    when(analysis.pathsWithCondition(any())).thenReturn(ImmutableSet.of());
    when(analysis.pathsFromOrigin(any())).thenReturn(ImmutableSet.of());
    when(analysis.combineSummaries(any())).thenReturn(ImmutableList.of(summary));
    var engine = new PartialReplaceExplorationEngine(analysis, preconditions, conditions);

    assertThat(engine.explore(false).summaries()).containsExactly(summary);
    boundaryVersion.incrementAndGet();
    assertThat(engine.explore(false).summaries()).containsExactly(summary);
    assertThat(engine.explore(false).summaries()).isEmpty();
    verify(analysis, times(1)).runBlockAnalysis(any(), any(), any());
    verify(analysis, times(2)).combineSummaries(any());

    // Non-predicate changes still invalidate exact exploration.
    when(analysis.explorationPrecisionVersion()).thenReturn(1L);
    engine.explore(false);
    verify(analysis, times(2)).runBlockAnalysis(any(), any(), any());

    // Withheld summaries remain unresolved even when duplicate suppression emits no messages.
    var path = mock(ARGPath.class);
    when(path.getFullPath()).thenReturn(ImmutableList.of());
    var violation = new ArgPathAndCondition(path, null);
    when(analysis.pathsFromOrigin(any())).thenReturn(ImmutableSet.of(violation));
    when(analysis.explorationPrecisionVersion()).thenReturn(2L);
    assertThat(engine.explore(false).summaries()).isEmpty();
    assertThat(engine.hasUnresolvedViolations()).isTrue();
    assertThat(engine.explore(false).violationConditions()).isEmpty();
    assertThat(engine.hasUnresolvedViolations()).isTrue();

    // A later refinement can discharge the obligation and permit a proof again.
    when(analysis.pathsFromOrigin(any())).thenReturn(ImmutableSet.of());
    when(analysis.explorationPrecisionVersion()).thenReturn(3L);
    assertThat(engine.explore(false).summaries()).isEmpty();
    assertThat(engine.hasUnresolvedViolations()).isFalse();
  }
}
