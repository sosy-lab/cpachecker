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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.core.ReachedElements;
import org.sosy_lab.cpachecker.core.algorithm.cbmctools.AbstractPathToCTranslator;
import org.sosy_lab.cpachecker.core.algorithm.cbmctools.CProver;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CBMCAlgorithm implements Algorithm, StatisticsProvider {

  private final Map<String, CFAFunctionDefinitionNode> cfa;
  private final Algorithm algorithm;

  public CBMCAlgorithm(Map<String, CFAFunctionDefinitionNode> cfa, Algorithm algorithm) throws CPAException {
    this.cfa = cfa;
    this.algorithm = algorithm;

    if (!(algorithm.getCPA() instanceof ARTCPA)) {
      throw new CPAException("Need ART CPA for CBMC check");
    }
  }

  @Override
  public void run(ReachedElements reached, boolean stopAfterError) throws CPAException {

    algorithm.run(reached, true);

    if (reached.getLastElement().isError()) {
      System.out.println("________ ERROR PATH ____________");
      // commented out because breaks locality, contains hard-coded path 
      // ARTStatistics.dumpARTToDotFile(reached, new File("/localhome/erkan/cbmcArt.dot"));
      List<ARTElement> elementsOnErrorPath = getElementsToErrorPath((ARTElement)reached.getLastElement());
      String pathProgram = AbstractPathToCTranslator.translatePaths(cfa, elementsOnErrorPath);
      int cbmcRes = CProver.checkSat(pathProgram);
      
      if(cbmcRes == 10) {
        System.out.println("CBMC comfirms the bug");
        // TODO: if stopAfterError != true, continue analysis

      } else if(cbmcRes == 0) {
        System.out.println("CBMC thinks this path contains no bug");
        // TODO: continue analysis
//      reached.setLastElementToFalse();
//      CPAAlgorithm.errorFound = false;
//      stopAnalysis = false;
      }
      System.out.println("________________________________");
    }
    return;
  }

  private List<ARTElement> getElementsToErrorPath(ARTElement pElement) {
    
    List<ARTElement> waitList = new ArrayList<ARTElement>();
    List<ARTElement> retList = new ArrayList<ARTElement>();
    
    waitList.add(pElement);
    
    while(waitList.size() > 0){
      ARTElement currentElement = waitList.remove(0);
      retList.add(currentElement);
      for(ARTElement parent: currentElement.getParents()){
        if((!retList.contains(parent)) && 
            (!waitList.contains(parent))){
          waitList.add(parent);
        }
      }
    }
    
    return retList;
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