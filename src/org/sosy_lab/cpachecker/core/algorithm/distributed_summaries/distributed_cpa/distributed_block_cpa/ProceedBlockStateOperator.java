// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

public class ProceedBlockStateOperator implements ProceedOperator {

  private final BlockNode block;
  private final AnalysisDirection direction;

  public ProceedBlockStateOperator(BlockNode pBlock, AnalysisDirection pDirection) {
    block = pBlock;
    direction = pDirection;
  }

  @Override
  public BlockSummaryMessageProcessing proceedForward(AbstractState pState) {
    if (Objects.equals(AbstractStates.extractLocation(pState), block.getStartNode())) {
      return BlockSummaryMessageProcessing.proceed();
    } else {
      return BlockSummaryMessageProcessing.stop();
    }
  }

  @Override
  public BlockSummaryMessageProcessing proceedBackward(AbstractState pState) {
    CFANode node = Objects.requireNonNull(AbstractStates.extractLocation(pState));
    if (!(node.equals(block.getLastNode())
        || (!node.equals(block.getStartNode()) && block.getNodesInBlock().contains(node)))) {
      return BlockSummaryMessageProcessing.stop();
    }
    return BlockSummaryMessageProcessing.proceed();
  }

  @Override
  public BlockSummaryMessageProcessing proceed(AbstractState pState)
      throws InterruptedException, SolverException {
    return direction == AnalysisDirection.FORWARD
        ? proceedForward(pState)
        : proceedBackward(pState);
  }
}
