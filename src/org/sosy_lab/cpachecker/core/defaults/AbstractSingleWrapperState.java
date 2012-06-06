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

import java.io.Serializable;
import java.util.Collections;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 * Base class for AbstractStates which wrap the abstract element of exactly
 * one CPA.
 */
public abstract class AbstractSingleWrapperState implements AbstractWrapperState, Targetable, Partitionable, Serializable {

  private static final long serialVersionUID = -332757795984736107L;
  private static Function<AbstractState, AbstractState> unwrapFunction
      = new Function<AbstractState, AbstractState>() {

    @Override
    public AbstractState apply(AbstractState pArg0) {
      Preconditions.checkArgument(pArg0 instanceof AbstractSingleWrapperState);

      return ((AbstractSingleWrapperState)pArg0).getWrappedState();
    }
  };

  public static Function<AbstractState, AbstractState> getUnwrapFunction() {
    return unwrapFunction;
  }

  private final AbstractState wrappedElement;

  public AbstractSingleWrapperState(AbstractState pWrappedElement) {
    // TODO this collides with some CPAs' way of handling TOP and BOTTOM, but it should really be not null here
    // Preconditions.checkNotNull(pWrappedElement);
    wrappedElement = pWrappedElement;
  }

  public AbstractState getWrappedState() {
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
  public Iterable<? extends AbstractState> getWrappedStates() {
    return Collections.singleton(wrappedElement);
  }
}