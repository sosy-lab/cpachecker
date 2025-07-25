// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.PseudoPartitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

/**
 * Base class for AbstractStates which wrap the abstract state of exactly one CPA. Note that there
 * also exists {@link AbstractSingleWrapperState} for cases where states do not need to be
 * serializable.
 *
 * <p>When updating one of these classes, please keep the other in sync (they cannot inherit from
 * each other because how badly Java serialization is designed).
 */
public abstract class AbstractSerializableSingleWrapperState
    implements AbstractWrapperState, Targetable, Partitionable, PseudoPartitionable, Serializable {

  @Serial private static final long serialVersionUID = 627183978717358384L;

  private final @Nullable AbstractState wrappedState;

  protected AbstractSerializableSingleWrapperState(@Nullable AbstractState pWrappedState) {
    // TODO this collides with some CPAs' way of handling dummy states, but it should really be not
    // null here
    // Preconditions.checkNotNull(pWrappedState);
    wrappedState = pWrappedState;
  }

  public @Nullable AbstractState getWrappedState() {
    return wrappedState;
  }

  @Override
  public boolean isTarget() {
    return wrappedState instanceof Targetable targetable && targetable.isTarget();
  }

  @Override
  public Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    checkState(isTarget());
    return ((Targetable) wrappedState).getTargetInformation();
  }

  @Override
  public @Nullable Object getPartitionKey() {
    if (wrappedState instanceof Partitionable partitionable) {
      return partitionable.getPartitionKey();
    } else {
      return null;
    }
  }

  @Override
  public @Nullable Comparable<?> getPseudoPartitionKey() {
    if (wrappedState instanceof PseudoPartitionable pseudoPartitionable) {
      return pseudoPartitionable.getPseudoPartitionKey();
    } else {
      return null;
    }
  }

  @Override
  public @Nullable Object getPseudoHashCode() {
    if (wrappedState instanceof PseudoPartitionable pseudoPartitionable) {
      return pseudoPartitionable.getPseudoHashCode();
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
