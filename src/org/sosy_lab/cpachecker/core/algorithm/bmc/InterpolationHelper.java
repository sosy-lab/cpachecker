// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This helper class provides utility functions for algorithms that require formulas encoding the
 * transition of a complete loop-unrolling in their analysis. Such algorithms, including {@link
 * IMCAlgorithm}, often assume single-loop programs as input and have to be used with large-blocking
 * encoding such that the block formulas of the abstraction states at loop heads represent the
 * transition formula of a complete loop. An ARG under this setting has a sequence of abstraction
 * states whose locations are the unique loop head.
 *
 * <p>This class implements methods to check if an ARG has the required shape and collect formulas
 * from such ARGs.
 */
public final class InterpolationHelper {
  /**
   * A method to check whether the ARG has the required shape. The ARG must satisfy 1) no covered
   * states exist and 2) there is a unique stop state. If there are multiple stop states and the
   * option {@code removeUnreachableStopStates} is {@code true}, this method will remove unreachable
   * stop states and only disable interpolation if there are multiple reachable stop states. Note:
   * enabling this option is potentially UNSOUND!
   *
   * @param pReachedSet Abstract Reachability Graph
   */
  static boolean checkAndAdjustARG(
      LogManager logger,
      ConfigurableProgramAnalysis cpa,
      BooleanFormulaManagerView bfmgr,
      Solver solver,
      ReachedSet pReachedSet,
      boolean removeUnreachableStopStates)
      throws SolverException, InterruptedException {
    if (hasCoveredStates(pReachedSet)) {
      logger.log(Level.WARNING, "Covered states in ARG: interpolation might be unsound!");
      return false;
    }
    FluentIterable<AbstractState> stopStates = getStopStates(pReachedSet);
    if (stopStates.size() > 1) {
      if (!removeUnreachableStopStates) {
        logger.log(Level.WARNING, "Multiple stop states: interpolation might be unsound!");
        return false;
      }
      List<AbstractState> unreachableStopStates =
          getUnreachableStopStates(bfmgr, solver, stopStates);
      boolean hasMultiReachableStopStates = (stopStates.size() - unreachableStopStates.size() > 1);
      if (!unreachableStopStates.isEmpty()) {
        logger.log(Level.FINE, "Removing", unreachableStopStates.size(), "unreachable stop states");
        ARGReachedSet reachedSetARG = new ARGReachedSet(pReachedSet, cpa);
        for (ARGState s : from(unreachableStopStates).filter(ARGState.class)) {
          reachedSetARG.removeInfeasiblePartofARG(s);
        }
      }
      if (hasMultiReachableStopStates) {
        logger.log(Level.WARNING, "Multi reachable stop states: interpolation might be unsound!");
        return false;
      }
    }
    return true;
  }

  static boolean hasCoveredStates(final ReachedSet pReachedSet) {
    return !from(pReachedSet).transformAndConcat(e -> ((ARGState) e).getCoveredByThis()).isEmpty();
  }

  private static FluentIterable<AbstractState> getStopStates(final ReachedSet pReachedSet) {
    return from(pReachedSet)
        .filter(AbstractBMCAlgorithm::isStopState)
        .filter(AbstractBMCAlgorithm::isRelevantForReachability);
  }

  private static List<AbstractState> getUnreachableStopStates(
      BooleanFormulaManagerView bfmgr,
      Solver solver,
      final FluentIterable<AbstractState> pStopStates)
      throws SolverException, InterruptedException {
    List<AbstractState> unreachableStopStates = new ArrayList<>();
    for (AbstractState stopState : pStopStates) {
      BooleanFormula reachFormula = buildReachFormulaForStates(bfmgr, FluentIterable.of(stopState));
      if (solver.isUnsat(reachFormula)) {
        unreachableStopStates.add(stopState);
      }
    }
    return unreachableStopStates;
  }

  static void removeUnreachableTargetStates(ReachedSet pReachedSet) {
    if (pReachedSet.wasTargetReached()) {
      TargetLocationCandidateInvariant.INSTANCE.assumeTruth(pReachedSet);
    }
  }

  /**
   * This method builds a formula that encodes the path formulas to the input goal states. It
   * assumes that the program is unrolled with large-block encoding and conjoins the block formulas
   * of the abstraction states to a goal state. If a goal state is not an abstraction state, it
   * additionally adds its path formula into the conjunction for completeness. The final formula is
   * a disjunction of all path formulas to individual goal states.
   *
   * @param bfmgr Boolean formula manager
   * @param pGoalStates The states for which we want to build path formulas
   * @return A formula encoding the reachability of the input goal states
   */
  private static BooleanFormula buildReachFormulaForStates(
      BooleanFormulaManagerView bfmgr, final FluentIterable<AbstractState> pGoalStates) {
    List<BooleanFormula> pathFormulas = new ArrayList<>();
    for (AbstractState goalState : pGoalStates) {
      BooleanFormula pathFormula =
          getAbstractionStatesToRoot(goalState)
              .transform(e -> getPredicateAbstractionBlockFormula(e).getFormula())
              .stream()
              .collect(bfmgr.toConjunction());
      if (!PredicateAbstractState.containsAbstractionState(goalState)) {
        pathFormula =
            bfmgr.and(
                pathFormula,
                PredicateAbstractState.getPredicateState(goalState).getPathFormula().getFormula());
      }
      pathFormulas.add(pathFormula);
    }
    return bfmgr.or(pathFormulas);
  }

  static BooleanFormula buildReachTargetStateFormula(
      BooleanFormulaManagerView bfmgr, final ReachedSet pReachedSet) {
    return buildReachFormulaForStates(bfmgr, AbstractStates.getTargetStates(pReachedSet));
  }

  static BooleanFormula buildBoundingAssertionFormula(
      BooleanFormulaManagerView bfmgr, final ReachedSet pReachedSet) {
    return buildReachFormulaForStates(bfmgr, getStopStates(pReachedSet));
  }

  static FluentIterable<ARGState> getAbstractionStatesToRoot(AbstractState pTargetState) {
    return from(ARGUtils.getOnePathTo((ARGState) pTargetState).asStatesList())
        .filter(PredicateAbstractState::containsAbstractionState);
  }

  static PathFormula getPredicateAbstractionBlockFormula(AbstractState pState) {
    return PredicateAbstractState.getPredicateState(pState)
        .getAbstractionFormula()
        .getBlockFormula();
  }

  static FluentIterable<AbstractState> getTargetStatesAfterLoop(final ReachedSet pReachedSet) {
    return AbstractStates.getTargetStates(pReachedSet)
        .filter(InterpolationHelper::isTargetStateAfterLoopStart);
  }

  private static boolean isTargetStateAfterLoopStart(AbstractState pTargetState) {
    return getAbstractionStatesToRoot(pTargetState).size() > 2;
  }

  static BooleanFormula createDisjunctionFromStates(
      BooleanFormulaManagerView bfmgr, final FluentIterable<AbstractState> pStates) {
    return pStates.transform(e -> getPredicateAbstractionBlockFormula(e).getFormula()).stream()
        .collect(bfmgr.toDisjunction());
  }

  static PathFormula makeFalsePathFormula(
      PathFormulaManager pfmgr, BooleanFormulaManagerView bfmgr) {
    return pfmgr.makeEmptyPathFormula().withFormula(bfmgr.makeFalse());
  }

  /**
   * This method stores the final fixed point in the abstraction formula of every abstraction state
   * at the loop head in order to generate correctness witnesses.
   *
   * <p>In predicate analysis, the invariant at a program location is obtained by taking the
   * disjunction of all abstraction formulas at this location. Therefore, it is necessary to set the
   * abstraction formula of every loop-head abstraction state to the fixed point; otherwise, the
   * invariant will be the tautology.
   *
   * @throws InterruptedException on shutdown request.
   */
  @SuppressWarnings("resource")
  static void storeFixedPointAsAbstractionAtLoopHeads(
      ReachedSet pReachedSet,
      final BooleanFormula pFixedPoint,
      final PredicateAbstractionManager pPredAbsMgr,
      final PathFormulaManager pPfmgr)
      throws InterruptedException {
    // Find all abstraction states: they are at same loop head due to single-loop assumption
    List<AbstractState> abstractionStates =
        from(pReachedSet)
            .skip(1) // skip the root
            .filter(not(AbstractStates::isTargetState)) // target states may be abstraction states
            .filter(PredicateAbstractState::containsAbstractionState)
            .toList();
    for (AbstractState state : abstractionStates) {
      PredicateAbstractState predState = PredicateAbstractState.getPredicateState(state);
      predState.setAbstraction(
          pPredAbsMgr.asAbstraction(pFixedPoint, pPfmgr.makeEmptyPathFormula()));
    }
  }
}
