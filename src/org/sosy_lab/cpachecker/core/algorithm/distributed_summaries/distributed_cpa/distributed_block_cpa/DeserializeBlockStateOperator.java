// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DeserializeBlockStateOperator implements DeserializeOperator {

  private final BlockNode blockNode;
  private final AnalysisDirection direction;
  private final Supplier<BooleanFormula> currentErrorConditionSupplier;
  private final Map<Integer, CFANode> nodeMap;

  public DeserializeBlockStateOperator(BlockNode pBlockNode, AnalysisDirection pDirection, Supplier<BooleanFormula> pFutureErrorCondition) {
    blockNode = pBlockNode;
    direction = pDirection;
    currentErrorConditionSupplier = pFutureErrorCondition;
    nodeMap = new HashMap<>();
    pBlockNode.getNodesInBlock().forEach(n -> nodeMap.put(n.getNodeNumber(), n));
  }

  @Override
  public AbstractState deserialize(BlockSummaryMessage pMessage) throws InterruptedException {
    return new BlockState(nodeMap.get(pMessage.getTargetNodeNumber()), blockNode, direction, BlockStateType.INITIAL, false, Optional.of(currentErrorConditionSupplier.get()));
  }
}
