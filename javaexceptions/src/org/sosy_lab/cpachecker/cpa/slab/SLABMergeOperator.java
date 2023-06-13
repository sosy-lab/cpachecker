// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.slab;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class SLABMergeOperator implements MergeOperator {

  private SLABDomain domain;

  public SLABMergeOperator(SLABDomain pDomain) {
    domain = pDomain;
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    SLARGState state1 = (SLARGState) pState1;
    SLARGState state2 = (SLARGState) pState2;
    if (domain.isLessOrEqual(state1, state2)) {
      return state2;
    }
    if (domain.wrappedSubsumption(state1, state2)) {
      SLARGState result = new SLARGState(state2);
      result.useAsReplacement(state1, state2);
      return result;
    }
    return state2; // this is not a join!!
  }
}
