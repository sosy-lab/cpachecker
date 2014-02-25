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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PCCStrategy;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.strategy.PCCStrategyBuilder;

@Options
public class ProofCheckAlgorithm implements Algorithm, StatisticsProvider {

  private static class CPAStatistics implements Statistics {

    private Timer totalTimer = new Timer();
    private Timer readTimer = new Timer();

    @Override
    public String getName() {
      return "Proof Check algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,
        ReachedSet pReached) {
      out.println();
      out.println("Total time for proof check algorithm:     " + totalTimer);
      out.println("  Time for reading in proof:              " + readTimer);
    }
  }

  private final CPAStatistics stats = new CPAStatistics();
  private final LogManager logger;


  @Option(
      name = "pcc.strategy",
      description = "Qualified name for class which implements proof checking strategy to be used.")
  private String pccStrategy = "org.sosy_lab.cpachecker.pcc.strategy.ARGProofCheckerStrategy";

  private PCCStrategy checkingStrategy;


  public ProofCheckAlgorithm(ConfigurableProgramAnalysis cpa, Configuration pConfig,
      LogManager logger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    checkingStrategy = PCCStrategyBuilder.buildStrategy(pccStrategy, pConfig, logger, pShutdownNotifier, cpa);

    this.logger = logger;

    logger.log(Level.INFO, "Start reading proof.");
    try {
      stats.totalTimer.start();
      stats.readTimer.start();
      checkingStrategy.readProof();
    } catch (Throwable e) {
      e.printStackTrace();
      throw new RuntimeException("Failed reading proof.", e);
    } finally {
      stats.readTimer.stop();
      stats.totalTimer.stop();
    }
    logger.log(Level.INFO, "Finished reading proof.");

    System.gc();
  }

  protected ProofCheckAlgorithm(ConfigurableProgramAnalysis cpa, Configuration pConfig,
      LogManager logger, ShutdownNotifier pShutdownNotifier, ReachedSet pReachedSet)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    checkingStrategy = PCCStrategyBuilder.buildStrategy(pccStrategy, pConfig, logger, pShutdownNotifier, cpa);
    this.logger = logger;

    if (pReachedSet == null || pReachedSet.hasWaitingState()) { throw new IllegalArgumentException(
        "Parameter pReachedSet may not be null and may not have any states in its waitlist."); }

    stats.totalTimer.start();
    checkingStrategy.constructInternalProofRepresentation(pReachedSet);
    stats.totalTimer.stop();
  }

  @Override
  public boolean run(final ReachedSet reachedSet) throws CPAException, InterruptedException {

    logger.log(Level.INFO, "Proof check algorithm started.");
    stats.totalTimer.start();

    boolean result = false;
    result = checkingStrategy.checkCertificate(reachedSet);

    stats.totalTimer.stop();
    logger.log(Level.INFO, "Proof check algorithm finished.");

    return result;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    if (checkingStrategy instanceof StatisticsProvider) {
      ((StatisticsProvider)checkingStrategy).collectStatistics(pStatsCollection);
    }
  }
}
