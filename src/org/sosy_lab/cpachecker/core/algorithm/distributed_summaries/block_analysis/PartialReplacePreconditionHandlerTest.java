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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class PartialReplacePreconditionHandlerTest {

  private static final String A = "predecessor-a";
  private static final String B = "predecessor-b";
  private static final String C = "predecessor-c";

  private final Precision precision = mock(Precision.class);

  /** {@code subsumedBy.get(s)} are the states {@code s} is strictly subsumed by. */
  private final SetMultimap<AbstractState, AbstractState> subsumedBy = HashMultimap.create();

  private DssBlockAnalysis analysis;
  private DssMessageFactory messageFactory;
  private int messageCounter = 0;

  @Before
  public void setUp() throws Exception {
    messageFactory =
        new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
    analysis = mock(DssBlockAnalysis.class);
    BlockNode block = mock(BlockNode.class);
    when(analysis.getBlock()).thenReturn(block);
    when(analysis.statistics()).thenReturn(new DssSingleWorkerStatistics("test-block"));
    when(block.getPredecessorIds()).thenReturn(ImmutableSet.of(A, B, C));
    when(analysis.shouldProceedForward(any())).thenReturn(DssMessageProcessing.proceed());
    when(analysis.statesEqual(any(), any()))
        .thenAnswer(
            invocation -> {
              Collection<StateAndPrecision> states1 = invocation.getArgument(0);
              Collection<StateAndPrecision> states2 = invocation.getArgument(1);
              return allMatched(states1, states2, false) && allMatched(states2, states1, false);
            });
    when(analysis.allCovered(any(), any()))
        .thenAnswer(
            invocation -> allMatched(invocation.getArgument(0), invocation.getArgument(1), true));
  }

  private boolean allMatched(
      Collection<StateAndPrecision> pStates,
      Collection<StateAndPrecision> pCandidates,
      boolean pCoverage) {
    return pStates.stream()
        .allMatch(
            s ->
                pCandidates.stream()
                    .anyMatch(
                        c ->
                            s.state() == c.state()
                                || (pCoverage && subsumedBy.containsEntry(s.state(), c.state()))));
  }

  private AbstractState state() {
    return mock(AbstractState.class);
  }

  private DssPostConditionMessage message(String pSender, AbstractState... pStates)
      throws Exception {
    DssPostConditionMessage message =
        messageFactory.createDssPostConditionMessage(
            pSender,
            AlgorithmStatus.SOUND_AND_PRECISE,
            ImmutableMap.of("id", Integer.toString(messageCounter++)));
    ImmutableList.Builder<StateAndPrecision> states = ImmutableList.builder();
    for (AbstractState state : pStates) {
      states.add(new StateAndPrecision(state, precision));
    }
    when(analysis.deserialize(message)).thenReturn(states.build());
    return message;
  }

  @Test
  public void coveredPredecessorIsExploredAgainOnceCoverageIsLost() throws Exception {
    AbstractState x = state();
    AbstractState y = state();
    AbstractState z = state();
    subsumedBy.put(y, x);
    PartialReplacePreconditionHandler handler = new PartialReplacePreconditionHandler(analysis);

    assertThat(handler.store(message(A, x)).shouldProceed()).isTrue();
    // y is subsumed by x: nothing to explore, but B relies on A from now on
    assertThat(handler.store(message(B, y)).shouldProceed()).isFalse();
    assertThat(handler.getCoverer(B)).hasValue(A);
    assertThat(handler.getActivePreconditions().keySet()).containsExactly(A);

    // A no longer covers B, so B has to be explored, too
    assertThat(handler.store(message(A, z)).shouldProceed()).isTrue();
    assertThat(handler.getCoverer(B)).isEmpty();
    assertThat(handler.getActivePreconditions().keySet()).containsExactly(A, B);

    // repeating a set changes nothing
    assertThat(handler.store(message(A, z)).shouldProceed()).isFalse();
    assertThat(handler.store(message(B, y)).shouldProceed()).isFalse();
  }

  @Test
  public void dependentsMoveToTheCovererOfTheirCoverer() throws Exception {
    AbstractState c = state();
    AbstractState x = state();
    AbstractState x2 = state();
    AbstractState y = state();
    subsumedBy.put(y, x);
    subsumedBy.put(y, x2);
    // y is also subsumed by c, but only transitively: the handler must not ask for it
    subsumedBy.put(x2, c);
    PartialReplacePreconditionHandler handler = new PartialReplacePreconditionHandler(analysis);

    assertThat(handler.store(message(C, c)).shouldProceed()).isTrue();
    assertThat(handler.store(message(A, x)).shouldProceed()).isTrue();
    assertThat(handler.store(message(B, y)).shouldProceed()).isFalse();
    assertThat(handler.getCoverer(B)).hasValue(A);

    // A becomes covered by C. B is still covered by the new set of A and moves to C, so that
    // every covering predecessor stays active. What A contributed so far has to be withdrawn.
    assertThat(handler.store(message(A, x2)).shouldProceed()).isTrue();
    assertThat(handler.getCoverer(A)).hasValue(C);
    assertThat(handler.getCoverer(B)).hasValue(C);
    assertThat(handler.getActivePreconditions().keySet()).containsExactly(C);
  }

  @Test
  public void lostCoverageIsTakenOverByAnotherPredecessor() throws Exception {
    AbstractState x = state();
    AbstractState y = state();
    AbstractState z = state();
    AbstractState c = state();
    subsumedBy.put(y, x);
    subsumedBy.put(y, c);
    PartialReplacePreconditionHandler handler = new PartialReplacePreconditionHandler(analysis);

    assertThat(handler.store(message(A, x)).shouldProceed()).isTrue();
    assertThat(handler.store(message(B, y)).shouldProceed()).isFalse();
    assertThat(handler.getCoverer(B)).hasValue(A);
    assertThat(handler.store(message(C, c)).shouldProceed()).isTrue();

    // A no longer covers B, but C does: A has to be explored again, B still does not
    assertThat(handler.store(message(A, z)).shouldProceed()).isTrue();
    assertThat(handler.getCoverer(B)).hasValue(C);
    assertThat(handler.getActivePreconditions().keySet()).containsExactly(A, C);
  }

  @Test
  public void unreachableBlockEnds() throws Exception {
    AbstractState x = state();
    PartialReplacePreconditionHandler handler = new PartialReplacePreconditionHandler(analysis);
    DssPostConditionMessage unreachableA =
        messageFactory.createDssUnreachableBlockEndMessage(A, AlgorithmStatus.SOUND_AND_PRECISE);
    DssPostConditionMessage unreachableB =
        messageFactory.createDssUnreachableBlockEndMessage(B, AlgorithmStatus.SOUND_AND_PRECISE);
    DssPostConditionMessage unreachableC =
        messageFactory.createDssUnreachableBlockEndMessage(C, AlgorithmStatus.SOUND_AND_PRECISE);

    assertThat(handler.isAnyPredecessorSilent()).isTrue();
    assertThat(handler.store(unreachableA).shouldProceed()).isTrue();
    // an empty set is covered by any other set
    assertThat(handler.store(unreachableB).shouldProceed()).isFalse();
    assertThat(handler.isUnreachable()).isFalse();
    assertThat(handler.store(unreachableC).shouldProceed()).isFalse();
    assertThat(handler.isAnyPredecessorSilent()).isFalse();
    assertThat(handler.isUnreachable()).isTrue();
    assertThat(handler.store(unreachableC).shouldProceed()).isFalse();

    // a reachable block end is not covered by an unreachable one
    assertThat(handler.store(message(B, x)).shouldProceed()).isTrue();
    assertThat(handler.isUnreachable()).isFalse();
    assertThat(handler.getActivePreconditions().keySet()).containsExactly(A, B);
  }
}
