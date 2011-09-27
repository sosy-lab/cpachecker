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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.SSAMap.SSAMapBuilder;
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

  private Set<String> globalVariablesSet;

  private PathFormula falsePathFormula;

  private static  PathFormulaManager pfManager;

  public static PathFormulaManager getInstance(FormulaManager pFmgr, Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    if (pfManager == null){
      pfManager = new PathFormulaManagerImpl(pFmgr, config, pLogger);
    }
    return pfManager;
  }

  public PathFormulaManagerImpl(FormulaManager pFmgr, Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    super(config, pFmgr, pLogger);
    SSAMapBuilder emptyBuilder = SSAMap.emptySSAMap().builder();
    // TODO change to ordinary false, when problems with interpolation are resolved
    Formula dummy = fmgr.makePredicateVariable("dummy", 0);
    Formula not_dummy = fmgr.makeNot(dummy);
    Formula fancyFalse = fmgr.makeAnd(dummy, not_dummy);
    falsePathFormula = new PathFormula(fmgr.makeFalse(), emptyBuilder.build(), 0);
    //falsePathFormula = new PathFormula(fancyFalse, emptyBuilder.build(), 0);
  }


  @Override
  public PathFormula makeEmptyPathFormula() {
    return new PathFormula(fmgr.makeTrue(), SSAMap.emptySSAMap(), 0, 0);
  }

  @Override
  public PathFormula makeEmptyPathFormula(PathFormula oldFormula) {
    // remove primed variables from ssa map
    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    for (String var : oldFormula.getSsa().allVariables()){
      if (! var.contains(PathFormula.PRIME_SYMBOL)){
        ssa.setIndex(var, oldFormula.getSsa().getIndex(var));
      }
    }
    return new PathFormula(fmgr.makeTrue(), ssa.build(), 0, 0);
  }

  // returns an empty path formula with a clean SSAMap from variables that do not belong to this thread
  public PathFormula makeEmptyPathFormula(PathFormula oldFormula, int tid) {
    //
    SSAMap oldssa = oldFormula.getSsa();
    SSAMapBuilder cleanMap = SSAMap.emptySSAMap().builder();

    for (String var : oldssa.allVariables()){
      Pair<String, Integer> data = PathFormula.getPrimeData(var);
      if (data.getSecond() == tid){
        cleanMap.setIndex(var, oldssa.getIndex(var));
      }
    }
    return new PathFormula(fmgr.makeTrue(), cleanMap.build(), 0, tid);
  }

  @Override
  public PathFormula makeNewPathFormula(PathFormula oldFormula, SSAMap m) {
    return new PathFormula(oldFormula.getFormula(), m, oldFormula.getLength());
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
    int max_prime = Math.max(pF1.getPrimedNo(), pF2.getPrimedNo());


    return new PathFormula(newFormula, newSsa, newLength, max_prime);
  }


  public PathFormula makeRelyGuaranteeOr(PathFormula pF1, PathFormula pF2) {
    Formula formula1 = pF1.getFormula();
    Formula formula2 = pF2.getFormula();
    SSAMap ssa1 = pF1.getSsa();
    SSAMap ssa2 = pF2.getSsa();

    Pair<Pair<Formula, Formula>,SSAMap> pm = mergeRelyGuaranteeSSAMaps(ssa2, ssa1);

    // do not swap these two lines, that makes a huge difference in performance!
    Formula newFormula2 = fmgr.makeAnd(formula2, pm.getFirst().getFirst());
    Formula newFormula1 = fmgr.makeAnd(formula1, pm.getFirst().getSecond());

    Formula newFormula = fmgr.makeOr(newFormula1, newFormula2);
    SSAMap newSsa = pm.getSecond();

    int newLength = Math.max(pF1.getLength(), pF2.getLength());
    int max_prime = Math.max(pF1.getPrimedNo(), pF2.getPrimedNo());


    return new PathFormula(newFormula, newSsa, newLength, max_prime);
  }



  // TODO added for RelyGuarantee
  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, Formula pOtherFormula) {
    SSAMap ssa = pPathFormula.getSsa();
    Formula otherFormula =  fmgr.instantiate(pOtherFormula, ssa);
    Formula resultFormula = fmgr.makeAnd(pPathFormula.getFormula(), otherFormula);
    return new PathFormula(resultFormula, ssa, pPathFormula.getLength(), pPathFormula.getPrimedNo());
  }

  public PathFormula makeAnd(PathFormula pf1, PathFormula pf2){
    SSAMap ssa = SSAMap.merge(pf1.getSsa(), pf2.getSsa());
    Formula f = fmgr.makeAnd(pf1.getFormula(), pf2.getFormula());
    int length = Math.max(pf1.getLength(), pf2.getLength());
    int primedNo = Math.max(pf1.getPrimedNo(), pf2.getPrimedNo());
    return new PathFormula(f, ssa, length, primedNo);

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
  public Pair<Pair<Formula, Formula>, SSAMap> mergeSSAMaps(
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


  // like mergeSSAMaps, but equivalences are not build over primed variables
  private Pair<Pair<Formula, Formula>, SSAMap> mergeRelyGuaranteeSSAMaps(SSAMap ssa1, SSAMap ssa2) {

    SSAMap result = SSAMap.merge(ssa1, ssa2);
    Formula mt1 = fmgr.makeTrue();
    Formula mt2 = fmgr.makeTrue();

    for (String var : result.allVariables()) {
      // don't buid equalities over primed varables
      if (PathFormula.getPrimeData(var).getSecond() > 0) {
        continue;
      }
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

  /*
  @Override
  public PathFormula shiftFormula(PathFormula pathFormula, int offset) {
    Formula shiftedFormula = fmgr.shiftFormula(pathFormula.getFormula(), offset);
    SSAMap ssamap = pathFormula.getSsa();
    SSAMap.SSAMapBuilder builder = SSAMap.emptySSAMap().builder();

    for (String lVariable : ssamap.allVariables()) {
      builder.setIndex(lVariable, offset + ssamap.getIndex(lVariable));
    }

    // TODO functions

    SSAMap newmap = builder.build();

    PathFormula lShiftedFormula = new PathFormula(shiftedFormula, newmap, pathFormula.getLength());

    return lShiftedFormula;
  }*/

  /* @Override
  public PathFormula makeAnd(PathFormula localPathFormula, PathFormula envPathFormula,  int myTid, int sourceTid) {
    // hardcoded


    SSAMap ssa1 = localPathFormula.getSsa();
    SSAMap ssa2 = envPathFormula.getSsa();

    SSAMap mergedSSA = SSAMap.merge(ssa1, ssa2);
    SSAMapBuilder mergedWithEQSSA = mergedSSA.builder();
    Formula f1 = localPathFormula.getFormula();
    Formula f2 = envPathFormula.getFormula();
    Formula mergedFormula = fmgr.makeAnd(f1, f2);

    Formula mt = fmgr.makeTrue();


    for (String lVariable : ssa2.allVariables()){
      // check if global
      String pureVariable = lVariable.replace("_"+sourceTid, "");
      if (this.globalVariablesSet.contains(pureVariable)){
        int localIndex = ssa1.getIndex(pureVariable+"_"+myTid);
        if (localIndex == -1){
          localIndex = 1;
        }

        Formula lVar1 = fmgr.makeVariable(pureVariable+"_"+myTid, localIndex+1);
        Formula lVar2 = fmgr.makeVariable(lVariable, ssa2.getIndex(lVariable));
        Formula e = fmgr.makeEqual(lVar1, lVar2);
        mt = fmgr.makeAnd(mt, e);
        mergedWithEQSSA.setIndex(pureVariable+"_"+myTid, localIndex+1);
        // TODO what about nondet?
        if (useNondetFlags && lVariable.equals(CtoFormulaConverter.NONDET_FLAG_VARIABLE)) {
          throw new RuntimeException();
        }
      }
    }
    // TODO correct length
    return new PathFormula(fmgr.makeAnd(mergedFormula, mt), mergedWithEQSSA.build(), localPathFormula.getLength()+envPathFormula.getLength());
  }*/




  @Override
  // returns path formula primed given number of times
  public PathFormula primePathFormula(PathFormula envPF, int offset) {
    if (offset == 0) {
      return envPF;
    }
    // prime the formula
    Formula primedF = this.fmgr.primeFormula(envPF.getFormula(), offset);
    // build a new SSAMAP
    SSAMapBuilder primedSSA = SSAMap.emptySSAMap().builder();
    for (String var : envPF.getSsa().allVariables()) {
      Pair<String, Integer> data = PathFormula.getPrimeData(var);
      String bareName = data.getFirst();
      int currentPrime = data.getSecond() != null ? data.getSecond() : 0;
      int primeNo = currentPrime + offset;
      int idx = envPF.getSsa().getIndex(var);
      primedSSA.setIndex(bareName+PathFormula.PRIME_SYMBOL+primeNo, idx);
    }

    return new PathFormula(primedF, primedSSA.build(), envPF.getLength(), envPF.getPrimedNo()+offset);
  }

  @Override
  public PathFormula primePathFormula(PathFormula envPF, int offset, SSAMap ssa) {

    // prime the formula
    Formula primedF = fmgr.primeFormula(envPF.getFormula(), offset, ssa);
    // build a new SSAMAP
    SSAMapBuilder primedSSA = SSAMap.emptySSAMap().builder();
    for (String var : envPF.getSsa().allVariables()) {
      if (var.contains(PathFormula.THREAD_SYMBOL)){
        Pair<String, Integer> pair = PathFormula.getNonModularData(var);
        String bareName = pair.getFirst();
        int idx = ssa.getIndex(bareName);
        primedSSA.setIndex(var, idx);
      } else {
        Pair<String, Integer> data = PathFormula.getPrimeData(var);
        String bareName = data.getFirst();
        int primeNo = data.getSecond() + offset;
        int idx = envPF.getSsa().getIndex(var);
        primedSSA.setIndex(bareName+PathFormula.PRIME_SYMBOL+primeNo, idx);
      }

    }

    return new PathFormula(primedF, primedSSA.build(), envPF.getLength(), envPF.getPrimedNo()+offset);
  }



  @Override
  // merge two possibly primed formulas and add  equalities over their last values
  public PathFormula matchPaths(PathFormula localPF, PathFormula envPF, Set<String> globalVariablesSet, int offset) {
    assert offset > 0;

    Formula f = fmgr.makeAnd(localPF.getFormula(), envPF.getFormula());
    SSAMapBuilder matchedSSA = SSAMap.merge(localPF.getSsa(), envPF.getSsa()).builder();

    // unprimed variables in env should have offset x primes
    // build equalities over global variables
    for (String var : globalVariablesSet) {
      int lidx = localPF.getSsa().getIndex(var);
      int eidx = envPF.getSsa().getIndex(var+PathFormula.PRIME_SYMBOL+offset);
      if (lidx == -1) {
        lidx = 1;
        matchedSSA.setIndex(var, lidx);
      }
      if (eidx == -1) {
        eidx = 1;
        matchedSSA.setIndex(var+PathFormula.PRIME_SYMBOL+offset, eidx);
      }
      Formula lvar = fmgr.makeVariable(var, lidx);
      Formula evar = fmgr.makeVariable(var+PathFormula.PRIME_SYMBOL+offset, eidx);
      Formula eq  = fmgr.makeEqual(lvar, evar);
      f = fmgr.makeAnd(f, eq);
    }

    int length = Math.max(localPF.getLength(), envPF.getLength());

    return new PathFormula(f, matchedSSA.build(), length,  envPF.getPrimedNo());
  }


  /**
   * Generate equalities over last indexes of specified variables. Path formula pf1 should be unprimed.
   * @param pf1
   * @param pf2
   * @param variableSet
   * @return
   */
  public PathFormula buildEqualitiesOverVariables(PathFormula pf1, PathFormula pf2, Set<String> variableSet){
    assert pf1.getPrimedNo() == 0;

    Formula f = fmgr.makeTrue();
    SSAMapBuilder matchedSSA = SSAMap.merge(pf1.getSsa(), pf2.getSsa()).builder();

    // unprimed variables in env should have offset x primes
    int offset = pf1.getPrimedNo()+1;
    // build equalities for globalVariables variables
    for (String var : variableSet) {
      int lidx = pf1.getSsa().getIndex(var);
      int eidx = pf2.getSsa().getIndex(var+PathFormula.PRIME_SYMBOL+offset);
      if (lidx == -1) {
        lidx = 1;
        matchedSSA.setIndex(var, lidx);
      }
      if (eidx == -1) {
        eidx = 1;
        matchedSSA.setIndex(var+PathFormula.PRIME_SYMBOL+offset, eidx);
      }
      Formula lvar = fmgr.makeVariable(var, lidx);
      Formula evar = fmgr.makeVariable(var+PathFormula.PRIME_SYMBOL+offset, eidx);
      Formula eq  = fmgr.makeEqual(lvar, evar);
      f = fmgr.makeAnd(f, eq);
    }

    return new PathFormula(f, matchedSSA.build(), 0,  pf2.getPrimedNo());
  }

  @Override
  // reduces indexes to the lowest possible values, additionally it removes dead entries from ssa
  public PathFormula normalize(PathFormula pNewPF) {
    // reduce the formula are retrive the lowest indexes
    Pair<Formula,Map<String, Integer>> data = fmgr.normalize(pNewPF.getFormula());
    Formula f = data.getFirst();
    Map<String, Integer> lowestIndex = data.getSecond();
    // build a new SSAMap
    SSAMap oldSsa = pNewPF.getSsa();
    SSAMapBuilder newSsa = SSAMap.emptySSAMap().builder();
    for (String var : lowestIndex.keySet()){
      int nidx = oldSsa.getIndex(var) - lowestIndex.get(var) + 2;
      newSsa.setIndex(var, nidx);
    }
    return new PathFormula(f, newSsa.build(), pNewPF.getLength());
  }

  @Override
  public PathFormula makeFalsePathFormula() {
    return falsePathFormula;
  }

  @Override
  public PathFormula adjustPrimedNo(PathFormula pathFormula, Map<Integer, Integer> primedMap) {
    //Formula adjustedF = fmgr.adjustedPrimedNo(pathFormula.getFormula(), primedMap);
    Formula adjustedF = fmgr.adjustedPrimedNo(pathFormula.getFormula(), primedMap);
    SSAMapBuilder ssaBuilder = SSAMap.emptySSAMap().builder();
    SSAMap ssa = pathFormula.getSsa();
    int maxPrimed = 0;
    // adjust the ssa map
    for (String var : ssa.allVariables()){
      Pair<String, Integer> data = PathFormula.getPrimeData(var);
      if (primedMap.containsKey(data.getSecond())){
        int newPrimedNo = primedMap.get(data.getSecond());
        if (newPrimedNo == 0 && ssaBuilder.getIndex(data.getFirst()) == -1){
          ssaBuilder.setIndex(data.getFirst(), ssa.getIndex(var));
        } else if (ssaBuilder.getIndex(data.getFirst()+PathFormula.PRIME_SYMBOL+newPrimedNo) == -1) {
          ssaBuilder.setIndex(data.getFirst()+PathFormula.PRIME_SYMBOL+newPrimedNo, ssa.getIndex(var));
        }
        maxPrimed = Math.max(maxPrimed, newPrimedNo);
      } else {
        ssaBuilder.setIndex(var, ssa.getIndex(var));
        maxPrimed = Math.max(maxPrimed, data.getSecond());
      }
    }
    //

    return new PathFormula(adjustedF, ssaBuilder.build(), pathFormula.getLength(), maxPrimed);
  }

  @Override
  public PathFormula removePrimed(PathFormula pf, Set<Integer> primesToRemove) {
    Formula reducedF = fmgr.removePrimed(pf.getFormula(), primesToRemove);

    // remove primed entries from the SSA map
    SSAMap ssa = pf.getSsa();
    /* SSAMapBuilder ssaBldr = SSAMap.emptySSAMap().builder();
    for (String var : ssa.allVariables()){
      Integer pn = PathFormula.getPrimeData(var).getSecond();
      if (!primesToRemove.contains(pn)){
        ssaBldr.setIndex(var, ssa.getIndex(var));
      }
    }*/

    // TODO returning the same primed no is safe but imprecise
    return new PathFormula(reducedF, ssa, pf.getLength(), pf.getPrimedNo());
  }

  @Override
  public PathFormula makePrimedEqualities(PathFormula af, int i, int j) {

    Formula alleq = fmgr.makeTrue();
    SSAMap ssa = af.getSsa();
    SSAMapBuilder newssa = SSAMap.emptySSAMap().builder();

    for (String var : ssa.allVariables()){
      Pair<String, Integer> data = PathFormula.getPrimeData(var);
      if (data.getSecond() == i){
        Integer idx     = ssa.getIndex(var);
        assert idx > 0;
        Formula uf  = fmgr.makeVariable(var, idx);
        String pVar = data.getFirst()+PathFormula.PRIME_SYMBOL+j;
        Formula pf  = fmgr.makeVariable(pVar, idx);
        Formula eq  = fmgr.makeEqual(uf, pf);
        alleq       = fmgr.makeAnd(alleq, eq);

        newssa.setIndex(var, idx);
        newssa.setIndex(pVar, idx);

      }
    }

    return new PathFormula (alleq, newssa.build(), af.getLength());
  }

  @Override
  public PathFormula makePrimedEqualities(PathFormula pf1, int i, PathFormula pf2, int j) {

    Set<String> allUnprimedVars = new HashSet<String>();

    for (String var : pf1.getSsa().allVariables()){
      Pair<String, Integer> data = PathFormula.getPrimeData(var);
      allUnprimedVars.add(data.getFirst());
    }

    for (String var : pf2.getSsa().allVariables()){
      Pair<String, Integer> data = PathFormula.getPrimeData(var);
      allUnprimedVars.add(data.getFirst());
    }

    Formula alleq = fmgr.makeTrue();
    SSAMap ssa = pf1.getSsa();
    SSAMapBuilder newssa = SSAMap.emptySSAMap().builder();

    for (String var : allUnprimedVars){
      String var1 = var+PathFormula.PRIME_SYMBOL+i;
      int idx1    = pf1.getSsa().getIndex(var1);
      idx1        = idx1 > 0 ? idx1 : 1;

      String var2 = var+PathFormula.PRIME_SYMBOL+j;
      int idx2    = pf2.getSsa().getIndex(var2);
      idx2        = idx2 > 0 ? idx2 : 1;

      Formula fv1   = fmgr.makeVariable(var1, idx1);
      Formula fv2   = fmgr.makeVariable(var2, idx2);
      Formula feq   = fmgr.makeEqual(fv1, fv2);
      alleq         = fmgr.makeAnd(alleq, feq);


      newssa.setIndex(var1, idx1);
      newssa.setIndex(var2, idx2);
    }

  return new PathFormula (alleq, newssa.build(), 0, Math.max(i, j));
}


@Override
public PathFormula makeUnsatisifiableConstraintsForRedundantIndexes(SSAMap ssatop, SSAMap ssa, int tid) {
  Formula all = fmgr.makeTrue();
  Formula f0  = fmgr.makeNumber(0);
  Formula f1  = fmgr.makeNumber(1);
  for (String var : ssatop.allVariables()){
    Pair<String, Integer> data = PathFormula.getPrimeData(var);
    if (data.getSecond() == tid){
      int topIdx = ssatop.getIndex(var);
      int idx     = ssa.getIndex(var);
      idx         = idx < 1 ? 1 : idx;
      // make unstafiable constraint: v@x=1 & v@x=0
      for (int i=idx+1; i <= topIdx; i++){
        Formula fv   = fmgr.makeVariable(var, i);
        Formula fv0  = fmgr.makeEqual(fv, f0);
        Formula fv1  = fmgr.makeEqual(fv, f1);
        all          = fmgr.makeAnd(all, fv0);
        all          = fmgr.makeAnd(all, fv1);
      }
    }
  }

  return new PathFormula(all, ssatop, 0);
}



}
