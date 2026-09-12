// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;

public class BlockStateTest {

  private BlockNode block;
  private BlockState state;

  @Before
  public void setUp() {
    CFANode node = CFANode.newDummyCFANode();
    block =
        new BlockNode(
            "B",
            node,
            node,
            ImmutableSet.of(node),
            ImmutableSet.of(),
            ImmutableSet.of(),
            ImmutableSet.of());
    state =
        new BlockState(
            "B#0",
            null,
            node,
            block,
            BlockStateType.FINAL,
            ImmutableList.of(),
            BlockGraphPath.of(),
            SegmentedPaths.EMPTY);
  }

  @Test
  public void processingDoesNotChangeTheAbstractValueOrPartition() {
    BlockState first = state;
    BlockState second = state.withHistory(block);
    BlockState end = state.withViolationConditions(ImmutableList.of(first, second));
    Object partition = end.getPartitionKey();
    int hash = end.hashCode();
    end.setProcessedViolationConditions(ImmutableList.of(first));
    end.addHinderedByCallstack(second);
    assertThat(end.getViolationConditions()).containsExactly(first, second);
    assertThat(end.getPendingViolationConditions()).containsExactly(second);
    assertThat(end.getPartitionKey()).isEqualTo(partition);
    assertThat(end.hashCode()).isEqualTo(hash);
  }

  @Test
  public void anotherOccurrenceHasIndependentProcessingRecords() {
    BlockState end = state.withViolationConditions(ImmutableList.of(state));
    BlockState other = state.withViolationConditions(ImmutableList.of(state));
    end.setProcessedViolationConditions(ImmutableList.of(state));
    assertThat(end.getPendingViolationConditions()).isEmpty();
    assertThat(other.getPendingViolationConditions()).containsExactly(state);
  }

  @Test
  public void removedGhostStatesInvalidateCompletionRecords() {
    BlockState end = state.withViolationConditions(ImmutableList.of(state));
    end.setProcessedViolationConditions(ImmutableList.of(state));
    end.setProcessedViolationConditions(ImmutableList.of());
    assertThat(end.getPendingViolationConditions()).containsExactly(state);
  }

  @Test
  public void resetDoesNotLeakConditionsOrCallstackRejections() {
    BlockState end = state.withViolationConditions(ImmutableList.of(state)).withHistory(block);
    end.addHinderedByCallstack(state);
    BlockState reset = end.reset();
    assertThat(reset.getViolationConditions()).isEmpty();
    assertThat(reset.getHinderedByCallstack()).isEmpty();
    assertThat(reset.getPredecessor()).isNull();
    assertThat(reset.getHistory()).isEqualTo(end.getHistory());
    assertThat(end.getViolationConditions()).containsExactly(state);
  }

  @Test
  public void addingHistoryDoesNotMutateTheInput() {
    BlockState updated = state.withHistory(block);
    assertThat(state.getHistory().path()).isEmpty();
    assertThat(updated.getHistory().path()).containsExactly("B");
  }
}
