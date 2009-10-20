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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cfa.CFAMap;
import cpa.art.ARTCPA;
import cpa.art.ARTElement;
import cpa.common.ReachedElements;
import cpa.common.algorithm.cbmctools.AbstractPathToCTranslator;
import cpa.common.algorithm.cbmctools.CProver;
import cpa.common.interfaces.AbstractElement;
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

      List<ARTElement> elementsOnErrorPath = getElementsToErrorPath(reached);
      int cbmcRes = CProver.checkSat(AbstractPathToCTranslator.translatePaths(cfa, elementsOnErrorPath));
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

  private List<ARTElement> getElementsToErrorPath(ReachedElements pReached) {
    AbstractElement lastElement = pReached.getLastElement();
    ARTElement lastArtElement = (ARTElement)lastElement;

    List<ARTElement> waitlist = new ArrayList<ARTElement>();
    List<ARTElement> elements = new ArrayList<ARTElement>();
    Set<ARTElement> processed = new HashSet<ARTElement>();

    waitlist.add(lastArtElement);

    while(waitlist.size() > 0){
      ARTElement currentElement = waitlist.remove(0);
      processed.add(currentElement);
      elements.add(currentElement);

      for(ARTElement parent: currentElement.getParents()){
        if(!processed.contains(parent)){
          waitlist.add(parent);
        }
      }
    }
    return elements;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return algorithm.getCPA();
  }
}