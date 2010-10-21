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
package org.sosy_lab.cpachecker.cpa.symbpredabsCPA;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.PartialOrder;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;

@Options(prefix="cpas.symbpredabs")
public final class SymbPredAbsPartialOrder implements PartialOrder {
  
  @Option
  private boolean symbolicCoverageCheck = false; 
  
  // statistics
  int numCoverageCheck = 0;
  int numBddCoverageCheck = 0;
  int numSymbolicCoverageCheck = 0;
  long coverageCheckTime = 0;
  long bddCoverageCheckTime = 0;
  long symbolicCoverageCheckTime = 0;
  
  private final AbstractFormulaManager mAbstractFormulaManager;
  private final SymbPredAbsFormulaManager mgr;
    
  public SymbPredAbsPartialOrder(SymbPredAbsCPA pCpa) throws InvalidConfigurationException {
    pCpa.getConfiguration().inject(this);
    mAbstractFormulaManager = pCpa.getAbstractFormulaManager();
    mgr = pCpa.getFormulaManager();
  }
  
  @Override
  public boolean satisfiesPartialOrder(AbstractElement element1,
                                       AbstractElement element2) throws CPAException {
    numCoverageCheck++;
    long start = System.currentTimeMillis();
    try {
    
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

    if (e1.isAbstractionNode() && e2.isAbstractionNode()) {
      numBddCoverageCheck++;
      long startCheck = System.currentTimeMillis();
      
      // if e1's predicate abstraction entails e2's pred. abst.
      boolean result = mAbstractFormulaManager.entails(e1.getAbstraction().asAbstractFormula(), e2.getAbstraction().asAbstractFormula());
      
      bddCoverageCheckTime += System.currentTimeMillis() - startCheck;
      return result;

    } else if (e2.isAbstractionNode()) {
      if (symbolicCoverageCheck) {
        numSymbolicCoverageCheck++;
        long startCheck = System.currentTimeMillis();

        boolean result = mgr.checkCoverage(e1.getAbstraction(), e1.getPathFormula(), e2.getAbstraction());
      
        symbolicCoverageCheckTime += System.currentTimeMillis() - startCheck;
        return result;
        
      } else {
        return false; 
      }
      
    } else if (e1.isAbstractionNode()) {
      return false;
      
    } else {
      // only the fast check which returns true if a merge occurred for this element
      return e1.getMergedInto() == e2;
    }
    
    } finally {
      coverageCheckTime += System.currentTimeMillis() - start;
    }
  }
}