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
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;

public class PathBasedViolationConditionHandlerTest {

  private static final String SENDER = "successor-a";

  private DssMessageFactory messageFactory;
  private DssBlockAnalysis analysis;
  private PathBasedViolationConditionHandler handler;

  @Before
  public void setUp() throws Exception {
    messageFactory =
        new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
    analysis = mock(DssBlockAnalysis.class);
    when(analysis.getLogger()).thenReturn(mock(LogManager.class));
    when(analysis.statistics()).thenReturn(new DssSingleWorkerStatistics("test-block"));

    // The handler collects the conditions to explore through the analysis, so the mock has to
    // stand in for these two collaborators. All violation conditions of this test are pairwise
    // distinct states, so comparing them by identity is equivalent to the coverage-based
    // comparison that the real implementations use.
    when(analysis.deduplicateStatesAndPrecisions(any()))
        .thenAnswer(
            invocation -> {
              Iterable<StateAndPrecision> statesAndPrecisions = invocation.getArgument(0);
              return ImmutableList.copyOf(
                  new LinkedHashSet<>(ImmutableList.copyOf(statesAndPrecisions)));
            });
    when(analysis.statesEqual(any(), any()))
        .thenAnswer(
            invocation -> {
              Collection<StateAndPrecision> states1 = invocation.getArgument(0);
              Collection<StateAndPrecision> states2 = invocation.getArgument(1);
              return states1.containsAll(states2) && states2.containsAll(states1);
            });

    handler = new PathBasedViolationConditionHandler(analysis);
  }

  /** A violation condition whose id stems from the precondition with the given id. */
  private StateAndPrecision violationConditionFor(String pId) {
    BlockState state =
        new BlockState(
            pId,
            null,
            mock(CFANode.class),
            mock(BlockNode.class),
            BlockStateType.INITIAL,
            ImmutableList.of(),
            BlockGraphPath.of(),
            SegmentedPaths.EMPTY);
    return new StateAndPrecision(state, mock(Precision.class));
  }

  private DssViolationConditionMessage message(
      List<String> pRemainingPreconditions, StateAndPrecision... pViolationConditions)
      throws InterruptedException {
    DssViolationConditionMessage received =
        messageFactory.createViolationConditionMessage(
            SENDER,
            AlgorithmStatus.SOUND_AND_PRECISE,
            pRemainingPreconditions,
            ImmutableMap.of("state", Integer.toString(pViolationConditions.length)));
    when(analysis.deserialize(received)).thenReturn(ImmutableList.copyOf(pViolationConditions));
    return received;
  }

  /**
   * A violation condition that combines the conditions of several preconditions belongs to all of
   * them, but must be analyzed only once.
   */
  @Test
  public void combinedConditionIsStoredForEveryPreconditionButReportedOnce() throws Exception {
    StateAndPrecision combined = violationConditionFor("p1+p2");

    assertThat(handler.store(message(ImmutableList.of("p1", "p2"), combined)).shouldProceed())
        .isTrue();

    assertThat(handler.statesOf(Optional.of(SENDER))).containsExactly(combined.state());
    assertThat(handler.statesOf(Optional.empty())).containsExactly(combined.state());
  }

  /**
   * A new condition for one of the combined preconditions replaces the combined one only for that
   * precondition; the others keep it until they receive an update of their own.
   */
  @Test
  public void newConditionReplacesCombinedConditionOnlyForItsPrecondition() throws Exception {
    StateAndPrecision combined = violationConditionFor("p1+p2");
    StateAndPrecision onlyP1 = violationConditionFor("p1");
    StateAndPrecision onlyP2 = violationConditionFor("p2");

    handler.store(message(ImmutableList.of("p1", "p2"), combined));
    handler.store(message(ImmutableList.of("p1", "p2"), onlyP1));

    assertThat(handler.statesOf(Optional.of(SENDER)))
        .containsExactly(combined.state(), onlyP1.state());

    handler.store(message(ImmutableList.of("p1", "p2"), onlyP2));

    assertThat(handler.statesOf(Optional.of(SENDER)))
        .containsExactly(onlyP1.state(), onlyP2.state());
  }

  /** Conditions of preconditions that the sender does not know anymore are dropped. */
  @Test
  public void combinedConditionIsDroppedOncePreconditionIsGone() throws Exception {
    StateAndPrecision combined = violationConditionFor("p1+p2");
    StateAndPrecision onlyP2 = violationConditionFor("p2");

    handler.store(message(ImmutableList.of("p1", "p2"), combined));
    // the sender replaced p1, so the combined condition is neither valid for p1 nor for p2 anymore
    handler.store(message(ImmutableList.of("p2"), onlyP2));

    assertThat(handler.statesOf(Optional.of(SENDER))).containsExactly(onlyP2.state());
    assertThat(handler.isEmptyFor(SENDER)).isFalse();
  }
}
