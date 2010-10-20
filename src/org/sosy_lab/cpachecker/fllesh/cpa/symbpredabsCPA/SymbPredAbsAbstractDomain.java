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
package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA;

import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.JoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;
import org.sosy_lab.cpachecker.exceptions.CPAException;


/**
 * Abstract domain for Symbolic lazy abstraction with summaries.
 *
 * @author Erkan
 */
public class SymbPredAbsAbstractDomain implements AbstractDomain {

  private final AbstractFormulaManager mAbstractFormulaManager;
  private final SymbPredAbsFormulaManager mgr;
  
  private final boolean symbolicCoverageCheck;
  
  public SymbPredAbsAbstractDomain(AbstractFormulaManager pAbstractFormulaManager,
        SymbPredAbsFormulaManager pMgr, boolean pSymbolicCoverageCheck) {
    mAbstractFormulaManager = pAbstractFormulaManager;
    mgr = pMgr;
    symbolicCoverageCheck = pSymbolicCoverageCheck;
  }

  private final static class SymbPredAbsJoinOperator implements JoinOperator {
    @Override
    public SymbPredAbsTopElement join(AbstractElement element1,
                                AbstractElement element2) throws CPAException {
      return SymbPredAbsTopElement.INSTANCE;
    }
  }

  private final class SymbPredAbsPartialOrder implements PartialOrder {
    @Override
    public boolean satisfiesPartialOrder(AbstractElement element1,
                                         AbstractElement element2) throws CPAException {
      
      if (element2 == SymbPredAbsTopElement.INSTANCE) {
        return true;
      } else if (element1 == SymbPredAbsTopElement.INSTANCE) {
        return false;
      }

      if (element1 instanceof AbstractionElement && element2 instanceof AbstractionElement) {
        AbstractionElement lElement1 = (AbstractionElement)element1;
        AbstractionElement lElement2 = (AbstractionElement)element2;
        
        // if e1's predicate abstraction entails e2's pred. abst.
        return mAbstractFormulaManager.entails(lElement1.getAbstractionFormula(), lElement2.getAbstractionFormula());
      }
      else if (element2 instanceof AbstractionElement) {
        if (symbolicCoverageCheck) {
          NonabstractionElement e1 = (NonabstractionElement)element1;
          AbstractionElement e2 = (AbstractionElement)element2;
          
          return mgr.checkCoverage(e1.getAbstractionElement().getAbstractionFormula(), e1.getPathFormula(), e2.getAbstractionFormula());
        
        } else {
          return false; 
        }
      }
      else if (element1 instanceof AbstractionElement) {
        return false;
      }
      else {
        if (element2 instanceof MergedElement) {
          MergedElement lMergedElement = (MergedElement)element2;
          
          return (lMergedElement.getMergesInto() == element1);
        }
        
        return false;
      }
    }
  }

  private final static JoinOperator join = new SymbPredAbsJoinOperator();
  private final PartialOrder partial = new SymbPredAbsPartialOrder();

  @Override
  public JoinOperator getJoinOperator() {
    return join;
  }

  @Override
  public PartialOrder getPartialOrder() {
    return partial;
  }

  @Override
  public SymbPredAbsTopElement getTopElement() {
    return SymbPredAbsTopElement.INSTANCE;
  }
}