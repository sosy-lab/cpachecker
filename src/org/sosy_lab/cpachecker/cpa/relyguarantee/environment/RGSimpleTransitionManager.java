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
package org.sosy_lab.cpachecker.cpa.relyguarantee.environment;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGAbstractionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGCPA;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGVariables;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGSimpleTransition;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.SSAMapManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

public class RGSimpleTransitionManager extends RGEnvTransitionManagerFactory {

  private final FormulaManager fManager;
  private final PathFormulaManager pfManager;
  private final RGAbstractionManager absManager;
  private final SSAMapManager ssaManager;
  private final TheoremProver thmProver;
  private final RegionManager rManager;
  private final LogManager logger;
  private final Stats stats;

  public RGSimpleTransitionManager(FormulaManager pFManager, PathFormulaManager pPfManager, RGAbstractionManager absManager, SSAMapManager pSsaManager,TheoremProver pThmProver, RegionManager pRManager, RGVariables variables, Configuration pConfig, LogManager pLogger) {
    this.fManager = pFManager;
    this.pfManager = pPfManager;
    this.absManager = absManager;
    this.ssaManager = pSsaManager;
    this.thmProver = pThmProver;
    this.rManager = pRManager;
    this.logger  = pLogger;

    this.stats = new Stats();
  }

  @Override
  public RGSimpleTransition generateEnvTransition(RGEnvCandidate cand, Collection<AbstractionPredicate> pGlobalPreds, Multimap<CFANode, AbstractionPredicate> pLocalPreds) {
    // TODO Auto-generated method stub
    AbstractionFormula abs = cand.getRgElement().getAbstractionFormula();
    Formula absF = abs.asFormula();
    Region absReg = abs.asRegion();
    PathFormula pf = cand.getRgElement().getPathFormula();

    ARTElement laElement = RGCPA.findLastAbstractionARTElement(cand.getElement());
    if (laElement == null){
      System.out.println(this.getClass());
    }
    assert laElement != null;

    return new RGSimpleTransition(absF, absReg, pf, cand.getOperation(), laElement, cand.getSuccessor(), cand.getTid());
  }

  @Override
  public PathFormula formulaForAbstraction(RGAbstractElement elem, RGEnvTransition et, int unique) throws CPATransferException {
    RGSimpleTransition st = (RGSimpleTransition) et;

    AbstractionFormula lAbs = elem.getAbstractionFormula();
    PathFormula lPf = elem.getPathFormula();
    Formula eAbs = st.getAbstraction();
    PathFormula ePf = st.getPathFormula();

    /* if path formulas are true, then BDDs can detect unsatisfiable result */
    if (lPf.getFormula().isTrue() || ePf.getFormula().isTrue()){
      boolean isFalse = checkByBDD(st, lAbs);
      if (isFalse){
        stats.falseByBDD++;
        return pfManager.makeFalsePathFormula();
      }
    }

    // rename env. part to unique
    Formula eValF = fManager.makeAnd(ePf.getFormula(), eAbs);
    PathFormula eVal = new PathFormula(eValF, ePf.getSsa(), ePf.getLength());
    Map<Integer, Integer> rMap = new HashMap<Integer, Integer>(1);
    rMap.put(-1, unique);
    eVal = pfManager.changePrimedNo(eVal, rMap);

    // build equalities over the valuations of the env. transition and the element
    SSAMap lSSA = lPf.getSsa();
    SSAMap eSSA = eVal.getSsa();
    Formula eq = fManager.makePrimedEqualities(eSSA, unique, lSSA, -1);
    Formula appF = fManager.makeAnd(eq, eVal.getFormula());
    PathFormula appPf = new PathFormula(appF, eVal.getSsa(), eVal.getLength());

    // use the ssa of the local path formula
    appPf = new PathFormula(appPf.getFormula(), lSSA, 0);

    // apply the operation
    appPf = pfManager.makeAnd(appPf, st.getOperation());

    return appPf;
  }

  @Override
  public PathFormula formulaForRefinement(RGAbstractElement elem, RGEnvTransition et, int unique) throws CPATransferException {
    RGSimpleTransition st = (RGSimpleTransition) et;

    AbstractionFormula lAbs = elem.getAbstractionFormula();
    PathFormula lPf = elem.getPathFormula();
    PathFormula ePf = st.getPathFormula();

    /* if path formulas are true, then BDDs can detect unsatisfiable result */
    if (lPf.getFormula().isTrue() || ePf.getFormula().isTrue()){
      boolean isFalse = checkByBDD(st, lAbs);
      if (isFalse){
        // no stats here, they were counted at formulaForAbstraction
        return pfManager.makeFalsePathFormula();
      }
    }

    // rename env. part to unique
    Map<Integer, Integer> rMap = new HashMap<Integer, Integer>(1);
    rMap.put(-1, unique);
    ePf = pfManager.changePrimedNo(ePf, rMap);

    // build equalities over the valuations of the env. transition and the element
    SSAMap lSSA = lPf.getSsa();
    SSAMap eSSA = ePf.getSsa();
    Formula eq = fManager.makePrimedEqualities(eSSA, unique, lSSA, -1);
    Formula refF = fManager.makeAnd(ePf.getFormula(), eq);
    PathFormula refPf = new PathFormula(refF, ePf.getSsa(), 0);

    // use the ssa of the local path formula
    refPf = new PathFormula(refPf.getFormula(), lSSA, 0);

    // apply the operation
    refPf = pfManager.makeAnd(refPf, st.getOperation());

    return refPf;
  }


  @Override
  public boolean isLessOrEqual(RGEnvTransition et1, RGEnvTransition et2) {
    RGSimpleTransition st1 = (RGSimpleTransition) et1;
    RGSimpleTransition st2 = (RGSimpleTransition) et2;

    CFAEdge op1 = st1.getOperation();
    CFAEdge op2 = st2.getOperation();

    if (!op1.equals(op2)){
      return false;
    }

    ImmutableList<Integer> locCl1 = et1.getSourceARTElement().getLocationClasses();
    ImmutableList<Integer> locCl2 = et2.getSourceARTElement().getLocationClasses();

    if (!locCl1.equals(locCl2)){
      return false;
    }

    ImmutableList<Integer> tlocCl1 = et1.getTargetARTElement().getLocationClasses();
    ImmutableList<Integer> tlocCl2 = et2.getTargetARTElement().getLocationClasses();

    if (!tlocCl1.equals(tlocCl2)){
      return false;
    }

    Formula abs1 = st1.getAbstraction();
    Formula f1 = st1.getPathFormula().getFormula();
    Formula abs2 = st1.getAbstraction();
    Formula f2 = st2.getPathFormula().getFormula();

    if (abs1.isFalse() || f1.isFalse() || (abs2.isTrue() && f2.isTrue())){
      return true;
    }

    // TODO add checking by thmProver
    return false;
  }

  @Override
  public boolean isBottom(RGEnvTransition et) {
    RGSimpleTransition st = (RGSimpleTransition) et;

    Formula fAbs = st.getAbstraction();
    Formula f = st.getPathFormula().getFormula();

    f = fManager.makeAnd(f, fAbs);

    thmProver.init();
    boolean unsat = thmProver.isUnsat(fAbs);
    thmProver.reset();

    return unsat;
  }

  @Override
  public void collectStatistics(Collection<Statistics> scoll) {
    scoll.add(stats);
  }


  private boolean checkByBDD(RGSimpleTransition st, AbstractionFormula abs) {
    Region rEt = st.getAbstractionRegion();
    Region rElem = abs.asRegion();
    Region rAnd = rManager.makeAnd(rElem, rEt);
    return rManager.isFalse(rAnd);
  }




  public static class Stats implements Statistics {

    private int falseByBDD = 0;

    @Override
    public void printStatistics(PrintStream out, Result pResult,ReachedSet pReached) {
      out.println("env. app. falsified by BDD:      " + formatInt(falseByBDD));
    }

    private String formatInt(int val){
      return String.format("  %7d", val);
    }

    @Override
    public String getName() {
      return "RGFullyAbstractedManager";
    }
  }
}
