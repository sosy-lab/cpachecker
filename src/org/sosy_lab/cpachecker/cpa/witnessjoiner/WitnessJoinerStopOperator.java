// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.witnessjoiner;

import java.util.Collection;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class WitnessJoinerStopOperator implements StopOperator {

  private final StopOperator wrappedStop;

  public WitnessJoinerStopOperator(StopOperator pWrappedStop) {
    wrappedStop = pWrappedStop;
  }

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {
    return wrappedStop.stop(
        ((WitnessJoinerState) pState).getWrappedState(),
        Collections3.transformedImmutableListCopy(
            pReached, state -> ((WitnessJoinerState) state).getWrappedState()),
        pPrecision);
  }
}
