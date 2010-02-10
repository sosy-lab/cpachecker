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

import java.util.Collection;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import common.Pair;

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
  
  private static final Function<Pair<AbstractElement, Precision>, Pair<AbstractElement, Precision>> UNWRAP_WITH_PRECISION_FUNCTION =
    new Function<Pair<AbstractElement, Precision>, Pair<AbstractElement, Precision>>() {
      @Override
      public Pair<AbstractElement, Precision> apply(Pair<AbstractElement, Precision> pair) {
        return new Pair<AbstractElement, Precision>(((AssumptionCollectorElement)pair.getFirst()).getWrappedElement(), pair.getSecond());
      }
    };

  @Override
  public Pair<AbstractElement, Precision> prec(AbstractElement element,
      Precision precision, Collection<Pair<AbstractElement, Precision>> reachedElements)
  {
    AssumptionCollectorElement assumptionElement = (AssumptionCollectorElement)element;
    AbstractElement unwrappedElement = assumptionElement.getWrappedElement();
    Collection<Pair<AbstractElement, Precision>> unwrappedReached = Collections2.transform(reachedElements, UNWRAP_WITH_PRECISION_FUNCTION);
    Pair<AbstractElement, Precision> unwrappedResult = wrappedPrecisionAdjustment.prec(unwrappedElement, precision, unwrappedReached);
    
    AbstractElement resultElement;
    if (unwrappedElement != unwrappedResult.getFirst())
      resultElement = new AssumptionCollectorElement(unwrappedElement, assumptionElement.getCollectedAssumptions());
    else
      resultElement = element;
    return new Pair<AbstractElement, Precision>(resultElement, unwrappedResult.getSecond());
  }

}
