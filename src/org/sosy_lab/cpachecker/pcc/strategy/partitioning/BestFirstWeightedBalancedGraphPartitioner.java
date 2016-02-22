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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.BalancedGraphPartitioner;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedNode;

import com.google.common.collect.Sets;

/**
 * Compute a greedy graph partitioning in best-first-manner
 * Ordering according to chosen evaluation function
 */
@Options(prefix = "pcc.partitioning.bestfirst")
public class BestFirstWeightedBalancedGraphPartitioner implements BalancedGraphPartitioner {

  private final ShutdownNotifier shutdownNotifier;

  @SuppressWarnings("unused")
  private final LogManager logger;
  @Option(
      secure = true,
      description = "Evaluation function to determine exploration order of best-first-search")
  private EvaluationFunctions evaluationFunction = EvaluationFunctions.BEST_IMPROVEMENT_FIRST;

  public enum EvaluationFunctions {
    BEST_IMPROVEMENT_FIRST,
    BREADTH_FIRST,
    DEPTH_FIRST

  }

  private final static double balancePrecision = 1.0d;

  public BestFirstWeightedBalancedGraphPartitioner(Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    pConfig.inject(this);
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;

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
      if (compNode == null) { return -1; }
      if (this.getPriority() == compNode.getPriority()) {
        return this.getPriority() - compNode.getPriority();
      } else {//same priority==> use node with higher number
        int n1 = this.getNode().getNodeNumber();
        int n2 = compNode.getNode().getNodeNumber();
        return n2 - n1;
      }
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
  }


  @Override
  public List<Set<Integer>> computePartitioning(int pNumPartitions,
      PartialReachedSetDirectedGraph pGraph) throws InterruptedException {

    WeightedGraph wGraph = new WeightedGraph(pGraph); //Transform into weighted graph

    BitSet inPartition = new BitSet(wGraph.getNumNodes()); //Indicates whether node already in a partition
    PriorityQueue<NodePriority> waitlist = new PriorityQueue<>(); //Nodes which could be expanded
    int partitionSize = wGraph.getNumNodes() / pNumPartitions + 1;
    List<Set<Integer>> result = new ArrayList<>(pNumPartitions);
    List<Integer> partitionWeights = new ArrayList<>(pNumPartitions); //take nodes' weight into account
    for (int i = 0; i < pNumPartitions; i++) {
      result.add(Sets.<Integer> newHashSetWithExpectedSize(partitionSize));
      partitionWeights.add(0);
    }

    waitlist.add(new NodePriority(wGraph.getNode(0), 0));


    NodePriority nextChosen;
    WeightedNode nextNode;
    int priority;
    int partition = 0;
    while (!waitlist.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
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
              computePriority(result.get(partition), priority, succ, evaluationFunction, wGraph);
          waitlist.offer(new NodePriority(succ, succPriority));
        }
      }


    }
    return result;
  }

  /**
   *
   * Compute priority for node on waitlist to be expanded next, depending on actual situation and chosen evaluation function
   * @param partition The partition predecessor was added to
   * @param priority Priority of predecessor
   * @param node Node which is considered
   * @param evaluationFunction Chosen evaluation function
   * @param wGraph The graph algorithm is working on
   * @return Priority to expand successor as next node
   */
  private int computePriority(Set<Integer> partition, int priority, WeightedNode node,
      EvaluationFunctions evaluationFunction, WeightedGraph wGraph) {
    if (evaluationFunction == EvaluationFunctions.BREADTH_FIRST) {
      return priority + 1; //expand next level nodes, when this level complete
    } else if (evaluationFunction == EvaluationFunctions.DEPTH_FIRST) {
      return priority - 1; //expand next level nodes, as next step (assumption: PriorityQueue preserves order of inserting)
    } else if (evaluationFunction == EvaluationFunctions.BEST_IMPROVEMENT_FIRST) {
      /*
      * if node not in partition it has cost of its weight for the actual partition ==> nodeweight is gain
      * all of its successors which are not in the partition right now ==>  cost
      */
      Set<Integer> successors = wGraph.getIntSuccessors(node); //successors of this node
      successors.removeAll(partition); // successors, that are not in given partition
      int gain = node.getWeight();
      return WeightedGraph.computeWeight(successors, wGraph) - gain; //chance +/- since least priority is chosen first
    } else {
      return 0;
    }
  }

}
