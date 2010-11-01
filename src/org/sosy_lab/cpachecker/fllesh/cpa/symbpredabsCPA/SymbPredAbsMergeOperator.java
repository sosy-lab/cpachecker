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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.ssa.UnmodifiableSSAMap;


/**
 * Merge operator for symbolic predicate abstraction.
 * This is not a trivial merge operator in the sense that it implements
 * mergeSep and mergeJoin together. If the abstract element is on an
 * abstraction location we don't merge, otherwise we merge two elements
 * and update the {@link SymbPredAbsAbstractElement}'s pathFormula.
 *
 * @author Erkan
 */
public class SymbPredAbsMergeOperator implements MergeOperator {

  private final LogManager logger;
  private final FormulaManager<UnmodifiableSSAMap> formulaManager;
  
  private ArrayList<Map<NonabstractionElement, Map<NonabstractionElement, MergedElement>>> mMergeCache = new ArrayList<Map<NonabstractionElement, Map<NonabstractionElement, MergedElement>>>(); 

  long totalMergeTime = 0;

  public SymbPredAbsMergeOperator(SymbPredAbsCPA pCpa) {
    this.logger = pCpa.getLogger();
    formulaManager = pCpa.getFormulaManager();
  }
  
  private Map<NonabstractionElement, Map<NonabstractionElement, MergedElement>> getMergeCache(AbstractionElement pAbstractionElement) {
    if (mMergeCache.size() <= pAbstractionElement.ID) {
      createMergeCache(pAbstractionElement);
    }
    
    return mMergeCache.get(pAbstractionElement.ID);
  }
  
  private void createMergeCache(AbstractionElement pAbstractionElement) {
    for (int lIndex = mMergeCache.size(); lIndex <= pAbstractionElement.ID; lIndex++) {
      mMergeCache.add(new HashMap<NonabstractionElement, Map<NonabstractionElement, MergedElement>>());
    }
  }
  
  @Override
  public AbstractElement merge(AbstractElement element1,
                               AbstractElement element2, Precision precision) {

    if (element1 instanceof AbstractionElement || element2 instanceof AbstractionElement) {
      // we don't merge if this is an abstraction location
      return element2;
    }
    
    NonabstractionElement elem1 = (NonabstractionElement)element1;
    NonabstractionElement elem2 = (NonabstractionElement)element2;

    if (!elem1.getAbstractionElement().equals(elem2.getAbstractionElement())) {
      return element2;
    }
    
    // this will be the merged element
    MergedElement merged;
    
    Map<NonabstractionElement, Map<NonabstractionElement, MergedElement>> lCache = getMergeCache(elem1.getAbstractionElement());
    
    Map<NonabstractionElement, MergedElement> lSecondCache = lCache.get(elem1);
    
    if (lSecondCache == null) {
      lSecondCache = new HashMap<NonabstractionElement, MergedElement>();
      lCache.put(elem1, lSecondCache);
    }
    
    merged = lSecondCache.get(elem2);
    
    if (merged == null) {
      long start = System.currentTimeMillis();

      logger.log(Level.FINEST, "Merging two non-abstraction nodes.");

      PathFormula<UnmodifiableSSAMap> pathFormula = formulaManager.makeOr(elem1.getPathFormula(), elem2.getPathFormula());

      logger.log(Level.ALL, "New path formula is", pathFormula);

      merged = new MergedElement(elem1.getAbstractionElement(), pathFormula, Math.max(elem1.getSizeSinceAbstraction(), elem2.getSizeSinceAbstraction()), elem1);
      
      long end = System.currentTimeMillis();
      totalMergeTime = totalMergeTime + (end - start);
      
      lSecondCache.put(elem2, merged);
    }
    
    return merged;
  }

}
