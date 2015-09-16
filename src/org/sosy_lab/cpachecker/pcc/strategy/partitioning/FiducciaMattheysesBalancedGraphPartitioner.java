/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
 *  See the License for the specifi  c language governing permissions and
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

@Options(prefix = "pcc.partitioning.fm")
public class FiducciaMattheysesBalancedGraphPartitioner implements BalancedGraphPartitioner {

  private final ShutdownNotifier shutdownNotifier;

  private final LogManager logger;

  @Option(secure=true, description = "Heuristic for computing an initial partitioning of proof")
  private InitPartitioningHeuristics initialPartitioningStrategy = InitPartitioningHeuristics.RANDOM;

  public enum InitPartitioningHeuristics {
    RANDOM
  }

  @Option(secure=true, description = "Balance criterion for pairwise optimization of partitions")
  private double balanceCriterion = 1.5d;

  private final BalancedGraphPartitioner partitioner;

  public FiducciaMattheysesBalancedGraphPartitioner(Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    pConfig.inject(this);
  shutdownNotifier = pShutdownNotifier;
    logger = pLogger;

    switch (initialPartitioningStrategy) {
      // TODO support better strategies for initial partitioning
      default: // RANDOM
        partitioner = new RandomBalancedGraphPartitioner();
    }

  }

  @Override
  public List<Set<Integer>> computePartitioning(int pNumPartitions, PartialReachedSetDirectedGraph pGraph)
      throws InterruptedException {

    // TODO insert assertions

    /* Create initial partition which is going to be optimized later on */
    List<Set<Integer>> partition = partitioner.computePartitioning(pNumPartitions, pGraph);

    /* Optimize partitions pairwisely with FM algorithm */
    // TODO find better strategy or/and make this parallel
    long cutSizeAfter = 0;
    for(Set<Integer> v1 : partition) {
      for(Set<Integer> v2 : partition) {
        if(v1 == v2) {
          break;
        }
        shutdownNotifier.shutdownIfNecessary();
        FiducciaMattheysesAlgorithm fm = new FiducciaMattheysesAlgorithm(balanceCriterion, v1, v2, pGraph);
        long gain;
        do {
          shutdownNotifier.shutdownIfNecessary();
          gain = fm.improvePartitioning();
        } while(gain > 0);
        cutSizeAfter += pGraph.getNumEdgesBetween(v1, v2);
      }
    }
    logger.log(Level.FINE, String.format("[FM] Computed partitioning of cut size %d", cutSizeAfter));
    return partition;
  }

}
