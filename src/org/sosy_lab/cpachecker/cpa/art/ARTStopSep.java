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
import java.util.Collections;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ARTStopSep implements StopOperator {

  private final StopOperator wrappedStop;
  private final LogManager logger;

  public ARTStopSep(StopOperator pWrappedStop, LogManager pLogger) {
    wrappedStop = pWrappedStop;
    logger = pLogger;
  }

  @Override
  public boolean stop(AbstractElement pElement,
      Collection<AbstractElement> pReached, Precision pPrecision) throws CPAException {

    ARTElement artElement = (ARTElement)pElement;

    // First check if we can take a shortcut:
    // If the new element was merged into an existing element,
    // it is usually also covered by this existing element, so check this explicitly upfront.
    // We do this because we want to remove the new element from the ART completely
    // in this case and not mark it as covered.

    if (artElement.getMergedWith() != null) {
      ARTElement mergedWith = artElement.getMergedWith();

      if (pReached.contains(mergedWith)) {
        // we do this single check first as it should return true in most of the cases

        if (wrappedStop.stop(artElement.getWrappedElement(), Collections.singleton(mergedWith.getWrappedElement()), pPrecision)) {
          // merged and covered
          artElement.removeFromART();
          logger.log(Level.FINEST, "Element is covered by the element it was merged into");
          return true;

        } else {
          // unexpected case, but possible (if merge does not compute the join, but just widens e2)
          logger.log(Level.FINEST, "Element was merged but not covered:", pElement);
        }

      } else {
        // unexpected case, not sure if it this possible
        logger.log(Level.FINEST, "Element was merged into an element that's not in the reached set, merged-with element is", mergedWith);
      }
    }

    // Now do the usual coverage checks

    for (AbstractElement reachedElement : pReached) {
      ARTElement artReachedElement = (ARTElement)reachedElement;
      if (stop(artElement, artReachedElement, pPrecision)) {
        return true;
      }
    }
    return false;

  }

  private boolean stop(ARTElement pElement, ARTElement pReachedElement, Precision pPrecision)
                                                      throws CPAException {

    if (!pReachedElement.mayCover()) {
      return false;
    }

    AbstractElement wrappedElement = pElement.getWrappedElement();
    AbstractElement wrappedReachedElement = pReachedElement.getWrappedElement();

    boolean stop = wrappedStop.stop(wrappedElement, Collections.singleton(wrappedReachedElement), pPrecision);

    if (stop) {
      pElement.setCovered(pReachedElement);
    }
    return stop;
  }
}
