// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Collections.unmodifiableList;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.div;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Refiner implementation that does "global" refinements, uses interpolation,
 * and performs an Impact-like update of the ARG.
 * Global refinements mean that we do not refine a path from the ARG root
 * to a single target states, but instead all paths from the ARG root to all
 * existing target states.
 * We do so by recursively traversing the ARG in a DFS order,
 * refining infeasible paths one by one.
 */
public class ImpactGlobalRefiner implements Refiner, StatisticsProvider {

  private final LogManager logger;

  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;
  private final ARGCPA argCpa;
  private final ImpactUtility impact;

  // statistics
  private int refinementCalls = 0;
  private int refinementIterations = 0;
  private int totalNumberOfTargetStates = 0;
  private int pathsRefined = 0;
  private int totalPathLengthToInfeasibility = 0; // measured in blocks
  private int totalNumberOfAffectedStates = 0;

  private final Timer totalTime = new Timer();
  private final Timer satCheckTime = new Timer();
  private final Timer getInterpolantTime = new Timer();
  private final Timer coverTime = new Timer();
  private final Timer argUpdate = new Timer();

  private void printStatistics(PrintStream out) {
    if (refinementCalls > 0) {
      out.println("Avg. number of iterations per refinement:   " + div(refinementIterations, refinementCalls));
      out.println("Avg. number of target states per iteration: " + div(totalNumberOfTargetStates, refinementIterations));
      out.println("Avg. number of refined paths per iteration: " + div(pathsRefined, refinementIterations));
      out.println("Avg. number of sat checks per iteration:    " + div(satCheckTime.getNumberOfIntervals(), refinementIterations));
      out.println("Avg. length of refined path (in blocks):    " + div(totalPathLengthToInfeasibility, pathsRefined));
      out.println("Avg. number of affected states per path:    " + div(totalNumberOfAffectedStates, pathsRefined));
      out.println();
      out.println("Total time for predicate refinement:  " + totalTime);
      out.println("  Refinement sat check:               " + satCheckTime);
      out.println("  Interpolant computation:            " + getInterpolantTime);
      out.println("  Checking whether itp is new:        " + impact.itpCheckTime);
      out.println("  Coverage checks:                    " + coverTime);
      out.println("  ARG update:                         " + argUpdate);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes", "resource"})
  public static ImpactGlobalRefiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, ImpactRefiner.class);
    return new ImpactGlobalRefiner(predicateCpa.getConfiguration(),
                                    predicateCpa.getLogger(),
                                    (ARGCPA)pCpa,
                                    predicateCpa.getSolver(),
                                    predicateCpa.getPredicateManager());
  }

  private ImpactGlobalRefiner(Configuration config, LogManager pLogger,
      ARGCPA pArgCpa,
      Solver pSolver, PredicateAbstractionManager pPredAbsMgr)
          throws InvalidConfigurationException {

    logger = pLogger;
    argCpa = pArgCpa;
    solver = pSolver;
    bfmgr = solver.getFormulaManager().getBooleanFormulaManager();
    impact = new ImpactUtility(config, solver.getFormulaManager(), pPredAbsMgr);

    if (impact.requiresPreviousBlockAbstraction()) {
      // With global refinements, we go backwards through the trace,
      // and thus can't supply the abstraction of the previous block.
      throw new InvalidConfigurationException("Computing block abstractions" +
          "during refinement is not supported when using global refinements.");
    }
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    totalTime.start();
    refinementCalls++;
    try {

      List<AbstractState> targets = from(pReached).filter(AbstractStates::isTargetState).toList();
      assert !targets.isEmpty();

      do {
        refinementIterations++;
        totalNumberOfTargetStates += targets.size();
        boolean successful = performRefinement0(pReached, targets);

        if (!successful) {
          return false;
        }

        // there might be target states which were previously covered
        // and are now uncovered

        targets = from(pReached).filter(AbstractStates::isTargetState).toList();

      } while (!targets.isEmpty());

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
   * {@link #performRefinementOnPath(List, ARGState, Map, ReachedSet, InterpolatingProverEnvironment)}
   * on the root state of the ARG.
   */
  private boolean performRefinement0(ReachedSet pReached, List <AbstractState> targets)
      throws CPAException, InterruptedException, SolverException {
    logger.log(Level.FINE, "Starting refinement for", targets.size(), "elements.");

    Map<ARGState, ARGState> predecessors = new HashMap<>();
    SetMultimap<ARGState, ARGState> successors = HashMultimap.create();

    Deque<AbstractState> todo = new ArrayDeque<>(targets);

    while (!todo.isEmpty()) {
      final ARGState currentAbstractionState = (ARGState)todo.removeFirst();
      assert currentAbstractionState.mayCover();

      ARGState currentState = currentAbstractionState;
      do {
        currentState = currentState.getParents().iterator().next();
      } while (!getPredicateState(currentState).isAbstractionState());

      if (!currentState.getParents().isEmpty()
          && !predecessors.containsKey(currentState)) {
        todo.add(currentState);
      }

      predecessors.put(currentAbstractionState, currentState);
      successors.put(currentState, currentAbstractionState);
    }
    final ARGState root = (ARGState)pReached.getFirstState();
    assert successors.containsKey(root);

    // Now predecessors/successors contains all abstraction states on all error
    // paths and their relations.
    // These states and the relation form a tree.
    // We now iterate through this tree in a depth-first order.
    // For each state, we check reachability.
    // We do not descend beyond unreachable states,
    // but instead perform refinement on them.

    try (InterpolatingProverEnvironment<?> itpProver = solver.newProverEnvironmentWithInterpolation()) {
      return performRefinement0(root, successors, predecessors, pReached, targets, itpProver);
    }
  }

  // This is just a separate method to get the generics right.
  // (The arguments of the list and the prover need to match.)
  private <T> boolean performRefinement0(ARGState current, SetMultimap<ARGState, ARGState> successors,
      Map<ARGState, ARGState> predecessors, ReachedSet pReached, List<AbstractState> targets,
      InterpolatingProverEnvironment<T> itpProver)
      throws InterruptedException, SolverException, CPAException {
    List<T> itpStack = new ArrayList<>();
    boolean successful = step(current, itpStack, successors, predecessors, pReached, targets, itpProver);
    assert itpStack.isEmpty();
    return successful;
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
   * {@link #performRefinementOnPath(List, ARGState, Map, ReachedSet, InterpolatingProverEnvironment)}
   * to do the actual refinement.
   *
   * Note that the successor and predecessor relation contains only states
   * that belong to paths to a target state, so we refine only such paths,
   * and not all paths in the ARG.
   *
   * @param current The ARG state that is the root of the to-be-refined ARG part.
   * @param itpStack The stack of interpolation groups added to the solver environment so far.
   * @param successors The successor relation between abstraction states.
   * @param predecessors The predecessor relation between abstraction states.
   * @param pReached The complete reached set.
   * @param targets The set of target states.
   * @return False if a feasible counterexample was found, True if refinement was successful.
   */
  private <T> boolean step(ARGState current, List<T> itpStack, SetMultimap<ARGState, ARGState> successors,
      Map<ARGState, ARGState> predecessors, ReachedSet pReached, List<AbstractState> targets,
      InterpolatingProverEnvironment<T> itpProver)
      throws InterruptedException, SolverException, CPAException {

    for (ARGState succ : successors.get(current)) {
      assert succ.getChildren().isEmpty() == targets.contains(succ);
      assert succ.mayCover();

      BooleanFormula blockFormula = getPredicateState(succ).getAbstractionFormula().getBlockFormula().getFormula();
      itpStack.add(itpProver.push(blockFormula));
      try {
        satCheckTime.start();
        boolean isUnsat = itpProver.isUnsat();
        satCheckTime.stop();
        if (isUnsat) {
          logger.log(Level.FINE, "Found unreachable state", succ);
          performRefinementOnPath(unmodifiableList(itpStack), succ, predecessors, pReached, itpProver);

        } else if (targets.contains(succ)) {
          // We have found a reachable target state, immediately abort refinement.
          logger.log(Level.FINE, "Found reachable target state", succ);
          return false;

        } else {
          // Not yet infeasible, but path is longer,
          // so descend recursively.
          boolean successful = step(succ, itpStack, successors, predecessors, pReached, targets, itpProver);

          if (!successful) {
            return false;
          }
        }

        if (!current.mayCover()) {
          // The refinement along the current path made the current part of the ARG covered.
          // This may happens if some state up in the path was strengthened with
          // an interpolant and is now covered.
          // In this case, we do not need to do anything further in this part.
          break;
        }
      } finally {
        itpStack.remove(itpStack.size()-1);
        itpProver.pop();
      }
    }
    return true;
  }

  /**
   * Actually perform refinement on one path.
   * We compute the interpolants and then start with the unreachable state
   * going back up in the ARG until the interpolants are simply "true",
   * calling {@link #performRefinementForState(BooleanFormula, ARGState)} once for each
   * interpolant and its corresponding state.
   * Afterwards we call {@link #finishRefinementOfPath(ARGState, List, ReachedSet)}
   * once.
   *
   * @param itpStack The list with the interpolation groups.
   * @param unreachableState The first state in the path which is infeasible (this identifies the path).
   * @param predecessors The predecessor relation of abstraction states.
   * @param reached The reached set.
   */
  private <T> void performRefinementOnPath(List<T> itpStack, final ARGState unreachableState,
      Map<ARGState, ARGState> predecessors, ReachedSet reached,
      InterpolatingProverEnvironment<T> itpProver) throws CPAException,
      SolverException, InterruptedException {
    assert !itpStack.isEmpty();
    assert bfmgr.isFalse(itpProver.getInterpolant(itpStack)); // last interpolant is False

    pathsRefined++;
    totalPathLengthToInfeasibility += itpStack.size();

    itpStack = new ArrayList<>(itpStack); // copy because we will modify it
    List<ARGState> affectedStates = new ArrayList<>();

    // going upwards from unreachableState refining states with interpolants
    ARGState currentState = unreachableState;
    do {
      itpStack.remove(itpStack.size()-1); // remove last
      currentState = predecessors.get(currentState);
      if (itpStack.isEmpty()) {
        assert currentState.getParents().isEmpty(); // we should have reached the ARG root
        assert bfmgr.isTrue(itpProver.getInterpolant(itpStack));
        break;
      }


      getInterpolantTime.start();
      BooleanFormula currentItp = itpProver.getInterpolant(itpStack);
      getInterpolantTime.stop();

      if (bfmgr.isTrue(currentItp)) {
        // from here to the ARG root, all interpolants will be True
        break;
      }

      if (performRefinementForState(currentItp, currentState)) {
        break;
      } else {
        affectedStates.add(currentState);
      }

    } while (!itpStack.isEmpty());
    totalNumberOfAffectedStates += affectedStates.size();

    affectedStates = Lists.reverse(affectedStates); // reverse so that they are in top-down order

    finishRefinementOfPath(unreachableState, affectedStates, reached);
  }

  /**
   * Perform refinement on one state given the interpolant that was determined
   * by the solver for this state. This method is only called for states for
   * which there is a non-trivial interpolant (i.e., neither True nor False).
   *
   * For each interpolant, we strengthen the corresponding state by
   * conjunctively adding the interpolant to its state formula.
   *
   * @param interpolant The interpolant (with SSA indices).
   * @param state The state.
   * @return True if no refinement was necessary (this implies that refinement
   *          on all of the state's parents is also not necessary)
   */
  private boolean performRefinementForState(BooleanFormula interpolant,
      ARGState state) throws SolverException, InterruptedException {

    // Passing null as lastAbstraction is ok because
    // we check for impact.requirePreviousBlockAbstraction() in the constructor.
    boolean stateChanged = impact.strengthenStateWithInterpolant(
                                                    interpolant, state, null);

    // If the interpolant is implied by the current state formula,
    // then we don't need any of the interpolants between the ARG root
    // and this state.
    return !stateChanged;
  }

  /**
   * Do any necessary work after one path has been refined.
   *
   * After a path was strengthened, we need to take care of the coverage relation.
   * We also remove the infeasible part from the ARG,
   * and re-establish the coverage invariant (i.e., that states on the path
   * are either covered or cannot be covered).
   *
   * @param unreachableState The first state in the path which is infeasible (this identifies the path).
   * @param affectedStates The list of states that were affected by the refinement (ordered from top of ARG to target state).
   * @param reached The reached set.
   */
  private void finishRefinementOfPath(final ARGState unreachableState, List<ARGState> affectedStates,
      ReachedSet reached) throws CPAException, InterruptedException {
    ARGReachedSet arg = new ARGReachedSet(reached, argCpa);

    argUpdate.start();
    for (ARGState w : affectedStates) {
      arg.removeCoverageOf(w);
    }

    // remove ARG part from unreachableState downwards
    arg.removeInfeasiblePartofARG(unreachableState);
    argUpdate.stop();

    coverTime.start();
    try {
      for (ARGState w : affectedStates) {
        if (arg.tryToCover(w)) {
          break; // all further elements are covered anyway
        }
      }
    } finally {
      coverTime.stop();
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      @Override
      public String getName() {
        return "ImpactGlobalRefiner";
      }

      @Override
      public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
        ImpactGlobalRefiner.this.printStatistics(pOut);
      }
    });
  }
}