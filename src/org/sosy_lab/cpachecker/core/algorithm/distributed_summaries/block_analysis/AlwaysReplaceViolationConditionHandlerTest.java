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
import java.util.Optional;
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
    when(analysis.allCovered(any(), any()))
        .thenAnswer(
            invocation -> {
              Collection<StateAndPrecision> states = invocation.getArgument(0);
              Collection<StateAndPrecision> candidates = invocation.getArgument(1);
              return states.stream()
                  .allMatch(
                      state ->
                          candidates.stream()
                              .anyMatch(candidate -> candidate.state() == state.state()));
            });
    when(analysis.deduplicateStatesAndPrecisions(any()))
        .thenAnswer(
            invocation -> {
              Iterable<StateAndPrecision> states = invocation.getArgument(0);
              return ImmutableList.copyOf(new LinkedHashSet<>(ImmutableList.copyOf(states)));
            });

    AlwaysReplaceViolationConditionHandler handler =
        new AlwaysReplaceViolationConditionHandler(analysis);

    assertThat(handler.store(messageAX).shouldProceed()).isTrue();
    assertThat(handler.store(messageBX).shouldProceed()).isFalse();
    assertThat(handler.statesOf(Optional.empty())).containsExactly(conditionX);
    assertThat(handler.store(messageAY).shouldProceed()).isTrue();

    assertThat(handler.statesOf(Optional.of(senderA))).containsExactly(conditionY);
    assertThat(handler.statesOf(Optional.of(senderB))).containsExactly(conditionX);
    assertThat(handler.statesOf(Optional.empty())).containsExactly(conditionY, conditionX);
    assertThat(handler.store(messageBX).shouldProceed()).isFalse();
  }
}
