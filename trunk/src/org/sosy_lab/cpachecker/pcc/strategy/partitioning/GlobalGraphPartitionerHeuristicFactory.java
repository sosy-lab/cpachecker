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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.pcc.WeightedBalancedGraphPartitioner;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.BestFirstEvaluationFunctionFactory.BestFirstEvaluationFunctions;

/**
 * A factory class, to generate global partitioning strategies. These strategies are pure global strategies.
 * Not like FM or Multilevel, which both implement the WeightedBalancedGraphPartitioner interface as well.
 */
public class GlobalGraphPartitionerHeuristicFactory {

  private GlobalGraphPartitionerHeuristicFactory() {}

  public static enum GlobalPartitioningHeuristics {
    RANDOM,
    DFS,
    BFS,
    BEST_IMPROVEMENT_FIRST
  }

  public static WeightedBalancedGraphPartitioner createPartitioner(final Configuration pConfig,
      final LogManager pLogger,
      final GlobalPartitioningHeuristics pHeuristic)
          throws InvalidConfigurationException {
    switch (pHeuristic) {
      case RANDOM:
        return new RandomBalancedWeightedGraphPartitioner();
      case DFS:
       return new BestFirstWeightedBalancedGraphPartitioner(pConfig, pLogger,
            BestFirstEvaluationFunctions.DEPTH_FIRST);
      case BFS:
        return new BestFirstWeightedBalancedGraphPartitioner(pConfig, pLogger,
            BestFirstEvaluationFunctions.BREADTH_FIRST);
      default: //BEST_IMPROVEMENT_FIRST
        return new BestFirstWeightedBalancedGraphPartitioner(pConfig, pLogger);
    }
  }


}
