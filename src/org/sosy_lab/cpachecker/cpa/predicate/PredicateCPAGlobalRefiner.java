/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import static java.util.Collections.unmodifiableList;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import java.util.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class provides an implementation of a global refinement for predicate abstraction.
 *
 * It does not define any strategy for using the interpolants to update the
 * abstraction, this is left to an instance of {@link GlobalRefinementStrategy}.
 */
@Options(prefix="cpa.predicate.refinement.global")
public class PredicateCPAGlobalRefiner implements Refiner, StatisticsProvider {

  @Option(secure = true,
          description = "Instead of updating precision and arg we say that the refinement was not successful"
              + " after N times of refining. A real error state is not necessary to be found. Use 0 for"
              + " unlimited refinements (default).")
  @IntegerOption(min = 0)
  private int stopAfterNRefinements = 0;

  // statistics
  private final StatTimer totalTime = new StatTimer("Time for refinement");
  private final StatTimer interpolationTime = new StatTimer("Time for interpolation");
  private final StatTimer satCheckTime = new StatTimer("Time for sat-checks");

  private final LogManager logger;
  private final GlobalRefinementStrategy strategy;
  private final Solver solver;
  private final BooleanFormulaManager bfmgr;
  private final ARGCPA argCPA;

  public PredicateCPAGlobalRefiner(
      final LogManager pLogger,
      final FormulaManagerView pFmgr,
      final GlobalRefinementStrategy pStrategy,
      final Solver pSolver,
      final ARGCPA pArgcpa,
      final Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);

    logger = pLogger;
    bfmgr = pFmgr.getBooleanFormulaManager();
    solver = pSolver;
    strategy = pStrategy;
    argCPA = pArgcpa;

    logger.log(
        Level.INFO,
        "Using refinement for predicate analysis with "
            + strategy.getClass().getSimpleName()
            + " strategy.");
  }

  @Override
  public boolean performRefinement(final ReachedSet pReached)
      throws CPAException, InterruptedException {
    totalTime.start();
    try {

      List<AbstractState> targets =
          FluentIterable.from(pReached).filter(AbstractStates.IS_TARGET_STATE).toList();
      assert !targets.isEmpty();

      ARGReachedSet argReachedSet = new ARGReachedSet(pReached, argCPA);
      strategy.initializeGlobalRefinement();
      Optional<ARGState> errorState = doPathWiseRefinement(argReachedSet, targets);

      // TODO fix handling of counterexamples
      // + 1 for update count as the current interval is not finished
      if (errorState.isPresent() || stopAfterNRefinements == totalTime.getUpdateCount() + 1) {
        strategy.resetGlobalRefinement();
        return false;
      }

      strategy.updatePrecisionAndARG();
      return true;

    } catch (SolverException e) {
      throw new CPAException("Solver Exception", e);
    } finally {
      totalTime.stop();
    }
  }

  /**
   * Do refinement for a set of target states.
   *
   * The strategy is to first build the predecessor/successor relations for all
   * abstraction states on the paths to the target states, and then call
   * {@link #performRefinementOnPath(List, ARGState, List, ARGReachedSet, InterpolatingProverEnvironment)}
   * on the root state of the ARG.
   */
  private Optional<ARGState> doPathWiseRefinement(
      ARGReachedSet pReached, List<AbstractState> targets)
      throws CPAException, InterruptedException, SolverException {
    logger.log(Level.FINE, "Starting refinement for", targets.size(), "elements.");

    Map<ARGState, ARGState> predecessors = Maps.newHashMap();
    SetMultimap<ARGState, ARGState> successors = HashMultimap.create();

    Deque<AbstractState> todo = new ArrayDeque<>(targets);

    while (!todo.isEmpty()) {
      final ARGState currentAbstractionState = (ARGState) todo.removeFirst();
      assert currentAbstractionState.mayCover();

      ARGState currentState = currentAbstractionState;
      do {
        currentState = currentState.getParents().iterator().next();
      } while (!getPredicateState(currentState).isAbstractionState());

      if (!currentState.getParents().isEmpty() && !predecessors.containsKey(currentState)) {
        todo.add(currentState);
      }

      predecessors.put(currentAbstractionState, currentState);
      successors.put(currentState, currentAbstractionState);
    }
    final ARGState root = (ARGState) pReached.asReachedSet().getFirstState();
    assert successors.containsKey(root);

    // Now predecessors/successors contains all abstraction states on all error
    // paths and their relations.
    // These states and the relation form a tree.
    // We now iterate through this tree in a depth-first order.
    // For each state, we check reachability.
    // We do not descend beyond unreachable states,
    // but instead perform refinement on them.

    try (InterpolatingProverEnvironment<?> itpProver =
        solver.newProverEnvironmentWithInterpolation()) {
      return doPathWiseRefinement(root, successors, predecessors, pReached, targets, itpProver);
    }
  }

  // This is just a separate method to get the generics right.
  // (The arguments of the list and the prover need to match.)
  private <T> Optional<ARGState> doPathWiseRefinement(
      ARGState current,
      SetMultimap<ARGState, ARGState> successors,
      Map<ARGState, ARGState> predecessors,
      ARGReachedSet pReached,
      List<AbstractState> targets,
      InterpolatingProverEnvironment<T> itpProver)
      throws InterruptedException, SolverException, CPAException {
    List<T> itpStack = new ArrayList<>();
    LinkedList<ARGState> currentPath = new LinkedList<>();
    currentPath.add(current);
    Optional<ARGState> errorState =
        step(currentPath, itpStack, successors, predecessors, pReached, targets, itpProver);
    return errorState;
  }

  /**
   * Recursively perform refinement on the subgraph of the ARG starting with a given state.
   * Each recursion step corresponds to one "block" of the ARG. As one block
   * may have several successors, this is recursion on a tree.
   * We proceed in a DFS order.
   * Recursion stops as soon as the path has been determined to be infeasible
   * (so we do refinement as soon as possible) or a target state is reached
   * (then we found a feasible counterexample).
   * When an infeasible state was found, we call
   * {@link #performRefinementOnPath(List, ARGState, List, ARGReachedSet, InterpolatingProverEnvironment)}
   * to do the actual refinement.
   *
   * Note that the successor and predecessor relation contains only states
   * that belong to paths to a target state, so we refine only such paths,
   * and not all paths in the ARG.
   *
   * @param currentPath The list of ARG states from the root to the current element.
   * @param itpStack The stack of interpolation groups added to the solver environment so far.
   * @param successors The successor relation between abstraction states.
   * @param predecessors The predecessor relation between abstraction states.
   * @param pReached The complete reached set.
   * @param targets The set of target states.
   * @return The feasible error location or absent
   */
  private <T> Optional<ARGState> step(
      final LinkedList<ARGState> currentPath,
      final List<T> itpStack,
      final SetMultimap<ARGState, ARGState> successors,
      final Map<ARGState, ARGState> predecessors,
      final ARGReachedSet pReached,
      final List<AbstractState> targets,
      InterpolatingProverEnvironment<T> itpProver)
      throws InterruptedException, SolverException, CPAException {

    for (final ARGState succ : successors.get(currentPath.getLast())) {
      assert succ.getChildren().isEmpty() == targets.contains(succ);
      assert succ.mayCover();

      BooleanFormula blockFormula =
          getPredicateState(succ).getAbstractionFormula().getBlockFormula().getFormula();
      itpStack.add(itpProver.push(blockFormula));
      currentPath.add(succ);
      try {
        satCheckTime.start();
        boolean isUnsat = itpProver.isUnsat();
        satCheckTime.stop();
        if (isUnsat) {
          logger.log(Level.FINE, "Found unreachable state", succ);
          List<ARGState> abstractionStatesTrace = new ArrayList<>(currentPath);

          ARGState cur = succ;
          while (successors.containsKey(cur)) {
            // we just always use the first child, as every interpolant
            // below the unreacheable state will be false anyway we don't need
            // to have all paths to all reachable error states
            ARGState tmp = successors.get(cur).iterator().next();
            abstractionStatesTrace.add(tmp);
            cur = tmp;
          }
          assert cur.isTarget() : "Last state in path has to be a target state";

          performRefinementOnPath(
              unmodifiableList(itpStack), succ, abstractionStatesTrace, pReached, itpProver);

        } else if (targets.contains(succ)) {
          // We have found a reachable target state, immediately abort refinement.
          logger.log(Level.FINE, "Found reachable target state", succ);
          return Optional.of(succ);

        } else {
          // Not yet infeasible, but path is longer,
          // so descend recursively.
          Optional<ARGState> tmp =
              step(currentPath, itpStack, successors, predecessors, pReached, targets, itpProver);

          if (tmp.isPresent()) {
            return tmp;
          }
        }

      } finally {
        itpStack.remove(itpStack.size() - 1);
        itpProver.pop();
        currentPath.remove(currentPath.size() - 1);
      }
    }
    return Optional.empty();
  }

  /**
   * Actually perform refinement on one path. We compute the interpolants from
   * the first state to the unreachable one.
   *
   * @param itpStack The list with the interpolation groups.
   * @param unreachableState The first state in the path which is infeasible (this identifies the path).
   * @param pAbstractionStatesTrace The complete trace of abstraction states including the unreachable state
   * @param reached The reached set.
   */
  private <T> void performRefinementOnPath(
      List<T> itpStack,
      final ARGState unreachableState,
      List<ARGState> pAbstractionStatesTrace,
      ARGReachedSet reached,
      InterpolatingProverEnvironment<T> itpProver)
      throws CPAException, SolverException, InterruptedException {
    assert !itpStack.isEmpty();
    assert bfmgr.isFalse(itpProver.getInterpolant(itpStack)); // last interpolant is False

    pAbstractionStatesTrace = FluentIterable.from(pAbstractionStatesTrace).skip(1).toList();
    List<BooleanFormula> interpolants = Lists.newArrayList();

    boolean visitedUnreachable = false;
    int sublistCounter = 1;
    for (ARGState state : pAbstractionStatesTrace) {
      interpolationTime.start();
      visitedUnreachable = visitedUnreachable || state.equals(unreachableState);

      if (visitedUnreachable) {
        interpolants.add(
            bfmgr.makeBoolean(
                false)); // fill up interpolants with false as the states are unreachable
      } else {
        interpolants.add(itpProver.getInterpolant(itpStack.subList(0, sublistCounter)));
        sublistCounter++;
      }
      interpolationTime.stop();
    }

    // last interpolant will always be false and therefore it is required
    // to remove it, for having proper arguments to call performRefinement
    interpolants.remove(interpolants.size() - 1);

    // TODO repeated counterexample is always false currently, we also ignore the return value
    @SuppressWarnings("unused")
    boolean trackFurtherCEX =
        strategy.performRefinement(reached, pAbstractionStatesTrace, interpolants, false);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
  }

  private class Stats implements Statistics {

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      StatisticsWriter w0 = writingStatisticsTo(out);
      int numberOfRefinements = totalTime.getUpdateCount();
      w0.put("Number of predicate refinements", numberOfRefinements);
      if (numberOfRefinements > 0) {
        w0.put(totalTime).put(interpolationTime).put(satCheckTime);
      }
    }

    @Override
    public String getName() {
      return "Predicate Global Refiner";
    }
  }
}
