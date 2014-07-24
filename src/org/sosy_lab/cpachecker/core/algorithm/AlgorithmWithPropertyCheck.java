/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
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

  public AlgorithmWithPropertyCheck(Algorithm analysisAlgorithm, LogManager logger,
      PropertyCheckerCPA cpa) {
    analysis = analysisAlgorithm;
    this.logger = logger;
    this.cpa = cpa;
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    boolean result = false;

    logger.log(Level.INFO, "Start analysis.");

    result = analysis.run(pReachedSet);

    if (result) {
      logger.log(Level.INFO, "Start property checking.");
      result = cpa.getPropChecker().satisfiesProperty(pReachedSet.asCollection());
      // add dummy error state to get verification result FALSE instead of UNKNOWN
      if (!result) {
        pReachedSet.add(new DummyErrorState(pReachedSet.getLastState()), SingletonPrecision.getInstance());
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
