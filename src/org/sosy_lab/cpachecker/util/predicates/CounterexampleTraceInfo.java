/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;


/**
 * A class that stores information about a counterexample trace.
 * For spurious counterexamples, this stores a predicate map
 * with new predicates that are sufficient to rule out the trace in the
 * refined abstract model.
 */
public class CounterexampleTraceInfo {
    private final boolean spurious;
    private final Map<AbstractElement, Set<AbstractionPredicate>> pmap;
    private final Model mCounterexampleModel;
    private final List<Formula> mCounterexampleFormula;
    private final Map<Integer, Boolean> branchingPreds;

    public CounterexampleTraceInfo() {
      mCounterexampleFormula = Collections.emptyList();
      mCounterexampleModel = null;
      spurious = true;
      pmap = new HashMap<AbstractElement, Set<AbstractionPredicate>>();
      branchingPreds = ImmutableMap.of();
    }

    public CounterexampleTraceInfo(List<Formula> pCounterexampleFormula, Model pModel, Map<Integer, Boolean> preds) {
      Preconditions.checkNotNull(pCounterexampleFormula);
      Preconditions.checkNotNull(pModel);

      mCounterexampleFormula = pCounterexampleFormula;
      mCounterexampleModel = pModel;
      spurious = false;
      pmap = ImmutableMap.of();
      branchingPreds = ImmutableMap.copyOf(preds);
    }

    public CounterexampleTraceInfo(List<Formula> pCounterexampleFormula) {

      mCounterexampleFormula = pCounterexampleFormula;
      mCounterexampleModel = null;
      spurious = false;
      pmap = null;
      branchingPreds = null;
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
    public Collection<AbstractionPredicate> getPredicatesForRefinement(AbstractElement e) {
        return pmap.get(e);
    }

    /**
     * Adds some predicates to the list of those corresponding to the given
     * AbstractElement
     */
    public void addPredicatesForRefinement(AbstractElement e,
                                           Collection<AbstractionPredicate> preds) {
      Set<AbstractionPredicate> currentSet = pmap.get(e);
      if (currentSet == null){
        currentSet = new HashSet<AbstractionPredicate>(preds.size());
      }
      currentSet.addAll(preds);
      pmap.put(e, currentSet);
    }

    public Set<AbstractElement> getPredicatesForRefinmentKeys() {
      return pmap.keySet();
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
