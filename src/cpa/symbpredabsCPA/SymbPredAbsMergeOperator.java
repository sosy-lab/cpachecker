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

import common.Pair;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormula;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.mathsat.MathsatSymbolicFormula;
import exceptions.CPAException;

/**
 * trivial merge operation for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SymbPredAbsMergeOperator implements MergeOperator {

  private SymbPredAbsAbstractDomain domain;

  private SymbolicFormulaManager mgr;

  public SymbPredAbsMergeOperator(SymbPredAbsAbstractDomain d) {
    domain = d;
    mgr = d.getCPA().getSymbolicFormulaManager();
  }

  public AbstractElement merge(AbstractElement element1,
                               AbstractElement element2, Precision precision) {

    SymbPredAbsAbstractElement elem1 = (SymbPredAbsAbstractElement)element1;
    SymbPredAbsAbstractElement elem2 = (SymbPredAbsAbstractElement)element2;

    //TODO check
    boolean b = elem1.isAbstractionNode();
    SymbPredAbsAbstractElement merged;
    if(!b){
      if(!elem1.getParents().equals(elem2.getParents())){
        merged = elem2;
      }
      else{
        // we set parent to abstract element 2's parent
        merged = new SymbPredAbsAbstractElement(domain, false, elem1.getAbstractionLocation(), 
            null, null, elem1.getInitAbstractionSet(), elem1.getAbstraction(), 
            elem1.getParents(), elem1.getArtParent(), elem1.getPredicates());
        // TODO check
        MathsatSymbolicFormula form1 =
          (MathsatSymbolicFormula)elem1.getPathFormula().getSymbolicFormula();
        elem1.updateMaxIndex(elem1.getPathFormula().getSsa());
        MathsatSymbolicFormula form2 =
          (MathsatSymbolicFormula)elem2.getPathFormula().getSymbolicFormula();
        SSAMap ssa2 = elem2.getPathFormula().getSsa();
        SSAMap ssa1 = elem1.getPathFormula().getSsa();
        Pair<Pair<SymbolicFormula, SymbolicFormula>,SSAMap> pm = mgr.mergeSSAMaps(ssa2, ssa1, false);
        MathsatSymbolicFormula old = (MathsatSymbolicFormula)mgr.makeAnd(
            form2, pm.getFirst().getFirst());
        SymbolicFormula newFormula = mgr.makeAnd(form1, pm.getFirst().getSecond());
        newFormula = mgr.makeOr(old, newFormula);
        ssa1 = pm.getSecond();

        merged.setPathFormula(new PathFormula(newFormula, ssa1));

        List<Integer> pfParents = new ArrayList<Integer>();
        pfParents.addAll(elem2.getPfParents());
        // the successor should have only 1 element in its pfParents list
        assert(elem1.getPfParents().size() == 1);
        if(!pfParents.contains(elem1.getPfParents().get(0))){
          pfParents.add(elem1.getPfParents().get(0));
        }
        merged.setPfParents(pfParents);
        // TODO check, what is that?
        // merged.setMaxIndex(maxIndex)
        merged.updateMaxIndex(ssa1);
      }
    }
    else{
      // TODO we assume there is only one edge entering an abstraction location
//    // set path formula - it is true
//    PathFormula pf = elem1.getPathFormula();
//    merged.setPathFormula(pf);

//    // update initial formula
//    // TODO check
//    MathsatSymbolicFormula form1 =
//    (MathsatSymbolicFormula)elem1.getInitAbstractionSet().getSymbolicFormula();
//    MathsatSymbolicFormula form2 =
//    (MathsatSymbolicFormula)elem2.getInitAbstractionSet().getSymbolicFormula();
//    SSAMap ssa2 = elem2.getInitAbstractionSet().getSsa();
//    SSAMap ssa1 = elem1.getInitAbstractionSet().getSsa();
//    Pair<Pair<SymbolicFormula, SymbolicFormula>,SSAMap> pm = mgr.mergeSSAMaps(ssa2, ssa1, false);
//    MathsatSymbolicFormula old = (MathsatSymbolicFormula)mgr.makeAnd(
//    form2, pm.getFirst().getFirst());
//    SymbolicFormula newFormula = mgr.makeAnd(form1, pm.getFirst().getSecond());
//    newFormula = mgr.makeOr(old, newFormula);
//    ssa1 = pm.getSecond();

//    // TODO these parameters should be cloned (really?)
//    merged.setParents(elem1.getParents());
//    merged.setPredicates(elem1.getPredicates());
//    merged.setPathFormula(new PathFormula(newFormula, ssa1));

//    // TODO compute abstraction here
//    merged.setAbstraction(elem1.getAbstraction());

//    // TODO check, what is that?
//    // merged.setMaxIndex(maxIndex)
//    merged.updateMaxIndex(ssa1);
      merged = elem2;
    }
    return merged;
    //}
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
