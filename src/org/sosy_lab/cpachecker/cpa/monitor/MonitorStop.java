// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.monitor;

import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class MonitorStop implements StopOperator {

  private final ConfigurableProgramAnalysis wrappedCpa;

  public MonitorStop(ConfigurableProgramAnalysis cpa) {
    wrappedCpa = cpa;
  }

  @Override
  public boolean stop(
      AbstractState pElement, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {

    MonitorState monitorState = (MonitorState) pElement;
    if (monitorState.mustDumpAssumptionForAvoidance()) {
      return false;
    }

    AbstractState wrappedState = monitorState.getWrappedState();
    StopOperator stopOp = wrappedCpa.getStopOperator();

    for (AbstractState reachedState : pReached) {

      MonitorState monitorReachedState = (MonitorState) reachedState;
      if (monitorReachedState.mustDumpAssumptionForAvoidance()) {
        return false;
      }

      AbstractState wrappedReachedState = monitorReachedState.getWrappedState();

      if (stopOp.stop(wrappedState, Collections.singleton(wrappedReachedState), pPrecision)) {
        return true;
      }
    }

    return false;
  }
}
