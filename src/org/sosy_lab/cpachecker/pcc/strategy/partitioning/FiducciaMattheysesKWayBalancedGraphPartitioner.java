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
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartitioningRefiner;
import org.sosy_lab.cpachecker.core.interfaces.pcc.WeightedBalancedGraphPartitioner;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;

@Options(prefix = "pcc.partitioning.kwayfm")
public class FiducciaMattheysesKWayBalancedGraphPartitioner implements WeightedBalancedGraphPartitioner, PartitioningRefiner {

  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  private final LogManager logger;

  @Option(secure = true, description = "Balance criterion for pairwise optimization of partitions")
  private double balancePrecision = 1.5d;

  private final WeightedBalancedGraphPartitioner partitioner;
  @Option(secure = true, description = "Heuristic for computing an initial partitioning of arg")
  private InitPartitioningHeuristics initialPartitioningStrategy =
      InitPartitioningHeuristics.BEST_FIRST;

  public static enum InitPartitioningHeuristics {
    RANDOM,
    BEST_FIRST
  }

  @Option(
      secure = true,
      description = "Local optimization criterion. Minimize the NodeCut of partitions or an approximation of the frameworkspecific overhead")
  private OptimizationCriteria optimizationCriterion = OptimizationCriteria.NODECUT;

  public static enum OptimizationCriteria {
    //TODO: Framework-specific option, use strategy pattern here
    EDGECUT,
    NODECUT
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
        partitioner = new RandomBalancedWeightedGraphPartitioner();
    }

  }

  @Override
  public List<Set<Integer>> computePartitioning(int pNumPartitions,
      PartialReachedSetDirectedGraph pGraph) throws InterruptedException {
    return computePartitioning(pNumPartitions,new WeightedGraph(pGraph));

  }

  @Override
  public List<Set<Integer>> computePartitioning(int pNumPartitions, WeightedGraph wGraph)
      throws InterruptedException {
    if (pNumPartitions <= 0 || wGraph == null) { throw new IllegalArgumentException(
        "Partitioniong must contain at most 1 partition. Graph may not be null."); }
    if (pNumPartitions == 1) { //1-partitioning easy special case (Each node in the same partition)
      return wGraph.getGraphAsOnePartition();
    }
    if (pNumPartitions >= wGraph.getNumNodes()) {//Each Node has its own partition
      return wGraph.getNodesSeperatelyPartitioned(pNumPartitions);
    }

    //There is more than one partition, and at least one partition contains more than 1 node

    List<Set<Integer>> partition = partitioner.computePartitioning(pNumPartitions, wGraph);
    refinePartitioning(partition, wGraph, pNumPartitions);
    return partition;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningRefiner#refinePartitioning(java.util.List, org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph, int)
   */
  @Override
  public int refinePartitioning(List<Set<Integer>> partitioning,
      PartialReachedSetDirectedGraph pGraph, int numPartitions) {
    WeightedGraph wGraph = new WeightedGraph(pGraph);
    return refinePartitioning(partitioning, wGraph, numPartitions);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.pcc.strategy.partitioning.PartitioningRefiner#refinePartitioning(java.util.List, org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph, int)
   */
  @Override
  public int refinePartitioning(List<Set<Integer>> partitioning,
      WeightedGraph wGraph, int numPartitions) {
    int maxLoad = wGraph.computePartitionLoad(numPartitions);
    FiducciaMattheysesWeightedKWayAlgorithm fm = new FiducciaMattheysesWeightedKWayAlgorithm(
        partitioning, balancePrecision, wGraph, maxLoad,optimizationCriterion);
    int step = 1;
    int maxNumSteps = 50;
    int oldGain = 0;
    int totalGain = 0;
    int timesWithoutImprovement = 0;
    while (step <= maxNumSteps && timesWithoutImprovement < 5) {
      int newGain = fm.refinePartitioning();
      totalGain += newGain;
      if (oldGain == newGain) {
        timesWithoutImprovement++;
      }
      oldGain = newGain;
      step++;
    }
    logger.log(Level.FINE, String.format("[KWayFM] refinement gain %d after % d refinement steps", totalGain,step));
    return totalGain;
  }



}
