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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.MatchingGenerator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartitioningRefiner;
import org.sosy_lab.cpachecker.core.interfaces.pcc.WeightedBalancedGraphPartitioner;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedEdge;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedNode;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.GlobalGraphPartitionerHeuristicFactory.GlobalPartitioningHeuristics;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.MatchingGeneratorFactory.MatchingGenerators;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningRefinerFactory.RefinementHeuristics;
/**
 * Multilevel graph partitioning algorithm; Behavior: Coarsen down graph, compute initial partitioning, uncoarsen
 * graph, remap partitioning, refine partitioning, ... Until partitioning on initially given graph is computed.
 */
@Options(prefix = "pcc.partitioning.multilevel")
public class MultilevelBalancedGraphPartitioner implements WeightedBalancedGraphPartitioner {

  private final LogManager logger;

  @Option(
      secure = true,
      description = "Partitioning method applied in multilevel heuristic to compute initial partitioning.")
  private GlobalPartitioningHeuristics globalHeuristic =
      GlobalPartitioningHeuristics.BEST_IMPROVEMENT_FIRST;

  @Option(
      secure = true,
      description = "Refinement method applied in multilevel heuristic's uncoarsening phase.")
  private RefinementHeuristics refinementHeuristic = RefinementHeuristics.FM_NODECUT;

  @Option(
      secure = true,
      description = "Matching method applied to coarsen graph down in multilevel heuristic.")
  private MatchingGenerators matchingGenerator=MatchingGenerators.HEAVY_EDGE;

  private final PartitioningRefiner refiner;
  private final WeightedBalancedGraphPartitioner globalPartitioner;
  private final MatchingGenerator matcher;

  public MultilevelBalancedGraphPartitioner(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    globalPartitioner = GlobalGraphPartitionerHeuristicFactory.createPartitioner(pConfig, pLogger,
        globalHeuristic);

    refiner = PartitioningRefinerFactory.createRefiner(pConfig, pLogger, refinementHeuristic);
    matcher=MatchingGeneratorFactory.createMatchingGenerator(pLogger, matchingGenerator);


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

    Stack<WeightedGraph> levels = new Stack<>();
    Stack<Map<Integer, Integer>> matchings = new Stack<>();

    int maxLoad = wGraph.getNumNodes() / pNumPartitions + 1;
    //The graph size until graph should be contracted
    int minGraphSize = (int) Math.min(((double) maxLoad) / 15 + 1, 15) * pNumPartitions;
    logger.log(Level.FINE,
        String.format("[Multilevel] Coarsen graph down to at least %d nodes",
            minGraphSize));

    int currentLevel = 0;
    levels.push(wGraph);
    logger.log(Level.FINE,
        String.format("[Multilevel] Weighted Graph (size: %d) level %d pushed to Stack",
            wGraph.getNumNodes(), currentLevel++));

    //Coarsen the graph
    while (wGraph.getNumNodes() > minGraphSize) {
      wGraph = levels.peek();
      Map<Integer, Integer> matching = matcher.computeMatching(wGraph);
      matchings.push(matching);
      wGraph = createMatchedGraph(matching, wGraph);
      levels.push(wGraph);
      logger.log(Level.FINE,
          String.format("[Multilevel] Weighted Graph (size: %d) level %d pushed to Stack",
              wGraph.getNumNodes(), currentLevel++));
    }


    //Initial partitioning computed here
    wGraph = levels.pop();
    logger.log(Level.FINE,
        String.format("[Multilevel] Weighted Graph (size: %d) popped", wGraph.getNumNodes()));

    List<Set<Integer>> partitioning = globalPartitioner.computePartitioning(pNumPartitions, wGraph);
    refiner.refinePartitioning(partitioning, wGraph, pNumPartitions);

    //Uncoarsening phase, i.e. remap partitioning acc. to matching and refine the partitioning
    while (!levels.isEmpty()) {
      wGraph = levels.pop();
      logger.log(Level.FINE,
          String.format("[Multilevel] Weighted Graph (size: %d) popped", wGraph.getNumNodes()));
      Map<Integer, Integer> matching = matchings.pop();
      retransformPartitioning(partitioning, matching);
      refiner.refinePartitioning(partitioning, wGraph, pNumPartitions);
    }
    removeEmptyPartitions(partitioning);
    return partitioning;
  }

  /**
   * Method to remove all empty partitions from the partitioning. Empty partitions may slow down proof checking phase.
   * @param partitions the partitioning to be cleaned up.
   */
  private void removeEmptyPartitions(List<Set<Integer>> partitions) {
    for (Iterator<Set<Integer>> iter = partitions.listIterator(); iter.hasNext();) {
      Set<Integer> partition = iter.next();
      if (partition != null && partition.isEmpty()) {
        iter.remove();
      }
    }
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
      newGraph.insertNode(new WeightedNode(newNode, newNodeWeight));
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
