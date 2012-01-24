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
package org.sosy_lab.cpachecker.core.defaults;

import java.util.Collections;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperElement;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 * Base class for AbstractElements which wrap the abstract element of exactly
 * one CPA.
 */
public abstract class AbstractSingleWrapperElement implements AbstractWrapperElement, Targetable, Partitionable {

  private static Function<AbstractElement, AbstractElement> unwrapFunction
      = new Function<AbstractElement, AbstractElement>() {

    @Override
    public AbstractElement apply(AbstractElement pArg0) {
      Preconditions.checkArgument(pArg0 instanceof AbstractSingleWrapperElement);

      return ((AbstractSingleWrapperElement)pArg0).getWrappedElement();
    }
  };

  public static Function<AbstractElement, AbstractElement> getUnwrapFunction() {
    return unwrapFunction;
  }

  private final AbstractElement wrappedElement;

  public AbstractSingleWrapperElement(AbstractElement pWrappedElement) {
    // TODO this collides with some CPAs' way of handling TOP and BOTTOM, but it should really be not null here
    // Preconditions.checkNotNull(pWrappedElement);
    wrappedElement = pWrappedElement;
  }

  public AbstractElement getWrappedElement() {
    return wrappedElement;
  }

  @Override
  public boolean isTarget() {
    if (wrappedElement instanceof Targetable) {
      return ((Targetable)wrappedElement).isTarget();
    } else {
      return false;
    }
  }

  @Override
  public Object getPartitionKey() {
    if (wrappedElement instanceof Partitionable) {
      return ((Partitionable)wrappedElement).getPartitionKey();
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    return wrappedElement.toString();
  }

  @Override
  public Iterable<? extends AbstractElement> getWrappedElements() {
    return Collections.singleton(wrappedElement);
  }
}