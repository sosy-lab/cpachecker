// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.concurrent.LazyInit;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;

public class BlockCPA extends AbstractCPA {

  private @LazyInit BlockNode blockNode;

  public BlockCPA(Configuration pConfiguration) throws InvalidConfigurationException {
    super("sep", "sep", new FlatLatticeDomain(), new BlockTransferRelation(pConfiguration));
  }

  public void init(BlockNode pBlockNode) {
    assert pBlockNode != null;
    assert blockNode == null;
    blockNode = pBlockNode;
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new BlockState(
        node,
        blockNode,
        BlockStateType.INITIAL,
        ImmutableList.of(),
        ImmutableList.of(),
        ViolationWitness.EMPTY,
        false);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BlockCPA.class);
  }
}
