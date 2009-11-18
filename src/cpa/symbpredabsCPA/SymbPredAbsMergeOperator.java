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
import java.util.List;

import symbpredabstraction.PathFormula;
import symbpredabstraction.SSAMap;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import symbpredabstraction.mathsat.MathsatSymbolicFormula;

import common.Pair;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import exceptions.CPAException;

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

  private SymbPredAbsAbstractDomain domain;
  private SymbolicFormulaManager symbolicFormulaManager;

  public static long totalMergeTime = 0;
  
  public SymbPredAbsMergeOperator(SymbPredAbsAbstractDomain pDomain) {
    domain = pDomain;
    symbolicFormulaManager = domain.getCPA().getSymbolicFormulaManager();
  }

  public AbstractElement merge(AbstractElement element1,
                               AbstractElement element2, Precision precision) {

    SymbPredAbsAbstractElement elem1 = (SymbPredAbsAbstractElement)element1;
    SymbPredAbsAbstractElement elem2 = (SymbPredAbsAbstractElement)element2;

    // this will be the merged element
    SymbPredAbsAbstractElement merged;
    // if not abstraction node
    if(!elem1.isAbstractionNode()){
      // if two elements have different abstraction paths, do not merge
      if(elem1 == domain.getBottomElement()){
        merged = elem2;
      }
      else if(!elem1.getAbstractionPathList().equals(elem2.getAbstractionPathList())){
        merged = elem2;
      }
      // if they have the same abstraction paths, we will take the disjunction 
      // of two path formulas from two merged elements
      else{
        long start = System.currentTimeMillis();
        // create a new element, note that their abstraction formulas, initAbstractionFormula,
        // abstraction locations, artParents are same because they have the same
        // abstraction path
        assert (elem1.getAbstraction().equals(elem2.getAbstraction()));
        assert (elem1.getInitAbstractionFormula() == elem2.getInitAbstractionFormula());
        assert (elem1.getAbstractionLocation() == elem2.getAbstractionLocation());
        assert (elem1.getArtParent() == elem2.getArtParent());

        MathsatSymbolicFormula formula1 =
          (MathsatSymbolicFormula)elem1.getPathFormula().getSymbolicFormula();
        // TODO check
//        elem1.updateMaxIndex(elem1.getPathFormula().getSsa());
        MathsatSymbolicFormula formula2 =
          (MathsatSymbolicFormula)elem2.getPathFormula().getSymbolicFormula();
        SSAMap ssa1 = elem1.getPathFormula().getSsa();
        SSAMap ssa2 = elem2.getPathFormula().getSsa();
        Pair<Pair<SymbolicFormula, SymbolicFormula>,SSAMap> pm = symbolicFormulaManager.mergeSSAMaps(ssa2, ssa1, false);
        MathsatSymbolicFormula old = (MathsatSymbolicFormula)symbolicFormulaManager.makeAnd(
            formula2, pm.getFirst().getFirst());
        SymbolicFormula newFormula = symbolicFormulaManager.makeAnd(formula1, pm.getFirst().getSecond());
        newFormula = symbolicFormulaManager.makeOr(old, newFormula);
        ssa1 = pm.getSecond();

        PathFormula pathFormula = new PathFormula(newFormula, ssa1);
        
        // now we update the pfParents,
        List<Integer> pfParents = new ArrayList<Integer>();
        pfParents.addAll(elem2.getPfParents());
        // elem1 is the successor element and elem2 is the reached element from
        // the reached set the successor (elem1) should have only 1 element in 
        // its pfParents list
        assert(elem1.getPfParents().size() == 1);
        // now we merge elem1 and elem2's pfParents and set it as merged element's
        // pfParents
        if(!pfParents.contains(elem1.getPfParents().get(0))){
          pfParents.add(elem1.getPfParents().get(0));
        }
        
        merged = new SymbPredAbsAbstractElement(domain, false, elem1.getAbstractionLocation(), 
            pathFormula, pfParents, elem1.getInitAbstractionFormula(), elem1.getAbstraction(), 
            elem1.getAbstractionPathList(), elem1.getArtParent(), elem1.getPredicates());

        // TODO check
//        merged.updateMaxIndex(ssa1);
        long end = System.currentTimeMillis();
        totalMergeTime = totalMergeTime + (end - start);
      }
    }
    // we don't merge if this is an abstraction location
    else{
      merged = elem2;
    }

    return merged;
  }

  @Override
  public AbstractElementWithLocation merge(
                                           AbstractElementWithLocation pElement1,
                                           AbstractElementWithLocation pElement2,
                                           Precision pPrecision)
  throws CPAException {
    throw new CPAException ("Cannot return element with location information");
  }
}
