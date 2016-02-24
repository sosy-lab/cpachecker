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

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.BalancedGraphPartitioner;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;

@Options(prefix = "pcc.partitioning.kwayfm")
public class FiducciaMattheysesKWayBalancedGraphPartitioner implements BalancedGraphPartitioner {

  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  private final LogManager logger;

  @Option(secure = true, description = "Balance criterion for pairwise optimization of partitions")
  private double balancePrecision = 1.5d;

  private final BalancedGraphPartitioner partitioner;
  @Option(secure = true, description = "Heuristic for computing an initial partitioning of arg")
  private InitPartitioningHeuristics initialPartitioningStrategy =
      InitPartitioningHeuristics.BEST_FIRST;

  public enum InitPartitioningHeuristics {
    RANDOM,
    BEST_FIRST
  }


  public FiducciaMattheysesKWayBalancedGraphPartitioner(Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    pConfig.inject(this);
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;

    switch (initialPartitioningStrategy) {
      case BEST_FIRST:
        partitioner =
            new BestFirstWeightedBalancedGraphPartitioner(pConfig, pLogger, pShutdownNotifier);
        break;
      default: // RANDOM
        partitioner = new RandomBalancedGraphPartitioner();
    }

  }

  @Override
  public List<Set<Integer>> computePartitioning(int pNumPartitions,
      PartialReachedSetDirectedGraph pGraph) throws InterruptedException {
    //TODO: Save time, if just one partition is used ==> no improvement steps and so on...
    List<Set<Integer>> partition = partitioner.computePartitioning(pNumPartitions, pGraph);
    int maxLoad = computeNodesPerPartition(pNumPartitions, pGraph.getNumNodes());
    int step = 1;
    int maxNumSteps = 50;
    int oldGain = 0;
    int totalGain = 0;
    int timesWithoutImprovement = 0;
    while (step <= maxNumSteps && timesWithoutImprovement < 5) {
      int newGain = refinePartitioning(partition, pGraph, maxLoad);
      totalGain += newGain;
      if (oldGain == newGain) {
        timesWithoutImprovement++;
      }
      oldGain = newGain;
      step++;
    }
    logger.log(Level.FINE, String.format("[KWayFM] refinement gain %d", totalGain));
    return partition;
  }

  /**
   * Refine an initially given partitioning on a given graph according to a FiducciaMattheyses local improvement heuristic
   * @param initialPartitioning initial partitioning
   * @param pGraph given graph
   * @param maxLoad the maximal load per partition
   * @return the total gain according to the improvements
   */
  public int refinePartitioning(List<Set<Integer>> initialPartitioning,
      PartialReachedSetDirectedGraph pGraph, int maxLoad) {
    WeightedGraph wGraph = new WeightedGraph(pGraph);
    return refinePartitioning(initialPartitioning, wGraph, maxLoad);
  }

  /**
   * Refine an initially given partitioning on a given weighted graph according to a FiducciaMattheyses local improvement heuristic
   * @param initialPartitioning initial partitioning; This partitioning is changed!
   * @param wGraph a given weighted graph
   * @param maxLoad the maximal load per partition
   * @return the total gain according to the improvements
   */
  public int refinePartitioning(List<Set<Integer>> initialPartitioning,
      WeightedGraph wGraph, int maxLoad) {

    FiducciaMattheysesWeightedKWayAlgorithm fm = new FiducciaMattheysesWeightedKWayAlgorithm(
        initialPartitioning, balancePrecision, wGraph, maxLoad);
    return fm.refinePartitioning();
  }

  /**
   * Compute number of nodes which should be used per partition
   * @param numPartitions number of partitions that should be computed
   * @param numNodes number of nodes totally available
   * @return the number of nodes which should be at most in a partition
   */
  private int computeNodesPerPartition(int numPartitions, int numNodes) {
    return numNodes / numPartitions + 1;
  }

}
