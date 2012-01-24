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

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

import com.google.common.collect.ImmutableList;

public class CompositeElement implements AbstractWrapperElement, Targetable, Partitionable {
  private final ImmutableList<AbstractElement> elements;
  private Object partitionKey; // lazily initialized

  public CompositeElement(List<AbstractElement> elements)
  {
    this.elements = ImmutableList.copyOf(elements);
  }

  public List<AbstractElement> getElements()
  {
    return elements;
  }

  public int getNumberofElements(){
    return elements.size();
  }

  @Override
  public boolean isTarget() {
    for (AbstractElement element : elements) {
      if ((element instanceof Targetable) && ((Targetable)element).isTarget()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Object getPartitionKey() {
    if (partitionKey == null) {
      List<Object> keys = new ArrayList<Object>(elements.size());

      for (int i = 0; i < elements.size(); i++) {
        AbstractElement element = elements.get(i);
        if (element instanceof Partitionable) {
          keys.add(((Partitionable)element).getPartitionKey());
        } else {
          keys.add(null);
        }
      }

      partitionKey = keys;
    }

    return partitionKey;
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

    return otherComposite.elements.equals(this.elements);
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

  @Override
  public List<AbstractElement> getWrappedElements() {
    return elements;
  }
}
