// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Iterator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.NonMergeableAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Provides a MergeOperator implementation that delegates to the component CPA. If any of those CPAs
 * returns an state that does not cover both its input states, this implementation returns its
 * second input state (i.e., it behaves like MergeSep).
 *
 * <p>This operator is good for the combination of CPAs where some CPAs never merge and some may
 * merge.
 *
 * <p>Note that the definition of MergeOperator already requires that the returned state covers the
 * second input state. This implementation relies on that guarantee and always assumes this is true.
 */
class CompositeMergeAgreeOperator implements MergeOperator {

  private final ImmutableList<MergeOperator> mergeOperators;
  private final ImmutableList<StopOperator> stopOperators;

  CompositeMergeAgreeOperator(
      ImmutableList<MergeOperator> mergeOperators, ImmutableList<StopOperator> stopOperators) {
    this.mergeOperators = mergeOperators;
    this.stopOperators = stopOperators;
  }

  @Override
  public AbstractState merge(
      AbstractState successorState, AbstractState reachedState, Precision precision)
      throws CPAException, InterruptedException {

    // Merge Sep Code
    CompositeState compSuccessorState = (CompositeState) successorState;
    CompositeState compReachedState = (CompositeState) reachedState;
    CompositePrecision compPrecision = (CompositePrecision) precision;

    assert (compSuccessorState.getNumberOfStates() == compReachedState.getNumberOfStates());

    if (hasNonMergeableState(compSuccessorState) || hasNonMergeableState(compReachedState)) {
      // one CPA asks us to not merge at all
      return reachedState;
    }

    ImmutableList.Builder<AbstractState> mergedStates = ImmutableList.builder();
    Iterator<StopOperator> stopIter = stopOperators.iterator();
    Iterator<AbstractState> comp1Iter = compSuccessorState.getWrappedStates().iterator();
    Iterator<AbstractState> comp2Iter = compReachedState.getWrappedStates().iterator();
    Iterator<Precision> precIter = compPrecision.getWrappedPrecisions().iterator();

    boolean identicalStates = true;
    for (MergeOperator mergeOp : mergeOperators) {
      AbstractState absSuccessorState = comp1Iter.next();
      AbstractState absReachedState = comp2Iter.next();

      Precision prec = precIter.next();
      StopOperator stopOp = stopIter.next();

      AbstractState mergedState = mergeOp.merge(absSuccessorState, absReachedState, prec);

      // Check if 'mergedState' also covers 'absSuccessorState', i.e., if 'mergeOp' performed a
      // join.
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

  private static boolean hasNonMergeableState(CompositeState state) {
    return from(state.getWrappedStates()).anyMatch(instanceOf(NonMergeableAbstractState.class));
  }
}
