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
package org.sosy_lab.cpachecker.cpa.composite;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.FluentIterable.from;

import java.util.Collections;
import java.util.Iterator;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.NonMergeableAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

/**
 * Provides a MergeOperator implementation that delegates to the component CPA.
 * If any of those CPAs returns an state that does not cover both its input
 * states, this implementation returns its second input state
 * (i.e., it behaves like MergeSep).
 *
 * This operator is good for the combination of CPAs where some CPAs never merge
 * and some may merge.
 *
 * Note that the definition of MergeOperator already requires that the returned
 * state covers the second input state. This implementation relies on that
 * guarantee and always assumes this is true.
 */
public class CompositeMergeAgreeOperator implements MergeOperator {

  private static final Predicate<Object> NON_MERGEABLE_STATE = instanceOf(NonMergeableAbstractState.class);

  private final ImmutableList<MergeOperator> mergeOperators;
  private final ImmutableList<StopOperator> stopOperators;

  public CompositeMergeAgreeOperator(ImmutableList<MergeOperator> mergeOperators, ImmutableList<StopOperator> stopOperators) {
    this.mergeOperators = mergeOperators;
    this.stopOperators  = stopOperators;
  }

  @Override
  public AbstractState merge(AbstractState successorState,
                               AbstractState reachedState,
                               Precision precision) throws CPAException, InterruptedException {

    // Merge Sep Code
    CompositeState compSuccessorState = (CompositeState) successorState;
    CompositeState compReachedState   = (CompositeState) reachedState;
    CompositePrecision compPrecision  = (CompositePrecision) precision;

    assert (compSuccessorState.getNumberOfStates() == compReachedState.getNumberOfStates());

    if (from(compSuccessorState.getWrappedStates()).anyMatch(NON_MERGEABLE_STATE)
        || from(compReachedState.getWrappedStates()).anyMatch(NON_MERGEABLE_STATE)) {
      // one CPA asks us to not merge at all
      return reachedState;
    }

    ImmutableList.Builder<AbstractState> mergedStates = ImmutableList.builder();
    Iterator<StopOperator> stopIter   = stopOperators.iterator();
    Iterator<AbstractState> comp1Iter = compSuccessorState.getWrappedStates().iterator();
    Iterator<AbstractState> comp2Iter = compReachedState.getWrappedStates().iterator();
    Iterator<Precision> precIter      = compPrecision.getPrecisions().iterator();

    boolean identicalStates = true;
    for (MergeOperator mergeOp : mergeOperators) {
      AbstractState absSuccessorState = comp1Iter.next();
      AbstractState absReachedState   = comp2Iter.next();

      Precision prec      = precIter.next();
      StopOperator stopOp = stopIter.next();

      AbstractState mergedState = mergeOp.merge(absSuccessorState, absReachedState, prec);

      // Check if 'mergedState' also covers 'absSuccessorState', i.e., if 'mergeOp' performed a join.
      // By definition of MergeOperator, we know it covers 'absReachedState'.
      if (!stopOp.stop(absSuccessorState, Collections.singleton(mergedState), prec)) {
        // the result of merge does not cover 'absSuccessorState'
        // (which is the successor state currently considered by the CPAAlgorithm
        // We prevent merging for all CPAs in this case, because the current successor
        // state would not be covered anyway, so widening other states is just a loss of precision.
        return reachedState;
      }

      if (mergedState != absReachedState) {
        identicalStates = false;
      }

      mergedStates.add(mergedState);
    }

    if (identicalStates) {
      return reachedState;
    } else {
      return new CompositeState(mergedStates.build());
    }
  }
}
