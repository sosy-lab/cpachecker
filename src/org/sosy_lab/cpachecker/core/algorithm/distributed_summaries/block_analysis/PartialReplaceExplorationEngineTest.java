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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalyses.DssBlockAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class PartialReplaceExplorationEngineTest {

  /**
   * The mocked block analysis a cached source is explored through. The precision of a round is
   * whatever {@link DssBlockAnalysis#combinePrecisions} returns, so a test controls it by setting
   * {@link #roundPrecision}.
   */
  private static final class Fixture {

    private final DssBlockAnalysis analysis = mock(DssBlockAnalysis.class);
    private final PartialReplacePreconditionHandler preconditions =
        mock(PartialReplacePreconditionHandler.class);
    private final PartialReplaceViolationConditionHandler conditions =
        mock(PartialReplaceViolationConditionHandler.class);
    private final StateAndPrecision summary;

    private Precision roundPrecision;

    private Fixture() throws Exception {
      this(Configuration.defaultConfiguration());
    }

    private Fixture(Configuration pConfiguration) throws Exception {
      DistributedConfigurableProgramAnalysis dcpa =
          mock(DistributedConfigurableProgramAnalysis.class);
      Precision precision = mock(Precision.class);
      roundPrecision = precision;
      summary = new StateAndPrecision(mock(AbstractState.class), precision);
      AbstractState source = mock(AbstractState.class);
      ImmutableList<StateAndPrecision> group =
          ImmutableList.of(new StateAndPrecision(source, precision));

      when(preconditions.getActivePreconditions()).thenReturn(ImmutableMap.of("source", group));
      when(preconditions.getKnownPreconditions()).thenReturn(group);
      when(conditions.states()).thenReturn(ImmutableList.of());
      when(analysis.getDcpa()).thenReturn(dcpa);
      when(analysis.statistics()).thenReturn(new DssSingleWorkerStatistics("test"));
      when(analysis.getOptions()).thenReturn(new DssAnalysisOptions(pConfiguration));
      when(dcpa.reset(any())).thenAnswer(i -> i.getArgument(0));
      // the precision of the round, which the engine compares a cached source against
      when(analysis.combinePrecisions(any())).thenAnswer(i -> roundPrecision);
      when(analysis.combineStates(any()))
          .thenAnswer(i -> ImmutableList.copyOf(i.<Collection<AbstractState>>getArgument(0)));
      when(analysis.combineSummaries(any()))
          .thenAnswer(i -> ImmutableList.copyOf(i.<Collection<StateAndPrecision>>getArgument(0)));
      when(analysis.pathsWithCondition(any())).thenReturn(ImmutableSet.of());
      when(analysis.pathsFromOrigin(any())).thenReturn(ImmutableSet.of());
      DssBlockAnalysisResult result = mock(DssBlockAnalysisResult.class);
      when(analysis.summariesOf(result)).thenReturn(ImmutableList.of(summary));
      when(analysis.runBlockAnalysis(any(), any(), any())).thenReturn(result);
    }

    private PartialReplaceExplorationEngine engine() throws InterruptedException {
      return new PartialReplaceExplorationEngine(analysis, preconditions, conditions);
    }
  }

  @Test
  public void unchangedPrecisionKeepsTheCachedSourceResult() throws Exception {
    Fixture fixture = new Fixture();
    PartialReplaceExplorationEngine engine = fixture.engine();

    assertThat(engine.explore(false).summaries()).containsExactly(fixture.summary);
    // Nothing changed, so the source is a cache hit: its summaries are neither recomputed nor
    // published again.
    assertThat(engine.explore(false).summaries()).isEmpty();
    verify(fixture.analysis, times(1)).runBlockAnalysis(any(), any(), any());
  }

  @Test
  public void learnedPrecisionIsKeptForLaterRounds() throws Exception {
    Fixture fixture =
        new Fixture(
            Configuration.builder()
                .setOption("distributedSummaries.retainLearnedPrecision", "true")
                .build());
    Precision learned = mock(Precision.class);
    Precision union = mock(Precision.class);
    when(fixture.analysis.precisionOfLastRun()).thenReturn(learned);
    when(fixture.analysis.unionOf(fixture.roundPrecision, learned)).thenReturn(union);
    PartialReplaceExplorationEngine engine = fixture.engine();

    engine.explore(false);
    // Nothing was received in between, but the second round still explores with what the
    // refinements of the first one learned.
    engine.explore(false);

    verify(fixture.analysis).runBlockAnalysis(any(), eq(union), any());
  }

  @Test
  public void changedPrecisionReexploresTheCachedSource() throws Exception {
    Fixture fixture = new Fixture();
    PartialReplaceExplorationEngine engine = fixture.engine();
    engine.explore(false);

    // A refinement reaches this block as a received precision of a later round. The summaries the
    // source produced under the coarser precision are stale, so it has to be explored again.
    fixture.roundPrecision = mock(Precision.class);
    engine.explore(false);

    verify(fixture.analysis, times(2)).runBlockAnalysis(any(), any(), any());
  }
}
