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

import java.util.List;

import com.google.common.collect.ImmutableList;

import cpa.common.CallStack;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.AbstractWrapperElement;

public class CompositeElement implements AbstractWrapperElement {
  private final ImmutableList<AbstractElement> elements;
  private CallStack callStack;

  public CompositeElement(List<AbstractElement> elements, CallStack stack)
  {
    this.elements = ImmutableList.copyOf(elements);
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

  @Override
  public <T extends AbstractElement> T retrieveWrappedElement(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    }
    for (AbstractElement element : elements) {
      if (pType.isAssignableFrom(element.getClass())) {
        return pType.cast(element);
      } else if (element instanceof AbstractWrapperElement) {
        T result = ((AbstractWrapperElement)element).retrieveWrappedElement(pType);
        if (result != null) {
          return result;
        }
      }  
    }
    return null;
  }
  
  @Override
  public AbstractElementWithLocation retrieveLocationElement() {
    if (elements.get(0) instanceof AbstractElementWithLocation) {
      return (AbstractElementWithLocation)elements.get(0);
    } else {
      assert false;
      return retrieveWrappedElement(AbstractElementWithLocation.class);
    }
  }
  
  @Override
  public List<AbstractElement> getWrappedElements() {
    return elements;
  }
}
