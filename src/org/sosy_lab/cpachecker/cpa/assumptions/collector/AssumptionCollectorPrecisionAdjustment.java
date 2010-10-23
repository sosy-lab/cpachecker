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

import org.sosy_lab.cpachecker.util.assumptions.AssumptionWithLocation;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import org.sosy_lab.common.Triple;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetView;

/**
 * Precision adjustment operator, relying directly on the underlying
 * precision adjustment.
 * @author g.theoduloz
 */
public class AssumptionCollectorPrecisionAdjustment
  implements PrecisionAdjustment
{

  private final PrecisionAdjustment wrappedPrecisionAdjustment;

  public AssumptionCollectorPrecisionAdjustment(ConfigurableProgramAnalysis wrappedCPA)
  {
    wrappedPrecisionAdjustment = wrappedCPA.getPrecisionAdjustment();
  }

  private static final Function<Precision, Precision> UNWRAP_PRECISION_FUNCTION = Functions.<Precision>identity();

  @Override
  public Triple<AbstractElement, Precision, Action> prec(AbstractElement element,
      Precision oldPrecision, UnmodifiableReachedSet reachedElements)
  {
    AssumptionCollectorElement assumptionElement = (AssumptionCollectorElement)element;
    AbstractElement oldElement = assumptionElement.getWrappedElement();
    UnmodifiableReachedSet unwrappedReached = new UnmodifiableReachedSetView(reachedElements, AssumptionCollectorElement.getUnwrapFunction(), UNWRAP_PRECISION_FUNCTION);
    Triple<AbstractElement, Precision, Action> unwrappedResult = wrappedPrecisionAdjustment.prec(oldElement, oldPrecision, unwrappedReached);

    if (unwrappedResult == null) {
      // element is not reachable
      return null;
    }
    
    AbstractElement newElement = unwrappedResult.getFirst();
    Precision newPrecision = unwrappedResult.getSecond();
    Action action = unwrappedResult.getThird();
    
    if ((oldElement == newElement) && (oldPrecision == newPrecision)) {
      // nothing has changed
      return new Triple<AbstractElement, Precision, Action>(element, oldPrecision, action);
    }
    
    AssumptionWithLocation assumption = assumptionElement.getCollectedAssumptions();
    boolean stop = assumptionElement.isStop();
    AbstractElement resultElement = new AssumptionCollectorElement(assumptionElement.getCpa(), newElement, assumption, stop);
    
    return new Triple<AbstractElement, Precision, Action>(resultElement, newPrecision, action);
  }

}
