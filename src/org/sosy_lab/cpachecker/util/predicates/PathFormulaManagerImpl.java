/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

/**
 * Class implementing the FormulaManager interface,
 * providing some commonly used stuff which is independent from specific libraries.
 *
 * This class inherits from CtoFormulaConverter to import the stuff there.
 */
public class PathFormulaManagerImpl extends CtoFormulaConverter implements PathFormulaManager {

  public PathFormulaManagerImpl(FormulaManager pFmgr,
      Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    super(config, pFmgr, pLogger);
  }

  @Override
  public PathFormula makeEmptyPathFormula() {
    return new PathFormula(fmgr.makeTrue(), SSAMap.emptySSAMap(), 0, fmgr.makeTrue(), 0);
  }

  @Override
  public PathFormula makeEmptyPathFormula(PathFormula oldFormula) {
    return new PathFormula(fmgr.makeTrue(), oldFormula.getSsa(), 0,
        oldFormula.getReachingPathsFormula(), oldFormula.getBranchingCounter());
  }

  @Override
  public PathFormula makeNewPathFormula(PathFormula oldFormula, SSAMap m) {
    return new PathFormula(oldFormula.getFormula(), m, oldFormula.getLength(), oldFormula.getReachingPathsFormula(), oldFormula.getBranchingCounter());
  }

  @Override
  public PathFormula makeOr(PathFormula pF1, PathFormula pF2) {
    Formula formula1 = pF1.getFormula();
    Formula formula2 = pF2.getFormula();
    SSAMap ssa1 = pF1.getSsa();
    SSAMap ssa2 = pF2.getSsa();

    Pair<Pair<Formula, Formula>,SSAMap> pm = mergeSSAMaps(ssa2, ssa1);

    // do not swap these two lines, that makes a huge difference in performance!
    Formula newFormula2 = fmgr.makeAnd(formula2, pm.getFirst().getFirst());
    Formula newFormula1 = fmgr.makeAnd(formula1, pm.getFirst().getSecond());

    Formula newFormula = fmgr.makeOr(newFormula1, newFormula2);
    SSAMap newSsa = pm.getSecond();

    int newLength = Math.max(pF1.getLength(), pF2.getLength());
    Formula newReachingPathsFormula
    = fmgr.makeOr(pF1.getReachingPathsFormula(), pF2.getReachingPathsFormula());
    int newBranchingCounter = Math.max(pF1.getBranchingCounter(), pF2.getBranchingCounter());

    return new PathFormula(newFormula, newSsa, newLength,
        newReachingPathsFormula, newBranchingCounter);
  }

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, Formula pOtherFormula) {
    SSAMap ssa = pPathFormula.getSsa();
    Formula otherFormula =  fmgr.instantiate(pOtherFormula, ssa);
    Formula resultFormula = fmgr.makeAnd(pPathFormula.getFormula(), otherFormula);
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
   * @return A pair (Formula, SSAMap)
   */
  private Pair<Pair<Formula, Formula>, SSAMap> mergeSSAMaps(
      SSAMap ssa1, SSAMap ssa2) {
    SSAMap result = SSAMap.merge(ssa1, ssa2);
    Formula mt1 = fmgr.makeTrue();
    Formula mt2 = fmgr.makeTrue();

    for (String var : result.allVariables()) {
      if (var.equals(CtoFormulaConverter.NONDET_VARIABLE)) {
        continue; // do not add index adjustment terms for __nondet__
      }
      int i1 = ssa1.getIndex(var);
      int i2 = ssa2.getIndex(var);

      if (i1 > i2 && i1 > 1) {
        // i2:smaller, i1:bigger
        // => need correction term for i2
        Formula t;

        if (useNondetFlags && var.equals(CtoFormulaConverter.NONDET_FLAG_VARIABLE)) {
          t = makeNondetFlagMerger(Math.max(i2, 1), i1);
        }
        else {
          t = makeSSAMerger(var, Math.max(i2, 1), i1);
        }

        mt2 = fmgr.makeAnd(mt2, t);

      } else if (i1 < i2 && i2 > 1) {
        // i1:smaller, i2:bigger
        // => need correction term for i1
        Formula t;

        if (useNondetFlags && var.equals(CtoFormulaConverter.NONDET_FLAG_VARIABLE)) {
          t = makeNondetFlagMerger(Math.max(i1, 1), i2);
        }
        else {
          t = makeSSAMerger(var, Math.max(i1, 1), i2);
        }

        mt1 = fmgr.makeAnd(mt1, t);
      }
    }

    for (Pair<String, FormulaList> f : result.allFunctions()) {
      String name = f.getFirst();
      FormulaList args = f.getSecond();
      int i1 = ssa1.getIndex(f);
      int i2 = ssa2.getIndex(f);

      if (i1 > i2 && i1 > 1) {
        // i2:smaller, i1:bigger
        // => need correction term for i2
        Formula t = makeSSAMerger(name, args, Math.max(i2, 1), i1);
        mt2 = fmgr.makeAnd(mt2, t);

      } else if (i1 < i2 && i2 > 1) {
        // i1:smaller, i2:bigger
        // => need correction term for i1
        Formula t = makeSSAMerger(name, args, Math.max(i1, 1), i2);
        mt1 = fmgr.makeAnd(mt1, t);
      }
    }

    return Pair.of(Pair.of(mt1, mt2), result);
  }

  private Formula makeNondetFlagMerger(int iSmaller, int iBigger) {
    return makeMerger(CtoFormulaConverter.NONDET_FLAG_VARIABLE, iSmaller, iBigger, fmgr.makeNumber(0));
  }

  private Formula makeMerger(String var, int iSmaller, int iBigger, Formula pInitialValue) {
    assert iSmaller < iBigger;

    Formula lResult = fmgr.makeTrue();

    for (int i = iSmaller+1; i <= iBigger; ++i) {
      Formula currentVar = fmgr.makeVariable(var, i);
      Formula e = fmgr.makeEqual(currentVar, pInitialValue);
      lResult = fmgr.makeAnd(lResult, e);
    }

    return lResult;
  }

  // creates the mathsat terms
  // (var@iSmaller = var@iSmaller+1; ...; var@iSmaller = var@iBigger)
  private Formula makeSSAMerger(String var, int iSmaller, int iBigger) {
    return makeMerger(var, iSmaller, iBigger, fmgr.makeVariable(var, iSmaller));
  }

  private Formula makeSSAMerger(String name,
      FormulaList args, int iSmaller, int iBigger) {
    assert iSmaller < iBigger;

    Formula intialFunc = fmgr.makeUIF(name, args, iSmaller);
    Formula result = fmgr.makeTrue();

    for (int i = iSmaller+1; i <= iBigger; ++i) {
      Formula currentFunc = fmgr.makeUIF(name, args, i);
      Formula e = fmgr.makeEqual(currentFunc, intialFunc);
      result = fmgr.makeAnd(result, e);
    }
    return result;
  }
}
