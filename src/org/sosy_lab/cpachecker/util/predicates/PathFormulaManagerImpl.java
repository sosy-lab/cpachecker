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

import java.util.Collection;
import java.util.Map;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;

/**
 * Class implementing the FormulaManager interface,
 * providing some commonly used stuff which is independent from specific libraries.
 *
 * This class inherits from CtoFormulaConverter to import the stuff there.
 */
public class PathFormulaManagerImpl extends CtoFormulaConverter implements PathFormulaManager {

  private static PathFormulaManagerImpl singleton;
  private final  SSAMapManager ssaManager;

  public static PathFormulaManagerImpl getInstance(FormulaManager pFmgr, Configuration config, LogManager pLogger) throws InvalidConfigurationException{
    if (singleton == null){
      singleton = new PathFormulaManagerImpl(pFmgr, config, pLogger);
    }

    return singleton;
  }

  private PathFormulaManagerImpl(FormulaManager pFmgr,
      Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    super(config, pFmgr, pLogger);

    ssaManager = SSAMapManagerImpl.getInstance(fmgr, config, logger);
  }

  @Override
  public PathFormula makeEmptyPathFormula() {
    return new PathFormula(fmgr.makeTrue(), SSAMap.emptySSAMap(), 0);
  }

  @Override
  public PathFormula makeEmptyPathFormula(PathFormula oldFormula) {
    return new PathFormula(fmgr.makeTrue(), oldFormula.getSsa(), 0);
  }

  @Override
  public PathFormula makeFalsePathFormula() {
    return new PathFormula(fmgr.makeFalse(), SSAMap.emptySSAMap(), 0);
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
    Pair<Pair<Formula, Formula>,SSAMap> pm = ssaManager.mergeSSAMaps(ssa2, ssa1);

    // do not swap these two lines, that makes a huge difference in performance!
    Formula newFormula2 = fmgr.makeAnd(formula2, pm.getFirst().getFirst());
    Formula newFormula1 = fmgr.makeAnd(formula1, pm.getFirst().getSecond());

    Formula newFormula = fmgr.makeOr(newFormula1, newFormula2);
    SSAMap newSsa = pm.getSecond();

    int newLength = Math.max(pF1.getLength(), pF2.getLength());

    return new PathFormula(newFormula, newSsa, newLength);
  }

  @Override
  public PathFormula makeAnd(PathFormula pPathFormula, Formula pOtherFormula) {
    SSAMap ssa = pPathFormula.getSsa();
    Formula otherFormula =  fmgr.instantiate(pOtherFormula, ssa);
    Formula resultFormula = fmgr.makeAnd(pPathFormula.getFormula(), otherFormula);
    return new PathFormula(resultFormula, ssa, pPathFormula.getLength());
  }

  @Override
  public PathFormula makeAnd(PathFormula pf1, PathFormula pf2) {
    Pair<Pair<Formula, Formula>, SSAMap> merger = ssaManager.mergeSSAMaps(pf1.getSsa(), pf2.getSsa());
    Formula f = fmgr.makeAnd(pf1.getFormula(), pf2.getFormula());
    return new PathFormula(f, merger.getSecond(), pf1.getLength() + pf2.getLength());
  }

  @Override
  public PathFormula makePrimedEqualities(SSAMap ssa1, int i, SSAMap ssa2,int j) {
    Formula eq = fmgr.makePrimedEqualities(ssa1, i, ssa2, j);

    Collection<String> allUnprimedVars = ssaManager.getUnprimedVariables(ssa1);
    allUnprimedVars.addAll(ssaManager.getUnprimedVariables(ssa2));

    SSAMapBuilder newssa = SSAMap.emptySSAMap().builder();

    for (String var : allUnprimedVars){
      String var1 = fmgr.primeVariable(var, i);
      int idx1    = ssa1.getIndex(var1);
      idx1        = idx1 > 0 ? idx1 : 1;

      String var2 = fmgr.primeVariable(var, j);
      int idx2    = ssa2.getIndex(var2);
      idx2        = idx2 > 0 ? idx2 : 1;

      newssa.setIndex(var1, idx1);
      newssa.setIndex(var2, idx2);
    }

    return new PathFormula(eq, newssa.build(), 0);
  }

  @Override
  public PathFormula changePrimedNo(PathFormula pf, Map<Integer, Integer> map) {
    Formula f = fmgr.changePrimedNo(pf.getFormula(), map);
    SSAMap ssa = ssaManager.changePrimeNo(pf.getSsa(), map);
    return new PathFormula(f, ssa, pf.getLength());
  }

  @Override
  public PathFormula instantiateNextValue(Formula f, SSAMap low, SSAMap high) {
    f = fmgr.instantiateNextValue(f, low, high);
    return new PathFormula(f, high, 0);
  }




}
