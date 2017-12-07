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
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
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
public class NewtonRefinementManager {
  private final LogManager logger;
  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final PathFormulaManager pfmgr;

  // TODO: make to an option
  private static final boolean USE_UNSAT_CORE = true;

  public NewtonRefinementManager(
      LogManager pLogger,
      Solver pSolver,
      PathFormulaManager pPfmgr) {
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
      // Create the list of pathLocations(holding all relevant data)
      List<PathLocation> pathLocations = this.buildPathLocationList(pAllStatesTrace);

      Optional<List<BooleanFormula>> unsatCore;

      // Only compute if unsatCoreOption is set
      if (USE_UNSAT_CORE) {
        unsatCore = Optional.of(computeUnsatCore(pathLocations));
      } else {
        unsatCore = Optional.empty();
      }
      // TODO: Manipulate Path in such way, that unnecessary parts of the predicates are removed

      // Calculate StrongestPost
      List<BooleanFormula> predicates =
          this.calculateStrongestPostCondition(pathLocations, unsatCore);

      // TODO: Remove parts of the predicates that are not future live
      return CounterexampleTraceInfo.infeasible(predicates);
    }
  }

  /**
   * Compute the Unsatisfiable core as a list of BooleanFormulas
   *
   * @param pPathLocations The PathLocations on the infeasible trace
   * @return A List of BooleanFormulas
   * @throws CPAException Thrown if the solver failed while calculating unsatisfiable core
   * @throws InterruptedException If the Excecution is interrupted
   */
  private List<BooleanFormula> computeUnsatCore(List<PathLocation> pPathLocations)
      throws CPAException, InterruptedException {

    // Compute the conjunction of the pathFormulas
    BooleanFormula completePathFormula = fmgr.getBooleanFormulaManager().makeTrue();
    for (PathLocation loc : pPathLocations) {
      completePathFormula = fmgr.makeAnd(completePathFormula, loc.getPathFormula().getFormula());
    }

    // Compute the unsat core
    List<BooleanFormula> unsatCore;
    try {
      unsatCore = solver.unsatCore(completePathFormula);
    } catch (SolverException e) {
      throw new CPAException("Solver failed to compute the unsat core while Newton refinement.", e);
    }
    return ImmutableList.copyOf(unsatCore);
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
   * @param pPathLocations A list with the necessary information to all path locations
   * @param pUnsatCore An optional holding the unsatisfiable core in the form of a list of Formulas.
   *        If no list of formulas is applied it computes the regular postCondition
   * @return A list of BooleanFormulas holding the strongest postcondition of each edge on the path
   * @throws CPAException In case the Algorithm failed unexpected
   * @throws InterruptedException In case of interruption
   */
  private List<BooleanFormula>
      calculateStrongestPostCondition(
          List<PathLocation> pPathLocations,
          Optional<List<BooleanFormula>> pUnsatCore)
          throws CPAException, InterruptedException {
    logger.log(Level.FINE, "Calculate Strongest Postcondition for the error trace.");

    // First Predicate is always true
    BooleanFormula preCondition = fmgr.getBooleanFormulaManager().makeTrue();

    // Initialize the predicate list(first preCondition not assigned as always true and not needed
    // in CounterexampleTraceinfo
    List<BooleanFormula> predicates = new ArrayList<>();

    for (PathLocation location : pPathLocations) {
      BooleanFormula postCondition;

      CFAEdge edge = location.getLastEdge();
      PathFormula pathFormula = location.getPathFormula();


      // Decide whether to abstract this Formula(Only true if unsatCore is present and does not
      // contain the formula
      boolean abstractThisFormula = false;
      if (pUnsatCore.isPresent()) {
        // TODO: Check for all partial Formulas, reason why it fails for assumes as !(x|y)
        abstractThisFormula = !pUnsatCore.get().contains(pathFormula.getFormula());
      }
      switch (edge.getEdgeType()) {
        case AssumeEdge:
          // If this formula should be abstracted it does not imply any additional atoms
          if (abstractThisFormula) {
            postCondition = preCondition;
          }
          // Else make the conjunction of the precondition and the pathFormula
          else {
            postCondition = fmgr.makeAnd(preCondition, pathFormula.getFormula());
          }
          break;
        case StatementEdge:
        case DeclarationEdge:
          BooleanFormula toExist;
          // If this formula should be abstracted, this statement havocs the leftHand variable
          // Therefore its previous values can be existentially quantified in the preCondition
          if (abstractThisFormula) {
            toExist = preCondition;
          }
          // Else we create the disjunction and quantify the old value, yet get a new
          // Assignmentformula
          else {
            toExist = fmgr.makeAnd(preCondition, pathFormula.getFormula());
          }
          try {
            // Use Existential-Quantifier-Elimination on dead variables
            // FIXME: Fails while quantification with Z3(in apply tactics);
            postCondition = fmgr.eliminateDeadVariables(toExist, pathFormula.getSsa());
          } catch (SolverException e) {
            throw new CPAException("Solver failed to compute existence-quantor.", e);
          }
          break;
        case FunctionCallEdge:
          // TODO: Atleast for Asserts, a handling is necessary
        default:
          if(fmgr.getBooleanFormulaManager().isTrue(preCondition)) {
            logger.log(Level.FINE,"Pathformula is True, so no addtionial Formula in PostCondition for EdgeType: "+ edge.getEdgeType());
            postCondition = preCondition;
            break;
          }
          // TODO: Determine if it is necessary to do something for other edgetypes
          throw new UnsupportedOperationException(
              "Found unsupported Edgetype in Newton Refinement: "
                  + edge.getDescription()
                  + " of Type :"
                  + edge.getEdgeType());
      }
      if (location.hasCorrespondingARGState()) {
        predicates.add(fmgr.simplify(postCondition));
      }
      // PostCondition is preCondition for next location
      preCondition = postCondition;
    }
    // Check the unsatisfiability of the last predicate
    try {
      assert solver.isUnsat(predicates.get(predicates.size() - 1));
    } catch (SolverException e) {
      throw new CPAException("Solver failed to solve the unsatisfiability of the last predicate");
    }
    // Remove the last predicate as always false
    return ImmutableList.copyOf(predicates.subList(0, predicates.size() - 1));
  }


  /**
   * Builds a list of Path Location. Each Position holds information about its incoming CFAEdge,
   * corresponding PathFormula and the state. Designed for easier access at corresponding
   * information. The initial state is not stored.
   *
   * @param pPath The Path to build the path locations for.
   * @return A list of PathLocations
   * @throws CPAException if the calculation of a pathformula fails
   * @throws InterruptedException if interrupted
   */
  private List<PathLocation> buildPathLocationList(ARGPath pPath)
      throws CPAException, InterruptedException {
    List<PathLocation> pathLocationList = new ArrayList<>();

    // First state does not have an incoming edge. And it is not needed, as first predicate is
    // always true.
    PathIterator pathIterator = pPath.fullPathIterator();
    PathFormula pathFormula = pfmgr.makeEmptyPathFormula();

    while(pathIterator.hasNext()) {
      pathIterator.advance();
      CFAEdge lastEdge = pathIterator.getIncomingEdge();
      Optional<ARGState> state = pathIterator.isPositionWithState() ? Optional.of(pathIterator.getAbstractState())
              : Optional.empty();
      try {
        pathFormula =
            pfmgr.makeAnd(
                pfmgr.makeNewPathFormula(pfmgr.makeEmptyPathFormula(), pathFormula.getSsa()),
                lastEdge);
      } catch (CPATransferException e) {
        throw new CPAException(
            "Failed to compute the Pathformula for edge(" + lastEdge.toString() + ")",
            e);
      }
      pathLocationList.add(new PathLocation(lastEdge, pathFormula, state));
    }
    return pathLocationList;
  }

  private static class PathLocation {
    final CFAEdge lastEdge;
    final PathFormula pathFormula;
    final Optional<ARGState> state;

    PathLocation(
        final CFAEdge pLastEdge,
        final PathFormula pPathFormula,
        final Optional<ARGState> pState) {
      lastEdge = pLastEdge;
      pathFormula = pPathFormula;
      state = pState;
    }

    CFAEdge getLastEdge() {
      return lastEdge;
    }

    PathFormula getPathFormula() {
      return pathFormula;
    }

    boolean hasCorrespondingARGState() {
      return state.isPresent();
    }

    // Optional<ARGState> getARGState() {
    // return state;
    // }

    @Override
    public String toString() {
      return (lastEdge != null
          ? lastEdge.toString()
          : ("First State: " + state.get().toDOTLabel()))
          + ", PathFormula: "
          + pathFormula.toString();
    }
  }
}