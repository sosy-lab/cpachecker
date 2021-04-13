// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class LoopSummaryBasedRefiner implements Refiner, StatisticsProvider {

  @SuppressWarnings("unused")
  private final LogManager logger;

  private final Refiner firstRefiner;
  private final Refiner secondRefiner;

  public LoopSummaryBasedRefiner(
      Refiner pFirstRefiner, Refiner pSecondRefiner, LogManager pLogger) {
    firstRefiner = pFirstRefiner;
    secondRefiner = pSecondRefiner;
    logger = pLogger;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (firstRefiner instanceof StatisticsProvider) {
      ((StatisticsProvider) firstRefiner).collectStatistics(pStatsCollection);
    }
    if (secondRefiner instanceof StatisticsProvider) {
      ((StatisticsProvider) secondRefiner).collectStatistics(pStatsCollection);
    }
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Performing Double refinement");
    if (!firstRefiner.performRefinement(pReached)) {
      return secondRefiner.performRefinement(pReached);
    } else {
      return true;
    }
  }
}
