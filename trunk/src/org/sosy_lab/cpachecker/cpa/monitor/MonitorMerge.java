/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
  public AbstractState merge(
      AbstractState pElement1,
      AbstractState pElement2, Precision pPrecision)
  throws CPAException, InterruptedException {
    MonitorState monitorState1= (MonitorState)pElement1;
    MonitorState monitorState2 = (MonitorState)pElement2;

    if (monitorState1.mustDumpAssumptionForAvoidance() || monitorState2.mustDumpAssumptionForAvoidance()) {
      return pElement2;
    }

    MergeOperator mergeOperator = wrappedCpa.getMergeOperator();
    AbstractState wrappedState1 = monitorState1.getWrappedState();
    AbstractState wrappedState2 = monitorState2.getWrappedState();
    AbstractState retElement = mergeOperator.merge(wrappedState1, wrappedState2, pPrecision);
    if (retElement.equals(wrappedState2)) {
      return pElement2;
    }

    long totalTimeOnPath = Math.max(monitorState1.getTotalTimeOnPath(),
                                    monitorState2.getTotalTimeOnPath());

    MonitorState mergedElement = new MonitorState(
        retElement, totalTimeOnPath);

    return mergedElement;
  }

}
