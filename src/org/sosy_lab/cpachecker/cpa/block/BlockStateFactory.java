// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Collection;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BackwardsBlockState;

@Options(prefix = "cpa.block")
public class BlockStateFactory {

  private final BlockState[] states;
  private final AnalysisDirection locationType;
  private CFANode startNode;
  private final ImmutableSortedSet<CFANode> allNodes;

  public BlockStateFactory(CFA pCfa, AnalysisDirection pLocationType, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this);
    locationType = checkNotNull(pLocationType);
    startNode = pCfa.getMainFunction();

    Collection<CFANode> tmpNodes = pCfa.getAllNodes();
    if (tmpNodes instanceof ImmutableSortedSet) {
      allNodes = (ImmutableSortedSet<CFANode>) tmpNodes;
    } else {
      allNodes = ImmutableSortedSet.copyOf(tmpNodes);
    }

    int maxNodeNumber = allNodes.last().getNodeNumber();
    states = new BlockState[maxNodeNumber + 1];
    for (CFANode node : allNodes) {
      BlockState state = createLocationState(node);
      states[node.getNodeNumber()] = state;
    }
  }

  public void setStartNode(final CFANode pStartNode) {
    startNode = pStartNode;
    for (CFANode node : allNodes) {
      BlockState state = createLocationState(node);
      states[node.getNodeNumber()] = state;
    }
  }

  public BlockState getState(CFANode node) {
    int nodeNumber = checkNotNull(node).getNodeNumber();

    if (nodeNumber >= 0 && nodeNumber < states.length) {
      return Preconditions.checkNotNull(
          states[nodeNumber],
          "LocationState for CFANode %s in function %s requested,"
              + " but this node is not part of the current CFA.",
          node,
          node.getFunctionName());

    } else {
      return createLocationState(node);
    }
  }

  private BlockState createLocationState(CFANode node) {
    return locationType == AnalysisDirection.BACKWARD
           ? new BackwardsBlockState(node, checkNotNull(startNode, "You need to set a start node for backwards analysis."))
           : new BlockState(node);
  }
}
