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
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.BalancedGraphPartitioner;


public class GraphPartitionerFactory {

  private GraphPartitionerFactory() { }

  public static enum PartitioningHeuristics {
    RANDOM,
    DFS,
    BFS,
    OPTIMAL,
    BEST_FIRST,
    FM,
    FM_K_WAY,
    MULTILEVEL
  }

  public static BalancedGraphPartitioner createPartitioner(
      final LogManager pLogger,
      final PartitioningHeuristics pHeuristic,
      final ShutdownNotifier pShutdownNotifier,
      final Configuration pConfig) throws InvalidConfigurationException {
    switch (pHeuristic) {
    case DFS:
      return new ExplorationOrderBalancedGraphPartitioner(true, pShutdownNotifier);
    case BFS:
      return new ExplorationOrderBalancedGraphPartitioner(false, pShutdownNotifier);
    case OPTIMAL:
      return new ExponentialOptimalBalancedGraphPartitioner(pShutdownNotifier);
    case BEST_FIRST:
      return new BestFirstWeightedBalancedGraphPartitioner(pConfig, pLogger);
    case FM:
      return new FiducciaMattheysesBalancedGraphPartitioner(pConfig, pLogger, pShutdownNotifier);
    case FM_K_WAY:
      return new FiducciaMattheysesKWayBalancedGraphPartitioner(pConfig, pLogger);
    case MULTILEVEL:
      return new MultilevelBalancedGraphPartitioner(pConfig, pLogger);
    default: // RANDOM
      return new RandomBalancedGraphPartitioner();
    }
  }

}
