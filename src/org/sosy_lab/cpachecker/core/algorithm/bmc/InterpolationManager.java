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

public class InterpolationManager<T> {
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

  void push(BooleanFormula formula) throws InterruptedException {
    push(ImmutableList.of(formula));
  }

  void push(List<BooleanFormula> formulas) throws InterruptedException {
    for (BooleanFormula f : formulas) {
      pushedFormulas.add(itpProver.push(f));
    }
    resetSatStatus();
  }

  void popAll() {
    pop(pushedFormulas.size());
  }

  void pop() {
    pop(1);
  }

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

  boolean isUnsat() throws SolverException, InterruptedException {
    if (satStatus == Satisfiability.UNKNOWN) { // avoid redundant checks
      satStatus = itpProver.isUnsat() ? Satisfiability.UNSAT : Satisfiability.SAT;
    }
    return satStatus == Satisfiability.UNSAT;
  }

  /**
   * A helper method to derive an interpolant according to the given derivation direction.
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

  /** TODO: update description */
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
   * A helper method to derive an interpolation sequence. TODO: update description
   *
   * @throws InterruptedException On shutdown request.
   */
  List<BooleanFormula> getInterpolationSequence(int fromIndex, int toIndex, boolean reverse)
      throws SolverException, InterruptedException {
    List<BooleanFormula> itpSequence = new ArrayList<>(toIndex - fromIndex);
    for (int i = fromIndex; i < toIndex; ++i) {
      itpSequence.add(getInterpolantAt(i, reverse));
    }
    return itpSequence;
  }
}
