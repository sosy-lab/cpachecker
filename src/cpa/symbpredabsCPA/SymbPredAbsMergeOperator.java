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

import java.util.logging.Level;

import symbpredabstraction.PathFormula;
import symbpredabstraction.SSAMap;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import cfa.objectmodel.CFANode;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import common.Pair;

import cpa.common.CPAchecker;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;

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

  private final SymbolicFormulaManager symbolicFormulaManager;

  public long totalMergeTime = 0;
  
  public SymbPredAbsMergeOperator(SymbPredAbsCPA pCpa) {
    symbolicFormulaManager = pCpa.getSymbolicFormulaManager();
  }

  public AbstractElement merge(AbstractElement element1,
                               AbstractElement element2, Precision precision) {

    SymbPredAbsAbstractElement elem1 = (SymbPredAbsAbstractElement)element1;
    SymbPredAbsAbstractElement elem2 = (SymbPredAbsAbstractElement)element2;

    // this will be the merged element
    SymbPredAbsAbstractElement merged;

    if (elem1.isAbstractionNode() || elem2.isAbstractionNode()) {
      // we don't merge if this is an abstraction location
      merged = elem2;
    } else {
      // if two elements have different abstraction paths, do not merge
      if(!elem1.getAbstractionPathList().equals(elem2.getAbstractionPathList())){
        merged = elem2;
      }
      else if(!elem1.getAbstraction().equals(elem2.getAbstraction())) {
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

        CPAchecker.logger.log(Level.FINEST, "Merging two non-abstraction nodes with parents",
                elem1.getPfParents(), "and", elem2.getPfParents(), ".");

        SymbolicFormula formula1 = elem1.getPathFormula().getSymbolicFormula();
        SymbolicFormula formula2 = elem2.getPathFormula().getSymbolicFormula();
        SSAMap ssa1 = elem1.getPathFormula().getSsa();
        SSAMap ssa2 = elem2.getPathFormula().getSsa();
        Pair<Pair<SymbolicFormula, SymbolicFormula>,SSAMap> pm = symbolicFormulaManager.mergeSSAMaps(ssa2, ssa1);
        SymbolicFormula old = symbolicFormulaManager.makeAnd(
            formula2, pm.getFirst().getFirst());
        SymbolicFormula newFormula = symbolicFormulaManager.makeAnd(formula1, pm.getFirst().getSecond());
        newFormula = symbolicFormulaManager.makeOr(old, newFormula);
        ssa1 = pm.getSecond();

        PathFormula pathFormula = new PathFormula(newFormula, ssa1);
        
        CPAchecker.logger.log(Level.ALL, "New path formula is", pathFormula);

        // now we update the pfParents
        // elem1 is the successor element and elem2 is the reached element from
        // the reached set the successor (elem1) should have only 1 element in 
        // its pfParents list
        CFANode elem1Parent = Iterables.getOnlyElement(elem1.getPfParents());
        assert !elem2.getPfParents().contains(elem1Parent);
        
        ImmutableSet.Builder<CFANode> pfParents = ImmutableSet.builder();
        pfParents.addAll(elem2.getPfParents());
        pfParents.add(elem1Parent);
                
        merged = new SymbPredAbsAbstractElement(elem1.getAbstractionLocation(), 
            pathFormula, pfParents.build(), elem1.getInitAbstractionFormula(), elem1.getAbstraction(), 
            elem1.getAbstractionPathList(),
            Math.max(elem1.getSizeSinceAbstraction(), elem2.getSizeSinceAbstraction()));

        long end = System.currentTimeMillis();
        totalMergeTime = totalMergeTime + (end - start);
      }
    }

    return merged;
  }

}
