// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.nondeterminism;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.nondeterminism.NondeterminismState.NondeterminismNonAbstractionState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class NondeterminismMergeOperator implements MergeOperator {

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    NondeterminismState state1 = (NondeterminismState) pState1;
    NondeterminismState state2 = (NondeterminismState) pState2;
    if (state1 instanceof NondeterminismNonAbstractionState
        && state2 instanceof NondeterminismNonAbstractionState) {
      return state1.join(state2);
    }
    return pState2;
  }
}
