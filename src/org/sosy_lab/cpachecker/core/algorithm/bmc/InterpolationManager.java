// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This class manages the interpolation-related operations for {@link IMCAlgorithm} and {@link
 * ISMCAlgorithm}, which include
 *
 * <ul>
 *   <li>pushing/popping formulas onto/from the solver stack, and
 *   <li>deriving interpolants from unsatisfiable path formulas with different derivation
 *       directions.
 * </ul>
 */
class InterpolationManager<T> {
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
  enum ItpDeriveDirection {
    FORWARD,
    BACKWARD,
    BIDIRECTION_CONJUNCT,
    BIDIRECTION_DISJUNCT
  }

  private enum Satisfiability {
    SAT,
    UNSAT,
    UNKNOWN
  }

  private final BooleanFormulaManagerView bfmgr;
  private final InterpolatingProverEnvironment<T> itpProver;
  private final ItpDeriveDirection itpDeriveDirection;
  private final List<T> pushedFormulas;
  private Satisfiability satStatus;

  InterpolationManager(
      BooleanFormulaManagerView bfmgr,
      InterpolatingProverEnvironment<T> itpProver,
      ItpDeriveDirection itpDeriveDirection) {
    this.bfmgr = bfmgr;
    this.itpProver = itpProver;
    this.itpDeriveDirection = itpDeriveDirection;
    pushedFormulas = new ArrayList<>();
    satStatus = Satisfiability.UNKNOWN;
  }

  private void resetSatStatus() {
    satStatus = Satisfiability.UNKNOWN;
  }

  /** Returned the number of formulas pushed onto the solver stack. */
  int getNumPushedFormulas() {
    return pushedFormulas.size();
  }

  /** Push a Boolean formula onto the solver stack. */
  void push(BooleanFormula formula) throws InterruptedException {
    push(ImmutableList.of(formula));
  }

  /** Push a list of Boolean formulas onto the solver stack. */
  void push(List<BooleanFormula> formulas) throws InterruptedException {
    for (BooleanFormula f : formulas) {
      pushedFormulas.add(itpProver.push(f));
    }
    resetSatStatus();
  }

  /** Pop all the formulas from the solver stack. */
  void popAll() {
    pop(pushedFormulas.size());
  }

  /** Pop a formula from the solver stack. */
  void pop() {
    pop(1);
  }

  /** Pop the given number of formulas from the solver stack. */
  void pop(int n) {
    if (n > pushedFormulas.size()) {
      throw new IllegalArgumentException(
          "Cannot pop " + n + "elements from a solver stack of size " + pushedFormulas.size());
    }

    for (int i = 0; i < n; ++i) {
      itpProver.pop();
      pushedFormulas.remove(pushedFormulas.size() - 1);
    }
    resetSatStatus();
  }

  /** Return whether the conjunction of all the pushed formulas is unsatisfiable. */
  boolean isUnsat() throws SolverException, InterruptedException {
    if (satStatus == Satisfiability.UNKNOWN) { // avoid redundant checks
      satStatus = itpProver.isUnsat() ? Satisfiability.UNSAT : Satisfiability.SAT;
    }
    return satStatus == Satisfiability.UNSAT;
  }

  /**
   * A helper method to derive an interpolant according to the derivation direction.
   *
   * @param formulaA Formula A (prefix)
   * @param formulaB Formula B (suffix)
   * @return A {@code BooleanFormula} interpolant
   * @throws InterruptedException on shutdown request
   */
  private BooleanFormula getInterpolantFrom(final List<T> formulaA, final List<T> formulaB)
      throws SolverException, InterruptedException {
    switch (itpDeriveDirection) {
      case FORWARD:
        {
          return itpProver.getInterpolant(formulaA);
        }
      case BACKWARD:
        {
          return bfmgr.not(itpProver.getInterpolant(formulaB));
        }
      case BIDIRECTION_CONJUNCT:
        {
          return bfmgr.and(
              itpProver.getInterpolant(formulaA), bfmgr.not(itpProver.getInterpolant(formulaB)));
        }
      case BIDIRECTION_DISJUNCT:
        {
          return bfmgr.or(
              itpProver.getInterpolant(formulaA), bfmgr.not(itpProver.getInterpolant(formulaB)));
        }
      default:
        {
          throw new IllegalArgumentException(
              "InterpolationHelper does not support ItpDeriveDirection=" + itpDeriveDirection);
        }
    }
  }

  /**
   * Derive the interpolant at the given position.
   *
   * <p>The conjunction of the first {@code pos + 1} formulas are taken as the prefix formula
   * <i>A</i>, and the remaining are taken as the prefix formula <i>B</i>. If the argument {@code
   * reverse} is false, <i>I = ITP(A, B)</i> is returned, where <i>A &rarr; I</i> and <i>I &and; B =
   * &perp;</i>; otherwise, <i>I = ITP(B, A)</i> is returned, where <i>B &rarr; I</i> and <i>I &and;
   * A = &perp</i>;.
   *
   * <p>Note that the conjunction of all the pushed formulas has to be unsatisfiable in order to
   * derive an interpolant.
   *
   * @throws InterruptedException On shutdown request.
   */
  BooleanFormula getInterpolantAt(int pos, boolean reverse)
      throws SolverException, InterruptedException {
    if (!isUnsat()) {
      throw new AssertionError("The formula must be UNSAT to retrieve the interpolant.");
    }
    List<T> formulaA = pushedFormulas.subList(0, pos + 1);
    List<T> formulaB = pushedFormulas.subList(pos + 1, pushedFormulas.size());

    return reverse
        ? getInterpolantFrom(formulaB, formulaA)
        : getInterpolantFrom(formulaA, formulaB);
  }

  /**
   * Derive the interpolant at the given position. It is equivalent to {@link
   * InterpolationManager#getInterpolantAt(int, boolean)} with the second argument set to false.
   *
   * @see InterpolationManager#getInterpolantAt(int, boolean)
   */
  BooleanFormula getInterpolantAt(int pos) throws SolverException, InterruptedException {
    return getInterpolantAt(pos, false);
  }

  /**
   * Derive an interpolation sequence within the given index range.
   *
   * <p>Internally, the operation is delegated to {@link InterpolationManager#getInterpolantAt(int,
   * boolean)} with the first argument iteratively set to a number within {@code [fromIndex,
   * toIndex)} in ascending order.
   *
   * @throws InterruptedException On shutdown request.
   * @see InterpolationManager#getInterpolantAt(int, boolean)
   */
  List<BooleanFormula> getInterpolationSequence(int fromIndex, int toIndex, boolean reverse)
      throws SolverException, InterruptedException {
    List<BooleanFormula> itpSequence = new ArrayList<>(toIndex - fromIndex);
    for (int i = fromIndex; i < toIndex; ++i) {
      itpSequence.add(getInterpolantAt(i, reverse));
    }
    return ImmutableList.copyOf(itpSequence);
  }

  /**
   * Derive an interpolation sequence within the given index range. It is equivalent to {@link
   * InterpolationManager#getInterpolationSequence(int, int, boolean)} with the third argument set
   * to false.
   *
   * @see InterpolationManager#getInterpolationSequence(int, int, boolean)
   */
  List<BooleanFormula> getInterpolationSequence(int fromIndex, int toIndex)
      throws SolverException, InterruptedException {
    return getInterpolationSequence(fromIndex, toIndex, false);
  }
}
