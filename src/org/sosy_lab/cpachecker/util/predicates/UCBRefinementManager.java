// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
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
   * @param allStatesTrace a concrete path in the ARG.
   * @param abstractionStatesTrace the list of abstraction states along the path.
   * @return The Counterexample, containing UCB predicates if successful
   */
  public CounterexampleTraceInfo buildCounterexampleTrace(
      final ARGPath allStatesTrace,
      final List<ARGState> abstractionStatesTrace,
      final BlockFormulas pFormulas)
      throws CPAException, InterruptedException {

    List<ARGState> trace = new ArrayList<>(abstractionStatesTrace);
    // Add root to the trace in order to compute WPs correctly as 'abstractionStatesTrace'
    // does not contain the root element.
    trace.add(0, allStatesTrace.getFirstState());

    // Compute weakest preconditions on the error trace
    // If the list is empty then the trace is feasible,
    // otherwise compute UCB predicates by refining the weakest-preconditions.
    // If the list is non-empty, then the first element is the first non false WP.
    List<BooleanFormula> preds = computeWeakestPreconditions(trace);

    if (preds == null) {
      return CounterexampleTraceInfo.feasibleNoModel(pFormulas.getFormulas());
    } else {
      preds = computeUCB(trace, pFormulas, preds);
      preds.addAll(
          0,
          Collections.nCopies(abstractionStatesTrace.size() - preds.size() - 1, bfmgr.makeTrue()));

      return CounterexampleTraceInfo.infeasible(preds);
    }
  }

  private List<BooleanFormula> computeWeakestPreconditions(final List<ARGState> pTrace)
      throws CPAException, InterruptedException {

    BooleanFormula wpre = bfmgr.makeTrue();
    List<BooleanFormula> wpres = new ArrayList<>();

    logger.log(Level.FINEST, "Calculate weakest precondition for the error trace.");

    // Ignore last state as it's the error state
    for (int i = pTrace.size() - 2; i >= 0; i--) {
      try {
        wpre = computeWeakestPrecondition(pTrace.get(i), pTrace.get(i + 1), wpre);

        PredicateAbstractState abstractState =
            AbstractStates.extractStateByType(pTrace.get(i), PredicateAbstractState.class);

        BooleanFormula stateFormula = abstractState.getAbstractionFormula().asFormula();

        if (solver.isUnsat(bfmgr.and(stateFormula, wpre))) {
          logger.log(
              Level.FINEST,
              "Abstract state is disjoint with the weakest precondition. Found spurious error trace"
                  + " suffix.");

          return wpres;
        }
        wpres.add(wpre);
      } catch (SolverException e) {
        throw new CPAException(e.getMessage());
      }
    }

    return null;
  }

  private BooleanFormula computeWeakestPrecondition(
      final ARGState src, final ARGState dst, final BooleanFormula postCondition)
      throws CPAException, InterruptedException {

    CFAEdge singleEdge = src.getEdgeToChild(dst);
    if (singleEdge != null) {
      return pfmgr.buildWeakestPrecondition(singleEdge, postCondition);
    }

    CFANode srcNode = AbstractStates.extractLocation(src);
    CFANode dstNode = AbstractStates.extractLocation(dst);

    if (srcNode != null && dstNode != null) {
      return computeWeakestPrecondition(srcNode, dstNode, postCondition, ImmutableSet.of());
    }

    return bfmgr.makeFalse();
  }

  private BooleanFormula computeWeakestPrecondition(
      final CFANode src,
      final CFANode dst,
      final BooleanFormula postCondition,
      final Set<CFANode> visitedNodes)
      throws CPAException, InterruptedException {

    Set<CFANode> visited = new HashSet<>(visitedNodes);
    visited.add(src);

    BooleanFormula res = bfmgr.makeFalse();

    for (CFAEdge edge : CFAUtils.leavingEdges(src)) {
      CFANode next = edge.getSuccessor();

      BooleanFormula wp = bfmgr.makeFalse();
      BooleanFormula post = bfmgr.makeFalse();

      if (next.equals(dst)) {
        post = postCondition;
      } else if (!visited.contains(next)) {
        post = computeWeakestPrecondition(next, dst, postCondition, visited);
      }

      if (!bfmgr.isFalse(post)) {
        wp = pfmgr.buildWeakestPrecondition(edge, post);
      }

      if (bfmgr.isFalse(res)) {
        res = wp;
      } else if (!bfmgr.isFalse(wp)) {
        res = bfmgr.or(res, wp);
      }
    }

    return res;
  }

  private List<BooleanFormula> computeUCB(
      final List<ARGState> pTrace, final BlockFormulas pFormulas, final List<BooleanFormula> wpres)
      throws CPAException, InterruptedException {

    logger.log(Level.FINEST, "Calculate UCB predicates for the spurious trace suffix.");

    // We transform every wp into ucb in-place
    List<BooleanFormula> ucbs = new ArrayList<>(wpres);
    Collections.reverse(ucbs);

    // WPs list does not contain the first (= false) and the last (= true) nodes,
    // while the pTrace contains the whole path, i.e. from the root to the error node
    int startStateIdx = pTrace.size() - ucbs.size() - 2;

    BooleanFormula pred = null;
    PredicateAbstractState curState, nextState;

    for (int i = startStateIdx; i < pTrace.size() - 2; i++) {
      curState = AbstractStates.extractStateByType(pTrace.get(i), PredicateAbstractState.class);
      nextState =
          AbstractStates.extractStateByType(pTrace.get(i + 1), PredicateAbstractState.class);

      if (pred == null) {
        pred = curState.getAbstractionFormula().asFormula();
      }

      // Previously computed predicate
      pred = fmgr.instantiate(pred, curState.getPathFormula().getSsa());

      // Compute edge post-condition
      BooleanFormula pf = pFormulas.getFormulas().get(i);

      // Weakest precondition in the target location
      BooleanFormula ucb =
          fmgr.instantiate(ucbs.get(i - startStateIdx), nextState.getPathFormula().getSsa());

      // Clauses of the precondition to be refined
      Set<BooleanFormula> ucbConj = bfmgr.toConjunctionArgs(ucb, true);

      // All clauses of the formula: p__i && sp__i+1 && wp__i+1
      Set<BooleanFormula> conjs = new HashSet<>();
      conjs.add(pred);
      conjs.add(pf);
      conjs.addAll(ucbConj);

      try {
        BooleanFormula ucf =
            fmgr.uninstantiate(
                solver.unsatCore(conjs).stream()
                    .filter(uc -> ucbConj.contains(uc))
                    .collect(bfmgr.toConjunction()));

        // Avoid (not true|false) predicates as the refinement strategy
        // does not recognize them as trivial (and does not skip)
        // and hence complains that they are not relevant
        if (bfmgr.isTrue(ucf)) {
          pred = bfmgr.makeFalse();
        } else if (bfmgr.isFalse(ucf)) {
          pred = bfmgr.makeTrue();
        } else {
          pred = bfmgr.not(ucf);
        }

        ucbs.set(i - startStateIdx, pred);

      } catch (SolverException e) {
        throw new CPAException(e.getMessage());
      }
    }

    return ucbs;
  }
}
