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
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

/**
 * A class that stores information about a counterexample trace. For spurious counterexamples, this
 * stores the interpolants.
 */
public class CounterexampleTraceInfo {
  private final boolean spurious;
  private final ImmutableList<BooleanFormula> interpolants;
  private final ImmutableList<ValueAssignment> mCounterexampleModel;
  private final ImmutableList<BooleanFormula> mCounterexampleFormula;
  private final ImmutableMap<Integer, Boolean> branchingPreds;

  private CounterexampleTraceInfo(
      boolean pSpurious,
      ImmutableList<BooleanFormula> pInterpolants,
      ImmutableList<ValueAssignment> pCounterexampleModel,
      ImmutableList<BooleanFormula> pCounterexampleFormula,
      ImmutableMap<Integer, Boolean> pBranchingPreds) {
    spurious = pSpurious;
    interpolants = pInterpolants;
    mCounterexampleModel = pCounterexampleModel;
    mCounterexampleFormula = pCounterexampleFormula;
    branchingPreds = pBranchingPreds;
  }

  public static CounterexampleTraceInfo infeasible(List<BooleanFormula> pInterpolants) {
    return new CounterexampleTraceInfo(
        true, ImmutableList.copyOf(pInterpolants), null, ImmutableList.of(), ImmutableMap.of());
  }

  public static CounterexampleTraceInfo infeasibleNoItp() {
    return new CounterexampleTraceInfo(true, null, null, ImmutableList.of(), ImmutableMap.of());
  }

  public static CounterexampleTraceInfo feasible(
      List<BooleanFormula> pCounterexampleFormula,
      Iterable<ValueAssignment> pModel,
      Map<Integer, Boolean> preds) {
    return new CounterexampleTraceInfo(
        false,
        ImmutableList.of(),
        ImmutableList.copyOf(pModel),
        ImmutableList.copyOf(pCounterexampleFormula),
        ImmutableMap.copyOf(preds));
  }

  public static CounterexampleTraceInfo feasibleNoModel() {
    return CounterexampleTraceInfo.feasible(
        ImmutableList.of(), ImmutableList.of(), ImmutableMap.of());
  }

  /**
   * checks whether this trace is a real bug or a spurious counterexample
   *
   * @return true if this trace is spurious, false otherwise
   */
  public boolean isSpurious() {
    return spurious;
  }

  /**
   * Returns the list of interpolants that were discovered during counterexample analysis.
   *
   * @return a list of interpolants
   */
  public List<BooleanFormula> getInterpolants() {
    checkState(spurious);
    return interpolants;
  }

  @Override
  public String toString() {
    return "Spurious: " + isSpurious() + (isSpurious() ? ", interpolants: " + interpolants : "");
  }

  public List<BooleanFormula> getCounterExampleFormulas() {
    checkState(!spurious);
    return mCounterexampleFormula;
  }

  public ImmutableList<ValueAssignment> getModel() {
    checkState(!spurious);
    return mCounterexampleModel;
  }

  public Map<Integer, Boolean> getBranchingPredicates() {
    checkState(!spurious);
    return branchingPreds;
  }
}
