/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.refinement;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.DependencyTrackerState;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.BottomPolicy;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.ConglomeratePolicy;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.Edge;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.PolicyAlgebra;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.formula.FormulaRelation;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.formula.FormulaState;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.SecurityClasses;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.DependencyPrecision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.ImplicitDependencyPrecision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.SetUtil;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InfeasibleCounterexampleException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.predicates.smt.TaggedFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public class NIRefiner implements Refiner {

  @SuppressWarnings("unused")
  private ConfigurableProgramAnalysis cpa;

  public NIRefiner(ConfigurableProgramAnalysis pCpa) {
    this.cpa = pCpa;
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    @SuppressWarnings("unused")
    final ARGState lastElement = (ARGState) pReached.getLastState();
    List<AbstractState> targets =
        FluentIterable.from(pReached).filter(AbstractStates.IS_TARGET_STATE).toList();

    boolean result = false;
    for (AbstractState state : targets) {
      result = isSpurious(pReached, state);
    }

    return result;
  }



  private boolean isSpurious(ReachedSet pReached, AbstractState state)
      throws InfeasibleCounterexampleException, RefinementFailedException {
    boolean result = false;

    ARGState argstate = (ARGState) state;
    Precision prec = pReached.getPrecision(state);
    ImplicitDependencyPrecision dprec = null;
    //Collect relevant data
    FormulaRelation tr = null;
    TaggedFormulaManager formulaManager = null;
    Solver solver = null;
    FormulaState fstate = null;
    //Policy
    ConglomeratePolicy<SecurityClasses> pol = null;
    Map<Variable, SecurityClasses> asc = null;
    Map<Variable, SortedSet<SecurityClasses>> csc = null;
    Map<Variable, SortedSet<Variable>> dep = null;

    CFANode node = null;
    ARGPath path = ARGUtils.getOnePathTo(argstate);


    dprec = Precisions.extractPrecisionByType(prec, ImplicitDependencyPrecision.class);
    List<ARGState> list = path.asStatesList();

    argloop: for (ARGState argstate2 : list) {
      boolean shallstop = false;
      CompositeState wstate = (CompositeState) argstate2.getWrappedState();
      for (AbstractState cstate : wstate.getWrappedStates()) {
        if (cstate instanceof LocationState) {
          LocationState lstate = (LocationState) cstate;
          node = lstate.getLocationNode();
        }
        if (cstate instanceof DependencyTrackerState) {
          DependencyTrackerState dstate = (DependencyTrackerState) cstate;
          dep = dstate.getDependencies();
          pol = dprec.getPolicy();
          csc = new TreeMap<>();
          for (Entry<Variable, SortedSet<Variable>> entry : dep.entrySet()) {
            Variable var = entry.getKey();
            SortedSet<Variable> value = entry.getValue();
            SortedSet<SecurityClasses> cscV = new TreeSet<>();
            for (Variable rVar : value) {
              cscV.add(dprec.getSC(rVar));
            }
            csc.put(var, cscV);
          }

        }
        if (cstate instanceof FormulaState) {
          fstate = (FormulaState) cstate;
          tr = fstate.getPr();
          formulaManager = tr.getFormulaManager();
          solver = tr.getSolver();
        }
      }
      //1. Compute N(POL)
      SortedSet<Edge<SecurityClasses>> nset = computeNPol(pol);
      //2. Compute Candidate Security Classes
      Map<Edge<SecurityClasses>, SortedSet<Edge<SecurityClasses>>> cSCMapping =
          computeCandidateSC(nset, pol);
      //3. Compute Violation Set
      Map<Variable, Edge<SecurityClasses>> vios = computeViolations(pol, asc, csc);

      //4. Test for each Candidate
      //Compute Init-Formula Vars
      for (Entry<Variable, Edge<SecurityClasses>> vio : vios.entrySet()) {
        boolean refinementnecessary = false;
        Variable viovar = vio.getKey();
        List<Variable> rvars = new ArrayList<>();
        rvars.add(viovar);
        Edge<SecurityClasses> vioedge = vio.getValue();
        SortedSet<SecurityClasses> refinementSC =
            SetUtil.union(vio.getValue().getTo(), new TreeSet<>());

        for (Edge<SecurityClasses> candidate : cSCMapping.get(vioedge)) {

          List<Variable> lvars = new ArrayList<>();
          lvars.add(viovar);
          SortedSet<SecurityClasses> candidateSC = candidate.getTo();
          for (Variable lvar : dep.get(viovar)) {
            SecurityClasses lvarsc = asc.get(lvar);
            if (candidateSC.contains(lvarsc)) {
              lvars.add(lvar);
            }
          }
          //4.1 Compute Formula
          BooleanFormula formula = null;
          try {
            formula = createFormula(formulaManager, lvars, rvars, fstate);
          } catch (InterruptedException e) {

          }

          //4.2 Evaluate Formula
          boolean solverresult = true;
          try {
            solverresult = !solver.isUnsat(formula);
            if (!solverresult) {

            } else {

            }
          } catch (InterruptedException e) {

          } catch (SolverException e) {

          }
          //4.3 Determine Difference for UNSAT TODO
          if (!solverresult) {
            refinementSC = SetUtil.intersect(refinementSC, candidateSC);
          }

          refinementnecessary = refinementnecessary || !solverresult;
        }
        if (refinementnecessary) {
          SortedSet<Variable> moreprecisedep = new TreeSet<>();
          Iterator<Variable> it = dep.get(viovar).iterator();
          while (it.hasNext()) {
            Variable elem = it.next();
            if (refinementSC.contains(asc.get(elem))) {
              moreprecisedep.add(elem);
            }
          }
          dprec.addRefinementInfo(node, viovar, moreprecisedep);
          shallstop = true;
          result = true;
          break argloop;
        }
      }
    }

    //4.5. Modify ARG
    if (result) {
      modifyReachSet(pReached, dprec);
    } else {
      //        ARGPath path=ARGUtils.getOnePathTo(argstate);
      //        throw new RefinementFailedException(RefinementFailedException.Reason.InfeasibleCounterexample, path);
    }

    return result;
  }


  private Map<Variable, Edge<SecurityClasses>> computeViolations(ConglomeratePolicy policy,
      Map<Variable, SecurityClasses> allowedsecurityclassmapping,
      Map<Variable, SortedSet<SecurityClasses>> contentsecurityclasslevels) {
    Map<Variable, Edge<SecurityClasses>> result = new TreeMap<>();
    for (Entry<Variable, SortedSet<SecurityClasses>> entry : contentsecurityclasslevels
        .entrySet()) {
      Variable var = entry.getKey();
      SecurityClasses sink = allowedsecurityclassmapping.get(var);
      SortedSet<SecurityClasses> source = contentsecurityclasslevels.get(var);
      Edge<SecurityClasses> edge = new Edge<>(sink, source);
      if (!(policy.getEdges().contains(edge))) {
        result.put(var, edge);
      }
    }
    return result;
  }

  private List<Variable> computeViolationSet(ConglomeratePolicy policy,
      Map<Variable, SecurityClasses> allowedsecurityclassmapping,
      Map<Variable, SortedSet<SecurityClasses>> contentsecurityclasslevels) {
    List<Variable> returnset = new ArrayList<Variable>();
    for (Entry<Variable, SortedSet<SecurityClasses>> entry : contentsecurityclasslevels
        .entrySet()) {
      Variable var = entry.getKey();
      SecurityClasses sink = allowedsecurityclassmapping.get(var);
      SortedSet<SecurityClasses> source = contentsecurityclasslevels.get(var);
      Edge<SecurityClasses> edge = new Edge<>(sink, source);
      if (!(policy.getEdges().contains(edge))) {
        returnset.add(var);
      }
    }
    return returnset;
  }

  private SortedSet<Edge<SecurityClasses>> computeNPol(ConglomeratePolicy<SecurityClasses> policy) {
    PolicyAlgebra alg = new PolicyAlgebra();
    ConglomeratePolicy toppolicy = new BottomPolicy<SecurityClasses>(alg.getDomain(policy));
    SortedSet<Edge<SecurityClasses>> nset =
        SetUtil.setminus(toppolicy.getEdges(), policy.getEdges());
    return nset;
  }


  private Map<Edge<SecurityClasses>, SortedSet<Edge<SecurityClasses>>> computeCandidateSC(
      SortedSet<Edge<SecurityClasses>> nset, ConglomeratePolicy<SecurityClasses> pol) {
    Map<Edge<SecurityClasses>, SortedSet<Edge<SecurityClasses>>> result = new TreeMap<>();

    for (Edge<SecurityClasses> nedge : nset) {
      SortedSet<Edge<SecurityClasses>> max = new TreeSet<>();
      SecurityClasses nfrom = nedge.getFrom();
      SortedSet<SecurityClasses> nto = nedge.getTo();
      for (Edge<SecurityClasses> pedge : pol.getEdges()) {
        SecurityClasses pfrom = pedge.getFrom();
        SortedSet<SecurityClasses> pto = pedge.getTo();
        if (pfrom.equals(nfrom)) {
          //Subset
          if ((SetUtil.intersect(nto, pto).size()) == (pto.size())) {
            //PUT result
            boolean add = true;
            Iterator<Edge<SecurityClasses>> maxit = max.iterator();
            while (maxit.hasNext()) {
              Edge<SecurityClasses> next = maxit.next();
              SortedSet<SecurityClasses> rto = next.getTo();
              if ((SetUtil.intersect(pto, rto).size()) == (rto.size())) {
                //Take newone remove oldone
                max.remove(next);
              }
              if ((SetUtil.intersect(pto, rto).size()) == (pto.size())) {
                add = false;
              }

            }
            if (add) {
              max.add(pedge);
            }
          }
        }
      }
      result.put(nedge, max);
    }
    return result;
  }

  private BooleanFormula createFormula(TaggedFormulaManager formulaManager, List<Variable> lvars,
      List<Variable> rvars, FormulaState cstate) throws InterruptedException {
    FormulaRelation tr = cstate.getPr();

    BooleanFormula newformula = formulaManager.simplify(formulaManager
        .makeAnd(cstate.getPathFormula(1).getFormula(), cstate.getPathFormula(2).getFormula()));

    BooleanFormula init = tr.makeEqualforBothPaths(lvars, 2);
    BooleanFormula end = tr.makeEqualforBothPaths(rvars, cstate.getPathFormula(1).getSsa());
    end = formulaManager.makeNot(end);


    newformula = formulaManager.makeAnd(newformula, init);
    newformula = formulaManager.makeAnd(newformula, end);

    return newformula;
  }

  private void modifyReachSet(ReachedSet pReached, DependencyPrecision newdprec) {


    AbstractState astate = pReached.getFirstState();
    ARGState argstate = (ARGState) astate;
    CompositeState wstate = (CompositeState) argstate.getWrappedState();
    Precision oldprec = pReached.getPrecision(astate);
    Precision newprec = Precisions.replaceByType(oldprec, newdprec,
        Predicates.instanceOf(DependencyPrecision.class));

    pReached.clear();
    pReached.getWaitlist().clear();
    pReached.add(new ARGState(wstate, null), newprec);
  }

}
