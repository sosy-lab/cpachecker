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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
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
  private final PathFormulaManager pfmgr;
  private final PseudoExistQeManager qeManager;

  private final NewtonStatistics stats = new NewtonStatistics();


  @Option(
    secure = true,
    description =
        "use unsatisfiable Core in order to abstract the predicates produced while NewtonRefinement"
  )
  private boolean useUnsatCore = true;

  @Option(
    secure = true,
    description =
        "use live variables in order to abstract the predicates produced while NewtonRefinement"
  )
  private boolean useLiveVariables = true;

  @Option(
    secure = true,
    description =
        "sets the level of the pathformulas to use for abstraction. \n"
            + "  EDGE : Based on Pathformulas of every edge in ARGPath\n"
            + "  BLOCK: Based on Pathformulas at Abstractionstates"
  )
  private PathFormulaAbstractionLevel pathFormulAbstractionLevel = PathFormulaAbstractionLevel.EDGE;

  public enum PathFormulaAbstractionLevel {
    BLOCK, //Abstracts the whole Block(between abstraction states) at once
    EDGE //Abstracts every edge of the ARGPath
  }


  public NewtonRefinementManager(
      LogManager pLogger, Solver pSolver, PathFormulaManager pPfmgr, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this, NewtonRefinementManager.class);
    logger = pLogger;
    solver = pSolver;
    fmgr = solver.getFormulaManager();
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
      List<PathLocation> pathLocations = this.buildPathLocationList(pAllStatesTrace);
      if (isFeasible(pFormulas.getFormulas(), pAllStatesTrace)) {
        // Create feasible CounterexampleTrace
        return CounterexampleTraceInfo.feasible(
            pFormulas.getFormulas(), ImmutableList.of(), ImmutableMap.of());
      } else {
        List<BooleanFormula> predicates;
        switch (pathFormulAbstractionLevel) {
          case EDGE:
            List<BooleanFormula> pathFormulas =
                pathLocations
                    .stream()
                    .map(l -> l.getPathFormula().getFormula())
                    .collect(Collectors.toList());

            // TODO: Fails in some cases, most interestingly those simple tests called SSAMap Bug
            // Question: What was the ssa bug and how was it solved?
            assert isFeasible(pFormulas.getFormulas(), pAllStatesTrace)
                == isFeasible(pathFormulas, pAllStatesTrace);

            // Create the list of pathLocations(holding all relevant data)
            Optional<List<BooleanFormula>> unsatCore;
            // Only compute if unsatCoreOption is set
            if (useUnsatCore) {
              unsatCore = Optional.of(computeUnsatCore(pathFormulas, pAllStatesTrace));
            } else {
              unsatCore = Optional.empty();
            }

            // Calculate StrongestPost
            predicates =
                this.calculateStrongestPostCondition(pathLocations, unsatCore, pAllStatesTrace);
            break;
          case BLOCK:
            predicates = experimentalUnsatCoreWithoutNewtonRefinement(pAllStatesTrace, pFormulas);
            break;
          default:
            throw new UnsupportedOperationException(
                "The selected PathFormulaAbstractionLevel is not implemented.");
        }
        if (useLiveVariables) {
          predicates = filterFutureLiveVariables(pathLocations, predicates);
        }

        return CounterexampleTraceInfo.infeasible(predicates);
      }
    } finally {
      stats.totalTimer.stop();
    }
  }

  /**
   * Compute the Unsatisfiable core as a list of BooleanFormulas
   *
   * @param pFormulas The List of Formulas to compute the unsatisfiable core for
   * @return The unsatisfiable core of the list of formulas
   * @throws RefinementFailedException If the solver fails while calculating unsatisfiable core
   * @throws InterruptedException If the Execution is interrupted
   */
  private List<BooleanFormula> computeUnsatCore(List<BooleanFormula> pFormulas, ARGPath pPath)
      throws RefinementFailedException, InterruptedException {
    stats.unsatCoreTimer.start();

    try {
      // Compute the unsat core
      List<BooleanFormula> unsatCore;
      try {
        unsatCore = solver.unsatCore(new HashSet<>(pFormulas));
      } catch (SolverException e) {
        //Solver failed while computing unsat core
        throw new RefinementFailedException(Reason.NewtonRefinementFailed, pPath, e);
      }
      return ImmutableList.copyOf(unsatCore);
    } finally {
      stats.unsatCoreTimer.stop();
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
    try (ProverEnvironment prover = solver.newProverEnvironment()) {
      for (BooleanFormula formula : pFormulas) {
        prover.push(formula);
      }
      isFeasible = !prover.isUnsat();
    } catch (SolverException e) {
      // Prover failed while proving unsatisfiability
      throw new RefinementFailedException(Reason.NewtonRefinementFailed, pPath, e);
    }
    return isFeasible;
  }

  /**
   * Calculates the StrongestPostCondition at all states on a error-trace.
   *
   * <p>When applied to the Predicate states, they assure that the same error-trace won't occur
   * again.
   *
   * @param pPathLocations A list with the necessary information to all path locations
   * @param pUnsatCore An optional holding the unsatisfiable core in the form of a list of Formulas.
   *     If no list of formulas is applied it computes the regular postCondition
   * @return A list of BooleanFormulas holding the strongest postcondition of each edge on the path
   * @throws InterruptedException In case of interruption
   * @throws RefinementFailedException In case an exception in the solver.
   */
  private List<BooleanFormula> calculateStrongestPostCondition(
      List<PathLocation> pPathLocations, Optional<List<BooleanFormula>> pUnsatCore, ARGPath pPath)
      throws InterruptedException, RefinementFailedException {
    logger.log(Level.FINE, "Calculate Strongest Postcondition for the error trace.");
    stats.postConditionTimer.start();
    try {
      // First Predicate is always true
      BooleanFormula preCondition = fmgr.getBooleanFormulaManager().makeTrue();

      // Initialize the predicate list(first preCondition not assigned as always true and not needed
      // in CounterexampleTraceinfo
      List<BooleanFormula> predicates = new ArrayList<>();

      for (PathLocation location : pPathLocations) {
        BooleanFormula postCondition;

        CFAEdge edge = location.getLastEdge();
        PathFormula pathFormula = location.getPathFormula();
        Set<BooleanFormula> pathFormulaElements =
            fmgr.getBooleanFormulaManager().toConjunctionArgs(pathFormula.getFormula(), false);

        // Decide whether to abstract this Formula(Only true if unsatCore is present and does not
        // contain the formula
        Optional<BooleanFormula> requiredPart = Optional.empty();
        if (pUnsatCore.isPresent()) {

          // Split up any conjunction in the pathformula, to be able to identify if contained in
          // unsat core
          for (BooleanFormula pathFormulaElement : pathFormulaElements) {
            if (pUnsatCore.get().contains(pathFormulaElement)) {
              requiredPart = Optional.of(pathFormulaElement);
              break;
            }
          }
        } else {
          requiredPart = Optional.of(pathFormula.getFormula());
        }
        switch (edge.getEdgeType()) {
          case AssumeEdge:
            if (requiredPart.isPresent()) {
              postCondition = fmgr.makeAnd(preCondition, requiredPart.get());
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
            if (fmgr.getBooleanFormulaManager().isTrue(pathFormula.getFormula())) {
              logger.log(
                  Level.FINE,
                  "Pathformula is True, so no additional Formula in PostCondition for EdgeType: "
                      + edge.getEdgeType());
              postCondition = preCondition;
              break;
            }

            // Throw an exception if the type of the Edge is none of the above but it holds a PathFormula
            throw new UnsupportedOperationException(
                "Found unsupported EdgeType in Newton Refinement: "
                    + edge.getDescription()
                    + " of Type :"
                    + edge.getEdgeType());
        }
        if (location.hasCorrespondingARGState() && location.hasAbstractionState()) {
          predicates.add(fmgr.simplify(postCondition));
        }
        // PostCondition is preCondition for next location
        preCondition = postCondition;
      }

      // Normally here would be the place for checking unsatisfiability. But reoccuring counterexamples
      // throw an exception so this check is not necessary.
      try {
        if (!solver.isUnsat(predicates.get(predicates.size() - 1))) {
          logger.log(
              Level.SEVERE,
              "Created last predicate is not unsatisfiable. The refinement failed to find a sequence of assertions ruling out counterexample.");
        }
      } catch (SolverException e) {
        // Solver failed while showing unsatisfiability of last predicate.
        throw new RefinementFailedException(Reason.NewtonRefinementFailed, pPath, e);
      }
      // Remove the last predicate as always false
      return ImmutableList.copyOf(predicates.subList(0, predicates.size() - 1));
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
   * @throws InterruptedException When interrupted
   */
  private BooleanFormula calculatePostconditionForAssignment(
      BooleanFormula preCondition, PathFormula pathFormula, Optional<BooleanFormula> requiredPart)
      throws InterruptedException {

    BooleanFormula toExist;

    // If this formula should be abstracted(no requiredPart), this statement havocs the leftHand variable
    // Therefore its previous values can be existentially quantified in the preCondition
    if (requiredPart.isPresent()) {
      toExist = fmgr.makeAnd(preCondition, requiredPart.get());
    } else {
      toExist = preCondition;
    }
    // If the toExist is true, the postCondition is True too.
    if (toExist == fmgr.getBooleanFormulaManager().makeTrue()) {
      return toExist;
    }

    // Get all intermediate Variables, stored in map to hold both String and Formula
    // Mutable as removing entries might be necessary.
    Map<String, Formula> intermediateVars =
        Maps.newHashMap(
            Maps.filterEntries(
                fmgr.extractVariables(toExist),
                new Predicate<Entry<String, Formula>>() {

                  @Override
                  public boolean apply(@NullableDecl Entry<String, Formula> pInput) {
                    if (pInput == null) {
                      return false;
                    } else {
                      return fmgr.isIntermediate(pInput.getKey(), pathFormula.getSsa());
                    }
                  }
                }));

    // If there are no intermediate Variables, no quantification is necessary
    if (intermediateVars.isEmpty()) {
      return toExist;
    }
    // Now we existentially quantify all intermediate Variables
    // and use quantifier elimination to obtain a quantifier free formula

    Optional<BooleanFormula> result = qeManager.eliminateQuantifiers(intermediateVars, toExist);
    if (result.isPresent()) {
      return result.get();
    } else {
      logger.log(
          Level.FINE, "Quantifier elimination failed, keeping old assignements in predicate.");
      // Take the strongest possible assertion, as the SSA differs from other potential future assertions
      return toExist;
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

      // FIXME: The order of the new predicates is lost, major problem
      for (Entry<Integer, BooleanFormula> predEntry : predPosition.entrySet()) {
        BooleanFormula pred = predEntry.getValue(); // The predicate
        int predPos = predEntry.getKey(); // The position in the path

        // Map predicate to the variables that are future live at its position
        Set<String> futureLives = Maps.filterValues(lastOccurance, (v) -> v > predPos).keySet();

        // identify the variables that are not future live and can be quantified
        Map<String, Formula> toQuantify =
            Maps.filterEntries(
                fmgr.extractVariables(pred),
                (e) -> {
                  return !futureLives.contains(e.getKey());
                });

        // quantify the previously identified variables
        if (!toQuantify.isEmpty()) {
          Optional<BooleanFormula> quantifiedPred = qeManager.eliminateQuantifiers(toQuantify, pred);
          if (quantifiedPred.isPresent()) {
            newPredicates.add(quantifiedPred.get());
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
   * corresponding PathFormula and the state. Designed for easier access at corresponding
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
      try {
        pathFormula = pfmgr.makeAnd(pfmgr.makeEmptyPathFormula(pathFormula), lastEdge);
      } catch (CPATransferException e) {
        // Failed to compute the Pathformula
        throw new RefinementFailedException(Reason.NewtonRefinementFailed, pPath, e);
      }
      pathLocationList.add(new PathLocation(pos, lastEdge, pathFormula, state));
      pos++;
    }
    return pathLocationList;
  }

  private List<BooleanFormula> experimentalUnsatCoreWithoutNewtonRefinement(
      ARGPath pPath, BlockFormulas pFormulas)
      throws InterruptedException, RefinementFailedException {

    List<BooleanFormula> unsatCore = computeUnsatCore(pFormulas.getFormulas(), pPath);
    List<BooleanFormula> predicates = new ArrayList<>();

    BooleanFormula pred = fmgr.getBooleanFormulaManager().makeTrue();
    for (BooleanFormula pathFormula : pFormulas.getFormulas()) {
      if (unsatCore.contains(pathFormula)) {
        pred = fmgr.getBooleanFormulaManager().and(pred, pathFormula);

        //Try QE-here?
        pred = fmgr.simplify(pred);
      }
      predicates.add(pred);
    }

    return predicates.subList(0, predicates.size() - 1);
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
        return PredicateAbstractState.getPredicateState(state.get()).isAbstractionState();
      } else {
        return false;
      }
    }

    // Optional<ARGState> getARGState() {
    // return state;
    // }

    @Override
    public String toString() {
      return (lastEdge != null ? lastEdge.toString() : ("First State: " + state.get().toDOTLabel()))
          + ", PathFormula: "
          + pathFormula.toString();
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
      if (useUnsatCore) {
        pOut.println(
            "  Time spent for unsat Core                 : " + unsatCoreTimer.getSumTime());
      }
      if (useLiveVariables) {
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
