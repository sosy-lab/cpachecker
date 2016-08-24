/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.PseudoPartitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

import java.io.Serializable;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Base class for AbstractStates which wrap the abstract state of exactly
 * one CPA.
 */
public abstract class AbstractSingleWrapperState
    implements AbstractWrapperState, Targetable, Partitionable, PseudoPartitionable, Serializable {

  private static final long serialVersionUID = -332757795984736107L;

  public static Function<AbstractState, AbstractState> getUnwrapFunction() {
    return pArg0 -> ((AbstractSingleWrapperState)pArg0).getWrappedState();
  }

  private final @Nullable AbstractState wrappedState;

  public AbstractSingleWrapperState(@Nullable AbstractState pWrappedState) {
    // TODO this collides with some CPAs' way of handling dummy states, but it should really be not null here
    // Preconditions.checkNotNull(pWrappedState);
    wrappedState = pWrappedState;
  }

  public @Nullable AbstractState getWrappedState() {
    return wrappedState;
  }

  @Override
  public boolean isTarget() {
    if (wrappedState instanceof Targetable) {
      return ((Targetable)wrappedState).isTarget();
    } else {
      return false;
    }
  }

  @Override
  public Set<Property> getViolatedProperties() throws IllegalStateException {
    checkState(isTarget());
    return ((Targetable)wrappedState).getViolatedProperties();
  }

  @Override
  public Object getPartitionKey() {
    if (wrappedState instanceof Partitionable) {
      return ((Partitionable)wrappedState).getPartitionKey();
    } else {
      return null;
    }
  }

  @Override
  public Comparable<?> getPseudoPartitionKey() {
    if (wrappedState instanceof PseudoPartitionable) {
      return ((PseudoPartitionable) wrappedState).getPseudoPartitionKey();
    } else {
      return null;
    }
  }

  @Override
  public Object getPseudoHashCode() {
    if (wrappedState instanceof PseudoPartitionable) {
      return ((PseudoPartitionable) wrappedState).getPseudoHashCode();
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    return wrappedState.toString();
  }

  @Override
  public ImmutableList<AbstractState> getWrappedStates() {
    return ImmutableList.of(wrappedState);
  }
}