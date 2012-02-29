/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.RGEnvironmentManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.RGRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;


@Options(prefix="cpa.rg.refinement")
public class RGCEGARAlgorithm implements ConcurrentAlgorithm,  StatisticsProvider {

  @Option(description="If true, the analysis continues in the previous thread. If false, the first thread is analysed first.")
  private boolean continueThread = false;

  private final Stats stats;

  private RGAlgorithm algorithm;
  private RGRefiner refiner;
  private Configuration config;
  private LogManager logger;

  private static final int GC_PERIOD = 100;
  private int gcCounter = 0;

  public RGCEGARAlgorithm(RGAlgorithm pAlgorithm,  Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException, CPAException {
    this.algorithm = pAlgorithm;
    this.config = pConfig;
    this.logger = pLogger;
    this.stats  = new Stats();

    pConfig.inject(this, RGCEGARAlgorithm.class);
    // TODO for now only rg refiner is available
    refiner = RGRefiner.getInstance(algorithm, algorithm.getCPAs(), this.algorithm.getRelyGuaranteeEnvironment(),pConfig);
  }




  @Override
  public int run(ReachedSet[] reachedSets, int startThread) {

    stats.totalTimer.start();
    int errorThread = startThread;
    boolean continueAnalysis = false;
    do {

      /* rely-guarantee analysis */
      errorThread = analyse(reachedSets, errorThread);

      if (errorThread == -1){
        /* the program is safe */
        return -1;
      } else {
        /* refine error */
        continueAnalysis = refine(reachedSets, errorThread);
        stats.countIterations++;
      }
    } while (continueAnalysis);

    System.out.println("----------------------------------------------------------------------------------");
    stats.totalTimer.stop();
    return errorThread;
  }


  /**
   * Run rely-guarantee analysis.
   * @param pReachedSets
   * @param pErrorThread
   * @return
   */
  private int analyse(ReachedSet[] reachedSets, int startThread) {
    System.out.println();
    System.out.println("------------------------ Rely-guarantee analysis "+stats.countIterations+" -------------------------");

    stats.totalAnal.start();
    int errorThread = continueThread ? stats.countIterations % 2 : 0;
    errorThread = algorithm.run(reachedSets, errorThread);
    long time = stats.totalAnal.stop();

    System.out.println();
    System.out.println("\t\t\t----- RG analysis statistics -----");
    System.out.println("Time for rely-guarantee analysis:       " + Timer.formatTime(time));

    return errorThread;
  }


  /**
   * Refine abstract error.
   * @param reachedSets
   * @param errorThread
   * @return
   */
  private boolean refine(ReachedSet[] reachedSets, int errorThread) {
    boolean spurious = false;
    try {
      System.out.println();
      System.out.println("------------------------------ Refinement "+stats.countIterations+" ------------------------------");
      stats.totalRefinement.start();
      spurious = refiner.performRefinment(reachedSets, algorithm.getRelyGuaranteeEnvironment(), errorThread);
      long time = stats.totalRefinement.stop();

      System.out.println();
      System.out.println("Time for refinement:              " + Timer.formatTime(time));

    } catch (Exception e) {
      e.printStackTrace();
    }

    return spurious;

  }




  private void runGC() {
    if ((++gcCounter % GC_PERIOD) == 0) {
      //stats.gcTimer.start();
      System.gc();
      gcCounter = 0;
      //stats.gcTimer.stop();
    }
  }

  @Override
  public RGEnvironmentManager getRelyGuaranteeEnvironment() {
    return algorithm.getRelyGuaranteeEnvironment();
  }

  @Override
  public void collectStatistics(Collection<Statistics> scoll) {
    algorithm.collectStatistics(scoll);
    refiner.collectStatistics(scoll);
    scoll.add(stats);
  }

  @Override
  public ConfigurableProgramAnalysis[] getCPAs() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Result getResult() {
    // TODO Auto-generated method stub
    return null;
  }

  public class Stats implements Statistics {

    private Timer totalTimer        = new Timer();
    private Timer totalAnal        = new Timer();
    private Timer totalRefinement   = new Timer();

    private int   countIterations  = 0;

    @Override
    public String getName() {
      return "Rely-guarantee CEGAR";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,ReachedSet pReached) {
      out.println("number of refinements:           " + formatInt(countIterations));
      out.println("time on analysis:                " + totalAnal);
      out.println("time on refinement:              " + totalRefinement);
      out.println("time on CEGAR:                   " + totalTimer);
    }

    private String formatInt(int val){
      return String.format("  %7d", val);
    }
  }



}