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
package cpa.assumptions.collector;

import java.util.ArrayList;
import java.util.Collection;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.StopOperator;
import exceptions.CPAException;

/**
 * Stop operator for the assumption collector CPA. Stops if the stop flag is
 * true; otherwise rely on the stop operator of the wrapped CPA.
 * 
 * @author g.theoduloz
 */
public class AssumptionCollectorStop implements StopOperator {

  private final StopOperator wrappedStop;
  
  public AssumptionCollectorStop(ConfigurableProgramAnalysis wrappedCPA)
  {
    wrappedStop = wrappedCPA.getStopOperator();
  }
  
  @Override
  public boolean stop(AbstractElement element,
      Collection<AbstractElement> reached, Precision precision)
      throws CPAException
  {
    AssumptionCollectorElement assumptionElement = (AssumptionCollectorElement) element;
    
    // if stop, then do not stop to make sure the state is
    // added to the reached set
    if (assumptionElement.isStop())
      return false;
    
    ArrayList<AbstractElement> wrappedReached = new ArrayList<AbstractElement>(reached.size());
    for (AbstractElement reachedElement : reached) {
      wrappedReached.add(((AssumptionCollectorElement)reachedElement).getWrappedElement());
    }
    
    return wrappedStop.stop(assumptionElement.getWrappedElement(), wrappedReached, precision);
  }

  @Override
  public boolean stop(AbstractElement element, AbstractElement reachedElement)
      throws CPAException
  {
    AssumptionCollectorElement assumptionElement = (AssumptionCollectorElement) element;

    // if stop, then do not stop to make sure the state is
    // added to the reached set
    if (assumptionElement.isStop())
      return false;
    
    AssumptionCollectorElement reachedAssumptionElement = (AssumptionCollectorElement) reachedElement;
    return wrappedStop.stop(assumptionElement.getWrappedElement(), reachedAssumptionElement.getWrappedElement());
    
  }

}
