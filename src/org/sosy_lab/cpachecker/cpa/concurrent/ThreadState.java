// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public final class ThreadState extends AbstractSingleWrapperState implements AbstractStateWithLocation {

  // direct reference to location state as it is needed frequently
  private final AbstractStateWithLocation locationState;

  public ThreadState(CompositeState pWrappedState) {
    super(pWrappedState);
    this.locationState =
        AbstractStates.extractStateByType(pWrappedState, AbstractStateWithLocation.class);
    if (this.locationState == null) {
      throw new IllegalStateException("No location state found in thread state.");
    }
  }

  @Override
  public String toString() {
    return "(loc=%s)".formatted(getLocationNode().getNodeNumber());
  }

  @Override
  public CFANode getLocationNode() {
    return locationState.getLocationNode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    ThreadState other = (ThreadState) obj;
    return getWrappedState().equals(other.getWrappedState());
  }

  @Override
  public int hashCode() {
    return getWrappedState().hashCode();
  }
}
