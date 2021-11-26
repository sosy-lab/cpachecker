// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
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
}
