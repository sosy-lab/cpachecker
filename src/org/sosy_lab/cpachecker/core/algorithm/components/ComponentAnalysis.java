// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components;

import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.components.cut.BlockOperatorCutter;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockTree;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ComponentAnalysis implements Algorithm {

  private final Algorithm parentAlgorithm;
  private final Configuration configuration;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownNotifier notifier;

  public ComponentAnalysis(Algorithm pAlgorithm, Configuration pConfig, LogManager pLogger, CFA pCfa, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    parentAlgorithm = pAlgorithm;
    configuration = pConfig;
    logger = pLogger;
    cfa = pCfa;
    notifier = pShutdownNotifier;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet)
      throws CPAException, InterruptedException {

    try {
      BlockTree tree = new BlockOperatorCutter(configuration).cut(cfa);
      Runner.analyzeTree(tree, parentAlgorithm, logger);
    } catch (InvalidConfigurationException pE) {
      logger.log(Level.SEVERE, pE);
    }


    return parentAlgorithm.run(reachedSet);
  }
}
