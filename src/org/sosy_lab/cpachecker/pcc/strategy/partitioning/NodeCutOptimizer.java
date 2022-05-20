// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import org.sosy_lab.cpachecker.core.interfaces.pcc.FiducciaMattheysesOptimizer;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedEdge;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedNode;

public class NodeCutOptimizer implements FiducciaMattheysesOptimizer {

  @Override
  public int computeGain(int node, int toPartition, int[] nodeToPartition,
      WeightedGraph wGraph) {

    int gain =
        computeExternalDegree(node, toPartition, nodeToPartition, wGraph)
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
