// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryPostConditionMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.java_smt.api.SolverException;

public class ProceedBlockStateOperator implements ProceedOperator {

  private final BlockNode block;
  private final AnalysisDirection direction;

  public ProceedBlockStateOperator(BlockNode pBlock, AnalysisDirection pDirection) {
    block = pBlock;
    direction = pDirection;
  }

  @Override
  public BlockSummaryMessageProcessing proceedForward(BlockSummaryPostConditionMessage pMessage)
      throws InterruptedException {
    CFANode node = block.getNodeWithNumber(pMessage.getTargetNodeNumber());
    if (!block.getStartNode().equals(node)) {
      return BlockSummaryMessageProcessing.stop();
    }
    return BlockSummaryMessageProcessing.proceed();
  }

  @Override
  public BlockSummaryMessageProcessing proceedBackward(BlockSummaryErrorConditionMessage pMessage)
      throws InterruptedException, SolverException {
    CFANode node = block.getNodeWithNumber(pMessage.getTargetNodeNumber());
    if (!(node.equals(block.getLastNode())
        || (!node.equals(block.getLastNode())
        && !node.equals(block.getStartNode())
        && block.getNodesInBlock().contains(node)))) {
      return BlockSummaryMessageProcessing.stop();
    }
    return BlockSummaryMessageProcessing.proceed();
  }

  @Override
  public BlockSummaryMessageProcessing proceed(BlockSummaryMessage pMessage)
      throws InterruptedException, SolverException {
    return direction == AnalysisDirection.FORWARD ? proceedForward((BlockSummaryPostConditionMessage) pMessage) : proceedBackward((BlockSummaryErrorConditionMessage) pMessage);
  }

  @Override
  public boolean isFeasible(AbstractState pState) {
    return true;
  }

  @Override
  public void update(BlockSummaryPostConditionMessage pLatestOwnPreconditionMessage)
      throws InterruptedException {}
}
