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
package org.sosy_lab.cpachecker.cpa.composite;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElementWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CompositeStopOperator implements StopOperator{

  private final CompositeDomain compositeDomain;
  private final ImmutableList<StopOperator> stopOperators;

  public CompositeStopOperator(CompositeDomain compositeDomain, ImmutableList<StopOperator> stopOperators)
  {
    this.compositeDomain = compositeDomain;
    this.stopOperators = stopOperators;
  }

  @Override
  public boolean stop(AbstractElement element, Collection<AbstractElement> reached, Precision precision) throws CPAException
  {
    if(containsBottomElement(element)){
      return true;
    }
    
    for (AbstractElement e : reached) {
      if (stop(element, e)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsBottomElement(AbstractElement element) {
    if (element == compositeDomain.getBottomElement()) {
      return true;
    }

    CompositeElement compositeElement = (CompositeElement) element;
    List<AbstractElement> components = compositeElement.getElements ();
    
    for (int idx = 0; idx < components.size(); idx++) {
      AbstractElement abstElem = components.get(idx);
      AbstractDomain abstDomain = compositeDomain.getDomains().get(idx);
      if(abstElem == abstDomain.getBottomElement()){
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean stop(AbstractElement element, AbstractElement reachedElement)
  throws CPAException {
    CompositeElement compositeElement1 = (CompositeElement) element;
    CompositeElement compositeElement2 = (CompositeElement) reachedElement;
    if(!compositeElement1.getCallStack().equals(compositeElement2.getCallStack())){
      return false;
    }

    List<AbstractElement> compositeElements1 = compositeElement1.getElements ();
    List<AbstractElement> compositeElements2 = compositeElement2.getElements ();

    AbstractElementWithLocation locElem1 = (AbstractElementWithLocation)compositeElements1.get(0);
    AbstractElementWithLocation locElem2 = (AbstractElementWithLocation)compositeElements2.get(0);

    assert(locElem1.getLocationNode().equals(locElem2.getLocationNode()));

    for (int idx = 0; idx < compositeElements1.size (); idx++) {
      StopOperator stopOp = stopOperators.get(idx);
      AbstractElement absElem1 = compositeElements1.get(idx);
      AbstractElement absElem2 = compositeElements2.get(idx);
      if (!stopOp.stop(absElem1, absElem2)){
        return false;
      }
    }
    return true;
  }
}
