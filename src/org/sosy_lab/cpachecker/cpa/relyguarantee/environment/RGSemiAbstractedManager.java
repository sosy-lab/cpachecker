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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGVariables;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGSemiAbstracted;
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

/**
 * Manager for semi-abstracted environmental transitions.
 */
public class RGSemiAbstractedManager extends RGEnvTransitionManagerFactory {

  private final FormulaManager fManager;
  private final PathFormulaManager pfManager;
  private final RGAbstractionManager absManager;
  private final SSAMapManager ssaManager;
  private final TheoremProver thmProver;
  private final RegionManager rManager;
  private final RGVariables variables;

  private final LogManager logger;
  private final Stats stats;

  protected RGSemiAbstractedManager(FormulaManager fManager, PathFormulaManager pfManager, RGAbstractionManager absManager, SSAMapManager ssaManager, TheoremProver thmProver, RegionManager rManager, RGVariables variables, Configuration config,  LogManager logger) {
    this.fManager = fManager;
    this.pfManager = pfManager;
    this.absManager  = absManager;
    this.ssaManager = ssaManager;
    this.thmProver = thmProver;
    this.rManager  = rManager;
    this.variables = variables;
    this.logger = logger;
    this.stats = new Stats();
  }



  @Override
  public RGSemiAbstracted generateEnvTransition(RGEnvCandidate cand, Collection<AbstractionPredicate> globalPreds, Multimap<CFANode, AbstractionPredicate> localPreds) {
    ARTElement sourceART = cand.getSuccessor();
    CFANode loc = sourceART.retrieveLocationElement().getLocationNode();

    // get predicates for abstraction
    Set<AbstractionPredicate> preds = new LinkedHashSet<AbstractionPredicate>(globalPreds);
    preds.addAll(localPreds.get(loc));

    // abstract
    AbstractionFormula abs = cand.getRgElement().getAbstractionFormula();
    PathFormula pf = cand.getRgElement().getPathFormula();
    AbstractionFormula aFilter = absManager.buildAbstraction(abs, pf, preds);
    Formula aPred = fManager.uninstantiate(aFilter.asFormula());
    Region aPredReg = aFilter.asRegion();

    SSAMap ssa = cand.getRgElement().getPathFormula().getSsa();
    CFAEdge operation = cand.getOperation();
    RGSemiAbstracted sa = new RGSemiAbstracted(aPred, aPredReg, ssa, operation, cand.getElement(), cand.getSuccessor(), cand.getTid());
    return sa;
  }

  @Override
  public PathFormula formulaForAbstraction(RGAbstractElement elem ,RGEnvTransition et, int unique) throws CPATransferException {
    RGSemiAbstracted sa = (RGSemiAbstracted) et;
    PathFormula pf = elem.getPathFormula();
    AbstractionFormula abs = elem.getAbstractionFormula();

    /* if path formula is true, then BDDs can detect unsatisfiable result */
    if (pf.getFormula().isTrue()){
      boolean isFalse = checkByBDD(sa, abs);
      if (isFalse){
        stats.falseByBDD++;
        return pfManager.makeFalsePathFormula();
      }
    }

    // instantiate the precondition to the ssa map of the
    Formula prec = sa.getAbstractPrecondition();
    prec = fManager.instantiate(prec, pf.getSsa());
    PathFormula precPf = new PathFormula(prec, pf.getSsa(), 0);

    // apply the operation
    PathFormula appPf = pfManager.makeAnd(precPf, sa.getOperation());
    return appPf;
  }

  @Override
  public PathFormula formulaForRefinement(RGAbstractElement elem, RGEnvTransition et, int unique) throws CPATransferException {
    RGSemiAbstracted sa = (RGSemiAbstracted) et;
    PathFormula pf = elem.getPathFormula();
    AbstractionFormula abs = elem.getAbstractionFormula();
    SSAMap lSSA = pf.getSsa();
    SSAMap etSSA = sa.getSsa();

    /* if path formula is true, then BDDs can detect unsatisfiable result */
    if (pf.getFormula().isTrue()){
      boolean isFalse = checkByBDD(sa, abs);
      if (isFalse){
        // no stats here, since they are probably redundant with formulaForAbstraction
        return pfManager.makeFalsePathFormula();
      }
    }

    // rename the transition's SSA to unique
    Map<Integer, Integer> rMap = new HashMap<Integer, Integer>(1);
    rMap.put(-1, unique);
    etSSA = ssaManager.changePrimeNo(etSSA, rMap);

    // build equalities over the local path formula and the precondition
    PathFormula eqPf = pfManager.makePrimedEqualities(lSSA, -1, etSSA, unique);

    // apply the operation
    PathFormula appPf = pfManager.makeAnd(eqPf, sa.getOperation());

    return appPf;
  }

  private boolean checkByBDD(RGSemiAbstracted sa, AbstractionFormula abs) {
    Region rEt = sa.getAbstractPreconditionRegion();
    Region rElem = abs.asRegion();
    Region rAnd = rManager.makeAnd(rElem, rEt);
    return rManager.isFalse(rAnd);
  }

  @Override
  public boolean isLessOrEqual(RGEnvTransition et1, RGEnvTransition et2) {
    RGSemiAbstracted sa1 = (RGSemiAbstracted) et1;
    RGSemiAbstracted sa2 = (RGSemiAbstracted) et2;

    CFAEdge op1 = sa1.getOperation();
    CFAEdge op2 = sa2.getOperation();

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

    Formula prec1 = sa1.getAbstractPrecondition();
    Formula prec2 = sa2.getAbstractPrecondition();

    if (prec1.isFalse() || prec2.isTrue()){
      return true;
    }

    Formula fimpl = fManager.makeAnd(prec1, fManager.makeNot(prec2));
    thmProver.init();
    boolean valid = thmProver.isUnsat(fimpl);
    thmProver.reset();

    return valid;
  }

  @Override
  public boolean isBottom(RGEnvTransition et) {
    RGSemiAbstracted sa = (RGSemiAbstracted) et;

    return sa.getAbstractPrecondition().isFalse();
  }

  @Override
  public void collectStatistics(Collection<Statistics> scoll) {
    scoll.add(stats);
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
