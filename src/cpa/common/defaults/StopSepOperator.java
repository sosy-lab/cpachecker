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
package cpa.common.defaults;

import java.util.Collection;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.PartialOrder;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

/**
 * Standard stop-sep operator
 * @author g.theoduloz
 */
public class StopSepOperator implements StopOperator {

  private final PartialOrder partialOrder;
  
  /**
   * Creates a stop-sep operator based on the given
   * partial order
   */
  public StopSepOperator(PartialOrder order) {
    partialOrder = order;
  }
  
  @Override
  public boolean stop(AbstractElement el, Collection<AbstractElement> reached, Precision precision)
    throws CPAException
  {
    for (AbstractElement reachedElement : reached) {
      if (partialOrder.satisfiesPartialOrder(el, reachedElement))
        return true;
    }
    return false;
  }

  @Override
  public boolean stop(AbstractElement el, AbstractElement reachedElement)
      throws CPAException {
    return partialOrder.satisfiesPartialOrder(el, reachedElement);
  }

}
