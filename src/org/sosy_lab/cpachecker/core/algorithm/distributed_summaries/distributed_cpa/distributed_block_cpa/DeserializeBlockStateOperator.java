// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;

public class DeserializeBlockStateOperator implements DeserializeOperator {

  private final BlockNode blockNode;
  private final AnalysisDirection direction;
  private final ImmutableMap<Integer, CFANode> integerCFANodeMap;

  public DeserializeBlockStateOperator(
      BlockNode pBlockNode,
      ImmutableMap<Integer, CFANode> pIntegerCFANodeMap,
      AnalysisDirection pDirection) {
    blockNode = pBlockNode;
    direction = pDirection;
    integerCFANodeMap = pIntegerCFANodeMap;
  }

  @Override
  public AbstractState deserialize(BlockSummaryMessage pMessage) throws InterruptedException {
    return new BlockState(
        integerCFANodeMap.get(pMessage.getTargetNodeNumber()),
        blockNode,
        direction,
        BlockStateType.INITIAL,
        Optional.empty());
  }
}
