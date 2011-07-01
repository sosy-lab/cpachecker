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
package org.sosy_lab.cpachecker.cpa.art;

import java.util.Collection;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ARTStopSep implements StopOperator {

  private final ConfigurableProgramAnalysis wrappedCpa;

  public ARTStopSep(ConfigurableProgramAnalysis cpa) {
    this.wrappedCpa = cpa;
  }

  @Override
  public boolean stop(AbstractElement pElement,
      Collection<AbstractElement> pReached, Precision pPrecision) throws CPAException {

    ARTElement artElement = (ARTElement)pElement;

    for (AbstractElement reachedElement : pReached) {
      ARTElement artReachedElement = (ARTElement)reachedElement;
      if (stop(artElement, artReachedElement)) {
        return true;
      }
    }
    return false;

  }

  private boolean stop(ARTElement pElement, ARTElement pReachedElement)
                                                      throws CPAException {

    if (!pReachedElement.mayCover()) {
      return false;
    }

    AbstractElement wrappedElement = pElement.getWrappedElement();
    AbstractElement wrappedReachedElement = pReachedElement.getWrappedElement();

    StopOperator stopOp = wrappedCpa.getStopOperator();
    boolean stop = stopOp.stop(wrappedElement, wrappedReachedElement);

    if (stop) {
      if (pElement.getMergedWith() == pReachedElement) {
        pElement.removeFromART();
      } else {
        pElement.setCovered(pReachedElement);
      }
    }
    return stop;
  }

  @Override
  public boolean stop(AbstractElement pElement, AbstractElement pReachedElement)
      throws CPAException {
    return stop((ARTElement)pElement, (ARTElement)pReachedElement);
  }
}
