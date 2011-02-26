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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.cbmctools.AbstractPathToCTranslator;
import org.sosy_lab.cpachecker.core.algorithm.cbmctools.CProver;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix="cbmc")
public class CBMCAlgorithm implements Algorithm, StatisticsProvider, Statistics {

  private final Map<String, CFAFunctionDefinitionNode> cfa;
  private final Algorithm algorithm;
  private final LogManager logger;

  private int numberOfInfeasiblePaths = 0;
  private final Timer programCreationTime = new Timer();
  private final Timer cbmcTime = new Timer();
  
  @Option(name="dumpCBMCfile", type=Option.Type.OUTPUT_FILE)
  private File CBMCFile;
  
  @Option
  private boolean continueAfterInfeasibleError = false;
  
  public CBMCAlgorithm(Map<String, CFAFunctionDefinitionNode> cfa, Algorithm algorithm, Configuration config, LogManager logger) throws InvalidConfigurationException, CPAException {
    this.cfa = cfa;
    this.algorithm = algorithm;
    this.logger = logger;
    config.inject(this);
    
    if (!(algorithm.getCPA() instanceof ARTCPA)) {
      throw new CPAException("Need ART CPA for CBMC check");
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
      
      ARTElement element = (ARTElement)lastElement;
      if (!element.isTarget()) {
        // no analysis necessary
        break;
      }
      
      programCreationTime.start();
      Set<ARTElement> elementsOnErrorPath = getElementsOnErrorPath(element);
      
      String pathProgram = AbstractPathToCTranslator.translatePaths(cfa, (ARTElement)reached.getFirstElement(), elementsOnErrorPath);
      programCreationTime.stop();
      
      boolean cbmcResult;
      cbmcTime.start();
      try {
        cbmcResult = CProver.checkFeasibility(pathProgram, logger, CBMCFile);
      } catch (IOException e) {
        throw new CPAException("Could not verify program with CBMC (" + e.getMessage() + ")");
      } finally {
        cbmcTime.stop();
      }
      
      if (cbmcResult) {
        logger.log(Level.INFO, "Bug found which was confirmed by CBMC.");
        break;

      } else {
        numberOfInfeasiblePaths++;
        Set<ARTElement> parents = element.getParents();
        
        // remove re-added parents to prevent computing
        // the same error element over and over
        for(ARTElement parent: parents){
          reached.remove(parent);
          parent.removeFromART();
        }
        
        // remove the error element
        reached.remove(element);
        element.removeFromART();

        // WARNING: continuing analysis is unsound, because the elements of this
        // infeasible path may cover another path that is actually feasible
        // We would need to find the first element of this path that is
        // not reachable and cut the path there.
        sound = false;

        logger.log(Level.WARNING, "Bug found which was denied by CBMC. Analysis will continue, but the result may be unsound.");

        if (!continueAfterInfeasibleError) {
          break;
        }
      }
    }
    return sound;
  }

  private Set<ARTElement> getElementsOnErrorPath(ARTElement pElement) {

    Set<ARTElement> result = new HashSet<ARTElement>();
    Deque<ARTElement> waitList = new ArrayDeque<ARTElement>();

    result.add(pElement);
    waitList.add(pElement);

    while (!waitList.isEmpty()) {
      ARTElement currentElement = waitList.poll();
      for (ARTElement parent : currentElement.getParents()) {
        if (result.add(parent)) {
          waitList.push(parent);
        }
      }
    }

    return result;
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
    
    out.println("Number of times CBMC was called:    " + cbmcTime.getNumberOfIntervals());
    if (cbmcTime.getNumberOfIntervals() > 0) {
      out.println("Number of infeasible paths:         " + numberOfInfeasiblePaths + " (" + toPercent(numberOfInfeasiblePaths, cbmcTime.getNumberOfIntervals()) +")" );
      out.println("Time for creation of path programs: " + programCreationTime);
      out.println("Time for running CBMC:              " + cbmcTime);
    }
  }
  
  private static String toPercent(double val, double full) {
    return String.format("%1.0f", val/full*100) + "%";
  }

  @Override
  public String getName() {
    return "CBMC Algorithm";
  }
}