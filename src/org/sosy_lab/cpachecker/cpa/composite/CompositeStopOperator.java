/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.composite;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.ImmutableList;

public class CompositeStopOperator implements StopOperator{

  protected final ImmutableList<StopOperator> stopOperators;

  public CompositeStopOperator(ImmutableList<StopOperator> stopOperators)
  {
    this.stopOperators = stopOperators;
  }

  @Override
  public boolean stop(AbstractElement element, Collection<AbstractElement> reached, Precision precision) throws CPAException {
    CompositeElement compositeElement = (CompositeElement) element;
    CompositePrecision compositePrecision = (CompositePrecision) precision;

    for (AbstractElement e : reached) {
      if (stop(compositeElement, (CompositeElement)e, compositePrecision)) {
        return true;
      }
    }
    return false;
  }

  private boolean stop(CompositeElement compositeElement, CompositeElement compositeReachedElement, CompositePrecision compositePrecision) throws CPAException {
    List<AbstractElement> compositeElements = compositeElement.getElements();
    List<AbstractElement> compositeReachedElements = compositeReachedElement.getElements();

    List<Precision> compositePrecisions = compositePrecision.getPrecisions();

    for (int idx = 0; idx < compositeElements.size(); idx++) {
      StopOperator stopOp = stopOperators.get(idx);

      AbstractElement absElem1 = compositeElements.get(idx);
      AbstractElement absElem2 = compositeReachedElements.get(idx);
      Precision prec = compositePrecisions.get(idx);

      if (!stopOp.stop(absElem1, Collections.singleton(absElem2), prec)){
        return false;
      }
    }
    return true;
  }
}
