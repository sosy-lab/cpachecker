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
package org.sosy_lab.cpachecker.fllesh.fql.fllesh.util;

import java.util.Map;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;


// TODO: where is the right place to collect statistics?
public class CPAchecker extends org.sosy_lab.cpachecker.core.CPAchecker {
  private Map<String, CFAFunctionDefinitionNode> mCFAMap;
  private CFAFunctionDefinitionNode mMainFunction;
  
  public CPAchecker(Configuration pConfiguration, LogManager pLogManager) throws InvalidConfigurationException {
    super(pConfiguration, pLogManager);
    
    // get code file name
    String[] names = pConfiguration.getPropertiesArray("analysis.programNames");
    if (names.length != 1) {
      org.sosy_lab.cpachecker.core.CPAchecker.logger.log(Level.SEVERE, "Exactly one code file has to be given!");
      
      System.exit(1);
    }
    
    // parse code file
    IASTTranslationUnit lAst = null;
    try {
      lAst = super.parse(names[0]);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    
    Pair<Map<String, CFAFunctionDefinitionNode>, CFAFunctionDefinitionNode> lCFA = super.createCFA(lAst);
    
    mCFAMap = lCFA.getFirst();
    mMainFunction = lCFA.getSecond();
  }
  
  public Map<String, CFAFunctionDefinitionNode> getCFAMap() {
    return mCFAMap;
  }
  
  public CFAFunctionDefinitionNode getMainFunction() {
    return mMainFunction;
  }

  /*public ReachedElements run(Algorithm pAlgorithm, AbstractElement pInitialElement, Precision pInitialPrecision) throws CPAException {
    
    ReachedElements lReached = null;
    
    try {
      
      lReached = new ReachedElements(mConfiguration.getProperty("analysis.traversal"));
    } catch (IllegalArgumentException e) {
      
      mLogManager.logException(Level.SEVERE, e, "ERROR, unknown traversal option");
      System.exit(1);
    }
    
    lReached.add(pInitialElement, pInitialPrecision);

    run(pAlgorithm, lReached);

    return lReached;
  }
  
  public void run(Algorithm pAlgorithm, ReachedElements pReachedElements) throws CPAException {
    
    assert(pAlgorithm != null);
    assert(pReachedElements != null);
    
    mLogManager.log(Level.FINE, "CPA Algorithm starting ...");
    mStatistics.startAnalysisTimer();
    
    pAlgorithm.run(pReachedElements, mConfiguration.getBooleanValue("analysis.stopAfterError"));
    
    mStatistics.stopAnalysisTimer();
    mLogManager.log(Level.FINE, "CPA Algorithm finished");
  }
  
  public CPAStatistics.Result runAlgorithm() throws CPAException {

    mLogManager.log(Level.FINE, "Creating CPAs");
      
    ConfigurableProgramAnalysis cpa = CompositeCPA.getCompositeCPA(mMainFunction);

    if (mConfiguration.getBooleanValue("analysis.useART")) {
      cpa = ARTCPA.getARTCPA(mMainFunction, cpa);
    }
          
    if (cpa instanceof CPAWithStatistics) {
      ((CPAWithStatistics)cpa).collectStatistics(mStatistics.getSubStatistics());
    }
      
    // create algorithm
    Algorithm algorithm = new CPAAlgorithm(cpa);
      
    if (mConfiguration.getBooleanValue("analysis.useRefinement")) {
      algorithm = new CEGARAlgorithm(algorithm);
    }
      
    if (mConfiguration.getBooleanValue("analysis.useInvariantDump")) {
      algorithm = new InvariantCollectionAlgorithm(algorithm);
    }
      
    if (mConfiguration.getBooleanValue("analysis.useCBMC")) {
      algorithm = new CBMCAlgorithm(mCFAMap, algorithm);
    }
    
    ReachedElements reached = run(algorithm, cpa.getInitialElement(mMainFunction), cpa.getInitialPrecision(mMainFunction));
    
    for (AbstractElement reachedElement : reached) {
      if (reachedElement.isError()) {
        return CPAStatistics.Result.UNSAFE;
      }
    }
        
    return CPAStatistics.Result.SAFE;
  }*/
  
}
