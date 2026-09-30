// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class AnalysisResultTest {
  private static StateAndPrecision summary() {
    return new StateAndPrecision(mock(AbstractState.class), SingletonPrecision.getInstance());
  }

  @Test
  public void mixedResultsAreRejected() {
    ImmutableSet<ArgPathAndCondition> violations = ImmutableSet.of(mock(ArgPathAndCondition.class));
    assertThrows(
        IllegalArgumentException.class,
        () -> new AnalysisResult(ImmutableSet.of(summary()), violations, false));
    assertThrows(
        IllegalArgumentException.class,
        () -> new AnalysisResult(ImmutableSet.of(), violations, true));
  }

  @Test
  public void violationsFromAnyGroupSuppressEveryPostcondition() {
    ArgPathAndCondition first = mock(ArgPathAndCondition.class);
    ArgPathAndCondition second = mock(ArgPathAndCondition.class);
    AnalysisResult result =
        AnalysisResult.merge(
            ImmutableList.of(
                AnalysisResult.ofSummaries(ImmutableSet.of(summary())),
                AnalysisResult.ofViolationConditions(ImmutableSet.of(first)),
                AnalysisResult.unreachableBlockEnd(),
                AnalysisResult.ofViolationConditions(ImmutableSet.of(second))));
    assertThat(result.violationConditions()).containsExactly(first, second);
    assertThat(result.summaries()).isEmpty();
    assertThat(result.blockEndUnreachable()).isFalse();
  }

  @Test
  public void safeGroupsRetainAllSummaries() {
    StateAndPrecision first = summary();
    StateAndPrecision second = summary();
    AnalysisResult result =
        AnalysisResult.merge(
            ImmutableList.of(
                AnalysisResult.ofSummaries(ImmutableSet.of(first)),
                AnalysisResult.unreachableBlockEnd(),
                AnalysisResult.ofSummaries(ImmutableSet.of(second))));
    assertThat(result.violationConditions()).isEmpty();
    assertThat(result.summaries()).containsExactly(first, second);
    assertThat(result.blockEndUnreachable()).isFalse();
  }

  @Test
  public void unreachableRequiresEveryRunToEstablishIt() {
    AnalysisResult unreachable = AnalysisResult.unreachableBlockEnd();
    assertThat(
            AnalysisResult.merge(ImmutableList.of(unreachable, unreachable)).blockEndUnreachable())
        .isTrue();
    assertThat(
            AnalysisResult.merge(ImmutableList.of(unreachable, AnalysisResult.empty()))
                .blockEndUnreachable())
        .isFalse();
    assertThat(AnalysisResult.merge(ImmutableList.of()).blockEndUnreachable()).isFalse();
  }
}
