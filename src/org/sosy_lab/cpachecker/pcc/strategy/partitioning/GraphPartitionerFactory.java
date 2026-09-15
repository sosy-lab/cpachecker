// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.BalancedGraphPartitioner;

public class GraphPartitionerFactory {

  private GraphPartitionerFactory() {}

  public enum PartitioningHeuristics {
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
      final Configuration pConfig)
      throws InvalidConfigurationException {
    return switch (pHeuristic) {
      case DFS -> new ExplorationOrderBalancedGraphPartitioner(true, pShutdownNotifier);
      case BFS -> new ExplorationOrderBalancedGraphPartitioner(false, pShutdownNotifier);
      case OPTIMAL -> new ExponentialOptimalBalancedGraphPartitioner(pShutdownNotifier);
      case BEST_FIRST -> new BestFirstWeightedBalancedGraphPartitioner(pConfig, pLogger);
      case FM ->
          new FiducciaMattheysesBalancedGraphPartitioner(pConfig, pLogger, pShutdownNotifier);
      case FM_K_WAY -> new FiducciaMattheysesKWayBalancedGraphPartitioner(pConfig, pLogger);
      case MULTILEVEL -> new MultilevelBalancedGraphPartitioner(pConfig, pLogger);
      default -> // RANDOM
          new RandomBalancedGraphPartitioner();
    };
  }
}
