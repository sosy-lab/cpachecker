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
package org.sosy_lab.cpachecker.cpa.assumptions.collector;

import java.util.Collection;

import com.google.common.collect.Collections2;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

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
    return true;
    
//    AssumptionCollectorElement assumptionElement = (AssumptionCollectorElement) element;
//
//    // if stop, then do not stop to make sure the state is
//    // added to the reached set
//    if (assumptionElement.isStop())
//      return false;
//
//    Collection<AbstractElement> wrappedReached = Collections2.transform(reached, AssumptionCollectorElement.getUnwrapFunction());
//
//    return wrappedStop.stop(assumptionElement.getWrappedElement(), wrappedReached, precision);
  }

  @Override
  public boolean stop(AbstractElement element, AbstractElement reachedElement)
      throws CPAException
  {
    return true;
//    AssumptionCollectorElement assumptionElement = (AssumptionCollectorElement) element;
//
//    // if stop, then do not stop to make sure the state is
//    // added to the reached set
//    if (assumptionElement.isStop())
//      return false;
//
//    AssumptionCollectorElement reachedAssumptionElement = (AssumptionCollectorElement) reachedElement;
//    return wrappedStop.stop(assumptionElement.getWrappedElement(), reachedAssumptionElement.getWrappedElement());

  }

}
