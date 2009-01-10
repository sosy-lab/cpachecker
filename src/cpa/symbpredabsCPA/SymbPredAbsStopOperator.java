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

import java.util.Collection;
import java.util.List;

import logging.CustomLogLevel;
import logging.LazyLogger;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import cpa.symbpredabs.AbstractFormulaManager;
import cpa.symbpredabs.SymbolicFormulaManager;
import exceptions.CPAException;

public class SymbPredAbsStopOperator implements StopOperator {

  private SymbPredAbsAbstractDomain domain;
  private SymbPredAbsCPA cpa;

  public SymbPredAbsStopOperator(AbstractDomain d) {
    domain = (SymbPredAbsAbstractDomain) d;
    cpa = domain.getCPA();
  }


  public AbstractDomain getAbstractDomain() {
    return domain;
  }


  public <AE extends AbstractElement> boolean stop (AE element, Collection<AE> reached, Precision prec) throws CPAException
  {
    for (AbstractElement e : reached) {
      if (stop(element, e)) {
        return true;
      }
    }
    return false;
  }

  public boolean stop(AbstractElement element, AbstractElement reachedElement)
  throws CPAException {

    // TODO move this into partialorder

    SymbPredAbsAbstractElement e1 = (SymbPredAbsAbstractElement)element;
    SymbPredAbsAbstractElement e2 = (SymbPredAbsAbstractElement)reachedElement;

    // if not an abstraction location
    if(!e1.isAbstractionNode()){
      if(e1.getParents().equals(e2.getParents())){

        List<Integer> succList = e1.getPfParents();
        List<Integer> reachedList = e2.getPfParents();

        assert(succList.size() == 1);
        
        return reachedList.containsAll(succList);
      }
      return false;
    }
    // if abstraction location
    else{

      if(e1.isBottomElement){
        return true;
      }

      SymbolicFormulaManager mgr = cpa.getSymbolicFormulaManager();   
      LazyLogger.log(LazyLogger.DEBUG_4,
          "Checking Coverage of element: ", element);

      SymbPredAbsCPA cpa = domain.getCPA();
      AbstractFormulaManager amgr = cpa.getAbstractFormulaManager();

      assert(e1.getAbstraction() != null);
      assert(e2.getAbstraction() != null);
      
//      assert((MathsatSymbolicFormula)e1.getInitAbstractionSet().getSymbolicFormula() != null);
//      assert((MathsatSymbolicFormula)e2.getInitAbstractionSet().getSymbolicFormula() != null);
//      
//      if(!e1.getParents().equals(e2.getParents()) &&
////          !(((MathsatSymbolicFormula)e1.getInitAbstractionSet().getSymbolicFormula()).toString().equals
////              (((MathsatSymbolicFormula)e2.getInitAbstractionSet().getSymbolicFormula()).toString()))
//        !(mgr.entails(e1.getInitAbstractionSet().getSymbolicFormula(), e2.getInitAbstractionSet().getSymbolicFormula())
//        && mgr.entails(e2.getInitAbstractionSet().getSymbolicFormula(), e1.getInitAbstractionSet().getSymbolicFormula()))
//      ){
//        return false;
//      }
      
//      if(
//        !(mgr.entails(e1.getInitAbstractionSet().getSymbolicFormula(), e2.getInitAbstractionSet().getSymbolicFormula())
//        && mgr.entails(e2.getInitAbstractionSet().getSymbolicFormula(), e1.getInitAbstractionSet().getSymbolicFormula()))
//      ){
//        System.out.println(" the formulas are not equal ");
//        assert(false);
//      }

      boolean ok = amgr.entails(e1.getAbstraction(), e2.getAbstraction());

      if (ok) {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel,
            "Element: ", element, " COVERED by: ", e2);
        // cpa.setCoveredBy(e1, e2);
      } else {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel,
        "NO, not covered");
      }

      return ok;
    }
  }
}
