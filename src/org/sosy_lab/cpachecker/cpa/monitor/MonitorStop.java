/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
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
  public boolean stop(AbstractElement pElement,
      Collection<AbstractElement> pReached, Precision pPrecision) throws CPAException {

    MonitorElement monitorElement = (MonitorElement)pElement;

    for (AbstractElement reachedElement : pReached) {
      MonitorElement monitorReachedElement = (MonitorElement)reachedElement;
      if (stop(monitorElement, monitorReachedElement)) {
        return true;
      }
    }
    return false;

  }

  public boolean stop(MonitorElement pElement, MonitorElement pReachedElement)
                                                      throws CPAException {

    AbstractElement wrappedElement = pElement.getWrappedElement();
    AbstractElement wrappedReachedElement = pReachedElement.getWrappedElement();
    StopOperator stopOp = wrappedCpa.getStopOperator();
    return stopOp.stop(wrappedElement, wrappedReachedElement);
  }

  @Override
  public boolean stop(AbstractElement pElement, AbstractElement pReachedElement)
      throws CPAException {
    return stop((MonitorElement)pElement, (MonitorElement)pReachedElement);
  }

}
