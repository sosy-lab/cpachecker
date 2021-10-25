// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;

public abstract class AbstractLoopSummaryCPA extends AbstractSingleWrapperCPA {

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  private final LoopSummaryCPAStatistics stats;

  protected AbstractLoopSummaryCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super(pCpa);
    // pConfig.inject(this, AbstractLoopSummaryCPA.class);

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    stats = new LoopSummaryCPAStatistics(pConfig, pLogger, this);
  }

  @Override
  protected ConfigurableProgramAnalysis getWrappedCpa() {
    // override for visibility
    // TODO There is an error when casting to ConfigurableProgramAnalysisWithLoopSummary like is
    // done in AbstractBAMCPA
    return super.getWrappedCpa();
  }

  public LogManager getLogger() {
    return logger;
  }

  LoopSummaryCPAStatistics getStatistics() {
    return stats;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    super.collectStatistics(pStatsCollection);
  }
}
