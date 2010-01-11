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
package compositeCPA;

import java.util.Iterator;
import java.util.List;

import cfa.objectmodel.CFANode;
import cpa.common.CallStack;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.AbstractWrapperElement;

public class CompositeElement implements AbstractElementWithLocation, AbstractWrapperElement {
  private final List<AbstractElement> elements;
  private CallStack callStack;

  public CompositeElement(List<AbstractElement> elements, CallStack stack)
  {
    this.elements = elements;
    this.callStack = stack;
  }

  public List<AbstractElement> getElements()
  {
    return elements;
  }

  public int getNumberofElements(){
    return elements.size();
  }

  @Override
  public boolean isError() {
    for (AbstractElement element : elements) {
      if (element.isError()) {
        return true;
      }
    }
    return false;
  }
  
  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }

    if (other == null || !(other instanceof CompositeElement)) {
      return false;
    }

    CompositeElement otherComposite = (CompositeElement) other;
    
    return (otherComposite.elements.equals(this.elements))
        && otherComposite.getCallStack().equals(this.getCallStack());
  }

  @Override
  public int hashCode() {
    return elements.hashCode();
  }

  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    builder.append('(');
    for (AbstractElement element : elements) {
      builder.append(element.getClass().getSimpleName());
      builder.append(": ");
      builder.append(element.toString());
      builder.append("\n ");
    }
    builder.replace(builder.length() - 1, builder.length(), ")");

    return builder.toString();
  }

  public AbstractElement get(int idx) {
    return elements.get(idx);
  }

  public CallStack getCallStack() {
    return callStack;
  }

  public void setCallStack(CallStack callStack) {
    this.callStack = callStack;
  }

  public CFANode getLocationNode() {
    return getElementWithLocation().getLocationNode();
  }

  public AbstractElementWithLocation getElementWithLocation() {
    assert (elements.get(0) instanceof AbstractElementWithLocation);
    return (AbstractElementWithLocation)elements.get(0);
  }

  @Override
  public AbstractElement retrieveElementOfType(String pElementClass){
    for(AbstractElement item:elements) {
      if(item.getClass().getSimpleName().equals(pElementClass)){
        return item;
      }
      else if(item instanceof AbstractWrapperElement){
        AbstractElement wrappedElement = 
          ((AbstractWrapperElement)item).retrieveElementOfType(pElementClass);
        if(wrappedElement != null){
          if(wrappedElement.getClass().getSimpleName().equals(pElementClass)){
            return wrappedElement;
          }
        }
      }
    }
    return null;
  }

  @Override
  public Iterable<AbstractElement> getWrappedElements() {
    return elements;
  }
}
