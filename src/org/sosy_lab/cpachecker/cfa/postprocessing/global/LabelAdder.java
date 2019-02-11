/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.postprocessing.global;

import java.util.List;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

/**
 * Post-processing step for {@link CFA} that adds labels at specified points.
 */
public class LabelAdder {

  private static final String LABEL_NAME = "BLOCK_END_";

  /**
   * Add a unique label at each block end in the given CFA.
   * The configuration of {@link BlockOperator} is used to decide the size and structure
   * of blocks.
   *
   * @param pCfa the CFA to add labels to
   * @param pConfig the configuration to use
   */
  public void addLabelsAtBlockEnds(final MutableCFA pCfa, final Configuration pConfig) {
    BlockOperator blk = new BlockOperator();
    try {
      pConfig.inject(blk);
      blk.setCFA(pCfa);
    } catch (InvalidConfigurationException e) {
      // Should never happen here
      throw new AssertionError(e);
    }

    Set<CFANode> allNodes = CFATraversal.dfs().collectNodesReachableFrom(pCfa.getMainFunction());
    int labelsAdded = 0;
    for (CFANode n : allNodes) {
      if (blk.isBlockEnd(n, -1) && !n.equals(pCfa.getMainFunction())) {
        // add labels before the block end, and not after,
        // so that we know that they are reachable.
        // if we add labels after the block end,
        // they may be unreachable (e.g., "return 0; BLOCK_END_1:;")
        // This means that we don't create labels at the beginning of a code block,
        // but at it's end. This doesn't matter because, if the end is executed,
        // the beginning must be executed, too.
        int added = addLabelBefore(n, labelsAdded, pCfa);
        labelsAdded += added;
      }
    }
  }

  private int addLabelBefore(CFANode pNode, int pLabelNumber, MutableCFA pCfa) {
    final List<CFAEdge> enteringEdges = CFAUtils.enteringEdges(pNode).toList();
    int num = 0;
    for (CFAEdge e : enteringEdges)  {
      addLabelAfter(e, pLabelNumber + num, pCfa);
      num++;
    }
    return num;
  }

  private void addLabelAfter(CFAEdge pEdge, int pLabelNumber, MutableCFA pCfa) {
    final String labelName = LABEL_NAME + pLabelNumber;
    final CFANode start = pEdge.getPredecessor();
    final CFANode end = pEdge.getSuccessor();
    final String functionName = start.getFunctionName();
    final FunctionCloner fc = new FunctionCloner(functionName, functionName, false);
    final CFANode labelConnector = new CFANode(functionName);
    final CFANode labelNode = new CLabelNode(functionName, labelName);
    pCfa.addNode(labelNode);
    pCfa.addNode(labelConnector);

    final CFAEdge redirectedEdge = fc.cloneEdge(pEdge, start, labelConnector);
    start.removeLeavingEdge(pEdge);
    start.addLeavingEdge(redirectedEdge);
    end.removeEnteringEdge(pEdge);
    labelConnector.addEnteringEdge(redirectedEdge);

    final CFAEdge labelEdge = new BlankEdge(labelName + ":; ", FileLocation.DUMMY, labelConnector, labelNode, "Label: " + labelName);
    labelConnector.addLeavingEdge(labelEdge);
    labelNode.addEnteringEdge(labelEdge);
    final CFAEdge connectorEdge = new BlankEdge("", FileLocation.DUMMY, labelNode, end, "label connector");
    labelNode.addLeavingEdge(connectorEdge);
    end.addEnteringEdge(connectorEdge);
  }
}
