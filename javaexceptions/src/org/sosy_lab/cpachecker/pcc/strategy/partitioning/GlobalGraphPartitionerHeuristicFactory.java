// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.WeightedBalancedGraphPartitioner;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.BestFirstEvaluationFunctionFactory.BestFirstEvaluationFunctions;

/**
 * A factory class, to generate global partitioning strategies. These strategies are pure global
 * strategies. Not like FM or Multilevel, which both implement the WeightedBalancedGraphPartitioner
 * interface as well.
 */
public class GlobalGraphPartitionerHeuristicFactory {

  private GlobalGraphPartitionerHeuristicFactory() {}

  public enum GlobalPartitioningHeuristics {
    RANDOM,
    DFS,
    BFS,
    BEST_IMPROVEMENT_FIRST
  }

  public static WeightedBalancedGraphPartitioner createPartitioner(
      final Configuration pConfig,
      final LogManager pLogger,
      final GlobalPartitioningHeuristics pHeuristic)
      throws InvalidConfigurationException {
    switch (pHeuristic) {
      case RANDOM:
        return new RandomBalancedWeightedGraphPartitioner();
      case DFS:
        return new BestFirstWeightedBalancedGraphPartitioner(
            pConfig, pLogger, BestFirstEvaluationFunctions.DEPTH_FIRST);
      case BFS:
        return new BestFirstWeightedBalancedGraphPartitioner(
            pConfig, pLogger, BestFirstEvaluationFunctions.BREADTH_FIRST);
      default: // BEST_IMPROVEMENT_FIRST
        return new BestFirstWeightedBalancedGraphPartitioner(pConfig, pLogger);
    }
  }
}
