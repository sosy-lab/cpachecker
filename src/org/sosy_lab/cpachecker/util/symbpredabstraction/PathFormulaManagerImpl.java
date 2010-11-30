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
package org.sosy_lab.cpachecker.util.symbpredabstraction;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;

/**
 * Class implementing the FormulaManager interface,
 * providing some commonly used stuff which is independent from specific libraries.
 * 
 * This class inherits from CtoFormulaConverter to import the stuff there.
 * 
 * @author Philipp Wendler
 */
public class PathFormulaManagerImpl extends CtoFormulaConverter implements PathFormulaManager {

  public PathFormulaManagerImpl(SymbolicFormulaManager pSmgr,
      Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    super(config, pSmgr, pLogger);
  }

  @Override
  public PathFormula makeEmptyPathFormula() {
    return new PathFormula(smgr.makeTrue(), SSAMap.emptySSAMap(), 0, smgr.makeTrue(), 0);
  }

  @Override
  public PathFormula makeEmptyPathFormula(PathFormula oldFormula) {
    return new PathFormula(smgr.makeTrue(), oldFormula.getSsa(), 0,
        oldFormula.getReachingPathsFormula(), oldFormula.getBranchingCounter());
  }

  @Override
  public PathFormula makeOr(PathFormula pF1, PathFormula pF2) {
    SymbolicFormula formula1 = pF1.getSymbolicFormula();
    SymbolicFormula formula2 = pF2.getSymbolicFormula();
    SSAMap ssa1 = pF1.getSsa();
    SSAMap ssa2 = pF2.getSsa();

    Pair<Pair<SymbolicFormula, SymbolicFormula>,SSAMap> pm = mergeSSAMaps(ssa2, ssa1);

    // do not swap these two lines, that makes a huge difference in performance!
    SymbolicFormula newFormula2 = smgr.makeAnd(formula2, pm.getFirst().getFirst());
    SymbolicFormula newFormula1 = smgr.makeAnd(formula1, pm.getFirst().getSecond());

    SymbolicFormula newFormula = smgr.makeOr(newFormula1, newFormula2);
    SSAMap newSsa = pm.getSecond();

    int newLength = Math.max(pF1.getLength(), pF2.getLength());
    SymbolicFormula newReachingPathsFormula
    = smgr.makeOr(pF1.getReachingPathsFormula(), pF2.getReachingPathsFormula());
    int newBranchingCounter = Math.max(pF1.getBranchingCounter(), pF2.getBranchingCounter());

    return new PathFormula(newFormula, newSsa, newLength,
        newReachingPathsFormula, newBranchingCounter);
  }  

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, SymbolicFormula pOtherFormula) {
    SSAMap ssa = pPathFormula.getSsa();
    SymbolicFormula otherFormula =  smgr.instantiate(pOtherFormula, ssa);
    SymbolicFormula resultFormula = smgr.makeAnd(pPathFormula.getSymbolicFormula(), otherFormula);
    return new PathFormula(resultFormula, ssa, pPathFormula.getLength(),
        pPathFormula.getReachingPathsFormula(), pPathFormula.getBranchingCounter());
  }

  /**
   * builds a formula that represents the necessary variable assignments
   * to "merge" the two ssa maps. That is, for every variable X that has two
   * different ssa indices i and j in the maps, creates a new formula
   * (X_k = X_i) | (X_k = X_j), where k is a fresh ssa index.
   * Returns the formula described above, plus a new SSAMap that is the merge
   * of the two.
   *
   * @param ssa1 an SSAMap
   * @param ssa2 an SSAMap
   * @return A pair (SymbolicFormula, SSAMap)
   */
  private Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> mergeSSAMaps(
      SSAMap ssa1, SSAMap ssa2) {
    SSAMap result = SSAMap.merge(ssa1, ssa2);
    SymbolicFormula mt1 = smgr.makeTrue();
    SymbolicFormula mt2 = smgr.makeTrue();

    for (String var : result.allVariables()) {
      if (var.equals(CtoFormulaConverter.NONDET_VARIABLE)) {
        continue; // do not add index adjustment terms for __nondet__
      }
      int i1 = ssa1.getIndex(var);
      int i2 = ssa2.getIndex(var);

      if (i1 > i2 && i1 > 1) {
        // i2:smaller, i1:bigger
        // => need correction term for i2
        SymbolicFormula t;
        
        if (useNondetFlags && var.equals(CtoFormulaConverter.NONDET_FLAG_VARIABLE)) {
          t = makeNondetFlagMerger(Math.max(i2, 1), i1);
        }
        else {
          t = makeSSAMerger(var, Math.max(i2, 1), i1);
        }
        
        mt2 = smgr.makeAnd(mt2, t);

      } else if (i1 < i2 && i2 > 1) {
        // i1:smaller, i2:bigger
        // => need correction term for i1
        SymbolicFormula t;
        
        if (useNondetFlags && var.equals(CtoFormulaConverter.NONDET_FLAG_VARIABLE)) {
          t = makeNondetFlagMerger(Math.max(i1, 1), i2);
        }
        else {
          t = makeSSAMerger(var, Math.max(i1, 1), i2); 
        }
        
        mt1 = smgr.makeAnd(mt1, t); 
      }
    }

    for (Pair<String, SymbolicFormulaList> f : result.allFunctions()) {
      String name = f.getFirst();
      SymbolicFormulaList args = f.getSecond();
      int i1 = ssa1.getIndex(f);
      int i2 = ssa2.getIndex(f);

      if (i1 > i2 && i1 > 1) {
        // i2:smaller, i1:bigger
        // => need correction term for i2
        SymbolicFormula t = makeSSAMerger(name, args, Math.max(i2, 1), i1);
        mt2 = smgr.makeAnd(mt2, t);

      } else if (i1 < i2 && i2 > 1) {
        // i1:smaller, i2:bigger
        // => need correction term for i1
        SymbolicFormula t = makeSSAMerger(name, args, Math.max(i1, 1), i2); 
        mt1 = smgr.makeAnd(mt1, t); 
      }
    }

    Pair<SymbolicFormula, SymbolicFormula> sp =
      new Pair<SymbolicFormula, SymbolicFormula>(mt1, mt2);
    return new Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap>(sp, result);
  }
  
  private SymbolicFormula makeNondetFlagMerger(int iSmaller, int iBigger) {
    return makeMerger(CtoFormulaConverter.NONDET_FLAG_VARIABLE, iSmaller, iBigger, smgr.makeNumber(0));
  }
  
  private SymbolicFormula makeMerger(String var, int iSmaller, int iBigger, SymbolicFormula pInitialValue) {
    assert iSmaller < iBigger;
    
    SymbolicFormula lResult = smgr.makeTrue();

    for (int i = iSmaller+1; i <= iBigger; ++i) {
      SymbolicFormula currentVar = smgr.makeVariable(var, i);
      SymbolicFormula e = smgr.makeEqual(currentVar, pInitialValue);
      lResult = smgr.makeAnd(lResult, e);
    }
    
    return lResult;
  }

  // creates the mathsat terms
  // (var@iSmaller = var@iSmaller+1; ...; var@iSmaller = var@iBigger)
  private SymbolicFormula makeSSAMerger(String var, int iSmaller, int iBigger) {
    return makeMerger(var, iSmaller, iBigger, smgr.makeVariable(var, iSmaller));
  }

  private SymbolicFormula makeSSAMerger(String name,
      SymbolicFormulaList args, int iSmaller, int iBigger) {
    assert iSmaller < iBigger;

    SymbolicFormula intialFunc = smgr.makeUIF(name, args, iSmaller);
    SymbolicFormula result = smgr.makeTrue();

    for (int i = iSmaller+1; i <= iBigger; ++i) {
      SymbolicFormula currentFunc = smgr.makeUIF(name, args, i);
      SymbolicFormula e = smgr.makeEqual(currentFunc, intialFunc);
      result = smgr.makeAnd(result, e);
    }
    return result;
  }
}
