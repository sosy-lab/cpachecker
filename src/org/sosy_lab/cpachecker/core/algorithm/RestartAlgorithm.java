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
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Preconditions;

public class RestartAlgorithm implements Algorithm, StatisticsProvider {

  private static class RestartAlgorithmStatistics implements Statistics {

    @Override
    public String getName() {
      return "Restart Algorithm";
    }

    @Override
    public void printStatistics(PrintStream pOut, Result pResult,
        ReachedSet pReached) {
      pOut.println("Restart Algorithm ended");
    }

  }

  private final RestartAlgorithmStatistics stats = new RestartAlgorithmStatistics();
  private final List<Pair<Algorithm, ReachedSet>> algorithms;
  private Algorithm currentAlgorithm;
  private final LogManager logger;

  public RestartAlgorithm(List<Pair<Algorithm, ReachedSet>> algorithms, Configuration config, LogManager logger) throws InvalidConfigurationException, CPAException {
    this.algorithms = algorithms;
    this.logger = logger;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return currentAlgorithm.getCPA();
  }

  @Override
  public boolean run(ReachedSet reached) throws CPAException,
  InterruptedException {

    boolean sound = true;

    int idx = 0;

    boolean continueAnalysis;
    do {
      continueAnalysis = false;

      Pair<Algorithm, ReachedSet> currentPair = algorithms.get(idx++);

      currentAlgorithm = currentPair.getFirst();
      ReachedSet currentReached = currentPair.getSecond();

      copyToReachedSet(reached, currentReached);

      // run algorithm
      Preconditions.checkNotNull(reached);
      sound = currentAlgorithm.run(reached);

      // if the analysis is not sound and we can proceed with
      // another algorithm, continue with the next algorithm
      if(!sound){
        // if there are no more algorithms to proceed with,
        // return the result
        if(idx == algorithms.size()){
          logger.log(Level.INFO, "RestartAlgorithm result is unsound.");
        }

        else{
          logger.log(Level.INFO, "RestartAlgorithm switches to the next algorithm ...");
          continueAnalysis = true;
        }
      }
    } while (continueAnalysis);

    return sound;
  }

  private void copyToReachedSet(ReachedSet pReached, ReachedSet pCurrentReached) {
    pReached.add(pCurrentReached.getFirstElement(), pCurrentReached.getPrecision(pCurrentReached.getFirstElement()));
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if(currentAlgorithm instanceof StatisticsProvider)
      ((StatisticsProvider)currentAlgorithm).collectStatistics(pStatsCollection);
    pStatsCollection.add(stats);
  }
}
