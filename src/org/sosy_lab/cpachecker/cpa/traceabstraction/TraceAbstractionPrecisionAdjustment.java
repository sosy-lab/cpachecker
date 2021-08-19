// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.core.defaults.SingleWrapperPrecision;
import org.sosy_lab.cpachecker.core.defaults.SingleWrappingPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;

public class TraceAbstractionPrecisionAdjustment extends SingleWrappingPrecisionAdjustment {

  protected TraceAbstractionPrecisionAdjustment(PrecisionAdjustment pWrappedPrecOp) {
    super(pWrappedPrecOp);
  }

  @Override
  protected Precision createWrapperPrecision(
      Precision pPrecision, Precision pNewDelegatePrecision) {
    return new SingleWrapperPrecision(pNewDelegatePrecision);
  }

  @Override
  protected AbstractState createWrapperState(
      AbstractState pState, AbstractState pNewDelegateState) {
    Preconditions.checkArgument(
        pState instanceof TraceAbstractionState,
        "pState is expected to be of type TraceAbstractionState");

    TraceAbstractionState taState = (TraceAbstractionState) pState;
    return new TraceAbstractionState(pNewDelegateState, taState.getActivePredicates());
  }
}
