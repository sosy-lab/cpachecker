// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineViolationConditionsOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;

public class BlockStateCombineViolationConditionOperator
    implements CombineViolationConditionsOperator {

  @Override
  public AbstractState combineViolationConditionsAtSameProgramHash(
      Collection<AbstractState> states) {
    ImmutableSet<CFANode> locations =
        FluentIterable.from(states)
            .filter(BlockState.class)
            .transform(BlockState::getLocationNode)
            .toSet();
    ImmutableList<ImmutableList<String>> finalWitness =
        FluentIterable.from(states)
            .filter(BlockState.class)
            .transformAndConcat(BlockState::getWitness)
            .toList();
    boolean stemsFromTopState =
        Iterables.any(states, s -> s instanceof BlockState b && b.isStemsFromTopState());
    AbstractState reference = Iterables.getFirst(states, null);
    Preconditions.checkNotNull(reference);
    BlockState blockState = (BlockState) reference;
    return new BlockState(
        Iterables.getOnlyElement(locations),
        blockState.getBlockNode(),
        blockState.getType(),
        blockState.getViolationConditions(),
        blockState.getHistory(),
        finalWitness,
        stemsFromTopState);
  }
}
