/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
    this.wrappedCpa = cpa;
  }

  @Override
  public boolean stop(AbstractState pElement,
      Collection<AbstractState> pReached, Precision pPrecision) throws CPAException {

    MonitorElement monitorElement = (MonitorElement)pElement;
    if (monitorElement.mustDumpAssumptionForAvoidance()) {
      return false;
    }

    AbstractState wrappedElement = monitorElement.getWrappedElement();
    StopOperator stopOp = wrappedCpa.getStopOperator();

    for (AbstractState reachedElement : pReached) {

      MonitorElement monitorReachedElement = (MonitorElement)reachedElement;
      if (monitorReachedElement.mustDumpAssumptionForAvoidance()) {
        return false;
      }

      AbstractState wrappedReachedElement = monitorReachedElement.getWrappedElement();

      if (stopOp.stop(wrappedElement, Collections.singleton(wrappedReachedElement), pPrecision)) {
        return true;
      }
    }

    return false;
  }
}
