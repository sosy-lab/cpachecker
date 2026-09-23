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
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.LinkedHashSet;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class AlwaysReplaceViolationConditionHandlerTest {

  /** Whether every state of {@code pStates} also occurs in {@code pCandidates}. */
  private static boolean containsAllStatesOf(
      Collection<StateAndPrecision> pStates, Collection<StateAndPrecision> pCandidates) {
    return pStates.stream()
        .allMatch(
            state ->
                pCandidates.stream().anyMatch(candidate -> candidate.state() == state.state()));
  }

  /** An update from one successor must not erase a violation condition owned by another. */
  @Test
  public void sameConditionFromTwoSuccessorsIsNotLost() throws Exception {
    String senderA = "successor-a";
    String senderB = "successor-b";
    AbstractState conditionX = mock(AbstractState.class);
    AbstractState conditionY = mock(AbstractState.class);
    Precision precision = mock(Precision.class);
    StateAndPrecision conditionXAndPrecision = new StateAndPrecision(conditionX, precision);
    StateAndPrecision conditionYAndPrecision = new StateAndPrecision(conditionY, precision);

    DssMessageFactory messageFactory =
        new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
    DssViolationConditionMessage messageAX =
        messageFactory.createViolationConditionMessage(
            senderA,
            AlgorithmStatus.SOUND_AND_PRECISE,
            ImmutableList.of(),
            ImmutableMap.of("state", "x"));
    DssViolationConditionMessage messageBX =
        messageFactory.createViolationConditionMessage(
            senderB,
            AlgorithmStatus.SOUND_AND_PRECISE,
            ImmutableList.of(),
            ImmutableMap.of("state", "x"));
    DssViolationConditionMessage messageAY =
        messageFactory.createViolationConditionMessage(
            senderA,
            AlgorithmStatus.SOUND_AND_PRECISE,
            ImmutableList.of(),
            ImmutableMap.of("state", "y"));

    DssBlockAnalysis analysis = mock(DssBlockAnalysis.class);
    DistributedConfigurableProgramAnalysis dcpa =
        mock(DistributedConfigurableProgramAnalysis.class);
    BlockNode block = mock(BlockNode.class);
    when(analysis.getDcpa()).thenReturn(dcpa);
    when(analysis.getBlock()).thenReturn(block);
    when(analysis.getLogger()).thenReturn(mock(LogManager.class));
    when(analysis.statistics()).thenReturn(new DssSingleWorkerStatistics("test-block"));
    when(block.getSuccessorIds()).thenReturn(ImmutableSet.of(senderA, senderB));
    when(dcpa.computeProgramPointId(any())).thenReturn(1);
    when(analysis.deserialize(messageAX)).thenReturn(ImmutableList.of(conditionXAndPrecision));
    when(analysis.deserialize(messageBX)).thenReturn(ImmutableList.of(conditionXAndPrecision));
    when(analysis.deserialize(messageAY)).thenReturn(ImmutableList.of(conditionYAndPrecision));
    when(analysis.violationConditionsEqual(any(), any()))
        .thenAnswer(
            invocation -> {
              Collection<StateAndPrecision> states1 = invocation.getArgument(0);
              Collection<StateAndPrecision> states2 = invocation.getArgument(1);
              return containsAllStatesOf(states1, states2) && containsAllStatesOf(states2, states1);
            });
    when(analysis.deduplicateViolationConditions(any()))
        .thenAnswer(
            invocation -> {
              Iterable<StateAndPrecision> states = invocation.getArgument(0);
              return ImmutableList.copyOf(new LinkedHashSet<>(ImmutableList.copyOf(states)));
            });

    AlwaysReplaceViolationConditionHandler handler =
        new AlwaysReplaceViolationConditionHandler(analysis);

    assertThat(handler.store(messageAX).shouldProceed()).isTrue();
    assertThat(handler.store(messageBX).shouldProceed()).isFalse();
    assertThat(handler.states()).containsExactly(conditionX);
    assertThat(handler.store(messageAY).shouldProceed()).isTrue();

    assertThat(handler.getConditions().getStatesForKey(senderA)).containsExactly(conditionY);
    assertThat(handler.getConditions().getStatesForKey(senderB)).containsExactly(conditionX);
    assertThat(handler.states()).containsExactly(conditionY, conditionX);
    assertThat(handler.store(messageBX).shouldProceed()).isFalse();
  }

  /**
   * Skipping the exploration of a condition must not drop it. Two successors report different
   * conditions, then the one that reported first retracts its own: the condition of the other
   * successor stays, and the block explores it.
   */
  @Test
  public void conditionIsKeptWhenItsExplorationIsSkipped() throws Exception {
    String senderA = "successor-a";
    String senderB = "successor-b";
    AbstractState conditionX = mock(AbstractState.class);
    AbstractState conditionY = mock(AbstractState.class);
    Precision precision = mock(Precision.class);
    StateAndPrecision conditionXAndPrecision = new StateAndPrecision(conditionX, precision);
    StateAndPrecision conditionYAndPrecision = new StateAndPrecision(conditionY, precision);

    DssMessageFactory messageFactory =
        new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
    DssViolationConditionMessage messageAX =
        messageFactory.createViolationConditionMessage(
            senderA, AlgorithmStatus.SOUND_AND_PRECISE, ImmutableMap.of("state", "x"));
    DssViolationConditionMessage messageBY =
        messageFactory.createViolationConditionMessage(
            senderB, AlgorithmStatus.SOUND_AND_PRECISE, ImmutableMap.of("state", "y"));
    DssViolationConditionMessage messageBX =
        messageFactory.createViolationConditionMessage(
            senderB, AlgorithmStatus.SOUND_AND_PRECISE, ImmutableMap.of("state", "x"));

    DssBlockAnalysis analysis = mock(DssBlockAnalysis.class);
    DistributedConfigurableProgramAnalysis dcpa =
        mock(DistributedConfigurableProgramAnalysis.class);
    BlockNode block = mock(BlockNode.class);
    when(analysis.getDcpa()).thenReturn(dcpa);
    when(analysis.getBlock()).thenReturn(block);
    when(analysis.getLogger()).thenReturn(mock(LogManager.class));
    when(analysis.statistics()).thenReturn(new DssSingleWorkerStatistics("test-block"));
    when(block.getSuccessorIds()).thenReturn(ImmutableSet.of(senderA, senderB));
    when(dcpa.computeProgramPointId(any())).thenReturn(1);
    when(analysis.deserialize(messageAX)).thenReturn(ImmutableList.of(conditionXAndPrecision));
    when(analysis.deserialize(messageBY)).thenReturn(ImmutableList.of(conditionYAndPrecision));
    when(analysis.deserialize(messageBX)).thenReturn(ImmutableList.of(conditionXAndPrecision));
    when(analysis.violationConditionsEqual(any(), any()))
        .thenAnswer(
            invocation -> {
              Collection<StateAndPrecision> states1 = invocation.getArgument(0);
              Collection<StateAndPrecision> states2 = invocation.getArgument(1);
              return containsAllStatesOf(states1, states2) && containsAllStatesOf(states2, states1);
            });
    when(analysis.deduplicateViolationConditions(any()))
        .thenAnswer(
            invocation -> {
              Iterable<StateAndPrecision> states = invocation.getArgument(0);
              return ImmutableList.copyOf(new LinkedHashSet<>(ImmutableList.copyOf(states)));
            });

    AlwaysReplaceViolationConditionHandler handler =
        new AlwaysReplaceViolationConditionHandler(analysis);

    // Two successors report different conditions: both are new, so both are explored.
    assertThat(handler.store(messageAX).shouldProceed()).isTrue();
    assertThat(handler.store(messageBY).shouldProceed()).isTrue();
    assertThat(handler.states()).containsExactly(conditionX, conditionY);

    // The second successor replaces its own condition by the one the first successor also reports.
    // The set the block explores loses a condition, which is a change like any other: dropping a
    // condition is progress, and the block has to be explored again without it.
    assertThat(handler.store(messageBX).shouldProceed()).isTrue();
    assertThat(handler.states()).containsExactly(conditionX);

    // Both successors now report the same condition. Repeating it adds nothing to explore, but the
    // condition stays stored for both of them.
    assertThat(handler.store(messageAX).shouldProceed()).isFalse();
    assertThat(handler.getConditions().getStatesForKey(senderA)).containsExactly(conditionX);
    assertThat(handler.getConditions().getStatesForKey(senderB)).containsExactly(conditionX);
    assertThat(handler.states()).containsExactly(conditionX);
  }
}
