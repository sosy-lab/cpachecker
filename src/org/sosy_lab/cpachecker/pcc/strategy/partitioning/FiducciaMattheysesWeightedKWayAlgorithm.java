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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.pcc.FiducciaMattheysesOptimizer;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedNode;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.FiducciaMattheysesOptimzerFactory.OptimizationCriteria;
public class FiducciaMattheysesWeightedKWayAlgorithm {

  private final double balanceCriterion;
  private final int maxLoad;
  private final WeightedGraph wGraph;
  private List<Set<Integer>> actualPartitioning;
  private int[] partitionWeights;
  private int[] nodeToPartition;

  private final FiducciaMattheysesOptimizer optimizer;

  public FiducciaMattheysesWeightedKWayAlgorithm(List<Set<Integer>> pInitPartitioning,
      double pBalanceCriterion, WeightedGraph pWGraph, int pMaxLoad, OptimizationCriteria opt) {
    super();
    optimizer=FiducciaMattheysesOptimzerFactory.createFMOptimizer(opt);
    balanceCriterion = pBalanceCriterion;
    wGraph = pWGraph;
    actualPartitioning = pInitPartitioning;
    maxLoad = pMaxLoad;
    initializePartitionStructures();
  }


  /**
   * Initialize node to partition Mapping and the actual partitions weights
   */
  private void initializePartitionStructures() {
    int numNodes = wGraph.getNumNodes();
    int numPartitions = actualPartitioning.size();
    partitionWeights = new int[numPartitions];
    nodeToPartition = new int[numNodes];

    for (int partition = 0; partition < numPartitions; partition++) {
      for (Integer node : actualPartitioning.get(partition)) {
        int nodeWeight = wGraph.getNode(node).getWeight();
        nodeToPartition[node] = partition;
        partitionWeights[partition] += nodeWeight;
      }
    }
  }


  /**
  * Get partition given node belongs to afterwards structure was initialized
  * @param node node to be looked up
  * @return number of containing partition
  */
  private int getPartition(int node) {
    return nodeToPartition[node];
  }

  /**
   * Refines a partition with a greedy KL/FM-algorithm
   * Visit each node in a randomized order once; check if it can be moved resulting in a better partition
   * @return the total gain due to the node movements
   */
  public int refinePartitioning() {
    int totalGain = 0;
    for (WeightedNode node : wGraph.randomIterator()) {
      int nodeNum = node.getNodeNumber();
      int maxGain = 0;
      int from = getPartition(nodeNum);
      int maxUnbalancing=node.getWeight();
      int to = from;
      for (Integer toPartition : getSuccessorPartitions(nodeNum)) {
        if (isNodeMovable(nodeNum, from, toPartition)) {
          int gain = optimizer.computeGain(nodeNum, toPartition,nodeToPartition,wGraph);
          if (gain > maxGain) {
            maxGain = gain;
            to = toPartition;
          }else if(maxGain<=0&&gain==0){
            //There was no good partner found until now, check if partitions are unbalanced
            int unbalancing=partitionWeights[from]-partitionWeights[to];
            if(unbalancing>maxUnbalancing){ //switch it to the most unbalanced partition
              maxUnbalancing=unbalancing;
              to = toPartition;
            }
          }
        }
      }
      moveNode(nodeNum, from, to);
      totalGain += maxGain;
    }
    return totalGain;
  }

  /**
   * Compute all succeeding partitions of a given node
   * @param node node, whose following partitions are computed
   * @return set of all succeeding partition
   */
  private Set<Integer> getSuccessorPartitions(int node) {
    Set<Integer> succPartitions = new HashSet<>(wGraph.getSuccessors(node).size());
    for (Integer succ : wGraph.getIntSuccessors(node)) {
      succPartitions.add(getPartition(succ));
    }
    return succPartitions;
  }

  /**
   * Checks if a node can be moved from partition x to partition y regarding balancing constraints
   * @param node node to be checked if movable
   * @param fromPartition Move from fromPartition
   * @param toPartition Move to toPartition
   * @return true if fromPartition!=toPartition and weight of fromPartition added to node's weight holds balancing constraints
   */
  private boolean isNodeMovable(int node, int fromPartition, int toPartition) {
    int partWeight = partitionWeights[toPartition];//actual weight of goal partition
    int nodeWeight = wGraph.getNode(node).getWeight();
    if (partWeight + nodeWeight > balanceCriterion * maxLoad) { return false; }
    if (fromPartition == toPartition) { //Not "move" inside a partition
      return false;
    }
    return true;
  }

  /**
   * Move a node from a partition to a partition
   * updates data in internalDegree,externalDegree and returns the gain
   * check if nodeIsMovable is not done here!
   * @param node node to be moved
   * @param fromPartition partition node is in
   * @param toPartition partition node should be stored
   */
  private boolean moveNode(int node, int fromPartition, int toPartition) {
    if (!isNodeMovable(node, fromPartition, toPartition)) { //Node cannot be moved this way
      return false;
    }
    int nodeWeight = wGraph.getNode(node).getWeight();
    //Update partition weights and partition itself
    partitionWeights[fromPartition] -= nodeWeight;
    partitionWeights[toPartition] += nodeWeight;
    nodeToPartition[node] = toPartition;
    actualPartitioning.get(fromPartition).remove(node);
    actualPartitioning.get(toPartition).add(node);
    return true; //Node was moved
  }
}
