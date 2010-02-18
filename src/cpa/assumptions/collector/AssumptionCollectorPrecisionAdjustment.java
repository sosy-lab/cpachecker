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

import assumptions.AssumptionWithLocation;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import common.Pair;

import cpa.common.UnmodifiableReachedElements;
import cpa.common.UnmodifiableReachedElementsView;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;

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
  
  private static final Function<AbstractElement, AbstractElement> UNWRAP_ELEMENT_FUNCTION =
    new Function<AbstractElement, AbstractElement>() {
      @Override
      public AbstractElement apply(AbstractElement from) {
        return ((AssumptionCollectorElement)from).getWrappedElement();
      }
    };
    
  private static final Function<Precision, Precision> UNWRAP_PRECISION_FUNCTION = Functions.<Precision>identity();

  @Override
  public Pair<AbstractElement, Precision> prec(AbstractElement element,
      Precision precision, UnmodifiableReachedElements reachedElements)
  {
    AssumptionCollectorElement assumptionElement = (AssumptionCollectorElement)element;
    AbstractElement unwrappedElement = assumptionElement.getWrappedElement();
    UnmodifiableReachedElements unwrappedReached = new UnmodifiableReachedElementsView(reachedElements, UNWRAP_ELEMENT_FUNCTION, UNWRAP_PRECISION_FUNCTION);
    Pair<AbstractElement, Precision> unwrappedResult = wrappedPrecisionAdjustment.prec(unwrappedElement, precision, unwrappedReached);
    
    AbstractElement resultElement;
    if (unwrappedElement != unwrappedResult.getFirst()) {
      AssumptionWithLocation assumption = assumptionElement.getCollectedAssumptions();
      boolean stop = assumptionElement.isStop();
      resultElement = new AssumptionCollectorElement(unwrappedElement, assumption, stop);
    } else {
      resultElement = element;
    }
    return new Pair<AbstractElement, Precision>(resultElement, unwrappedResult.getSecond());
  }

}
