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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;


/**
 * A class that stores information about a counterexample trace.
 * For spurious counterexamples, this stores a predicate map
 * with new predicates that are sufficient to rule out the trace in the
 * refined abstract model.
 */
public class CounterexampleTraceInfo<I> {
    private final boolean spurious;
    private final List<I> pmap;
    private final Model mCounterexampleModel;
    private final List<Formula> mCounterexampleFormula;
    private final Map<Integer, Boolean> branchingPreds;

    public CounterexampleTraceInfo() {
      mCounterexampleFormula = Collections.emptyList();
      mCounterexampleModel = null;
      spurious = true;
      pmap = Lists.newArrayList();
      branchingPreds = ImmutableMap.of();
    }

    public CounterexampleTraceInfo(List<Formula> pCounterexampleFormula, Model pModel, Map<Integer, Boolean> preds) {
      Preconditions.checkNotNull(pCounterexampleFormula);
      Preconditions.checkNotNull(pModel);

      mCounterexampleFormula = pCounterexampleFormula;
      mCounterexampleModel = pModel;
      spurious = false;
      pmap = ImmutableList.of();
      branchingPreds = preds;
    }

    /**
     * checks whether this trace is a real bug or a spurious counterexample
     * @return true if this trace is spurious, false otherwise
     */
    public boolean isSpurious() { return spurious; }

    /**
     * returns the list of Predicates that were discovered during
     * counterexample analysis for the given AbstractElement. The invariant is
     * that the union of all the predicates for all the AbstractElements in
     * the spurious counterexample is sufficient for refining the abstract
     * model such that this trace is no longer feasible in it
     *
     * @return a list of predicates
     */
    public List<I> getPredicatesForRefinement() {
        return pmap;
    }

    /**
     * Adds some predicates to the list of those corresponding to the given
     * AbstractElement
     */
    public void addPredicatesForRefinement(I preds) {
      pmap.add(preds);
    }

    @Override
    public String toString() {
      return "Spurious: " + isSpurious() +
        (isSpurious() ? ", new predicates: " + pmap : "");
    }

    public boolean hasCounterexample() {
      return (mCounterexampleModel != null);
    }

    public List<Formula> getCounterExampleFormulas() {
      return mCounterexampleFormula;
    }

    public Model getCounterexample() {
      Preconditions.checkState(hasCounterexample());

      return mCounterexampleModel;
    }

    public Map<Integer, Boolean> getBranchingPredicates() {
      return branchingPreds;
    }
}
