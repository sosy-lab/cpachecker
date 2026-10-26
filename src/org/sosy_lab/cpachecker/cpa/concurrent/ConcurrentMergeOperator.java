// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import java.util.Map.Entry;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class ConcurrentMergeOperator implements MergeOperator {

  private final MergeOperator wrappedMerge;

  ConcurrentMergeOperator(ConfigurableProgramAnalysis pWrappedCpa) {
    wrappedMerge = pWrappedCpa.getMergeOperator();
  }

  @Override
  public AbstractState merge(AbstractState state1, AbstractState state2, Precision precision)
      throws CPAException, InterruptedException {
    if (state1 instanceof ConcurrentState pConcurrentState1
        && state2 instanceof ConcurrentState pConcurrentState2
        && precision instanceof ConcurrentPrecision pConcurrentPrecision) {
      if (canMerge(pConcurrentState1, pConcurrentState2)) {
        AbstractState wrapped1 = pConcurrentState1.getWrappedState();
        AbstractState wrapped2 = pConcurrentState2.getWrappedState();
        Precision wrappedPrecision = pConcurrentPrecision.getWrappedPrecision();
        AbstractState mergedWrapped = wrappedMerge.merge(wrapped1, wrapped2, wrappedPrecision);
        if (wrapped2.equals(mergedWrapped)) {
          return pConcurrentState2;
        } else {
          return pConcurrentState1.withWrappedState(mergedWrapped);
        }
      }
    }
    return state2;
  }

  private boolean canMerge(ConcurrentState c1, ConcurrentState c2) {
    if (!c1.livePids.equals(c2.livePids)) {
      return false;
    }
    if (c1.threads.size() != c2.threads.size()) {
      return false;
    }
    for (Entry<Integer, ThreadState> entry : c1.threads.entrySet()) {
      ThreadState thisThreadState = entry.getValue();
      ThreadState otherThreadState = c2.threads.get(entry.getKey());
      if (otherThreadState == null) {
        return false;
      }
      if (!thisThreadState.getLocationNode().equals(otherThreadState.getLocationNode())) {
        return false;
      }
    }
    return true;
  }
}
