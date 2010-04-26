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
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class AssumptionCollectorMerge implements MergeOperator {

  private final ConfigurableProgramAnalysis wrappedCPA;

  public AssumptionCollectorMerge(ConfigurableProgramAnalysis pWrappedCPA) {
    wrappedCPA = pWrappedCPA;
  }

  @Override
  public AbstractElement merge(AbstractElement element1,
      AbstractElement element2, Precision precision) throws CPAException {

    AssumptionCollectorElement collectorElement1= (AssumptionCollectorElement)element1;
    AssumptionCollectorElement collectorElement2 = (AssumptionCollectorElement)element2;

    MergeOperator mergeOperator = wrappedCPA.getMergeOperator();
    AbstractElement wrappedElement1 = collectorElement1.getWrappedElements().iterator().next();
    AbstractElement wrappedElement2 = collectorElement2.getWrappedElements().iterator().next();
    AbstractElement retElement = mergeOperator.merge(wrappedElement1, wrappedElement2, precision);
    if(retElement.equals(wrappedElement2)){
      return element2;
    }

    AssumptionWithLocation assumption1 = collectorElement1.getCollectedAssumptions();
    boolean shouldStop1 = collectorElement1.isStop();

    AssumptionWithLocation assumption2 = collectorElement2.getCollectedAssumptions();
    boolean shouldStop2 = collectorElement2.isStop();

    AssumptionWithLocation mergedAssumption = assumption1.and(assumption2);
    boolean mergedShouldStop = shouldStop1 && shouldStop2;

    AssumptionCollectorElement mergedElement = new AssumptionCollectorElement(retElement, mergedAssumption, mergedShouldStop);

    return mergedElement;
  }

}
