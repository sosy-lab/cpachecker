// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import static com.google.common.base.Preconditions.checkArgument;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;

public class BlockCPA extends AbstractCPA {

  private final LocationStateFactory factory;
  private CFANode startNode;

  public BlockCPA(LocationStateFactory pStateFactory) {
    super("join", "sep", new BlockTransferRelation(pStateFactory));
    factory = pStateFactory;
  }

  public void init(BlockNode pBlockNode) {
    startNode = pBlockNode.getStartNode();
    TransferRelation relation = getTransferRelation();
    checkArgument(relation instanceof BlockTransferRelation, "Expected BlockTransferRelation but got " + relation.getClass());
    ((BlockTransferRelation)relation).init(pBlockNode);
  }

  public static CPAFactory factory() {
    return new BlockCPAFactory();
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return factory.getState(startNode);
  }

  public static BlockCPA create(CFA pCFA, Configuration pConfig)
      throws InvalidConfigurationException {
    return new BlockCPA(new LocationStateFactory(pCFA, AnalysisDirection.FORWARD, pConfig));
  }
}
