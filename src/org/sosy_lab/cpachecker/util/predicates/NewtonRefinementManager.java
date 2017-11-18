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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Class designed to perform a Newton based refinement
 *
 * based on:
 *
 * "Generating Abstract Explanations of Spurious Counterexamples in C Programs" by Thomas Ball
 * Sriram K. Rajamani
 *
 * "Craig vs. Newton in Software Model Checking" by Daniel Dietsch, Matthias Heizmann, Betim Musa,
 * Alexander Nutz, Andreas Podelski
 */
@SuppressWarnings("options")
@Options(prefix = "cpa.predicate.refinement")
public class NewtonRefinementManager {
  private final LogManager logger;
  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final PathFormulaManager pfmgr;
  public NewtonRefinementManager(
      Configuration pConfig,
      LogManager pLogger,
      Solver pSolver,
      PathFormulaManager pPfmgr)
      throws InvalidConfigurationException {

    pConfig.inject(this, NewtonRefinementManager.class);
    logger = pLogger;
    solver = pSolver;
    fmgr = solver.getFormulaManager();
    pfmgr = pPfmgr;
  }

  /**
   * Creates the CounterexampleTraceInfo for the given error path based on the Newton-based
   * refinement approach.
   *
   * The counterexample should hold pseudo-interpolants based on StrongestPostCondition performed by
   * Newton.
   *
   * @param pAllStatesTrace The error path
   * @param pFormulas The Block formulas computed in previous step
   * @return The Counterexample, containing pseudo-interpolants if successful
   */
  public CounterexampleTraceInfo
      buildCounterexampleTrace(ARGPath pAllStatesTrace, BlockFormulas pFormulas)
          throws CPAException, InterruptedException {
    if (isFeasible(pFormulas.getFormulas())) {
      // Create feasible Counterexampletrace
      return CounterexampleTraceInfo.feasible(
          pFormulas.getFormulas(),
          ImmutableList.<ValueAssignment>of(),
          ImmutableMap.<Integer, Boolean>of());
    } else {
      // TODO: Compute the infeasible core
      // TODO: Manipulate Path in such way, that unnecessary parts of the predicates are removed

      // Calculate StrongestPost
      List<BooleanFormula> predicates =
          this.calculateStrongestPostCondition(pFormulas, pAllStatesTrace);

      // TODO: Remove parts of the predicates that are not future live
      return CounterexampleTraceInfo.infeasible(predicates);
    }
  }

  /**
   * Check the feasibility of the traceformula
   *
   * @param pFormulas The path formula
   * @return <code>true</code> if the trace is feasible
   * @throws CPAException Thrown if the solver failed while proving unsatisfiability
   * @throws InterruptedException If the Excecution is interrupted
   */
  private boolean isFeasible(List<BooleanFormula> pFormulas)
      throws CPAException, InterruptedException {
    boolean isFeasible;
    try (ProverEnvironment prover = solver.newProverEnvironment()) {
      for (BooleanFormula formula : pFormulas) {
        prover.push(formula);
      }
      isFeasible = !prover.isUnsat();
    } catch (SolverException e) {
      throw new CPAException(
          "Prover failed while proving unsatisfiability in Newtonrefinement.",
          e);
    }
    return isFeasible;
  }

  /**
   * Calculates the StrongestPostCondition at all states on a error-trace.
   *
   * When applied to the Predicate states, they assure that the same error-trace won't occur again.
   *
   * @param pFormulas The Block-formulas as computed before
   * @param allStatesTrace The trace to the error location
   * @return A list of BooleanFormulas holding the strongest postcondition of each edge on the path
   * @throws CPAException In case the Algorithm failed unexpected
   * @throws InterruptedException In case of interruption
   */
  private List<BooleanFormula>
      calculateStrongestPostCondition(BlockFormulas pFormulas, ARGPath allStatesTrace)
          throws CPAException, InterruptedException {
    // First Predicate is always true
    BooleanFormula preCondition = fmgr.getBooleanFormulaManager().makeTrue();
    PathIterator pathiterator = allStatesTrace.fullPathIterator();
    Iterator<BooleanFormula> formulaIterator = pFormulas.getFormulas().iterator();

    // Initialize the predicate list(first preCondition not assigned as always true and not needed
    // in CounterexampleTraceinfo
    List<BooleanFormula> predicates = new ArrayList<>();

    // Initialize the path
    List<CFAEdge> path = new ArrayList<>();
    while (pathiterator.hasNext()) {
      assert formulaIterator.hasNext();
      BooleanFormula blockFormula = formulaIterator.next();
      pfmgr.makeFormulaForPath(path);

      // Get the edges between this state and and the surrounding states
      List<CFAEdge> edges = new ArrayList<>();
      edges.add(pathiterator.getOutgoingEdge());
      pathiterator.advance();

      while (!pathiterator.isPositionWithState()) {
        edges.add(pathiterator.getOutgoingEdge());
        assert pathiterator.hasNext();
        pathiterator.advance();
      }

      // Get the pathformula
      path.addAll(edges);
      PathFormula pathFormula = pfmgr.makeFormulaForPath(path);

      // Build the strongest Postcondition based on the type of the edge
      BooleanFormula postCondition = preCondition;

      for (CFAEdge edge : edges) {
        if (edge == null) {
          if (fmgr.getBooleanFormulaManager().isTrue(blockFormula)) {
            postCondition = preCondition;
          }else {
            logger.log(Level.SEVERE, "Could not determine the postcondition, as CFAEdge is null.");
            throw new CPAException(
                "NewtonRefinement-Postcondition failed: Could not determine the postcondition of, as CFAEdge is null.");
          }
        } else if (edge.getEdgeType() == CFAEdgeType.AssumeEdge) {
          postCondition = fmgr.makeAnd(preCondition, blockFormula);
        } else if (edge.getEdgeType() == CFAEdgeType.StatementEdge) {

          BooleanFormula toExist = fmgr.makeAnd(blockFormula, preCondition);
          try {
            // Use Existential-Quantifier-Elimination on dead variables
            // FIXME: Fails while quantification with Z3(in apply tactics);
            postCondition =
                fmgr.eliminateDeadVariables(
                    toExist,
                    pathFormula.getSsa());
          } catch (SolverException e) {
            // logger.log(Level.SEVERE, "Solver failed to compute existence-quantor.");
            throw new CPAException("Solver failed to compute existence-quantor.", e);
          }
        } else {
          // TODO: Determine if it is necessary to do something
          logger.log(
              java.util.logging.Level.INFO,
              "Unhandled EdgeType: "
                  + edge.getEdgeType()
                  + " with formula :"
                  + pfmgr.makeAnd(pfmgr.makeEmptyPathFormula(), edge));
          postCondition = preCondition;
        }
        // Set postCondition as preCondition for next loop iteration
        preCondition = postCondition;
      }

      // If we are in the last state assert unsatisfiability
      if (pathiterator.hasNext()) {
        predicates.add(postCondition);
      } else {
        try {
          assert solver.isUnsat(postCondition);
        } catch (SolverException e) {
          // logger.log(Level.SEVERE, "Solver failed to solve the unsatisfiable last predicate.");
          throw new CPAException("Solver failed to solve the unsatisfiable last predicate", e);
        }
      }

    }
    return ImmutableList.copyOf(predicates);
  }
}
