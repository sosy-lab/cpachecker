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

import java.util.Collection;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeEnvironment;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix="rely-guarantee cegar")
public class RelyGuaranteeCEGARAlgorithm implements ConcurrentAlgorithm,  StatisticsProvider {

  private ConcurrentAlgorithm algorithm;
  private RelyGuaranteeRefiner refiner;
  private Configuration config;
  private LogManager logger;

  private static final int GC_PERIOD = 100;
  private int gcCounter = 0;

  public RelyGuaranteeCEGARAlgorithm(ConcurrentAlgorithm pAlgorithm,  Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException, CPAException {
    this.algorithm = pAlgorithm;
    this.config = pConfig;
    this.logger = pLogger;

    // TODO for now only rg refiner is available
    refiner = RelyGuaranteeRefiner.getInstance(algorithm.getCPAs());
  }


  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    // TODO Auto-generated method stub

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

  @Override
  /**
   * Returns -1 if the threads are safe, otherwise it returns the thread id with the error
   */
  public int run(ReachedSet[] reachedSets, int startThread) {

    // TODO stats
    int runThread = startThread;
    int refinmentNo = 0;
    boolean continueAnalysis = false;
    do {
      System.out.println();
      System.out.println("-------------------------- CEGAR iteration "+refinmentNo+" --------------------------");
      runThread = algorithm.run(reachedSets, runThread);
      System.out.println();
      System.out.println("-----------------------------------------------------------------------");
      if (runThread == -1){
        // the program is safe
        continueAnalysis = false;
      } else {
        // the program is unsafe, so perform refinement
        try {
          continueAnalysis = refiner.performRefinment(reachedSets, algorithm.getRelyGuaranteeEnvironment(), runThread);
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        //AbstractElement error = reachedSets[errorThr].getLastElement();
        refinmentNo++;
      }

    } while (continueAnalysis);

    return runThread;
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
  public RelyGuaranteeEnvironment getRelyGuaranteeEnvironment() {
    return algorithm.getRelyGuaranteeEnvironment();
  }



}