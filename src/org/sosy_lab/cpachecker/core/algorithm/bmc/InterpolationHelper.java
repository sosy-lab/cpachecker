// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

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
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

public final class InterpolationHelper {
  /**
   * Represent the direction to derive interpolants.
   *
   * <ul>
   *   <li>{@code FORWARD}: compute interpolants from the prefix <i>itp(A, B)</i>.
   *   <li>{@code BACKWARD}: compute interpolants from the suffix <i>!itp(B, A)</i>.
   *   <li>{@code BIDIRECTION_CONJUNCT}: compute interpolants from both the prefix and the suffix
   *       and conjunct the two <i>itp(A, B) &and; !itp(B, A)</i>.
   *   <li>{@code BIDIRECTION_DISJUNCT}: compute interpolants from both the prefix and the suffix
   *       and disjunct the two <i>itp(A, B) &or; !itp(B, A)</i>.
   * </ul>
   */
  public enum ItpDeriveDirection {
    FORWARD,
    BACKWARD,
    BIDIRECTION_CONJUNCT,
    BIDIRECTION_DISJUNCT
  }

  /**
   * A helper method to derive an interpolant. It computes either <i>itp(A, B)</i>, <i>!itp(B,
   * A)</i>, <i>itp(A, B) &and; !itp(B, A)</i>, or <i>itp(A, B) &or; !itp(B, A)</i> according to the
   * given direction.
   *
   * @param bfmgr Boolean formula manager
   * @param itpProver SMT solver stack
   * @param itpDeriveDirection the direction to derive an interplant
   * @param formulaA Formula A (prefix)
   * @param formulaB Formula B (suffix)
   * @return A {@code BooleanFormula} interpolant
   * @throws InterruptedException On shutdown request.
   */
  static <T> BooleanFormula getInterpolantFrom(
      BooleanFormulaManagerView bfmgr,
      InterpolatingProverEnvironment<T> itpProver,
      ItpDeriveDirection itpDeriveDirection,
      final List<T> formulaA,
      final List<T> formulaB)
      throws SolverException, InterruptedException {
    BooleanFormula forwardItp = itpProver.getInterpolant(formulaA);
    BooleanFormula backwardItp = bfmgr.not(itpProver.getInterpolant(formulaB));
    switch (itpDeriveDirection) {
      case FORWARD:
        {
          return forwardItp;
        }
      case BACKWARD:
        {
          return backwardItp;
        }
      case BIDIRECTION_CONJUNCT:
        {
          return bfmgr.and(forwardItp, backwardItp);
        }
      case BIDIRECTION_DISJUNCT:
        {
          return bfmgr.or(forwardItp, backwardItp);
        }
      default:
        {
          throw new IllegalArgumentException(
              "InterpolationHelper does not support ItpDeriveDirection=" + itpDeriveDirection);
        }
    }
  }

  // Utility functions for IMC and ISMC

  static boolean hasCoveredStates(final ReachedSet pReachedSet) {
    return !from(pReachedSet).transformAndConcat(e -> ((ARGState) e).getCoveredByThis()).isEmpty();
  }

  static void removeUnreachableTargetStates(ReachedSet pReachedSet) {
    if (pReachedSet.wasTargetReached()) {
      TargetLocationCandidateInvariant.INSTANCE.assumeTruth(pReachedSet);
    }
  }

  /**
   * A method to check whether interpolation is applicable. For interpolation to be applicable, ARG
   * must satisfy 1) no covered states exist and 2) there is a unique stop state. If there are
   * multiple stop states and the option {@code removeUnreachableStopStates} is {@code true}, this
   * method will remove unreachable stop states and only disable interpolation if there are multiple
   * reachable stop states. Enabling this option indeed increases the number of solved tasks, but
   * also results in some wrong proofs.
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

  static FluentIterable<ARGState> getAbstractionStatesToRoot(AbstractState pTargetState) {
    return from(ARGUtils.getOnePathTo((ARGState) pTargetState).asStatesList())
        .filter(PredicateAbstractState::containsAbstractionState);
  }

  private static boolean isTargetStateAfterLoopStart(AbstractState pTargetState) {
    return getAbstractionStatesToRoot(pTargetState).size() > 2;
  }

  static FluentIterable<AbstractState> getTargetStatesAfterLoop(final ReachedSet pReachedSet) {
    return AbstractStates.getTargetStates(pReachedSet)
        .filter(InterpolationHelper::isTargetStateAfterLoopStart);
  }

  static PathFormula getPredicateAbstractionBlockFormula(AbstractState pState) {
    return PredicateAbstractState.getPredicateState(pState)
        .getAbstractionFormula()
        .getBlockFormula();
  }

  static BooleanFormula createDisjunctionFromStates(
      BooleanFormulaManagerView bfmgr, final FluentIterable<AbstractState> pStates) {
    return pStates.transform(e -> getPredicateAbstractionBlockFormula(e).getFormula()).stream()
        .collect(bfmgr.toDisjunction());
  }

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

  private static FluentIterable<AbstractState> getStopStates(final ReachedSet pReachedSet) {
    return from(pReachedSet)
        .filter(AbstractBMCAlgorithm::isStopState)
        .filter(AbstractBMCAlgorithm::isRelevantForReachability);
  }

  static BooleanFormula buildReachTargetStateFormula(
      BooleanFormulaManagerView bfmgr, final ReachedSet pReachedSet) {
    return buildReachFormulaForStates(bfmgr, AbstractStates.getTargetStates(pReachedSet));
  }

  static BooleanFormula buildBoundingAssertionFormula(
      BooleanFormulaManagerView bfmgr, final ReachedSet pReachedSet) {
    return buildReachFormulaForStates(bfmgr, getStopStates(pReachedSet));
  }

  static PathFormula makeFalsePathFormula(
      PathFormulaManager pfmgr, BooleanFormulaManagerView bfmgr) {
    return pfmgr.makeEmptyPathFormula().withFormula(bfmgr.makeFalse());
  }
}
