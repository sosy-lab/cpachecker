// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.witnessjoiner;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class WitnessJoinerMergeOperator implements MergeOperator {

  private final MergeOperator wrappedMerge;

  public WitnessJoinerMergeOperator(MergeOperator pWrappedMerge) {
    wrappedMerge = pWrappedMerge;
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    if (pState1 instanceof WitnessJoinerState && pState2 instanceof WitnessJoinerState) {
      AbstractState wrappedState1 = ((WitnessJoinerState) pState1).getWrappedState();
      AbstractState wrappedState2 = ((WitnessJoinerState) pState2).getWrappedState();

      AbstractState wrappedMergeResult =
          wrappedMerge.merge(wrappedState1, wrappedState2, pPrecision);
      if (wrappedMergeResult == wrappedState2) {
        return pState2;
      }
      if (wrappedMergeResult == wrappedState1) {
        return pState1;
      }
      return new WitnessJoinerState(wrappedMergeResult);
    }
    return pState2;
  }
}
