// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pseudoQE.PseudoExistQeManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Class designed to perform a Newton-based refinement
 *
 * <p>based on:
 *
 * <p>"Generating Abstract Explanations of Spurious Counterexamples in C Programs" by Thomas Ball
 * Sriram K. Rajamani
 *
 * <p>"Craig vs. Newton in Software Model Checking" by Daniel Dietsch, Matthias Heizmann, Betim
 * Musa, Alexander Nutz, Andreas Podelski
 */
@Options(prefix = "cpa.predicate.refinement.newtonrefinement")
public class NewtonRefinementManager implements StatisticsProvider {
  private final LogManager logger;
  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final PathFormulaManager pfmgr;
  private final PseudoExistQeManager qeManager;

  private final NewtonStatistics stats = new NewtonStatistics();

  @Option(
      secure = true,
      description =
          "use unsatisfiable Core in order to abstract the predicates produced while"
              + " NewtonRefinement")
  private boolean infeasibleCore = true;

  @Option(
      secure = true,
      description =
          "use live variables in order to abstract the predicates produced while NewtonRefinement")
  private boolean liveVariables = true;

  @Option(
      secure = true,
      description =
          "sets the level of the pathformulas to use for abstraction. \n"
              + "  EDGE : Based on Pathformulas of every edge in ARGPath\n"
              + "  BLOCK: Based on Pathformulas at Abstractionstates")
  private PathFormulaAbstractionLevel abstractionLevel = PathFormulaAbstractionLevel.EDGE;

  public enum PathFormulaAbstractionLevel {
    BLOCK, // Abstracts the whole Block(between abstraction states) at once
    EDGE // Abstracts every edge of the ARGPath
  }

  // TODO: Default true once it is tested
  @Option(
      secure = true,
      description =
          "Activate fallback to interpolation. Typically in case of a repeated counterexample.")
  private boolean fallback = false;

  public NewtonRefinementManager(
      LogManager pLogger, Solver pSolver, PathFormulaManager pPfmgr, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this, NewtonRefinementManager.class);
    logger = pLogger;
    solver = pSolver;
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pfmgr = pPfmgr;
    qeManager = new PseudoExistQeManager(solver, config, logger);
  }

  /**
   * Creates the CounterexampleTraceInfo for the given error path based on the Newton-based
   * refinement approach.
   *
   * <p>The counterexample should hold pseudo-interpolants based on StrongestPostCondition performed
   * by Newton.
   *
   * @param pAllStatesTrace The error path
   * @param pFormulas The Block formulas computed in previous step
   * @return The Counterexample, containing pseudo-interpolants if successful
   * @exception RefinementFailedException If the Newton refinement fails
   */
  public CounterexampleTraceInfo buildCounterexampleTrace(
      ARGPath pAllStatesTrace, BlockFormulas pFormulas)
      throws RefinementFailedException, InterruptedException {
    stats.noOfRefinements++;
    stats.totalTimer.start();
    try {
      List<PathLocation> pathLocations = buildPathLocationList(pAllStatesTrace);
      if (isFeasible(pFormulas.getFormulas(), pAllStatesTrace)) {
        // Create feasible CounterexampleTrace
        return CounterexampleTraceInfo.feasible(
            pFormulas.getFormulas(), ImmutableList.of(), ImmutableMap.of());
      } else {
        // Create infeasible Counterexample
        List<BooleanFormula> predicates;
        switch (abstractionLevel) {
          case EDGE:
            predicates = createPredicatesEdgeLevel(pAllStatesTrace, pFormulas, pathLocations);
            break;
          case BLOCK:
            predicates = createPredicatesBlockLevel(pAllStatesTrace, pFormulas, pathLocations);
            break;
          default:
            throw new UnsupportedOperationException(
                "The selected PathFormulaAbstractionLevel is not implemented.");
        }

        // Test if the predicate of the error state is unsatisfiable
        try {
          if (!solver.isUnsat(predicates.get(predicates.size() - 1))) {
            throw new RefinementFailedException(Reason.SequenceOfAssertionsToWeak, pAllStatesTrace);
          }
        } catch (SolverException e) {
          throw new RefinementFailedException(Reason.NewtonRefinementFailed, pAllStatesTrace, e);
        }

        // Apply Live Variable filtering if configured
        if (liveVariables) {
          predicates = filterFutureLiveVariables(pathLocations, predicates);
        }
        // Drop last predicate as it should always be false.
        return CounterexampleTraceInfo.infeasible(predicates.subList(0, predicates.size() - 1));
      }
    } finally {
      stats.totalTimer.stop();
    }
  }

  /**
   * Check if fallback to interpolation-based refinement is active
   *
   * @return true if active
   */
  public boolean fallbackToInterpolation() {
    return fallback;
  }

  /**
   * Create the sequence of assertions at the abstraction states using the PathFormulas of each
   * CFAEdge.
   *
   * @param pPath The Error Path
   * @param pFormulas The BlockFormulas
   * @param pathLocations Aggregates information to each path location
   * @return A list of Formulas, each Formula represents an assertion at the corresponding
   *     abstraction state, the last formula should be unsatisfiable(representing Error state)
   * @throws InterruptedException if interrupted
   * @throws RefinementFailedException if the refinement failed
   */
  private List<BooleanFormula> createPredicatesEdgeLevel(
      ARGPath pPath, BlockFormulas pFormulas, List<PathLocation> pathLocations)
      throws RefinementFailedException, InterruptedException {

    // Create the list of path
    List<BooleanFormula> pathFormulas =
        transformedImmutableListCopy(pathLocations, l -> l.getPathFormula().getFormula());

    assert isFeasible(pFormulas.getFormulas(), pPath) == isFeasible(pathFormulas, pPath);

    // Compute the unsatisfiable core if configured, else create empty Optional
    Optional<List<BooleanFormula>> unsatCore;
    if (infeasibleCore) {
      unsatCore = Optional.of(computeUnsatCore(pathFormulas, pPath));
    } else {
      unsatCore = Optional.empty();
    }

    // Calculate Strongest Post Condition of all pathLocations
    return calculateStrongestPostCondition(pathLocations, unsatCore);
  }

  /**
   * Create the sequence of assertions at the abstraction states using the BlockFormulas for
   * abstraction.
   *
   * @param pPath The Error Path
   * @param pFormulas The BlockFormulas
   * @param pPathLocations List of location on error trace
   * @return A list of Formulas, each Formula represents an assertion at the corresponding
   *     abstraction state, the last formula should be unsatisfiable(representing Error state)
   * @throws InterruptedException if interrupted
   * @throws RefinementFailedException if the refinement failed
   */
  private List<BooleanFormula> createPredicatesBlockLevel(
      ARGPath pPath, BlockFormulas pFormulas, List<PathLocation> pPathLocations)
      throws InterruptedException, RefinementFailedException {
    List<BooleanFormula> unsatCore = computeUnsatCore(pFormulas.getFormulas(), pPath);
    List<BooleanFormula> predicates = new ArrayList<>();

    // Filter pathlocations to only abstractionstate locations
    Iterator<PathLocation> abstractionLocations =
        pPathLocations.stream()
            .filter(l -> l.hasAbstractionState())
            .collect(ImmutableList.toImmutableList())
            .iterator();

    BooleanFormula pred = bfmgr.makeTrue();
    for (BooleanFormula pathFormula : pFormulas.getFormulas()) {
      assert abstractionLocations.hasNext();
      PathFormula pathFormulaWithSsa = abstractionLocations.next().getPathFormula();

      if (unsatCore.contains(pathFormula)) {
        pred = bfmgr.and(pred, pathFormula);
        pred = eliminateIntermediateVariables(pathFormulaWithSsa, pred);
        pred = fmgr.simplify(pred);
      }
      predicates.add(pred);
    }

    return ImmutableList.copyOf(predicates);
  }

  /**
   * Calculates the StrongestPostCondition at all states on a error-trace.
   *
   * @param pPathLocations A list with the necessary information to all path locations
   * @param pUnsatCore An optional holding the unsatisfiable core in the form of a list of Formulas.
   *     If no list of formulas is applied it computes the regular postCondition
   * @return A list of Formulas, each Formula represents an assertion at the corresponding
   *     abstraction state, the last formula should be unsatisfiable(representing Error state)
   * @throws InterruptedException In case of interruption
   * @throws RefinementFailedException In case an exception in the solver.
   */
  private List<BooleanFormula> calculateStrongestPostCondition(
      List<PathLocation> pPathLocations, Optional<List<BooleanFormula>> pUnsatCore)
      throws InterruptedException, RefinementFailedException {
    logger.log(Level.FINE, "Calculate Strongest Postcondition for the error trace.");
    stats.postConditionTimer.start();
    try {
      // First Predicate is always true
      BooleanFormula preCondition = bfmgr.makeTrue();

      // Initialize the predicate list(first preCondition not assigned as always true and not needed
      // in CounterexampleTraceinfo
      List<BooleanFormula> predicates = new ArrayList<>();

      for (PathLocation location : pPathLocations) {
        BooleanFormula postCondition;

        // Get the Path Formula of the incoming edge
        CFAEdge edge = location.getLastEdge();
        PathFormula pathFormula = location.getPathFormula();

        // Abstract the formula based on unsatCore iff present
        List<BooleanFormula> requiredPart = new ArrayList<>();
        if (pUnsatCore.isPresent()) {
          Set<BooleanFormula> pathFormulaElements =
              bfmgr.toConjunctionArgs(pathFormula.getFormula(), true);
          for (BooleanFormula pathFormulaElement : pathFormulaElements) {
            if (pUnsatCore.orElseThrow().contains(pathFormulaElement)) {
              requiredPart.add(pathFormulaElement);
              break;
            }
          }
        } else {
          requiredPart.add(pathFormula.getFormula());
        }

        // Apply abstraction to postCondition and eliminate quantifiers if possible
        switch (edge.getEdgeType()) {
          case AssumeEdge:
            if (!requiredPart.isEmpty()) {
              postCondition =
                  eliminateIntermediateVariables(
                      pathFormula, bfmgr.and(preCondition, bfmgr.and(requiredPart)));
            }
            // Else no additional assertions
            else {
              postCondition = preCondition;
            }
            break;
          case StatementEdge:
          case DeclarationEdge:
          case FunctionCallEdge:
          case ReturnStatementEdge:
          case FunctionReturnEdge:
            postCondition =
                calculatePostconditionForAssignment(preCondition, pathFormula, requiredPart);
            break;
          default:
            if (bfmgr.isTrue(pathFormula.getFormula())) {
              logger.log(
                  Level.FINE,
                  "Pathformula is True, so no additional Formula in PostCondition for EdgeType: ",
                  edge.getEdgeType());
              postCondition = preCondition;
              break;
            }

            // Throw an exception if the type of the Edge is none of the above but it holds a
            // PathFormula
            throw new UnsupportedOperationException(
                "Found unsupported EdgeType in Newton Refinement: "
                    + edge.getDescription()
                    + " of Type :"
                    + edge.getEdgeType());
        }

        // Add to predicates iff location has an abstraction state
        if (location.hasCorrespondingARGState() && location.hasAbstractionState()) {
          predicates.add(postCondition);
        }
        // PostCondition is preCondition for next location
        preCondition = postCondition;
      }

      // Remove the last predicate as it is should be false
      return ImmutableList.copyOf(predicates);
    } finally {
      stats.postConditionTimer.stop();
    }
  }

  /**
   * Calculate the Strongest postcondition of an assignment
   *
   * @param preCondition The condition prior to the assignment
   * @param pathFormula The PathFormula associated with the assignment
   * @param requiredPart The part of the PathFormula that must be kept
   * @return The postCondition as BooleanFormula
   * @throws InterruptedException If interrupted
   */
  private BooleanFormula calculatePostconditionForAssignment(
      BooleanFormula preCondition, PathFormula pathFormula, List<BooleanFormula> requiredPart)
      throws InterruptedException {

    BooleanFormula toExist;

    // If this formula should be abstracted(no requiredPart), this statement havocs the leftHand
    // variable
    // Therefore its previous values can be existentially quantified in the preCondition
    if (!requiredPart.isEmpty()) {
      toExist = bfmgr.and(preCondition, bfmgr.and(requiredPart));
    } else {
      toExist = preCondition;
    }

    // If the toExist is true, the postCondition is True too.
    if (toExist == bfmgr.makeTrue()) {
      return toExist;
    }

    return eliminateIntermediateVariables(pathFormula, toExist);
  }

  /**
   * Try to eliminate Intermediate Variables from a Formula
   *
   * @param pathFormula the pathformula needed for SSaMap
   * @param toExist the formula to eliminate the vars in
   * @return The formula after eliminating Variables or the original formula if elimination not
   *     possible
   * @throws InterruptedException if interrupted
   */
  private BooleanFormula eliminateIntermediateVariables(
      PathFormula pathFormula, BooleanFormula toExist) throws InterruptedException {
    // Get all intermediate Variables, stored in map to hold both String and Formula
    // Mutable as removing entries might be necessary.
    Map<String, Formula> intermediateVars =
        ImmutableMap.copyOf(
            Maps.filterKeys(
                fmgr.extractVariables(toExist),
                varName -> fmgr.isIntermediate(varName, pathFormula.getSsa())));

    // If there are no intermediate Variables, no quantification is necessary
    if (intermediateVars.isEmpty()) {
      return toExist;
    }

    // Try to eliminate the intermediate Variables
    Optional<BooleanFormula> result = qeManager.eliminateQuantifiers(intermediateVars, toExist);
    if (result.isPresent()) {
      return result.orElseThrow();
    } else {
      logger.log(
          Level.FINE, "Quantifier elimination failed, keeping old assignments in predicate.");
      return toExist;
    }
  }

  /**
   * Check the feasibility of the trace formula
   *
   * @param pFormulas The path formula
   * @param pPath The path of the counterexample
   * @return <code>true</code> if the trace is feasible
   * @throws RefinementFailedException If the solver failed while proving unsatisfiability
   * @throws InterruptedException If the Execution is interrupted
   */
  private boolean isFeasible(List<BooleanFormula> pFormulas, ARGPath pPath)
      throws RefinementFailedException, InterruptedException {
    boolean isFeasible;
    logger.log(Level.FINEST, "Show feasiblity for:", pFormulas);
    try (ProverEnvironment prover = solver.newProverEnvironment()) {
      for (BooleanFormula formula : pFormulas) {
        prover.push(formula);
      }
      isFeasible = !prover.isUnsat();
    } catch (SolverException e) {
      // Prover failed while proving unsatisfiability
      throw new RefinementFailedException(Reason.NewtonRefinementFailed, pPath, e);
    }
    logger.log(Level.FINEST, isFeasible ? "The trace is feasible." : "The trace is infeasible");
    return isFeasible;
  }

  /**
   * Compute the Unsatisfiable core as a list of BooleanFormulas
   *
   * @param pFormulas The List of Formulas to compute the unsatisfiable core for
   * @param pPath The path to the Error(Needed for RefinementFailedException)
   * @return The unsatisfiable core of the list of formulas
   * @throws RefinementFailedException If the solver fails while calculating unsatisfiable core
   * @throws InterruptedException If the Execution is interrupted
   */
  private List<BooleanFormula> computeUnsatCore(List<BooleanFormula> pFormulas, ARGPath pPath)
      throws RefinementFailedException, InterruptedException {
    stats.unsatCoreTimer.start();
    try {
      logger.log(Level.FINEST, "Compute unsatisfiable core of: ", pFormulas);
      // Compute the unsat core
      List<BooleanFormula> unsatCore;
      try {
        unsatCore = solver.unsatCore(ImmutableSet.copyOf(pFormulas));
      } catch (SolverException e) {
        // Solver failed while computing unsat core
        throw new RefinementFailedException(Reason.NewtonRefinementFailed, pPath, e);
      }
      logger.log(Level.FINEST, "Unsatisfiable Core is: ", unsatCore);
      return ImmutableList.copyOf(unsatCore);
    } finally {
      stats.unsatCoreTimer.stop();
    }
  }

  /**
   * Projects the Predicate on the future live variables.
   *
   * <p>Future live variables are all variables that are present in PathFormulas after the
   * corresponding predicate. Due to the SSAMap we can consider each variable final as for a
   * reassignment a fresh SSAIndex is assigned.
   *
   * @param pPathLocations The path of the counterexample
   * @param pPredicates The predicates as derived in previous steps
   * @return The new predicates without variables that are not future live
   * @throws InterruptedException If interrupted
   */
  private List<BooleanFormula> filterFutureLiveVariables(
      List<PathLocation> pPathLocations, List<BooleanFormula> pPredicates)
      throws InterruptedException {
    stats.futureLivesTimer.start();
    try {
      // Only variables that are in the predicates need be considered
      Set<String> variablesToTest = new HashSet<>();
      for (BooleanFormula pred : pPredicates) {
        variablesToTest.addAll(fmgr.extractVariableNames(pred));
      }

      Map<String, Integer> lastOccurance = new HashMap<>(); // Last occurance of var
      Map<Integer, BooleanFormula> predPosition = new TreeMap<>(); // Pos of pred
      int predCounter = 0;

      for (PathLocation location : pPathLocations) {
        // Map variables to the last location it occurs in the path
        Set<String> localVars = fmgr.extractVariableNames(location.getPathFormula().getFormula());
        for (String var : variablesToTest) {
          if (localVars.contains(var)) {
            lastOccurance.put(var, location.getPathPosition());
          }
        }

        // Map the abstraction state locations to the predicates
        if (location.hasAbstractionState() && predCounter < pPredicates.size()) {
          predPosition.put(location.getPathPosition(), pPredicates.get(predCounter));
          predCounter++;
        }
      }
      assert predPosition.size() == pPredicates.size();

      List<BooleanFormula> newPredicates = new ArrayList<>();

      for (Entry<Integer, BooleanFormula> predEntry : predPosition.entrySet()) {
        BooleanFormula pred = predEntry.getValue(); // The predicate
        int predPos = predEntry.getKey(); // The position in the path

        // Map predicate to the variables that are future live at its position
        Set<String> futureLives = Maps.filterValues(lastOccurance, (v) -> v > predPos).keySet();

        // identify the variables that are not future live and can be quantified
        Map<String, Formula> toQuantify =
            Maps.filterKeys(fmgr.extractVariables(pred), varName -> !futureLives.contains(varName));

        // quantify the previously identified variables
        if (!toQuantify.isEmpty()) {
          Optional<BooleanFormula> quantifiedPred =
              qeManager.eliminateQuantifiers(toQuantify, pred);
          if (quantifiedPred.isPresent()) {
            newPredicates.add(quantifiedPred.orElseThrow());
            stats.noOfQuantifiedFutureLives += toQuantify.size();
          } else {
            // Keep the old predicate as QE is not possible
            newPredicates.add(pred);
          }
        } else {
          newPredicates.add(pred);
        }
      }
      assert newPredicates.size() == pPredicates.size();
      return newPredicates;
    } finally {
      stats.futureLivesTimer.stop();
    }
  }

  /**
   * Builds a list of Path Location. Each Position holds information about its incoming CFAEdge,
   * corresponding PathFormula and the state. Designed for easier access to corresponding
   * information. The initial state is not stored.
   *
   * @param pPath The Path to build the path locations for.
   * @return A list of PathLocations
   * @throws RefinementFailedException if the calculation of a PathFormula fails
   * @throws InterruptedException if interrupted
   */
  private List<PathLocation> buildPathLocationList(ARGPath pPath)
      throws RefinementFailedException, InterruptedException {
    List<PathLocation> pathLocationList = new ArrayList<>();

    // First state does not have an incoming edge. And it is not needed, as first predicate is
    // always true.
    PathIterator pathIterator = pPath.fullPathIterator();
    PathFormula pathFormula = pfmgr.makeEmptyPathFormula();
    int pos = 0;

    while (pathIterator.hasNext()) {
      pathIterator.advance();
      CFAEdge lastEdge = pathIterator.getIncomingEdge();
      Optional<ARGState> state =
          pathIterator.isPositionWithState()
              ? Optional.of(pathIterator.getAbstractState())
              : Optional.empty();
      // Build PathFormula
      try {
        pathFormula =
            pfmgr.makeAnd(pfmgr.makeEmptyPathFormulaWithContextFrom(pathFormula), lastEdge);
      } catch (CPATransferException e) {
        // Failed to compute the Pathformula
        throw new RefinementFailedException(Reason.NewtonRefinementFailed, pPath, e);
      }
      // Add the location to the list
      pathLocationList.add(new PathLocation(pos, lastEdge, pathFormula, state));
      pos++;
    }
    return pathLocationList;
  }

  /**
   * Class holding the information of a location on program path. Each Location is associated to its
   * incoming CFAEdge.
   *
   * <p>Internal implementation used to aggregate the corresponding information in a way to make
   * iteration-steps more comprehensible
   */
  private static class PathLocation {
    final int pos; // Position in the path
    final CFAEdge lastEdge;
    final PathFormula pathFormula;
    final Optional<ARGState> state;

    PathLocation(
        final int pPosition,
        final CFAEdge pLastEdge,
        final PathFormula pPathFormula,
        final Optional<ARGState> pState) {
      pos = pPosition;
      lastEdge = pLastEdge;
      pathFormula = pPathFormula;
      state = pState;
    }
    /**
     * Get the position of the location in the path
     *
     * @return The position of the location in the path
     */
    int getPathPosition() {
      return pos;
    }

    /**
     * Get the incoming edge of this location
     *
     * @return The CFAEdge
     */
    CFAEdge getLastEdge() {
      return lastEdge;
    }

    /**
     * Get the pathFormula of the location. Is the PathFormula of the incoming edge, but with the
     * context of the location in the path
     *
     * @return The PathFormula
     */
    PathFormula getPathFormula() {
      return pathFormula;
    }

    /**
     * Check if the location has a corresponding ARGState
     *
     * @return true iff there is a ARGState associated to the location
     */
    boolean hasCorrespondingARGState() {
      return state.isPresent();
    }

    /**
     * Check if the location has a corresponding Abstraction state
     *
     * @return true iff there is an corresponding state and this state also is an abstraction state
     */
    boolean hasAbstractionState() {
      if (hasCorrespondingARGState()) {
        return PredicateAbstractState.getPredicateState(state.orElseThrow()).isAbstractionState();
      } else {
        return false;
      }
    }

    @Override
    public String toString() {
      return (lastEdge != null
              ? lastEdge.toString()
              : ("First State: " + state.orElseThrow().toDOTLabel()))
          + ", PathFormula: "
          + pathFormula;
    }
  }

  private class NewtonStatistics implements Statistics {
    // Counter
    private int noOfRefinements = 0;
    private int noOfQuantifiedFutureLives = 0;

    // Timer
    private final Timer totalTimer = new Timer();
    private final Timer postConditionTimer = new Timer();
    private final Timer unsatCoreTimer = new Timer();
    private final Timer futureLivesTimer = new Timer();

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      pOut.println("Number of Newton Refinements                : " + noOfRefinements);
      pOut.println("  Total Time spent                          : " + totalTimer.getSumTime());
      pOut.println(
          "  Time spent for strongest postcondition    : " + postConditionTimer.getSumTime());
      if (infeasibleCore) {
        pOut.println(
            "  Time spent for unsat Core                 : " + unsatCoreTimer.getSumTime());
      }
      if (liveVariables) {
        pOut.println(
            "  Time spent for Live Variable projection   : " + futureLivesTimer.getSumTime());
        pOut.println("  Number of quantified Future Live variables: " + noOfQuantifiedFutureLives);
      }
    }

    @Override
    public @Nullable String getName() {
      return "Newton Refinement Algorithm";
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    qeManager.collectStatistics(pStatsCollection);
  }
}
