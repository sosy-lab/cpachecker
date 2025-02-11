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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ProceedBlockStateOperator implements ProceedOperator {

  private final BlockNode block;

  public ProceedBlockStateOperator(BlockNode pBlock) {
    block = pBlock;
  }

  @Override
  public DssMessageProcessing processForward(AbstractState pState) {
    if (Objects.equals(AbstractStates.extractLocation(pState), block.getInitialLocation())) {
      return DssMessageProcessing.proceed();
    } else {
      return DssMessageProcessing.stop();
    }
  }

  @Override
  public DssMessageProcessing processBackward(AbstractState pState) {
    CFANode node = Objects.requireNonNull(AbstractStates.extractLocation(pState));
    if (!(node.equals(block.getFinalLocation())
        || (!node.equals(block.getInitialLocation()) && block.getNodes().contains(node)))) {
      return DssMessageProcessing.stop();
    }
    return DssMessageProcessing.proceed();
  }
}
