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
package org.sosy_lab.cpachecker.cpa.assumptions.collector.progressobserver;

import java.util.Collection;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Stop operator for the observer CPA. Returns always true except when
 * the element is bottom.
 *
 * We return true in all cases except bottom because if all other CPAs in
 * the composition return true, then we should also return true to allow
 * the analysis to stop.
 *
 * In the case the element is bottom, we return false to force the bottom
 * element to be stored in the reach set. It will not have any successors
 * because of either the assumption collector, or the fact that its
 * transfer would return an empty set.
 *
 * To test whether an element is bottom, we use the must-stop reporting
 * (mustDumpAssumptionForAvoidance)
 *
 * @author g.theoduloz
 */
public class ProgressObserverStop implements StopOperator {

  @Override
  public boolean stop(AbstractElement pElement,
      Collection<AbstractElement> pReached, Precision pPrecision)
      throws CPAException {
    ProgressObserverElement observerElement = (ProgressObserverElement)pElement;
    return !observerElement.mustDumpAssumptionForAvoidance();
  }

  @Override
  public boolean stop(AbstractElement pElement, AbstractElement pReachedElement)
      throws CPAException {
    ProgressObserverElement observerElement = (ProgressObserverElement)pElement;
    return !observerElement.mustDumpAssumptionForAvoidance();
  }

}
