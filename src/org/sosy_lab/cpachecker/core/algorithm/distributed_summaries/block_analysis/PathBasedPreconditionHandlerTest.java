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
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;

public class PathBasedPreconditionHandlerTest {

  private static final String THIS_BLOCK = "B";
  private static final String PREDECESSOR = "A";

  private DssMessageFactory messageFactory;
  private DssBlockAnalysis analysis;
  private BlockNode block;

  /** Pairs (covered, covering) of state ids that the mocked coverage check treats as covered. */
  private final Set<List<String>> coverage = new LinkedHashSet<>();

  private PathBasedPreconditionHandler handler;

  @Before
  public void setUp() throws Exception {
    messageFactory =
        new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
    block = mock(BlockNode.class);
    when(block.getId()).thenReturn(THIS_BLOCK);
    when(block.isRoot()).thenReturn(false);
    when(block.getPredecessorIds()).thenReturn(ImmutableSet.of(PREDECESSOR));

    analysis = mock(DssBlockAnalysis.class);
    when(analysis.getBlock()).thenReturn(block);
    when(analysis.getLogger()).thenReturn(mock(LogManager.class));
    when(analysis.statistics()).thenReturn(new DssSingleWorkerStatistics("test-block"));
    when(analysis.shouldProceedForward(any())).thenReturn(DssMessageProcessing.proceed());
    when(analysis.makeStartState(true)).thenReturn(state("start").state());
    when(analysis.makeStartPrecision()).thenReturn(mock(Precision.class));
    // the receiver appends itself to the history of every precondition it receives
    when(analysis.withBlockInHistory(ArgumentMatchers.<Collection<StateAndPrecision>>any()))
        .thenAnswer(
            invocation -> {
              Collection<StateAndPrecision> received = invocation.getArgument(0);
              received.forEach(sap -> sap.getBlockState().addHistory(block));
              return ImmutableList.copyOf(received);
            });
    // States of this test are equal if and only if they have the same id: a repeated message
    // brings freshly deserialized states, i.e., equal ones, but not the same objects.
    when(analysis.statesEqual(any(), any()))
        .thenAnswer(
            invocation -> {
              Collection<StateAndPrecision> states1 = invocation.getArgument(0);
              Collection<StateAndPrecision> states2 = invocation.getArgument(1);
              return ids(states1).equals(ids(states2));
            });
    when(analysis.allCovered(any(), any()))
        .thenAnswer(
            invocation -> {
              Collection<StateAndPrecision> states = invocation.getArgument(0);
              Collection<StateAndPrecision> candidates = invocation.getArgument(1);
              return states.stream()
                  .allMatch(
                      covered ->
                          candidates.stream()
                              .anyMatch(
                                  covering ->
                                      coverage.contains(
                                          ImmutableList.of(id(covered), id(covering)))));
            });

    handler = new PathBasedPreconditionHandler(analysis);
  }

  private static String id(StateAndPrecision pState) {
    return pState.getBlockState().getUniqueId();
  }

  private static ImmutableSet<String> ids(Collection<StateAndPrecision> pStates) {
    return pStates.stream()
        .map(PathBasedPreconditionHandlerTest::id)
        .collect(ImmutableSet.toImmutableSet());
  }

  private static StateAndPrecision state(String pId, String... pHistory) {
    BlockState blockState =
        new BlockState(
            pId,
            null,
            mock(CFANode.class),
            mock(BlockNode.class),
            BlockStateType.INITIAL,
            ImmutableList.of(),
            BlockGraphPath.of(pHistory),
            SegmentedPaths.EMPTY);
    return new StateAndPrecision(blockState, mock(Precision.class));
  }

  private DssPostConditionMessage message(
      List<BlockGraphPath> pRetracted, StateAndPrecision... pPostconditions)
      throws InterruptedException {
    DssPostConditionMessage received =
        messageFactory.createDssPostConditionMessage(
            PREDECESSOR,
            AlgorithmStatus.SOUND_AND_PRECISE,
            serialized(pPostconditions),
            pRetracted,
            ImmutableMap.of());
    when(analysis.deserialize(received)).thenReturn(ImmutableList.copyOf(pPostconditions));
    return received;
  }

  private DssPostConditionMessage message(StateAndPrecision... pPostconditions)
      throws InterruptedException {
    return message(ImmutableList.of(), pPostconditions);
  }

  private static ImmutableMap<String, String> serialized(StateAndPrecision... pPostconditions) {
    // the content is irrelevant, deserialization is mocked
    return ImmutableMap.of("states", Integer.toString(pPostconditions.length));
  }

  private static BlockGraphPath path(String... pBlocks) {
    return BlockGraphPath.of(pBlocks);
  }

  /** A context that is new and unrelated to every known path is kept beside the others. */
  @Test
  public void unrelatedContextIsKept() throws Exception {
    StateAndPrecision viaX = state("s1", "X", PREDECESSOR);
    StateAndPrecision viaY = state("s2", "Y", PREDECESSOR);

    assertThat(handler.store(message(viaX)).shouldProceed()).isTrue();
    assertThat(handler.store(message(viaY)).shouldProceed()).isTrue();

    assertThat(handler.getKnownPreconditions()).containsExactly(viaX, viaY);
    assertThat(handler.consumeChangedContexts())
        .containsExactly(path("X", PREDECESSOR, THIS_BLOCK), path("Y", PREDECESSOR, THIS_BLOCK));
  }

  /** Receiving the same states for the same path again changes nothing. */
  @Test
  public void repeatedContextStops() throws Exception {
    StateAndPrecision first = state("s1", "X", PREDECESSOR);
    handler.store(message(first));
    // what the exploration consumes and publishes
    handler.consumeChangedContexts();
    handler.consumeWithholdingToAnnounce();

    DssPostConditionMessage repeated = message(state("s1", "X", PREDECESSOR));
    assertThat(handler.store(repeated).shouldProceed()).isFalse();
    assertThat(handler.consumeChangedContexts()).isEmpty();
  }

  /**
   * An update replaces only the context of its own path. A context derived from the replaced one is
   * re-derived by exploring the update, which may confirm it unchanged, so it has to stay.
   */
  @Test
  public void updateReplacesOnlyItsOwnPath() throws Exception {
    StateAndPrecision entry = state("s1", "X", PREDECESSOR);
    StateAndPrecision derived = state("s2", "X", PREDECESSOR, THIS_BLOCK, PREDECESSOR);
    handler.store(message(entry, derived));

    StateAndPrecision updatedEntry = state("s3", "X", PREDECESSOR);
    assertThat(handler.store(message(updatedEntry)).shouldProceed()).isTrue();

    assertThat(handler.getKnownPreconditions()).containsExactly(updatedEntry, derived);
    assertThat(handler.consumeRetractedContexts()).isEmpty();
  }

  /**
   * A retracted context of the predecessor removes the context that continues it in this block and
   * every context derived from that one, and this block retracts the removed contexts in turn.
   */
  @Test
  public void retractionRemovesDescendantsAndIsPassedOn() throws Exception {
    StateAndPrecision entry = state("s1", "X", PREDECESSOR);
    StateAndPrecision derived = state("s2", "X", PREDECESSOR, THIS_BLOCK, PREDECESSOR);
    StateAndPrecision unrelated = state("s3", "Y", PREDECESSOR);
    handler.store(message(entry, derived, unrelated));
    handler.consumeRetractedContexts();

    DssPostConditionMessage retraction = message(ImmutableList.of(path("X", PREDECESSOR)));
    assertThat(handler.store(retraction).shouldProceed()).isTrue();

    assertThat(handler.getKnownPreconditions()).containsExactly(unrelated);
    assertThat(handler.consumeRetractedContexts())
        .containsExactly(
            path("X", PREDECESSOR, THIS_BLOCK),
            path("X", PREDECESSOR, THIS_BLOCK, PREDECESSOR, THIS_BLOCK));
  }

  /**
   * A postcondition that descends from a context the same message retracts was derived from that
   * context before the sender learned that it is gone, so it must not be stored.
   */
  @Test
  public void postconditionOfContextRetractedInSameMessageIsDropped() throws Exception {
    StateAndPrecision entry = state("s1", "X", PREDECESSOR);
    handler.store(message(entry));

    StateAndPrecision outdated = state("s2", "X", PREDECESSOR, THIS_BLOCK, PREDECESSOR);
    handler.store(message(ImmutableList.of(path("X", PREDECESSOR)), outdated));

    assertThat(handler.getKnownPreconditions()).isEmpty();
  }

  /**
   * A new generation of a path that another context covers is parked instead of explored, so what
   * the successors derived from the previous generation has to be retracted.
   */
  @Test
  public void coveredUpdateRetractsPreviousGeneration() throws Exception {
    StateAndPrecision covering = state("s1", "Y", PREDECESSOR);
    StateAndPrecision previous = state("s2", "X", PREDECESSOR);
    handler.store(message(covering, previous));
    handler.consumeRetractedContexts();

    StateAndPrecision update = state("s3", "X", PREDECESSOR);
    coverage.add(ImmutableList.of(id(update), id(covering)));
    handler.store(message(update));

    assertThat(handler.getKnownPreconditions()).containsExactly(covering);
    assertThat(handler.consumeRetractedContexts())
        .containsExactly(path("X", PREDECESSOR, THIS_BLOCK));
  }

  /**
   * Violation conditions found from the speculative start state stay valid for the predecessor as
   * long as this block has not heard from all its predecessors.
   */
  @Test
  public void speculativeStartCountsAsExploredUntilAllPredecessorsSent() throws Exception {
    String speculativeId =
        handler.getSpeculativeStart().orElseThrow().getBlockState().getUniqueId();
    assertThat(handler.getIdsOfExploredStates()).containsExactly(speculativeId);

    handler.store(message(state("s1", "X", PREDECESSOR)));

    assertThat(handler.getIdsOfExploredStates()).containsExactly("s1");
  }
}
