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

import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ResultCheckAlgorithm implements Algorithm, StatisticsProvider {

  private static class ResultCheckStatistics implements Statistics {

    private Timer checkTimer = new Timer();
    private Timer analysisTimer = new Timer();
    private int stopChecks = 0;

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
      pOut.println("Number of checks:           " + stopChecks);
      pOut.println("Time for Analysis:          " + analysisTimer);
      pOut.println("Time for Result Check:      " + checkTimer);

      if (checkTimer.getNumberOfIntervals() > 0) {
        pOut.println("Speed up checking:        " + ((float) analysisTimer.getSumTime().asNanos()) / checkTimer.getSumTime().asNanos());
      }

    }

    @Override
    public String getName() {
      return "ResultCheckAlgorithm";
    }

  }

  private LogManager logger;
  private Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private Algorithm analysisAlgorithm;
  private ConfigurableProgramAnalysis cpa;
  private CFA analyzedProgram;
  private ResultCheckStatistics stats;

  public ResultCheckAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa, CFA pCfa,
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    analysisAlgorithm = pAlgorithm;
    analyzedProgram = pCfa;
    cpa = pCpa;
    logger = pLogger;
    config = pConfig;
    shutdownNotifier = pShutdownNotifier;
    stats = new ResultCheckStatistics();
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    boolean result = false;

    logger.log(Level.INFO, "Start analysis.");

    try {
      stats.analysisTimer.start();
      result = analysisAlgorithm.run(pReachedSet);
    } finally {
      stats.analysisTimer.stop();
      logger.log(Level.INFO, "Analysis stopped.");
    }

    if (result && pReachedSet.getWaitlist().size() == 0) {
      logger.log(Level.INFO, "Analysis successful.", "Start checking analysis result");
      try {
        stats.checkTimer.start();
        ProofCheckAlgorithm checker = new ProofCheckAlgorithm(cpa, config, logger, shutdownNotifier, pReachedSet);
        CoreComponentsFactory factory = new CoreComponentsFactory(config, logger, shutdownNotifier);
        ReachedSet reached = factory.createReachedSet();
        reached.add(cpa.getInitialState(analyzedProgram.getMainFunction()),
            cpa.getInitialPrecision(analyzedProgram.getMainFunction()));
        result = checker.run(reached);
      } catch (InvalidConfigurationException e) {
        result = false;
      } finally {
        stats.checkTimer.stop();
        logger.log(Level.INFO, "Stop checking analysis result.");
      }

      if (result) {
        logger.log(Level.INFO, "Analysis result checked successfully.");
        return true;
      }
      logger.log(Level.INFO, "Analysis result could not be checked.");

    } else {
      logger.log(Level.WARNING, "Analysis incomplete.");
    }

    return false;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (analysisAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) analysisAlgorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }
}
