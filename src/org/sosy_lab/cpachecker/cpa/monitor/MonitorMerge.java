/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class MonitorMerge implements MergeOperator{

  private ConfigurableProgramAnalysis wrappedCpa;

  public MonitorMerge(ConfigurableProgramAnalysis pWrappedCPA) {
    wrappedCpa = pWrappedCPA;
  }

  @Override
  public AbstractElement merge(
      AbstractElement pElement1,
      AbstractElement pElement2, Precision pPrecision)
  throws CPAException {
    MonitorElement monitorElement1= (MonitorElement)pElement1;
    MonitorElement monitorElement2 = (MonitorElement)pElement2;

    if (monitorElement1.mustDumpAssumptionForAvoidance() || monitorElement2.mustDumpAssumptionForAvoidance()) {
      return pElement2;
    }

    MergeOperator mergeOperator = wrappedCpa.getMergeOperator();
    AbstractElement wrappedElement1 = monitorElement1.getWrappedElement();
    AbstractElement wrappedElement2 = monitorElement2.getWrappedElement();
    AbstractElement retElement = mergeOperator.merge(wrappedElement1, wrappedElement2, pPrecision);
    if(retElement.equals(wrappedElement2)){
      return pElement2;
    }

    long totalTimeOnPath = Math.max(monitorElement1.getTotalTimeOnPath(),
                                    monitorElement2.getTotalTimeOnPath());

    MonitorElement mergedElement = new MonitorElement(
        retElement, totalTimeOnPath);

    return mergedElement;
  }

}
