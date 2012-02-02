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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
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
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransitionType;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGFullyAbstracted;
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

import com.google.common.collect.Multimap;

/**
 * Manager for fully-abstracted environmental transitions.
 */
public class RGFullyAbstractedManager extends RGEnvTransitionManagerFactory {

  private final FormulaManager fManager;
  private final PathFormulaManager pfManager;
  private final RGAbstractionManager absManager;
  private final SSAMapManager ssaManager;
  private final TheoremProver thmProver;
  private final RegionManager rManager;
  private final RGVariables variables;

  private final LogManager logger;
  private final Stats stats;


  protected RGFullyAbstractedManager(FormulaManager fManager, PathFormulaManager pfManager, RGAbstractionManager absManager, SSAMapManager ssaManager, TheoremProver thmProver, RegionManager rManager, RGVariables variables, Configuration config, LogManager logger){
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
  public RGFullyAbstracted generateEnvTransition(RGEnvCandidate cand, Collection<AbstractionPredicate> globalPreds, Multimap<CFANode, AbstractionPredicate> localPreds)  {
    ARTElement sourceART = cand.getSuccessor();
    CFANode loc = sourceART.retrieveLocationElement().getLocationNode();

    // get predicates for abstraction
    Set<AbstractionPredicate> preds = new LinkedHashSet<AbstractionPredicate>(globalPreds);
    preds.addAll(localPreds.get(loc));

    // get the predicates for the transition
    int sourceTid = cand.getTid();
    PathFormula oldPf = cand.getRgElement().getPathFormula();
    AbstractionFormula oldAbs = cand.getRgElement().getAbstractionFormula();
    PathFormula newPf = null;

    // compute the sucessor's path formula
    try {
      newPf = pfManager.makeAnd(oldPf, cand.getOperation());
    } catch (CPATransferException e) {
      e.printStackTrace();
    }

    // increment indexes of variables global and local to this thread by 1, mimimal index is 2
    Set<String> vars = new HashSet<String>(variables.globalVars);
    vars.addAll(variables.localVars.get(sourceTid));
    SSAMap oldSsa = oldPf.getSsa();
    SSAMap newSsa = ssaManager.incrementMap(oldSsa, vars, 1);

    // create a formula, where every index is increased - either by operation or by equivalence
    Pair<Pair<Formula, Formula>, SSAMap> equivs = ssaManager.mergeSSAMaps(newPf.getSsa(), newSsa);
    Formula newF = fManager.makeAnd(newPf.getFormula(), equivs.getFirst().getFirst());
    newPf = new PathFormula(newF, newSsa, newPf.getLength());

    // abstract
    AbstractionFormula newAbs = absManager.buildNextValAbstraction(oldAbs, oldPf, newPf, preds, sourceTid);
    SSAMap highSSA = newAbs.asPathFormula().getSsa();
    return new RGFullyAbstracted(newAbs.asFormula(), newAbs.asRegion(), oldSsa, highSSA, cand.getSuccessor(), sourceTid);
  }


  @Override
  public PathFormula formulaForAbstraction(RGAbstractElement elem, RGEnvTransition et, int unique) {

    RGFullyAbstracted fa = (RGFullyAbstracted) et;
    AbstractionFormula abs = elem.getAbstractionFormula();
    PathFormula pf = elem.getPathFormula();

    /* if path formula is true, then BDDs can detect unsatisfiable result */
    if (pf.getFormula().isTrue()){
      boolean isFalse = checkByBDD(fa, abs);
      if (isFalse){
        stats.falseByBDD++;
        return pfManager.makeFalsePathFormula();
      }
    }

    // rename filter to tid
    Formula filter = fa.getAbstractTransition();

    // increment all indexes that the transition can change by 1
    Set<String> nlVars = new HashSet<String>(variables.globalVars);
    int sourceTid = fa.getTid();
    nlVars.addAll(variables.localVars.get(sourceTid));
    SSAMap lowSSA = pf.getSsa();
    SSAMap highSSA = ssaManager.incrementMap(lowSSA, nlVars, 1);

    // instantiate the filter
    Formula iFilter = fManager.instantiateNextValue(filter, lowSSA, highSSA);
    PathFormula appPf = new PathFormula(iFilter, highSSA, 1);

    return appPf;
  }


  @Override
  public PathFormula formulaForRefinement(RGAbstractElement elem, RGEnvTransition et, int unique) {
      RGFullyAbstracted fa = (RGFullyAbstracted) et;
      AbstractionFormula abs = elem.getAbstractionFormula();
      PathFormula pf = elem.getPathFormula();

      /* if path formula is true, then BDDs can detect unsatisfiable result */
      if (pf.getFormula().isTrue()){
        boolean isFalse = checkByBDD(fa, abs);
        if (isFalse){
          // no stats here, since they are probably redundant with formulaForAbstraction
          return pfManager.makeFalsePathFormula();
        }
      }

      SSAMap lowSSA = pf.getSsa();
      Map<Integer, Integer> rMap = new HashMap<Integer, Integer>(1);
      rMap.put(-1, unique);
      SSAMap gSsa = ssaManager.changePrimeNo(fa.getLowSSA(), rMap);

      // build equalities between the local variables and the variables that generated the transition
      PathFormula lowPf = pfManager.makePrimedEqualities(lowSSA, -1, gSsa, unique);

      // increment all indexes that the transition can change by 1
      Set<String> nlVars = new HashSet<String>(variables.globalVars);
      int sourceTid = fa.getTid();
      nlVars.addAll(variables.localVars.get(sourceTid));

      SSAMap highSSA = ssaManager.incrementMap(lowSSA, nlVars, 1);

      SSAMap fSsa = ssaManager.changePrimeNo(fa.getHighSSA(), rMap);

      // build equalities between the highest indexes of the instantiated filter
      PathFormula hiPf = pfManager.makePrimedEqualities(highSSA, -1, fSsa, unique);

      Formula appF = fManager.makeAnd(hiPf.getFormula(), lowPf.getFormula());
      PathFormula appPf = new PathFormula(appF, hiPf.getSsa(), 0);


    return appPf;
  }

  private boolean checkByBDD(RGFullyAbstracted fa, AbstractionFormula abs) {
    Region rEt = fa.getAbstractTransitionRegion();
    Region rElem = abs.asRegion();
    Region rAnd = rManager.makeAnd(rElem, rEt);
    return rManager.isFalse(rAnd);
  }


  @Override
  public boolean isLessOrEqual(RGEnvTransition et1, RGEnvTransition et2) {
    assert et1.getRGType() == RGEnvTransitionType.FullyAbstracted;
    assert et1.getRGType() == RGEnvTransitionType.FullyAbstracted;

    RGFullyAbstracted efa1 = (RGFullyAbstracted) et1;
    RGFullyAbstracted efa2 = (RGFullyAbstracted) et2;
    Region r1 = efa1.getAbstractTransitionRegion();
    Region r2 = efa2.getAbstractTransitionRegion();

    return rManager.entails(r1, r2);
  }

  @Override
  public boolean isBottom(RGEnvTransition et) {
    RGFullyAbstracted fa = (RGFullyAbstracted) et;

    return fa.getAbstractTransition().isFalse();
  }


  @Override
  public void collectStatistics(Collection<Statistics> scoll) {
    scoll.add(stats);
  }

  public static class Stats implements Statistics {

    public int falseByBDD = 0;

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
