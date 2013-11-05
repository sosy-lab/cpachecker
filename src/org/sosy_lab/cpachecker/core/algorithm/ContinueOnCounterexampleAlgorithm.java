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
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.Iterables;

@Options(prefix="counterexample")
public class ContinueOnCounterexampleAlgorithm implements Algorithm, StatisticsProvider, Statistics {

  private final Algorithm algorithm;
  private final LogManager logger;

  private final Timer checkTime = new Timer();
  private int numberOfCounterexamples = 0;

  public ContinueOnCounterexampleAlgorithm(Algorithm algorithm, ConfigurableProgramAnalysis pCpa, Configuration config, LogManager logger, ReachedSetFactory reachedSetFactory, CFA cfa) throws InvalidConfigurationException, CPAException {
    this.algorithm = algorithm;
    this.logger = logger;
    config.inject(this);
  }

  @Override
  public boolean run(ReachedSet reached) throws CPAException, InterruptedException {
    boolean sound = true;

    while (reached.hasWaitingState()) {
      sound &= algorithm.run(reached);

      numberOfCounterexamples++;

      if(sound) {
        checkTime.start();
      }

      AbstractState lastElement = reached.getLastState();

      if (!(lastElement instanceof ARGState)) {
        // no analysis possible
        break;
      }

      ARGState errorElement = (ARGState)lastElement;
      if (!errorElement.isTarget()) {
        // no analysis necessary
        break;
      }

      removeErrorElement(reached, errorElement);

      sound = false;
    }

    if(!sound) {
      checkTime.stop();
    }

    return sound;
  }

  private void removeErrorElement(ReachedSet reached, ARGState errorElement) {
    // remove re-added parent of errorElement to prevent computing
    // the same error element over and over
    Collection<ARGState> parents = errorElement.getParents();
    assert parents.size() == 1 : "error element that was merged";

    ARGState parent = Iterables.getOnlyElement(parents);

    //TODO: siblings of error element?
    assert parent.getChildren().size() == 1;


    reached.add(errorElement, reached.getPrecision(parent));
    reached.add(parent, reached.getPrecision(parent));
    reached.removeOnlyFromWaitlist(errorElement);
    reached.removeOnlyFromWaitlist(parent);

  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(this);
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult,
      ReachedSet pReached) {

    if (checkTime.getNumberOfIntervals() > 0) {
      out.println("Number of ignroed counterexamples:         " + numberOfCounterexamples);
      out.println("Time for postprocessing:               " + checkTime);
    }
  }

  @Override
  public String getName() {
    return "Continue-On-Counterexample Algorithm";
  }

  @Override
  public boolean reset() {
    return algorithm.reset();
  }
}
