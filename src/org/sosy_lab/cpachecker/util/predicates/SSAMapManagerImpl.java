/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;

@Options(prefix="cpa.predicate")
public class SSAMapManagerImpl implements SSAMapManager {
  @Option(description="add special information to formulas about non-deterministic functions")
  protected boolean useNondetFlags = false;

  private static SSAMapManagerImpl singleton;
  private final FormulaManager fManager;

  private final Map<Pair<SSAMap, Map<Integer, Integer>>, SSAMap> changePrimeNoCache
              = new HashMap<Pair<SSAMap, Map<Integer, Integer>>, SSAMap>();

  public static SSAMapManagerImpl getInstance(FormulaManager fManager, Configuration config, LogManager logger) throws InvalidConfigurationException{
    if (singleton == null){
      singleton = new SSAMapManagerImpl(fManager, config, logger);
    }
    return singleton;
  }

  private SSAMapManagerImpl(FormulaManager fManager, Configuration config, LogManager logger) throws InvalidConfigurationException{
    config.inject(this, SSAMapManagerImpl.class);
    this.fManager = fManager;
  }

  @Override
  public SSAMap incrementMap(SSAMap ssa, Collection<String> variables, int shift) {
    assert shift >= 0;
    if (variables == null){
      return ssa;
    }

    SSAMapBuilder sSsa = ssa.builder();
    for (String var : variables){
      int idx = ssa.getIndex(var);
      idx = idx >= 1 ? idx : 1;
      sSsa.setIndex(var, idx+shift);
    }

    return sSsa.build();
  }

  @Override
  public SSAMap changePrimeNo(SSAMap ssa, Map<Integer, Integer> map) {
    if (map.isEmpty()){
      return ssa;
    }

    Pair<SSAMap, Map<Integer, Integer>> key = Pair.of(ssa, map);
    if (changePrimeNoCache.containsKey(key)){
      return changePrimeNoCache.get(key);
    }

    Set<String> allVars = ssa.allVariables();
    SSAMapBuilder ssaBuilder = SSAMap.emptySSAMap().builder();
    // adjust the ssa map
    for (String var : allVars){
      Pair<String, Integer> data = fManager.getPrimeData(var);
      Integer newPrimedNo = map.get(data.getSecond());
      if (newPrimedNo != null){
        String nVar   = fManager.primeVariable(data.getFirst(), newPrimedNo);
        ssaBuilder.setIndex(nVar, ssa.getIndex(var));
      }
      else {
        ssaBuilder.setIndex(var, ssa.getIndex(var));
      }
    }

    SSAMap nSsa = ssaBuilder.build();
    changePrimeNoCache.put(key, nSsa);
    return nSsa;

  }

  @Override
  public Collection<String> getUnprimedVariables(SSAMap ssa) {
    Set<String> allVars = ssa.allVariables();
    Set<String> unprimed = new HashSet<String>(allVars.size());

    for (String var : allVars){
      Pair<String, Integer> pair = fManager.getPrimeData(var);
      unprimed.add(pair.getFirst());
    }

    return unprimed;
  }

  @Override
  public Pair<Pair<Formula, Formula>, SSAMap> mergeSSAMaps(SSAMap ssa1, SSAMap ssa2) {
    SSAMap result = SSAMap.merge(ssa1, ssa2);
    Formula mt1 = fManager.makeTrue();
    Formula mt2 = fManager.makeTrue();

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

        mt2 = fManager.makeAnd(mt2, t);

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

        mt1 = fManager.makeAnd(mt1, t);
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
        mt2 = fManager.makeAnd(mt2, t);

      } else if (i1 < i2 && i2 > 1) {
        // i1:smaller, i2:bigger
        // => need correction term for i1
        Formula t = makeSSAMerger(name, args, Math.max(i1, 1), i2);
        mt1 = fManager.makeAnd(mt1, t);
      }
    }

    return Pair.of(Pair.of(mt1, mt2), result);
  }

  private Formula makeNondetFlagMerger(int iSmaller, int iBigger) {
    return makeMerger(CtoFormulaConverter.NONDET_FLAG_VARIABLE, iSmaller, iBigger, fManager.makeNumber(0));
  }

  private Formula makeMerger(String var, int iSmaller, int iBigger, Formula pInitialValue) {
    assert iSmaller < iBigger;

    Formula lResult = fManager.makeTrue();

    for (int i = iSmaller+1; i <= iBigger; ++i) {
      Formula currentVar = fManager.makeVariable(var, i);
      Formula e = fManager.makeEqual(currentVar, pInitialValue);
      lResult = fManager.makeAnd(lResult, e);
    }

    return lResult;
  }

  // creates the mathsat terms
  // (var@iSmaller = var@iSmaller+1; ...; var@iSmaller = var@iBigger)
  private Formula makeSSAMerger(String var, int iSmaller, int iBigger) {
    return makeMerger(var, iSmaller, iBigger, fManager.makeVariable(var, iSmaller));
  }

  private Formula makeSSAMerger(String name,
      FormulaList args, int iSmaller, int iBigger) {
    assert iSmaller < iBigger;

    Formula intialFunc = fManager.makeUIF(name, args, iSmaller);
    Formula result = fManager.makeTrue();

    for (int i = iSmaller+1; i <= iBigger; ++i) {
      Formula currentFunc = fManager.makeUIF(name, args, i);
      Formula e = fManager.makeEqual(currentFunc, intialFunc);
      result = fManager.makeAnd(result, e);
    }
    return result;
  }

}
