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

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.BestFirstEvaluationFunction;
import org.sosy_lab.cpachecker.core.interfaces.pcc.WeightedBalancedGraphPartitioner;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedNode;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.BestFirstEvaluationFunctionFactory.BestFirstEvaluationFunctions;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;

/**
 * Compute a greedy graph partitioning in best-first-manner
 * Exploration order according to chosen evaluation function
 * Mainly based on the GGGP algorithm described in
 * http://www.cc.gatech.edu/~bader/COURSES/GATECH/CSE6140-Fall2007/papers/KK95a.pdf, with a modified best_improvment
 *  evaluation function
 */
@Options(prefix = "pcc.partitioning.bestfirst")
public class BestFirstWeightedBalancedGraphPartitioner implements WeightedBalancedGraphPartitioner {

  private final LogManager logger;
  private final BestFirstEvaluationFunction evaluationFunction;

  @Option(
      secure = true,
      description = "Evaluation function to determine exploration order of best-first-search")
  private BestFirstEvaluationFunctions chosenFunction =
      BestFirstEvaluationFunctions.BEST_IMPROVEMENT_FIRST;

  @Option(
      secure = true,
      description = "[Best-first] Balance criterion for pairwise optimization of partitions")
  private double balancePrecision = 1.0d;

  public BestFirstWeightedBalancedGraphPartitioner(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    evaluationFunction =
        BestFirstEvaluationFunctionFactory.createEvaluationFunction(chosenFunction);
  }

  /**
   * Initialize the BestFirstWeightedBalancedGraphPartitioner with a function given via parameter.
   * @param pConfig the configuration object
   * @param pLogger the logger object
   * @param function the chosen function, on which the final evaluation function is created
   * @throws InvalidConfigurationException Configuration
   */
  public BestFirstWeightedBalancedGraphPartitioner(Configuration pConfig, LogManager pLogger,
      BestFirstEvaluationFunctions function)
          throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    chosenFunction = function;
    evaluationFunction =
        BestFirstEvaluationFunctionFactory.createEvaluationFunction(function);
  }

  /**
   * Store the node together with its exploration priority
   * Use this type within PriorityQueue
   */
  private static class NodePriority implements Comparable<NodePriority> {

    private final WeightedNode node;
    private final int priority;


    public WeightedNode getNode() {
      return node;
    }


    public int getPriority() {
      return priority;
    }

    public NodePriority(WeightedNode pNode, int pPriority) {
      super();
      node = pNode;
      priority = pPriority;
    }


    @Override
    public int compareTo(NodePriority compNode) {
      if (compNode == null) {
        return -1;
      }
      return ComparisonChain.start()
          .compare(this.getPriority(), compNode.getPriority())
          // same priority ==> use node with higher number
          .compare(compNode.getNode().getNodeNumber(), this.getNode().getNodeNumber())
          .result();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) { return false; }
      if (obj == this) { return true; }

      if (obj.getClass() == this.getClass()) {
        NodePriority compNode = (NodePriority) obj;
        return compareTo(compNode) == 0;
      }
      return false;
    }


    @Override
    public int hashCode() {
      return this.getPriority() + this.getNode().getNodeNumber();
    }

    @Override
    public String toString() {
      StringBuilder s = new StringBuilder(node.toString());
      s.append("[Prio:").append(priority).append("]");
      return s.toString();
    }
  }


  @Override
  public List<Set<Integer>> computePartitioning(int pNumPartitions,
      PartialReachedSetDirectedGraph pGraph) throws InterruptedException {
    if (pNumPartitions <= 0 || pGraph == null) { throw new IllegalArgumentException(
        "Partitioniong must contain at least 1 partition. Graph may not be null."); }
    WeightedGraph wGraph = new WeightedGraph(pGraph); //Transform into weighted graph
    return computePartitioning(pNumPartitions, wGraph);
  }

  @Override
  public List<Set<Integer>> computePartitioning(int pNumPartitions,
      WeightedGraph wGraph) throws InterruptedException {
    if (pNumPartitions <= 0 || wGraph == null) { throw new IllegalArgumentException(
        "Partitioniong must contain at least 1 partition. Graph may not be null."); }

    logger.log(Level.FINE,
        String.format(
            "[best-first] Compute %d-partitioning with %.2f balance precision. %s evaluation function. Graph size %d",
            pNumPartitions, balancePrecision, chosenFunction, wGraph.getNumNodes()));


    if (pNumPartitions == 1) { //1-partitioning easy special case (Each node in the same partition)
      return wGraph.getGraphAsOnePartition();
    }
    if (pNumPartitions >= wGraph.getNumNodes()) {//Each Node has its own partition
      return wGraph.getNodesSeperatelyPartitioned(pNumPartitions);
    }

    //There is more than one partition, and at least one partition contains more than 1 node

    BitSet inPartition = new BitSet(wGraph.getNumNodes()); //Indicates whether node already in a partition
    PriorityQueue<NodePriority> waitlist = new PriorityQueue<>(); //Nodes which could be expanded
    int partitionSize = wGraph.computePartitionLoad(pNumPartitions);
    List<Set<Integer>> result = new ArrayList<>(pNumPartitions);
    List<Integer> partitionWeights = new ArrayList<>(pNumPartitions); //take nodes' weight into account
    for (int i = 0; i < pNumPartitions; i++) {
      result.add(Sets.<Integer> newHashSetWithExpectedSize(partitionSize));
      partitionWeights.add(0);
    }


    NodePriority nextChosen;
    WeightedNode nextNode;
    int priority;
    int partition = 0;
    int nextUnpartitionedNode = 0;

    while (nextUnpartitionedNode < wGraph.getNumNodes() && nextUnpartitionedNode >= 0) {
      //Need this loop, since it's possible, that graph is not strongly connected
      waitlist.add(new NodePriority(wGraph.getNode(nextUnpartitionedNode), nextUnpartitionedNode));
      while (!waitlist.isEmpty()) {
        nextChosen = waitlist.poll();
        nextNode = nextChosen.getNode(); //node with highest priority
        priority = nextChosen.getPriority();
        if (!inPartition.get(nextNode.getNodeNumber())) { //duplicate nodes in waitlist possible
          int nodeWeight = nextNode.getWeight();
          if (partitionWeights.get(partition) + nodeWeight > balancePrecision * partitionSize) { // next partition
            partition++;
            if (partition >= pNumPartitions) { //Due to weighted nodes, pNumPartitions maybe to small
              result.add(Sets.<Integer> newHashSetWithExpectedSize(partitionSize));
              partitionWeights.add(0);
            }
          }
          result.get(partition).add(nextNode.getNodeNumber());
          partitionWeights.set(partition, partitionWeights.get(partition) + nodeWeight); //nodeWeight added to the partition weight
          inPartition.set(nextNode.getNodeNumber());

          for (WeightedNode succ : wGraph.getSuccessors(nextNode)) {
            int succPriority =
                evaluationFunction.computePriority(result.get(partition), priority, succ, wGraph);
            waitlist.offer(new NodePriority(succ, succPriority));
          }
        }

      }
      nextUnpartitionedNode = inPartition.nextClearBit(nextUnpartitionedNode); //The next node without partition so far
    }

    return result;
  }
}
