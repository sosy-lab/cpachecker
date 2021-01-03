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
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.specification.Specification;

// @Options(prefix = "cpa.loopsummary")
public abstract class AbstractLoopSummaryCPA extends AbstractSingleWrapperCPA {

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  private final LoopSummaryCPAStatistics stats;

  protected AbstractLoopSummaryCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this, AbstractLoopSummaryCPA.class);

    if (!(pCpa instanceof ConfigurableProgramAnalysisWithLoopSummary)) {
      throw new InvalidConfigurationException("BAM needs CPAs that are capable for Loop Summary");
    }

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    stats = new LoopSummaryCPAStatistics(pConfig, pLogger, this);
  }

  @Override
  protected ConfigurableProgramAnalysisWithLoopSummary getWrappedCpa() {
    // override for visibility
    return (ConfigurableProgramAnalysisWithLoopSummary) super.getWrappedCpa();
  }

  LogManager getLogger() {
    return logger;
  }

  LoopSummaryCPAStatistics getStatistics() {
    return stats;
  }
}
