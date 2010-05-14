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

import symbpredabstraction.interfaces.AbstractFormulaManager;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import exceptions.CPAException;


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

  private final static class SymbPredAbsBottomElement extends SymbPredAbsAbstractElement {
    @Override
    public String toString() {
      return "<BOTTOM>";
    }
  }
  private final static class SymbPredAbsTopElement extends SymbPredAbsAbstractElement {
    @Override
    public String toString() {
      return "<TOP>";
    }
  }

  private final static class SymbPredAbsJoinOperator implements JoinOperator {
    public AbstractElement join(AbstractElement element1,
                                AbstractElement element2) throws CPAException {
      return top;
    }
  }

  private final class SymbPredAbsPartialOrder implements PartialOrder {
    public boolean satisfiesPartialOrder(AbstractElement element1,
                                         AbstractElement element2) throws CPAException {
      SymbPredAbsAbstractElement e1 = (SymbPredAbsAbstractElement)element1;
      SymbPredAbsAbstractElement e2 = (SymbPredAbsAbstractElement)element2;

      // TODO time statistics (previously in formula manager)
      /*
    long start = System.currentTimeMillis();
    entails(f1, f2);
    long end = System.currentTimeMillis();
    stats.bddCoverageCheckMaxTime = Math.max(stats.bddCoverageCheckMaxTime,
        (end - start));
    stats.bddCoverageCheckTime += (end - start);
    ++stats.numCoverageChecks;
       */
      
      if (e1 == bottom) {
        return true;
      } else if (e2 == top) {
        return true;
      } else if (e2 == bottom) {
        // we should not put this in the reached set
        assert(false);
        return false;
      } else if (e1 == top) {
        return false;
      }

      if (e1.isAbstractionNode() && e2.isAbstractionNode()) {
        // if e1's predicate abstraction entails e2's pred. abst.
        return mAbstractFormulaManager.entails(e1.getAbstraction(), e2.getAbstraction());

      } else if (e2.isAbstractionNode()) {
        if (symbolicCoverageCheck) {
          return mgr.checkCoverage(e1.getAbstraction(), e1.getPathFormula(), e2.getAbstraction());
        
        } else {
          return false; 
        }
        
      } else if (e1.isAbstractionNode()) {
        return false;
        
      } else {
        // if not an abstraction location
        if (e1.getAbstractionLocation().equals(e2.getAbstractionLocation()) 
          && e1.getAbstractionPathList().equals(e2.getAbstractionPathList())
          && e1.getAbstraction().equals(e2.getAbstraction())) {

          assert(e1.getPfParents().size() == 1);
          // and if pfParents of e1 is included in e2's pfParents
          // we don't need to add again, return true
          // this is useful after merging two elements which are not
          // abstraction locations and stop not to add the same
          // element again
          return e2.getPfParents().containsAll(e1.getPfParents());
        }
        return false;
      }
    }
  }

  private final static SymbPredAbsBottomElement bottom = new SymbPredAbsBottomElement();
  private final static SymbPredAbsTopElement top = new SymbPredAbsTopElement();
  private final static JoinOperator join = new SymbPredAbsJoinOperator();
  private final PartialOrder partial = new SymbPredAbsPartialOrder();

  public SymbPredAbsAbstractElement getBottomElement() {
    return bottom;
  }

  public JoinOperator getJoinOperator() {
    return join;
  }

  public PartialOrder getPartialOrder() {
    return partial;
  }

  public AbstractElement getTopElement() {
    return top;
  }
}