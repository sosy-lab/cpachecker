// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.PseudoPartitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

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

  protected AbstractSingleWrapperState(@Nullable AbstractState pWrappedState) {
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