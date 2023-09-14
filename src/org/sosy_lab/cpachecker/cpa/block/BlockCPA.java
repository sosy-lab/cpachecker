// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.cpa.block.BlockTransferRelation.ForwardBlockTransferRelation;

public class BlockCPA extends AbstractCPA {

  private BlockNode blockNode;

  public BlockCPA() {
    super("sep", "sep", new FlatLatticeDomain(), new ForwardBlockTransferRelation());
  }

  public void init(BlockNode pBlockNode) {
    assert pBlockNode != null;
    assert blockNode == null;
    blockNode = pBlockNode;
  }

  public static CPAFactory factory() {
    return new BlockCPAFactory();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new BlockState(node, blockNode, BlockStateType.INITIAL, Optional.empty());
  }

  public static BlockCPA create() {
    return new BlockCPA();
  }
}
