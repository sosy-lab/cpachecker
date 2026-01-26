// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;

public class DeserializeBlockStateOperator implements DeserializeOperator {

  private final BlockNode blockNode;

  public DeserializeBlockStateOperator(BlockNode pBlockNode) {
    blockNode = pBlockNode;
  }

  @Override
  public AbstractState deserialize(DssMessage pMessage) throws InterruptedException {
    String content = pMessage.getAbstractStateContent(BlockState.class).get(STATE_KEY);
    List<String> idAndHistory = Splitter.on(", ").limit(2).splitToList(content);
    String serializedBlockState = idAndHistory.getFirst();
    List<String> history =
        idAndHistory.size() == 2
            ? Splitter.on(", ").splitToList(idAndHistory.get(1))
            : ImmutableList.of();
    Preconditions.checkNotNull(serializedBlockState);
    Preconditions.checkArgument(
        blockNode.getPredecessorIds().contains(serializedBlockState)
            || blockNode.getSuccessorIds().contains(serializedBlockState));
    return new BlockState(
        DeserializeOperator.startLocationFromMessageType(pMessage, blockNode),
        blockNode,
        BlockStateType.INITIAL,
        ImmutableList.of(),
        history);
  }
}
