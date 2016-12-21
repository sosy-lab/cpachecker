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
package org.sosy_lab.cpachecker.util.predicates.interpolation;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Map;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;


/**
 * A class that stores information about a counterexample trace.
 * For spurious counterexamples, this stores the interpolants.
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
      return new CounterexampleTraceInfo(true,
          ImmutableList.copyOf(pInterpolants),
          null,
          ImmutableList.<BooleanFormula>of(),
          ImmutableMap.<Integer, Boolean>of()
          );
    }

    public static CounterexampleTraceInfo infeasibleNoItp() {
      return new CounterexampleTraceInfo(true,
          null,
          null,
          ImmutableList.<BooleanFormula>of(),
          ImmutableMap.<Integer, Boolean>of()
          );
    }

  public static CounterexampleTraceInfo feasible(
      List<BooleanFormula> pCounterexampleFormula,
      Iterable<ValueAssignment> pModel,
      Map<Integer, Boolean> preds) {
      return new CounterexampleTraceInfo(false,
          ImmutableList.<BooleanFormula>of(),
          ImmutableList.copyOf(pModel),
          ImmutableList.copyOf(pCounterexampleFormula),
          ImmutableMap.copyOf(preds)
          );
    }

    /**
     * checks whether this trace is a real bug or a spurious counterexample
     * @return true if this trace is spurious, false otherwise
     */
    public boolean isSpurious() { return spurious; }

    /**
     * Returns the list of interpolants that were discovered during
     * counterexample analysis.
     *
     * @return a list of interpolants
     */
    public List<BooleanFormula> getInterpolants() {
      checkState(spurious);
      return interpolants;
    }

    @Override
    public String toString() {
      return "Spurious: " + isSpurious() +
        (isSpurious() ? ", interpolants: " + interpolants : "");
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
