// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.pcc;

import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.error.DummyErrorState;

public class AlgorithmWithPropertyCheck implements Algorithm, StatisticsProvider {

  private final Algorithm analysis;
  private final LogManager logger;
  private PropertyCheckerCPA cpa;

  public AlgorithmWithPropertyCheck(
      Algorithm analysisAlgorithm, LogManager logger, PropertyCheckerCPA cpa) {
    analysis = analysisAlgorithm;
    this.logger = logger;
    this.cpa = cpa;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    logger.log(Level.INFO, "Start analysis.");

    AlgorithmStatus result = analysis.run(pReachedSet);

    if (result.isSound()) {
      logger.log(Level.INFO, "Start property checking.");
      result = result.withSound(cpa.getPropChecker().satisfiesProperty(pReachedSet.asCollection()));
      // add dummy error state to get verification result FALSE instead of UNKNOWN
      if (!result.isSound()) {
        pReachedSet.add(
            new DummyErrorState(pReachedSet.getLastState()), SingletonPrecision.getInstance());
      }
    }

    logger.log(Level.INFO, "Finished analysis");
    return result;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (analysis instanceof StatisticsProvider) {
      ((StatisticsProvider) analysis).collectStatistics(pStatsCollection);
    }
  }
}
