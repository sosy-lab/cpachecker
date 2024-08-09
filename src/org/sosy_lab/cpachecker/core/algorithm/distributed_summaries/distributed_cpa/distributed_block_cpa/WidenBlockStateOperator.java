// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.widen.WidenOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class WidenBlockStateOperator implements WidenOperator {

  @Override
  public AbstractState combine(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    BlockState blockState1 = (BlockState) state1;
    BlockState blockState2 = (BlockState) state2;
    Preconditions.checkArgument(
        blockState1.getLocationNode().equals(blockState2.getLocationNode()));
    Preconditions.checkArgument(blockState1.getBlockNode().equals(blockState2.getBlockNode()));
    Preconditions.checkArgument(blockState1.getType().equals(blockState2.getType()));
    return new BlockState(
        blockState1.getLocationNode(),
        blockState1.getBlockNode(),
        blockState1.getType(),
        ImmutableSet.<AbstractState>builder()
            .addAll(blockState1.getErrorConditions())
            .addAll(blockState2.getErrorConditions())
            .build());
  }
}
