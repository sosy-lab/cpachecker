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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.algorithm.cbmctools.AbstractPathToCTranslator;
import org.sosy_lab.cpachecker.core.algorithm.cbmctools.CProver;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix="cbmc")
public class CBMCAlgorithm implements Algorithm, StatisticsProvider {

  private final Map<String, CFAFunctionDefinitionNode> cfa;
  private final Algorithm algorithm;
  private final LogManager logger;

  @Option(name="dumpCBMCfile", type=Option.Type.OUTPUT_FILE)
  private File CBMCFile;
  
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
  public void run(ReachedSet reached) throws CPAException {

    algorithm.run(reached);

    AbstractElement lastElement = reached.getLastElement();
    if ((lastElement instanceof Targetable) && ((Targetable)lastElement).isTarget()) {
      Set<ARTElement> elementsOnErrorPath = getElementsOnErrorPath((ARTElement)lastElement);
      
      String pathProgram = AbstractPathToCTranslator.translatePaths(cfa, (ARTElement)reached.getFirstElement(), elementsOnErrorPath);
      
      boolean cbmcResult;
      try {
        cbmcResult = CProver.checkFeasibility(pathProgram, logger, CBMCFile);
      } catch (IOException e) {
        throw new CPAException("Could not verify program with CBMC (" + e.getMessage() + ")");
      }
      
      if (cbmcResult) {
        logger.log(Level.INFO, "CBMC confirms the bug");

      } else {
        logger.log(Level.INFO, "CBMC thinks this path contains no bug");
        // TODO: continue analysis
      }
    }
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
  }
}