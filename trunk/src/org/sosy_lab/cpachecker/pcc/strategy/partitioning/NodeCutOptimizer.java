/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import org.sosy_lab.cpachecker.core.interfaces.pcc.FiducciaMattheysesOptimizer;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedEdge;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedNode;


public class NodeCutOptimizer implements FiducciaMattheysesOptimizer {

  @Override
  public int computeGain(int node, int toPartition, int[] nodeToPartition,
      WeightedGraph wGraph) {
    int gain = 0;
    gain = computeExternalDegree(node, toPartition, nodeToPartition, wGraph)
        - computeInternalDegree(node, nodeToPartition, wGraph);
    return gain;
  }

  @Override
  public int computeInternalDegree(int node, int[] nodeToPartition, WeightedGraph wGraph) {
    int internalDegree = 0;
    int partition = nodeToPartition[node];

    for (WeightedNode neighbor : wGraph.getNeighbors(node)) {
      if (nodeToPartition[neighbor.getNodeNumber()] == partition) {
        internalDegree += neighbor.getWeight();
      }
    }


    for (WeightedEdge inEdge : wGraph.getIncomingEdges(node)) {
      if (nodeToPartition[inEdge.getStartNode().getNodeNumber()] == partition) {
        internalDegree += inEdge.getWeight();
      }
    }

    return internalDegree;
  }

  @Override
  public int computeExternalDegree(int node, int toPartition, int[] nodeToPartition,
      WeightedGraph wGraph) {
    int externalDegree = 0;
    for (WeightedNode neighbor : wGraph.getNeighbors(node)) {
      if (nodeToPartition[neighbor.getNodeNumber()] == toPartition) {
        externalDegree += neighbor.getWeight();
      }
    }

    for (WeightedEdge inEdge : wGraph.getIncomingEdges(node)) {
      if (nodeToPartition[inEdge.getStartNode().getNodeNumber()] == toPartition) {
        externalDegree += inEdge.getWeight();
      }
    }
    return externalDegree;
  }

}
