// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.MatchingGenerator;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedEdge;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedNode;


public class HeavyEdgeMatchingGenerator implements MatchingGenerator {
  private final LogManager logger;
  public HeavyEdgeMatchingGenerator(LogManager pLogger) {
    logger=pLogger;
  }

  /**
   * Computes a random maximal matching, by matching the edge with highest possible weight for a random node
   * @param wGraph  the weighted graph a matching is computed on
   * @return the computed matching  (Matching maps a node to its corresponding new node number!)
   */
  @Override
  public Map<Integer, Integer> computeMatching(WeightedGraph wGraph) {
    Map<Integer, Integer> matching = new HashMap<>(wGraph.getNumNodes() / 2);
    BitSet alreadyMatched = new BitSet(wGraph.getNumNodes());
    int currentSuperNode = 0;

    for (WeightedNode node : wGraph.randomIterator()) {//randomly iterate over nodes
      int nodeNum = node.getNodeNumber();
      if (!alreadyMatched.get(nodeNum)) {
        int maxWeight=-1;
        int maxNeighbor=-1;
        //Node wasn't matched, check if unmatched successor exists, take the one with highest edge weight to it
        //if no match-partner exists, node is lonely
        for (WeightedEdge succEdge : wGraph.getOutgoingEdges(node)) {
          WeightedNode succ = succEdge.getEndNode();
          int succNum = succ.getNodeNumber();
          if (!alreadyMatched.get(succNum)) {//match both
            if(succEdge.getWeight()>maxWeight){
              maxWeight=succEdge.getWeight();
              maxNeighbor=succNum;
            }
          }
        }
        if(maxNeighbor>-1){ //Add the max neighbor to the matching
          matching.put(nodeNum, currentSuperNode);
          matching.put(maxNeighbor, currentSuperNode);
          alreadyMatched.set(nodeNum);
          alreadyMatched.set(maxNeighbor);
          logger.log(Level.FINE,
              String.format(
                  "[Multilevel] Node %d and %d matched to supernode %d- matched heaviest edge weight %d",
                  nodeNum, maxNeighbor, currentSuperNode, maxWeight));
        }else {//no neighbor found
          matching.put(nodeNum, currentSuperNode);
          alreadyMatched.set(nodeNum);
          logger.log(Level.FINE,
              String.format("[Multilevel] Node %d lonely: Supernode %d", nodeNum,
                  currentSuperNode));
        }
        currentSuperNode++;
      }
    }
    return matching;
  }

}
