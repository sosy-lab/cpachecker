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
package cpa.symbpredabsCPA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.AbstractFormulaManager;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.trace.CounterexampleTraceInfo;
import cfa.objectmodel.CFAEdge;

import common.Pair;
import compositeCPA.CompositeCPA;

import cpa.art.ARTElement;
import cpa.art.ARTReachedSet;
import cpa.art.AbstractARTBasedRefiner;
import cpa.art.Path;
import cpa.common.CPAchecker;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.transferrelationmonitor.TransferRelationMonitorCPA;
import exceptions.CPAException;

public class McMillanRefiner extends AbstractARTBasedRefiner {

  private final AbstractFormulaManager abstractFormulaManager;
  private final SymbPredAbsFormulaManager formulaManager;
  
  public McMillanRefiner(final ConfigurableProgramAnalysis pCpa) throws CPAException {
    super(pCpa);

    ConfigurableProgramAnalysis cpa = this.getArtCpa().getWrappedCPA();
    
    SymbPredAbsCPA symbPredAbsCpa = null;
    if (cpa instanceof SymbPredAbsCPA) {
      symbPredAbsCpa = (SymbPredAbsCPA)cpa;
    
    } else {
      if (cpa instanceof CompositeCPA) {
        for (ConfigurableProgramAnalysis compCPA : ((CompositeCPA)cpa).getComponentCPAs()) {
          if (compCPA instanceof SymbPredAbsCPA) {
            symbPredAbsCpa = (SymbPredAbsCPA)compCPA;
            break;
          }
          else if (compCPA instanceof TransferRelationMonitorCPA){
            // TODO we assume that only one CPA is monitored
            ConfigurableProgramAnalysis cCpa = ((TransferRelationMonitorCPA)compCPA).getWrappedCPAs().iterator().next();
            if(cCpa instanceof SymbPredAbsCPA){
              symbPredAbsCpa = (SymbPredAbsCPA)cCpa;
              break;
            }
          }
        }
      }
      if (symbPredAbsCpa == null) {
        throw new CPAException(getClass().getSimpleName() + " needs a SymbPredAbsCPA");
      }
    }

    abstractFormulaManager = symbPredAbsCpa.getAbstractFormulaManager();
    formulaManager = symbPredAbsCpa.getFormulaManager();
  }

  @Override
  public boolean performRefinement(ARTReachedSet pReached, Path pPath) throws CPAException {

    CPAchecker.logger.log(Level.FINEST, "Starting refinement for SymbPredAbsCPA");
    
    // create path with all abstraction location elements (excluding the initial
    // element, which is not in pPath)
    // the last element is the element corresponding to the error location
    // (which is twice in pPath)
    ArrayList<SymbPredAbsAbstractElement> path = new ArrayList<SymbPredAbsAbstractElement>();
    SymbPredAbsAbstractElement lastElement = null;
    for (Pair<ARTElement,CFAEdge> artPair : pPath) {
      SymbPredAbsAbstractElement symbElement = 
        artPair.getFirst().retrieveWrappedElement(SymbPredAbsAbstractElement.class);
      
      if (symbElement.isAbstractionNode() && symbElement != lastElement) {
        path.add(symbElement);
      }
      lastElement = symbElement;
    }
    assert path.size() == pPath.size() - 1 : "not all elements are abstraction nodes?"; 

            
    // build the counterexample
    CounterexampleTraceInfo info = formulaManager.buildCounterexampleTrace(path);
        
    // if error is spurious refine
    if (info.isSpurious()) {
      CPAchecker.logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");
      performRefinement(pReached, pPath, info);
      
      return true;
    } else {
      CPAchecker.logger.log(Level.FINEST, "Error trace is not spurious");
      // we have a real error
      return false;
    }
  }

  private void performRefinement(ARTReachedSet pReached, 
      Path pArtPath, CounterexampleTraceInfo pInfo) throws CPAException {

    // the first element on the path which was discovered to be not reachable
    ARTElement root = null;
    
    // those elements where predicates have been added
//    Collection<ARTElement> strengthened = new ArrayList<ARTElement>();
    
    boolean foundInterpolant = false;
    for (Pair<ARTElement,CFAEdge> artPair : pArtPath) {
      ARTElement ae = artPair.getFirst();
      SymbPredAbsAbstractElement e = ae.retrieveWrappedElement(SymbPredAbsAbstractElement.class); 
      
      assert e.isAbstractionNode();
      
      Collection<Predicate> newpreds = pInfo.getPredicatesForRefinement(e);
      if (newpreds.size() == 0) {
        if (foundInterpolant) {
          // no predicates after some interpolants have been found means we have
          // reached that part of the path which is not reachable
          // (interpolant is false)
          
          root = ae;
          break;
        }
        
        // no predicates on the beginning of the path means the interpolant is true,
        // do nothing
        continue;
        
      } else {
        foundInterpolant = true;
      }
      
      AbstractFormula abs = e.getAbstraction();
      
      boolean newPred = false;
      
      for (Predicate p : newpreds) {
        AbstractFormula f = p.getFormula();
        if (abstractFormulaManager.isFalse(f)) {
          assert newpreds.size() == 1;
          
          root = ae;

        } else if (!abstractFormulaManager.entails(abs, f)) {
          newPred = true;
          abs = abstractFormulaManager.makeAnd(abs, p.getFormula());
        }
      }
      
      if (root != null) {
        // from here on, all elements will have the interpolant false
        // they will be removed from ART and reached set
        break;
      }
      
      if (newPred) {
        e.setAbstraction(abs);
        pReached.removeCoverage(ae);
//        strengthened.add(ae);
        
        if (pReached.checkForCoveredBy(ae)) {
          // this element is now covered by another element
          // the whole subtree has been removed
          
          return;
        }
      }
    }
    assert root != null : "Infeasible path without interpolant false at some time cannot exist";
    
//    pReached.removeCoverage(strengthened);
    pReached.replaceWithBottom(root);
  }
}
