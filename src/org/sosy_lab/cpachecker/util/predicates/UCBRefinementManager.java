/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.SolverException;

public class UCBRefinementManager {
  private final LogManager logger;
  private final Solver solver;
  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;

  public UCBRefinementManager(LogManager pLogger, Solver pSolver, PathFormulaManager pPfmgr) {
    logger = pLogger;
    solver = pSolver;
    pfmgr = pPfmgr;
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
  }

  /**
   * Creates the CounterexampleTraceInfo for the given error path based on the UCB refinement
   * approach.
   *
   * <p>The counterexample contains UCB predicates if it is infeasible.
   *
   * @param pAbstractionStatesTrace The error path
   * @return The Counterexample, containing UCB predicates if successful
   */
  public CounterexampleTraceInfo buildCounterexampleTrace(
      final List<ARGState> pAbstractionStatesTrace, final BlockFormulas pFormulas)
      throws CPAException, InterruptedException {

    // Compute weakest preconditions on the error trace
    // If the list is empty then the trace is feasible,
    // otherwise compute UCB predicates by refining the weakest-preconditions
    List<BooleanFormula> preds = computeWeakestPreconditions(pAbstractionStatesTrace);

    if (preds == null) {
      return CounterexampleTraceInfo.feasible(
          pFormulas.getFormulas(),
          ImmutableList.<ValueAssignment>of(),
          ImmutableMap.<Integer, Boolean>of());
    } else {
      preds = computeUCB(pAbstractionStatesTrace, preds);
      preds.addAll(
          0,
          Collections.nCopies(pAbstractionStatesTrace.size() - preds.size() - 1, bfmgr.makeTrue()));

      return CounterexampleTraceInfo.infeasible(preds);
    }
  }

  private List<BooleanFormula> computeWeakestPreconditions(final List<ARGState> pTrace)
      throws CPAException, InterruptedException {
    List<BooleanFormula> wpres = new ArrayList<>();
    BooleanFormula wpre = bfmgr.makeTrue();

    logger.log(Level.FINEST, "Calculate weakest precondition for the error trace.");

    // Ignore last state as it's the error state
    for (int i = pTrace.size() - 2; i >= 0; i--) {
      try {
        // TODO: support basic block aggregation
        ARGState state = pTrace.get(i);
        CFAEdge edge = state.getEdgeToChild(pTrace.get(i + 1));

        wpre = pfmgr.buildWeakestPrecondition(edge, wpre);

        PredicateAbstractState astate =
            AbstractStates.extractStateByType(state, PredicateAbstractState.class);

        BooleanFormula stateFormula = astate.getAbstractionFormula().asFormula();

        if (solver.isUnsat(bfmgr.and(stateFormula, wpre))) {
          logger.log(
              Level.FINEST,
              "Abstract state is disjoint with the weakest precondition. Found spurious error trace suffix.");

          return wpres;
        }
        wpres.add(wpre);
      } catch (SolverException e) {
        throw new CPAException(e.getMessage());
      }
    }

    return null;
  }

  private List<BooleanFormula> computeUCB(
      final List<ARGState> pTrace, final List<BooleanFormula> wpres)
      throws CPAException, InterruptedException {

    logger.log(Level.FINEST, "Calculate UCB predicates for the spurious trace suffix.");

    List<BooleanFormula> ucbs = new ArrayList<>(wpres);
    Collections.reverse(ucbs);

    int startStateIdx = pTrace.size() - ucbs.size() - 2;

    PredicateAbstractState state =
        AbstractStates.extractStateByType(pTrace.get(startStateIdx), PredicateAbstractState.class);
    BooleanFormula pred = state.getAbstractionFormula().asFormula();

    for(int i = startStateIdx; i < pTrace.size() - 2; i++) {
      SSAMap ssa = SSAMap.emptySSAMap().withDefault(1);

      // Previously computed predicate
      pred = fmgr.instantiate(pred, ssa);

      // Compute edge postcondition
      PathFormula pf =
          new PathFormula(bfmgr.makeTrue(), ssa, PointerTargetSet.emptyPointerTargetSet(), 0);
      CFAEdge edge = pTrace.get(i).getEdgeToChild(pTrace.get(i + 1));
      pf = pfmgr.makeAnd(pf, edge);

      // Weakest precondition in the target location
      BooleanFormula ucb = fmgr.instantiate(ucbs.get(i - startStateIdx), pf.getSsa());

      // Clauses of the precondition to be refined
      Set<BooleanFormula> ucbConj = bfmgr.toConjunctionArgs(ucb, true);

      // All clauses of the formula: p_i && spost_i+1 && wpre_i+1
      Set<BooleanFormula> conjs = new HashSet<>();
      conjs.add(pred);
      conjs.add(pf.getFormula());
      conjs.addAll(ucbConj);

      try {
        List<BooleanFormula> ucore = solver.unsatCore(conjs);
        ucore = ucore.stream().filter(uc -> ucbConj.contains(uc)).collect(Collectors.toList());
        pred = bfmgr.not(fmgr.uninstantiate(bfmgr.and(ucore)));
        ucbs.set(i - startStateIdx, pred);

      } catch (SolverException e) {
        throw new CPAException(e.getMessage());
      }
    }


    return ucbs;
  }
}
