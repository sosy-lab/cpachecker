// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class AbstractWrapperStopOperator implements StopOperator {

  private final StopOperator wrappedStop;

  public AbstractWrapperStopOperator(StopOperator pWrappedStopOperator) {
    wrappedStop = pWrappedStopOperator;
  }

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {
    return wrappedStop.stop(pState, pReached, pPrecision);
  }

  protected StopOperator getWrappedStop() {
    return wrappedStop;
  }
}
