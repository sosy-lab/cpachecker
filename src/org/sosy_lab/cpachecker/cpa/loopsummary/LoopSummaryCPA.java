// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.CPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.specification.Specification;

public class LoopSummaryCPA extends AbstractLoopSummaryCPA {

  private final LoopSummaryTransferRelation transfer;

  private LoopSummaryCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(pCpa, config, pLogger, pShutdownNotifier, pSpecification, pCfa);
    config.inject(this);


    AlgorithmFactory factory = new CPAAlgorithmFactory(this, logger, config, pShutdownNotifier);

    transfer = new LoopSummaryTransferRelation(this, pShutdownNotifier, factory);
  }

  @Override
  public LoopSummaryTransferRelation getTransferRelation() {
    return transfer;
  }

}
