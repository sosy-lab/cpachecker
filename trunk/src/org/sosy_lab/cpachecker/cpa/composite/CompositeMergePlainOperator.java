// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Provides a MergeOperator implementation that just delegates to the component CPAs without any
 * further logic.
 */
class CompositeMergePlainOperator implements MergeOperator {

  private final ImmutableList<MergeOperator> mergeOperators;

  CompositeMergePlainOperator(ImmutableList<MergeOperator> mergeOperators) {
    this.mergeOperators = mergeOperators;
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

    ImmutableList.Builder<AbstractState> mergedStates = ImmutableList.builder();
    Iterator<AbstractState> iter1 = compSuccessorState.getWrappedStates().iterator();
    Iterator<AbstractState> iter2 = compReachedState.getWrappedStates().iterator();
    Iterator<Precision> iterPrec = compPrecision.getWrappedPrecisions().iterator();

    boolean identicalStates = true;
    for (MergeOperator mergeOp : mergeOperators) {
      AbstractState absSuccessorState = iter1.next();
      AbstractState absReachedState = iter2.next();
      AbstractState mergedState =
          mergeOp.merge(absSuccessorState, absReachedState, iterPrec.next());

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
