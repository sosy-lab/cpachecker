/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.predicateabstraction;

import java.util.Collection;

import logging.CPACheckerLogger;
import logging.CustomLogLevel;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

public class PredicateAbstractionStopSep implements StopOperator {
  private final PredicateAbstractionDomain predicateAbstractionDomain;

  public PredicateAbstractionStopSep(PredicateAbstractionDomain predAbsDomain) {
    this.predicateAbstractionDomain = predAbsDomain;
  }

  public AbstractDomain getAbstractDomain() {
    return predicateAbstractionDomain;
  }

  public <AE extends AbstractElement> boolean stop(AE element,
                                                   Collection<AE> reached,
                                                   Precision prec)
                                                                  throws CPAException {
    PartialOrder partialOrder = predicateAbstractionDomain.getPartialOrder();
    for (AbstractElement testElement : reached) {
      CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
          " Partial order check: element:  " + element + " reached " + reached
              + " --> "
              + partialOrder.satisfiesPartialOrder(element, testElement));
      if (partialOrder.satisfiesPartialOrder(element, testElement))
                                                                   return true;
    }

    return false;
  }

  public boolean stop(AbstractElement element, AbstractElement reachedElement)
                                                                              throws CPAException {
    PartialOrder partialOrder = predicateAbstractionDomain.getPartialOrder();
    CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
        " Partial order check: element:  " + element + " reached element"
            + reachedElement + " --> "
            + partialOrder.satisfiesPartialOrder(element, reachedElement));
    if (partialOrder.satisfiesPartialOrder(element, reachedElement))
                                                                    return true;
    return false;
  }
}
