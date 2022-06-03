// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.termination;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TerminationMergeOperator implements MergeOperator {

  private final MergeOperator mergeOperator;

  public TerminationMergeOperator(MergeOperator pMergeOperator) {
    mergeOperator = Preconditions.checkNotNull(pMergeOperator);
  }

  @Override
  public TerminationState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    TerminationState state1 = (TerminationState) pState1;
    TerminationState state2 = (TerminationState) pState2;

    if (state1.isPartOfLoop() != state2.isPartOfLoop()) {
      return state2;

    } else {
      AbstractState wrappedState1 = state1.getWrappedState();
      AbstractState wrappedState2 = state2.getWrappedState();
      AbstractState mergedState = mergeOperator.merge(wrappedState1, wrappedState2, pPrecision);

      if (mergedState == wrappedState2) {
        return state2;
      } else {
        return state2.withWrappedState(mergedState);
      }
    }
  }
}
