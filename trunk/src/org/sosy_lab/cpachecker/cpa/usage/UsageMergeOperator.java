// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class UsageMergeOperator implements MergeOperator {

  private final MergeOperator wrappedMerge;

  public UsageMergeOperator(MergeOperator wrapped) {
    wrappedMerge = wrapped;
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {

    UsageState uState1 = (UsageState) pState1;
    UsageState uState2 = (UsageState) pState2;
    UsagePrecision prec = (UsagePrecision) pPrecision;

    AbstractState wrappedState1 = uState1.getWrappedState();
    AbstractState wrappedState2 = uState2.getWrappedState();

    AbstractState mergedState =
        wrappedMerge.merge(wrappedState1, wrappedState2, prec.getWrappedPrecision());

    UsageState result;

    if (uState1.isLessOrEqual(uState2)) {
      result = uState2.copy(mergedState);
    } else if (uState2.isLessOrEqual(uState1)) {
      result = uState1.copy(mergedState);
    } else {
      result = uState1.copy(mergedState);
      result.joinRecentUsagesFrom(uState2);
    }

    if (mergedState.equals(wrappedState2) && result.equals(uState2)) {
      return pState2;
    } else {
      return result;
    }
  }
}
