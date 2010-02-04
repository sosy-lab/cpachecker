/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.common.algorithm;

import java.util.ArrayList;
import java.util.List;

import cfa.CFAMap;
import cpa.art.ARTCPA;
import cpa.art.ARTElement;
import cpa.common.ReachedElements;
import cpa.common.algorithm.cbmctools.AbstractPathToCTranslator;
import cpa.common.algorithm.cbmctools.CProver;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import exceptions.CPAException;

public class CBMCAlgorithm implements Algorithm {

  private final CFAMap cfa;
  private final Algorithm algorithm;

  public CBMCAlgorithm(CFAMap cfa, Algorithm algorithm) throws CPAException {
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
}