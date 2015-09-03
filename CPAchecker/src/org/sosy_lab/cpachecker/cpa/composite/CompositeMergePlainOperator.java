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

import java.util.Iterator;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.ImmutableList;

/**
 * Provides a MergeOperator implementation that just delegates to the component
 * CPAs without any further logic.
 */
public class CompositeMergePlainOperator implements MergeOperator {

  private final ImmutableList<MergeOperator> mergeOperators;

  public CompositeMergePlainOperator(ImmutableList<MergeOperator> mergeOperators) {
    this.mergeOperators = mergeOperators;
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

    ImmutableList.Builder<AbstractState> mergedStates = ImmutableList.builder();
    Iterator<AbstractState> iter1 = compSuccessorState.getWrappedStates().iterator();
    Iterator<AbstractState> iter2 = compReachedState.getWrappedStates().iterator();
    Iterator<Precision> iterPrec  = compPrecision.getPrecisions().iterator();

    boolean identicalStates = true;
    for (MergeOperator mergeOp : mergeOperators) {
      AbstractState absSuccessorState = iter1.next();
      AbstractState absReachedState   = iter2.next();
      AbstractState mergedState       = mergeOp.merge(absSuccessorState, absReachedState, iterPrec.next());

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
