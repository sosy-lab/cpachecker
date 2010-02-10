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

import java.util.Collections;

import assumptions.AssumptionWithLocation;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.AbstractWrapperElement;

/**
 * Abstract element for the Collector CPA. Encapsulate a
 * symbolic formula
 * 
 * @author g.theoduloz
 */
public class AssumptionCollectorElement implements AbstractElement, AbstractWrapperElement {

  private final AssumptionWithLocation assumption;
  private final boolean stop;
  private final AbstractElement element;
 
  public AssumptionCollectorElement(AbstractElement wrappedElement, AssumptionWithLocation f, boolean forceStop)
  {
    element = wrappedElement;
    assumption = f;
    stop = forceStop;
  }
  
  public AssumptionCollectorElement(AbstractElement wrappedElement, AssumptionWithLocation f)
  {
    this(wrappedElement, f, false);
  }
  
  /**
   * Return the invariant in this state. May return
   * a null value in case no invariant is stored.
   */
  public AssumptionWithLocation getCollectedAssumptions()
  {
    if (assumption != null)
      return assumption;
    else
      return AssumptionWithLocation.TRUE;
  }
  
  @Override
  public boolean isError() {
    return false;
  }
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (stop)
      builder.append("<STOP> ");
    builder.append("assume: ");
    if (assumption == null)
      builder.append("(null)");
    else
      builder.append(assumption.toString());
    builder.append('\n');
    builder.append(element.toString());
    return builder.toString();
  }

  @Override
  public Iterable<? extends AbstractElement> getWrappedElements() {
    return Collections.singleton(element);
  }

  @Override
  public <T extends AbstractElement> T retrieveWrappedElement(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    } else if (pType.isAssignableFrom(element.getClass())) {
      return pType.cast(element);
    } else if (element instanceof AbstractWrapperElement) {
      return ((AbstractWrapperElement)element).retrieveWrappedElement(pType);
    } else {
      return null;
    }
  }
  
  public AbstractElement getWrappedElement()
  {
    return element;
  }
  
  @Override
  public AbstractElementWithLocation retrieveLocationElement() {
    if (element instanceof AbstractWrapperElement)
      return ((AbstractWrapperElement) element).retrieveLocationElement();
    else
      return retrieveWrappedElement(AbstractElementWithLocation.class);
  }
  
  public boolean isStop() {
    return stop;
  }
  
  @Override
  public boolean equals(Object other) {
    if (other instanceof AssumptionCollectorElement)
    {
      AssumptionCollectorElement otherElement = (AssumptionCollectorElement) other;
      return (otherElement.stop == stop) 
          && assumption.equals(otherElement.assumption);
    } else {
      return false;
    }
  }
  
}
