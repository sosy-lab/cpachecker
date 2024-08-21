// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.cpa.block.BlockTransferRelation.BackwardBlockTransferRelation;

public class BlockCPABackward extends AbstractCPA {

  private BlockNode blockNode;

  public BlockCPABackward() {
    super("sep", "sep", new FlatLatticeDomain(), new BackwardBlockTransferRelation());
  }

  public void init(BlockNode pBlockNode) {
    blockNode = pBlockNode;
    TransferRelation relation = getTransferRelation();
    checkState(
        relation instanceof BackwardBlockTransferRelation,
        "Expected %s but got %s",
        BackwardBlockTransferRelation.class,
        relation.getClass());
    ((BackwardBlockTransferRelation) relation).init(pBlockNode);
  }

  public static CPAFactory factory() {
    return new BlockCPAFactory(AnalysisDirection.BACKWARD);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new BlockState(
        node, blockNode, AnalysisDirection.BACKWARD, BlockStateType.INITIAL, false);
  }

  public static BlockCPABackward create() {
    return new BlockCPABackward();
  }
}
