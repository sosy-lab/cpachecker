// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

@Options(prefix = "cegar.refiner.summaries")
public class SummaryBasedRefiner implements Refiner, StatisticsProvider {

  private final LogManager logger;

  private final Refiner firstRefiner;
  private final Refiner secondRefiner;
  private final SummaryRefinerStatistics stats;
  protected final ARGCPA argCpa;

  @Option(
      secure = true,
      name = "maxAmntFirstRefinements",
      description = "Max amount of first refinements.")
  public int maxAmntFirstRefinements = 100;

  private int amntFirstRefinements = 0;

  public SummaryBasedRefiner(
      Refiner pFirstRefiner,
      Refiner pSecondRefiner,
      LogManager pLogger,
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig)
      throws InvalidConfigurationException {
    firstRefiner = pFirstRefiner;
    secondRefiner = pSecondRefiner;
    logger = pLogger;
    argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, Refiner.class);

    if (argCpa.getCfa().getSummaryInformation().isEmpty()) {
      // TODO: Better error handling
      stats = new SummaryRefinerStatistics(null, pConfig);
    } else {
      stats =
          new SummaryRefinerStatistics(
              argCpa.getCfa().getSummaryInformation().orElseThrow(), pConfig);
    }

    pConfig.inject(this);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (firstRefiner instanceof StatisticsProvider) {
      ((StatisticsProvider) firstRefiner).collectStatistics(pStatsCollection);
    }
    if (secondRefiner instanceof StatisticsProvider) {
      ((StatisticsProvider) secondRefiner).collectStatistics(pStatsCollection);
    }

    if (!pStatsCollection.contains(stats)) {
      pStatsCollection.add(stats);
    }
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    if (amntFirstRefinements > maxAmntFirstRefinements) {
      amntFirstRefinements = 0;
      stats.increaseDoubleRefinements();
      stats.increaseDoubleRefinementsCausedByMaximumAmountFirstRefinements();
      if (!secondRefiner.performRefinement(pReached)) {
        stats.recalculateDistinctStartegies();
        return firstRefiner.performRefinement(pReached);
      } else {
        return true;
      }
    } else {
      if (!firstRefiner.performRefinement(pReached)) {
        amntFirstRefinements = 0;
        logger.log(Level.INFO, "Performing Double refinement");
        stats.increaseDoubleRefinements();
        boolean resultSecondRefiner = secondRefiner.performRefinement(pReached);
        if (resultSecondRefiner) {
          stats.increaseStrategiesRefinedAway();
          stats.recalculateDistinctStartegies();
        }
        return resultSecondRefiner;
      } else {
        amntFirstRefinements += 1;
        return true;
      }
    }
  }
}
