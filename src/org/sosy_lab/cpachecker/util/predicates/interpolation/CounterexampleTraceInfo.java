// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.interpolation;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * A class that stores information about a counterexample trace. For spurious counterexamples, this
 * stores the interpolants, for real counterexamples information about the counterexample.
 */
public class CounterexampleTraceInfo {
  private final boolean spurious;
  private final ImmutableList<BooleanFormula> interpolants;
  private final ImmutableList<BooleanFormula> mCounterexampleFormula;
  private final ARGPath precisePath;

  private CounterexampleTraceInfo(
      boolean pSpurious,
      ImmutableList<BooleanFormula> pInterpolants,
      ImmutableList<BooleanFormula> pCounterexampleFormula,
      ARGPath pPrecisePath) {
    spurious = pSpurious;
    interpolants = pInterpolants;
    mCounterexampleFormula = pCounterexampleFormula;
    precisePath = pPrecisePath;
  }

  public static CounterexampleTraceInfo infeasible(List<BooleanFormula> pInterpolants) {
    return new CounterexampleTraceInfo(true, ImmutableList.copyOf(pInterpolants), null, null);
  }

  public static CounterexampleTraceInfo infeasibleNoItp() {
    return new CounterexampleTraceInfo(true, null, null, null);
  }

  public static CounterexampleTraceInfo feasible(
      List<BooleanFormula> pCounterexampleFormula, ARGPath pPrecisePath) {
    return new CounterexampleTraceInfo(
        false, ImmutableList.of(), ImmutableList.copyOf(pCounterexampleFormula), pPrecisePath);
  }

  public static CounterexampleTraceInfo feasibleImprecise(
      List<BooleanFormula> pCounterexampleFormula) {
    return CounterexampleTraceInfo.feasible(pCounterexampleFormula, null);
  }

  /** Return whether this trace is a feasible path or a spurious counterexample. */
  public boolean isSpurious() {
    return spurious;
  }

  /**
   * Returns the list of interpolants that were discovered during counterexample analysis. Available
   * if this counterexample is spurious.
   *
   * @return a list of interpolants
   */
  public ImmutableList<BooleanFormula> getInterpolants() {
    checkState(spurious);
    return interpolants;
  }

  @Override
  public String toString() {
    return "Spurious: " + isSpurious() + (isSpurious() ? ", interpolants: " + interpolants : "");
  }

  /**
   * Return a satisfiable list of formulas representing this counterexample. Available if this
   * counterexample is not spurious.
   */
  public ImmutableList<BooleanFormula> getCounterExampleFormulas() {
    checkState(!spurious);
    return mCounterexampleFormula;
  }

  /**
   * Return a precise representation of this counterexample in the ARG. Only available if this
   * counterexample is not spurious, but could be <code>null</code> if it could not be computed.
   */
  public @Nullable ARGPath getPrecisePath() {
    checkState(!spurious);
    return precisePath;
  }
}
