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
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartitioningRefiner;
import org.sosy_lab.cpachecker.pcc.strategy.partitioning.FiducciaMattheysesOptimzerFactory.OptimizationCriteria;

public class PartitioningRefinerFactory {

  private PartitioningRefinerFactory() {}

  public enum RefinementHeuristics {
    FM_NODECUT,
    FM_EDGECUT
  }

  public static PartitioningRefiner createRefiner(
      final Configuration pConfig, final LogManager pLogger, final RefinementHeuristics pHeuristic)
      throws InvalidConfigurationException {
    switch (pHeuristic) {
      case FM_EDGECUT:
        return new FiducciaMattheysesKWayBalancedGraphPartitioner(
            pConfig, pLogger, OptimizationCriteria.EDGECUT);
      default: // FM_K_WAY (NODE_CUT)
        return new FiducciaMattheysesKWayBalancedGraphPartitioner(
            pConfig, pLogger, OptimizationCriteria.NODECUT);
    }
  }
}
