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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartitioningRefiner;
import org.sosy_lab.cpachecker.core.interfaces.pcc.WeightedBalancedGraphPartitioner;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedEdge;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedNode;

//@Options(prefix = "pcc.partitioning.multilevel")
public class MultilevelBalancedGraphPartitioner implements WeightedBalancedGraphPartitioner {

  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;
  @SuppressWarnings("unused")
  private final LogManager logger;

  @SuppressWarnings("unused")
  private final Configuration config;


  //Make this use a local partitioner, global partitioner distinction
  private final PartitioningRefiner refiner;
  private final WeightedBalancedGraphPartitioner globalPartitioner;

  public MultilevelBalancedGraphPartitioner(Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    pConfig.inject(this);
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
    config = pConfig;
    //TODO: Use GlobalPartitioningHeuristicFactory, PartitioningRefinerFactory. Make choosable via option(s)
    refiner = new FiducciaMattheysesKWayBalancedGraphPartitioner(pConfig, pLogger, pShutdownNotifier);
    globalPartitioner=new BestFirstWeightedBalancedGraphPartitioner(pConfig, pLogger, pShutdownNotifier);
  }

  @Override
  public List<Set<Integer>> computePartitioning(int pNumPartitions,
      PartialReachedSetDirectedGraph pGraph) throws InterruptedException {
    return computePartitioning(pNumPartitions, new WeightedGraph(pGraph));
  }

  @Override
  public List<Set<Integer>> computePartitioning(int pNumPartitions, WeightedGraph wGraph)
      throws InterruptedException {
    if (pNumPartitions <= 0 || wGraph == null) { throw new IllegalArgumentException(
        "Partitioniong must contain at most 1 partition. Graph may not be null."); }
    if (pNumPartitions == 1) { //1-partitioning easy special case (all nodes in one partition)
      //No expensive computations to do
      return wGraph.getGraphAsOnePartition();
    }
    if (pNumPartitions >= wGraph.getNumNodes()) {//Each Node has its own partition
      return wGraph.getNodesSeperatelyPartitioned(pNumPartitions);
    }

    //There is more than one partition, and at least one partition contains more than 1 node

    Stack<WeightedGraph> levels = new Stack<>();//TODO: Maybe not store explicitly ==> Space vs computation time
    Stack<Map<Integer, Integer>> matchings = new Stack<>();

    int levelsToBuild = wGraph.getNumNodes() / pNumPartitions + 1; //TODO: should become kind of logarithmic depth

    levels.push(wGraph);
    logger.log(Level.FINE,
        String.format("Weighted Graph (size: %d) level 0 pushed to Stack", wGraph.getNumNodes()));
    for (int level = 1; level < levelsToBuild; level++) {
      wGraph = levels.peek();
      Map<Integer, Integer> matching = computeMatching(wGraph);
      matchings.push(matching);
      WeightedGraph newGraph = createMatchedGraph(matching, wGraph);
      levels.push(newGraph);
      logger.log(Level.FINE, String.format("Weighted Graph (size: %d) level %d pushed to Stack",
          newGraph.getNumNodes(), level));
    }
    //Initial partitioning computed here
    wGraph = levels.pop();
    logger.log(Level.FINE,
        String.format("Weighted Graph (size: %d) popped", wGraph.getNumNodes()));

    List<Set<Integer>> partitioning = globalPartitioner.computePartitioning(pNumPartitions, wGraph);
    while (!levels.isEmpty()) {
      wGraph = levels.pop();
      logger.log(Level.FINE,
          String.format("Weighted Graph (size: %d) popped", wGraph.getNumNodes()));
      Map<Integer, Integer> matching = matchings.pop();
      retransformPartitioning(partitioning, matching);
      refiner.refinePartitioning(partitioning, wGraph, pNumPartitions); //TODO: a refinement consists of several refinement steps
    }
    return partitioning;
  }

  /**
   * Compute a matching according to the chosen matching scheme
   * @param wGraph Weighted graph to be matched
   * @return a map from the old node to its corresponding new node
   */
  private Map<Integer, Integer> computeMatching(WeightedGraph wGraph) {
    return computeRandomMatching(wGraph);
  }

  /**
   * Computes a random maximal matching
   * @param wGraph  the weighted graph a matching is computed on
   * @return the computed matching  (Matching maps a node to its corresponding new node number!)
   */
  private Map<Integer, Integer> computeRandomMatching(WeightedGraph wGraph) {
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
            logger.log(Level.FINE,
                String.format(
                    "[Multilevel] Node %d and %d matched to supernode %d- matched weight %d",
                    nodeNum, succNum, currentSuperNode, succEdge.getWeight()));
            break;
          }
        }
        if (!nodeMatched) {
          matching.put(nodeNum, currentSuperNode);
          alreadyMatched.set(nodeNum);
          logger.log(Level.FINE, String.format("[Multilevel] Node %d lonely: Supernode %d", nodeNum,
              currentSuperNode));
        }
        currentSuperNode++;
      }
    }
    return matching;
  }

  /**
   * Create a new graph on base of the given matching, i.e. contract edges, compute new node/edge weights
   * @param matching the matching on which basis graph is created (Matching maps a node to its corresponding new node number!)
   * @param wGraph graph on which matching is applied
   * @return the new resulting graph on base of the given matching
   */
  private WeightedGraph createMatchedGraph(Map<Integer, Integer> matching,
      WeightedGraph wGraph) {
    int matchingSize = computeMatchingSize(matching);
    WeightedGraph newGraph = new WeightedGraph(matchingSize);
    int[] nodeWeights = new int[matchingSize];
    Map<Integer, Set<Integer>> newToOldNodes = computeInverseMappping(matching);

    //update the new node's weight, i.e. add the weight of old Node mapped to  new node
    for (Map.Entry<Integer, Integer> oldToNewNode : matching.entrySet()) {
      int newNode = oldToNewNode.getValue();
      int oldNode = oldToNewNode.getKey(); //this node is mapped to new node
      int oldNodeWeight = wGraph.getNode(oldNode).getWeight();
      nodeWeights[newNode] += oldNodeWeight;
    }

    for (Map.Entry<Integer, Integer> oldAndNewNode : matching.entrySet()) {
      int oldNode = oldAndNewNode.getKey();
      //new node will be the start node of the newly created edges in this iteration
      int newNode = oldAndNewNode.getValue();
      int newNodeWeight = nodeWeights[newNode];
      WeightedNode startNode = new WeightedNode(newNode, newNodeWeight);
      Set<Integer> contractedNeigbors = newToOldNodes.get(newNode);//set of nodes which were contracted into the super-node newNode
      //Now check all outgoing edges from the former node. If an edge leads to a contracted neighbor it is discarded
      for (WeightedEdge succEdge : wGraph.getOutgoingEdges(oldNode)) {
        int oldSuccNum = succEdge.getEndNode().getNodeNumber();
        int newSuccNum = matching.get(oldSuccNum);
        if (!contractedNeigbors.contains(oldSuccNum)) {//The edge is added to the graph, hyperedges are unioned by addEdg
          //succ nodes must be mapped to their corresponding supernodes
          int newSuccWeight = nodeWeights[newSuccNum];
          WeightedNode endNode = new WeightedNode(newSuccNum, newSuccWeight);
          WeightedEdge newEdge = new WeightedEdge(startNode, endNode, succEdge.getWeight());
          newGraph.addEdge(newEdge);
        }
      }
    }
    return newGraph;
  }

  /**
   * Computes the number of remaining nodes, after a matching is applied, e.g. v1->3<- 2 ==> in matched graph just counted as one node
   * @param matching the matching which is applied  (Matching maps a node to its corresponding new node number!)
   * @return number of remaining (super-)nodes after applying the matching
   */
  private int computeMatchingSize(Map<Integer, Integer> matching) {
    int count = new HashSet<>(matching.values()).size();
    return count;
  }

  /**
   * Transforms the given matching back into a bigger one, i.e. a super node is mapped
   * @param partitioning partitioning to be transformed back
   * @param matching matching according to which the nodes are transformed back  (Matching maps a node to its corresponding new node number!)
   */
  private void retransformPartitioning(List<Set<Integer>> partitioning,
      Map<Integer, Integer> matching) {
    Map<Integer, Set<Integer>> newToOldNodes = computeInverseMappping(matching); //kind of inverse of matching-Map

    for (int index = 0; index < partitioning.size(); index++) {
      Set<Integer> partition = partitioning.get(index);
      Set<Integer> newPartition = new HashSet<>(2 * partition.size()); //Usually a super-node stands for 2 nodes
      for (Integer node : partition) {
        Set<Integer> contractedNodes = newToOldNodes.get(node);
        newPartition.addAll(contractedNodes);
      }
      partitioning.set(index, newPartition);
    }

  }

  /**
   * Computes a matching's inverse mapping. Since several nodes can be and are mapped onto one super-node, it is mapped onto a set of nodes
   * @param matching the matching which maps a node to its belonging super-node
   * @return the inverse map super-node --> contracted nodes
   */
  private Map<Integer, Set<Integer>> computeInverseMappping(Map<Integer, Integer> matching) {
    Map<Integer, Set<Integer>> inverseMap = new HashMap<>(computeMatchingSize(matching));

    for (Map.Entry<Integer, Integer> oldToNewNode : matching.entrySet()) {
      int newNode = oldToNewNode.getValue();
      int oldNode = oldToNewNode.getKey(); //this node is mapped to new node
      if (!inverseMap.containsKey(newNode)) {
        inverseMap.put(newNode, new HashSet<Integer>(2));
      }
      inverseMap.get(newNode).add(oldNode); //old Node is mapped to newNode, so is in the set of mapped nodes
    }
    return inverseMap;
  }



}
