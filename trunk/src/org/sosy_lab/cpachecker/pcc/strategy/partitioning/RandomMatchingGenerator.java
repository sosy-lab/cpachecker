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

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.MatchingGenerator;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedEdge;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedNode;

/**
 * Compute a matching in a random fashion, i.e. take a node: If there is a unmatched neighbor, add it to the matching.
 */
public class RandomMatchingGenerator implements MatchingGenerator {

  private final LogManager logger;

  public RandomMatchingGenerator(LogManager pLogger) {
    logger = pLogger;
  }

  /**
   * Computes a random maximal matching
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
        boolean nodeMatched = false;
        //Node wasn't matched, check if unmatched successor exists, take first one
        //if no match-partner exists, node is lonely
        for (WeightedEdge succEdge : wGraph.getOutgoingEdges(node)) {
          WeightedNode succ = succEdge.getEndNode();
          int succNum = succ.getNodeNumber();
          if (!alreadyMatched.get(succNum)) {//match both
            matching.put(nodeNum, currentSuperNode);
            matching.put(succNum, currentSuperNode);
            alreadyMatched.set(nodeNum);
            alreadyMatched.set(succNum);
            nodeMatched = true;
            logger.log(Level.FINEST,
                String.format(
                    "[Multilevel] Node %d and %d matched to supernode %d- matched weight %d",
                    nodeNum, succNum, currentSuperNode, succEdge.getWeight()));
            break;
          }
        }
        if (!nodeMatched) {
          matching.put(nodeNum, currentSuperNode);
          alreadyMatched.set(nodeNum);
          logger.log(Level.FINEST,
              String.format("[Multilevel] Node %d lonely: Supernode %d", nodeNum,
                  currentSuperNode));
        }
        currentSuperNode++;
      }
    }
    return matching;
  }
}
