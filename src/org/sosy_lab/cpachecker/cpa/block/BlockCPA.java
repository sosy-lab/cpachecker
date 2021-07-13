// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;

public class BlockCPA extends AbstractCPA {

  private final LocationStateFactory factory;
  private final CFANode startNode;

  public BlockCPA(BlockNode pBlockNode, LocationStateFactory pStateFactory) {
    super("sep", "sep", new BlockTransferRelation(pStateFactory, pBlockNode));
    factory = pStateFactory;
    startNode = pBlockNode.getStartNode();
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return factory.getState(startNode);
  }
}
