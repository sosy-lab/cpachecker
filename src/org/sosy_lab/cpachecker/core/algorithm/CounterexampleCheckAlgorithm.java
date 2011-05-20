/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.cbmctools.CBMCChecker;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.CounterexampleChecker;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;

@Options(prefix="counterexample")
public class CounterexampleCheckAlgorithm implements Algorithm, StatisticsProvider, Statistics {

  private final Algorithm algorithm;
  private final CounterexampleChecker checker;
  private final LogManager logger;

  private final Timer checkTime = new Timer();
  private int numberOfInfeasiblePaths = 0;
  
  @Option(name="checker", toUppercase=true, values={"CBMC", "EXPLICIT"})
  private String checkerName = "CBMC";
  
  @Option
  private boolean continueAfterInfeasibleError = true;
  
  public CounterexampleCheckAlgorithm(Algorithm algorithm, Configuration config, LogManager logger) throws InvalidConfigurationException, CPAException {
    this.algorithm = algorithm;
    this.logger = logger;
    config.inject(this);
    
    if (!(algorithm.getCPA() instanceof ARTCPA)) {
      throw new InvalidConfigurationException("ART CPA needed for counterexample check");
    }
    
    if (checkerName.equals("CBMC")) {
      checker = new CBMCChecker(config, logger);
    } else if (checkerName.equals("EXPLICIT")) {
      checker = new CounterexampleCPAChecker(config, logger);
    } else {
      throw new AssertionError();
    }
  }

  @Override
  public boolean run(ReachedSet reached) throws CPAException {
    boolean sound = true;
    
    while (reached.hasWaitingElement()) {
      sound &= algorithm.run(reached);
  
      AbstractElement lastElement = reached.getLastElement();
      if (!(lastElement instanceof ARTElement)) {
        // no analysis possible
        break;
      }
      
      ARTElement errorElement = (ARTElement)lastElement;
      if (!errorElement.isTarget()) {
        // no analysis necessary
        break;
      }
      
      ARTElement rootElement = (ARTElement)reached.getFirstElement();
      
      checkTime.start();
      try {      
        Set<ARTElement> elementsOnErrorPath = ARTUtils.getAllElementsOnPathsTo(errorElement);
        
        boolean feasibility = checker.checkCounterexample(rootElement, errorElement, elementsOnErrorPath);
        
        if (feasibility) {
          logger.log(Level.INFO, "Bug found which was confirmed by counterexample check.");
          break;
  
        } else {
          numberOfInfeasiblePaths++;
          
          if (continueAfterInfeasibleError) {
            Set<ARTElement> parents = errorElement.getParents();
            
            // remove re-added parents to prevent computing
            // the same error element over and over
            for(ARTElement parent: parents){
              reached.remove(parent);
              parent.removeFromART();
            }
            
            // remove the error element
            reached.remove(errorElement);
            errorElement.removeFromART();
    
            // WARNING: continuing analysis is unsound, because the elements of this
            // infeasible path may cover another path that is actually feasible
            // We would need to find the first element of this path that is
            // not reachable and cut the path there.
            sound = false;
    
            logger.log(Level.WARNING, "Bug found which was denied by counterexample check. Analysis will continue, but the result may be unsound.");
          
          } else {
            Path path = ARTUtils.getOnePathTo(errorElement);
            throw new RefinementFailedException(Reason.InfeasibleCounterexample, path);
          }
        }
      } finally {
        checkTime.stop();
      }
    }
    return sound;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return algorithm.getCPA();
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
    
    out.println("Number of counterexample checks:    " + checkTime.getNumberOfIntervals());
    if (checkTime.getNumberOfIntervals() > 0) {
      out.println("Number of infeasible paths:         " + numberOfInfeasiblePaths + " (" + toPercent(numberOfInfeasiblePaths, checkTime.getNumberOfIntervals()) +")" );
      out.println("Time for counterexample checks:     " + checkTime);
    }
    if (checker instanceof Statistics) {
      ((Statistics)checker).printStatistics(out, pResult, pReached);
    }
  }
  
  private static String toPercent(double val, double full) {
    return String.format("%1.0f", val/full*100) + "%";
  }

  @Override
  public String getName() {
    return "Counterexample-Check Algorithm";
  }
}