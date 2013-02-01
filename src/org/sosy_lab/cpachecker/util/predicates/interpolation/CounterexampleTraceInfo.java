/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static com.google.common.base.Preconditions.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;


/**
 * A class that stores information about a counterexample trace.
 * For spurious counterexamples, this stores the interpolants.
 */
public class CounterexampleTraceInfo {
    private final boolean spurious;
    private final List<BooleanFormula> interpolants;
    private final Model mCounterexampleModel;
    private final List<BooleanFormula> mCounterexampleFormula;
    private final Map<Integer, Boolean> branchingPreds;

    public CounterexampleTraceInfo() {
      mCounterexampleFormula = Collections.emptyList();
      mCounterexampleModel = null;
      spurious = true;
      interpolants = Lists.newArrayList();
      branchingPreds = ImmutableMap.of();
    }

    public CounterexampleTraceInfo(List<BooleanFormula> pCounterexampleFormula, Model pModel, Map<Integer, Boolean> preds) {
      mCounterexampleFormula = checkNotNull(pCounterexampleFormula);
      mCounterexampleModel = checkNotNull(pModel);
      spurious = false;
      interpolants = ImmutableList.of();
      branchingPreds = preds;
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

    /**
     * Add an interpolant to the end of the list of interpolants.
     */
    public void addInterpolant(BooleanFormula itp) {
      checkState(spurious);
      interpolants.add(itp);
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

    public Model getCounterexample() {
      checkState(!spurious);
      return mCounterexampleModel;
    }

    public Map<Integer, Boolean> getBranchingPredicates() {
      checkState(!spurious);
      return branchingPreds;
    }
}
