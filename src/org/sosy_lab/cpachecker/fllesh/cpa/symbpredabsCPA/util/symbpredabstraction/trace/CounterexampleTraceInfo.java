/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.trace;

import java.util.Collection;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Model;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


/**
 * A class that stores information about a counterexample trace.
 * For spurious counterexamples, this stores a predicate map
 * with new predicates that are sufficient to rule out the trace in the
 * refined abstract model.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class CounterexampleTraceInfo {
    private final boolean spurious;
    private final Multimap<AbstractElement, Predicate> pmap;
    private final Model mCounterexample;

    public CounterexampleTraceInfo() {
      mCounterexample = null;
      spurious = true;
      pmap = ArrayListMultimap.create();
    }
    
    public CounterexampleTraceInfo(Model pModel) {
      Preconditions.checkNotNull(pModel);
      
      mCounterexample = pModel;
      spurious = false;
      pmap = HashMultimap.create();
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
    public Collection<Predicate> getPredicatesForRefinement(AbstractElement e) {
        return pmap.get(e);
    }

    /**
     * Adds some predicates to the list of those corresponding to the given
     * AbstractElement
     */
    public void addPredicatesForRefinement(AbstractElement e,
                                           Iterable<Predicate> preds) {
      pmap.putAll(e, preds);
    }

    @Override
    public String toString() {
      return "Spurious: " + isSpurious() +
        (isSpurious() ? ", new predicates: " + pmap : "");
    }
    
    public boolean hasCounterexample() {
      return (mCounterexample != null);
    }
    
    public Model getCounterexample() {
      Preconditions.checkState(hasCounterexample());
      
      return mCounterexample;
    }
    
}
