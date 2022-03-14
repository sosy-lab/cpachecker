// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.monitor;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class MonitorMerge implements MergeOperator {

  private ConfigurableProgramAnalysis wrappedCpa;

  public MonitorMerge(ConfigurableProgramAnalysis pWrappedCPA) {
    wrappedCpa = pWrappedCPA;
  }

  @Override
  public AbstractState merge(AbstractState pElement1, AbstractState pElement2, Precision pPrecision)
      throws CPAException, InterruptedException {
    MonitorState monitorState1 = (MonitorState) pElement1;
    MonitorState monitorState2 = (MonitorState) pElement2;

    if (monitorState1.mustDumpAssumptionForAvoidance()
        || monitorState2.mustDumpAssumptionForAvoidance()) {
      return pElement2;
    }

    MergeOperator mergeOperator = wrappedCpa.getMergeOperator();
    AbstractState wrappedState1 = monitorState1.getWrappedState();
    AbstractState wrappedState2 = monitorState2.getWrappedState();
    AbstractState retElement = mergeOperator.merge(wrappedState1, wrappedState2, pPrecision);
    if (retElement.equals(wrappedState2)) {
      return pElement2;
    }

    long totalTimeOnPath =
        Math.max(monitorState1.getTotalTimeOnPath(), monitorState2.getTotalTimeOnPath());

    MonitorState mergedElement = new MonitorState(retElement, totalTimeOnPath);

    return mergedElement;
  }
}
