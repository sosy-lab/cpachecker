// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.blocks.builder;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;

/** <code>PartitioningHeuristic</code> that creates blocks for each loop- and function-body. */
public class FunctionAndLoopPartitioning extends CompositePartitioning {

  public FunctionAndLoopPartitioning(LogManager pLogger, CFA pCfa, Configuration pConfig)
      throws InvalidConfigurationException {
    super(
        pLogger,
        pCfa,
        pConfig,
        new FunctionPartitioning(pLogger, pCfa, pConfig),
        new LoopPartitioning(pLogger, pCfa, pConfig));
  }
}
