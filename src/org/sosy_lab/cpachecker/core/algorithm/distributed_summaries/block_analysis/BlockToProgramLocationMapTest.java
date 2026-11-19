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

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.DssCallstackState;

public class BlockToProgramLocationMapTest {

  private static BlockToProgramLocationMap map() {
    DistributedConfigurableProgramAnalysis cpa = mock(DistributedConfigurableProgramAnalysis.class);
    when(cpa.computeProgramPointId(any())).thenReturn("entry");
    return new BlockToProgramLocationMap(cpa, ImmutableSet.of("known", "missing"));
  }

  private static StateAndPrecision state(boolean pUnknownCallstack) {
    return new StateAndPrecision(
        new DssCallstackState(
            new CallstackState(null, "main", CFANode.newDummyCFANode("main")), pUnknownCallstack),
        SingletonPrecision.getInstance());
  }

  @Test
  public void unreachablePredecessorsAreNotMissing() {
    BlockToProgramLocationMap map = map();
    map.addStateForKey("known", state(false));
    assertThat(map.isAnyPredecessorTrulyEmpty()).isTrue();
    map.markUnreachable("missing");
    assertThat(map.isAnyPredecessorTrulyEmpty()).isFalse();
    map.markReachable("missing");
    assertThat(map.isAnyPredecessorTrulyEmpty()).isTrue();
    map.addStateForKey("missing", state(false));
    assertThat(map.isAnyPredecessorTrulyEmpty()).isFalse();
  }

  @Test
  public void unknownCallstacksStillRequireSpeculation() {
    BlockToProgramLocationMap map = map();
    map.addStateForKey("known", state(true));
    map.markUnreachable("missing");
    assertThat(map.isAnyPredecessorTrulyEmpty()).isTrue();
  }
}
